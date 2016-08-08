package com.alibaba.middleware.innerlog;

import com.alibaba.middleware.innerlog.util.MethodUtils;
import com.alibaba.middleware.innerlog.util.StringUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <pre>
 * 内置log的lib加载classLoader,使用时根据appKey来划分
 * </pre>
 *
 * User: <a href="mailto:qihao@taobao.com">qihao</a> Date: 14-7-2 Time: 12:52 version 1.0
 */
public class LoggerClassLoader extends ClassLoader {

	/**
	 * 内置LogLib的文件名称,该文件其实是个jar
	 */
	private final static String LOGBACK_LIB = "logback-assemble-1.1.2.jlb";
	private final static long LOGBACK_LIB_CHECK_LENGTH = 724411;
	private final static String LOCK_FILE = "logback-assemble.lock";

	/**
	 * 极端情况下某些系统的jar 加载方式特殊,如spring-boot,那么可以通过JVM指定
	 * -DinnerLoggerJar=xxxx/xxxx/inner-logger-1.5.jar 方式指定加载路径
	 */
	private final static String SYSTEM_INNER_JAR = System.getProperty("innerLoggerJar");

	/**
	 * 将JAR内的innerLib导出到文件系统需要的常量
	 */
	private final static String SYSTEM_TMP_DIR = System.getProperty("user.home");
	private final static String SYSTEM_FILE_SEP = System.getProperty("file.separator");
	private final static String OUT_LIB_DIR_NAME = ".inner-logger";
	private final static String OUT_LIB_DIR_PATH = SYSTEM_TMP_DIR + SYSTEM_FILE_SEP + OUT_LIB_DIR_NAME;
	private final static String OUT_LIB_PATH = OUT_LIB_DIR_PATH + SYSTEM_FILE_SEP + LOGBACK_LIB;
	private final static String LOCK_PATH = OUT_LIB_DIR_PATH + SYSTEM_FILE_SEP + LOCK_FILE;

	/**
	 * method缓存部分,用来加速反射调用
	 */
	private Method logMethod;

	/**
	 * SL4绑定和configure用到的变量
	 */
	private Object innerFactory;
	private Class<?> sl4jLogFactoryClass;
	/**
	 * classLoader的id,在缓存加速的时候作为key匹配Method的 cache
	 */
	private Integer id;

	// 标记该classLoader对应的log系统是否已经configure过
	private boolean configure = false;

	protected LoggerClassLoader(Integer id) {
		// 去掉父的classLoader,防止干扰业务的classLoader
		super(null);
		this.id = id;
	}

	protected Class<?> findClass(String name) throws ClassNotFoundException {
		URL logLibUrl = Thread.currentThread().getContextClassLoader().getResource(LOGBACK_LIB);
		/**
		 *如果系统参数指定了inner-logger的jar 路径直接使用系统参数里设置的路径,主要是为了解决按照常规加载方法不适用的情况。
		 */
		if (StringUtils.isNotBlank(SYSTEM_INNER_JAR)) {
			String libPath = "file:" + SYSTEM_INNER_JAR + "!/" + LOGBACK_LIB;
			try {
				logLibUrl = new URL("jar", "", libPath);
			} catch (MalformedURLException e) {
				throw new ClassNotFoundException(
						"load -DinnerLoggerJar error! path: " + SYSTEM_INNER_JAR + " className: " + name, e);
			}
		}
		String libProtocol = logLibUrl.getProtocol();
		if ("file".equals(libProtocol)) {
			// 在inner-logger工程内部运行,由于加载的是main
			File libFile = new File(logLibUrl.getFile());
			try {
				JarFile libJarFile = new JarFile(libFile);
				return this.getClassFromJarEntry(name, libJarFile);
			} catch (IOException e) {
				throw new ClassNotFoundException("inner Logger className: " + name, e);
			}
		} else if ("jar".equals(libProtocol)) {
			String logbackUrl = logLibUrl.toString();
			// 获取inner-loger的jar的外部File绝对路径
			String outerJarPath = StringUtils.substringBeforeLast(StringUtils.substringAfter(logbackUrl, "jar:file:"),
					"!");
			return this.getaClassFromOutLib(name, outerJarPath);
		} else if ("vfs".equals(libProtocol)) {
			// 这里可能是jboss的VFS加载,File的路径就是绝对路径,直接获取到INNER的JAR并且导出内部logback
			String innerLoggerPath = StringUtils.substringBeforeLast(logLibUrl.getFile(), SYSTEM_FILE_SEP);
			return this.getaClassFromOutLib(name, innerLoggerPath);
		} else {
			// 考虑到lib加载方式要么是本地class文件,要么是本地jar方式,其他如ftp, http, nntp等网络的方式不支持
			throw new ClassNotFoundException("Not Supported Lib Protocol: " + libProtocol + " ClassName: " + name);
		}
	}

	private Class<?> getaClassFromOutLib(String name, String jarPath) throws ClassNotFoundException {
		File outLogLibFile = this.exportInnerLib2Local(jarPath);
		JarFile libJarFile = null;
		try {
			libJarFile = new JarFile(outLogLibFile);
			return this.getClassFromJarEntry(name, libJarFile);
		} catch (Exception e) {
			throw new ClassNotFoundException("Load Class From OutLib: " + outLogLibFile.getAbsolutePath()
					+ ", ClassName: " + name, e);
		} finally {
			if (null != libJarFile) {
				try {
					libJarFile.close();
				} catch (IOException e) {
					throw new ClassNotFoundException("close file occur error", e);
				}
			}
		}
	}

	private static void extractNestedJar(String innerLoggerPath) throws IOException {
		String absoluteJarPath = null;
		String currentPath = null;
		JarFile logbackJarFile = null;
		try {
			/**
			 * 传入的innerLoggerPath有以下几种情况： 1.
			 * /Users/juven/Code/git_alibaba/AliExpressMicrosevices/spring-boot-demo-application
			 * /target/spring-boot-demo-application.jar!/lib/inner-logger-1.5.3-SNAPSHOT.jar 2.
			 * /test-innerlogger.one-jar.jar!/lib/inner-logger-1.5.4-SNAPSHOT.jar
			 */
			currentPath = System.getProperty("user.dir");
			if (innerLoggerPath.contains(currentPath)) {
				// 对应1的情况，说明innerLoggerPath是一个完整的路径，不需要再拼绝对路径了
				absoluteJarPath = innerLoggerPath;
			} else {
				String outerJar = StringUtils.substringBefore(innerLoggerPath, ".jar");
				int startIndex = outerJar.lastIndexOf("/") + 1;
				absoluteJarPath = "jar:file:" + currentPath + SYSTEM_FILE_SEP + innerLoggerPath.substring(startIndex);
			}
			URL jarUrl = new URL(absoluteJarPath);
			JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
			JarFile innerLogJarFile = connection.getJarFile();
			JarEntry innerLogJarEntry = connection.getJarEntry();
			File tempJarFile = new File(OUT_LIB_DIR_PATH + SYSTEM_FILE_SEP + "temp-inner-logger.jar");
			FileOutputStream temp = new FileOutputStream(tempJarFile);
			copyInput2OutPut(innerLogJarFile.getInputStream(innerLogJarEntry), temp);

			logbackJarFile = new JarFile(tempJarFile);
			innerLogJarEntry = logbackJarFile.getJarEntry(LOGBACK_LIB);
			File outLogLibFile = new File(OUT_LIB_PATH);
			FileOutputStream outLibFileOut = new FileOutputStream(outLogLibFile);
			copyInput2OutPut(logbackJarFile.getInputStream(innerLogJarEntry), outLibFileOut);
		} catch (Exception e) {
			throw new RuntimeException("extractNestedJar occur error, innerLoggerPath : " + innerLoggerPath
					+ ", currentPath : " + currentPath + ", absoluteJarPath : " + absoluteJarPath, e);
		} finally {
			if (logbackJarFile != null) {
				try {
					logbackJarFile.close();
				} catch (Throwable e) {
					// skip exception
				}
			}
		}
	}

	private File exportInnerLib2Local(String jarPath) throws ClassNotFoundException {
		// JVM内Class同步,目的优化JVM内多线程同时调用时导致outLibFile判断不准确
		File outLogLibFile = new File(OUT_LIB_PATH);
		if (outLogLibFile.exists() && LOGBACK_LIB_CHECK_LENGTH == outLogLibFile.length()) {
			return outLogLibFile;
		}
		synchronized (LoggerClassLoader.class) {
			File lockFile = new File(LOCK_PATH);
			if (!lockFile.getParentFile().exists()) {
				lockFile.getParentFile().mkdir();
			}
			FileOutputStream lockFileOut = null;
			FileLock lock = null;
			JarFile innerLogJarFile = null;
			JarEntry innerLogJarEntry;
			FileOutputStream outLibFileOut = null;
			try {
				lockFileOut = new FileOutputStream(lockFile);
				lock = lockFileOut.getChannel().lock();
				outLibFileOut = new FileOutputStream(outLogLibFile);
				if (jarPath.split(".jar").length > 1) {
					// onejar类型的包，会嵌套多层的.jar，需要先解压innerLogger，再解压logback.jlb
					extractNestedJar(jarPath);
				} else {
					// 如果外部outlib文件不存在，或者文件大小不一致，则覆写一个。读取第一层jar，即inner-loger.jar
					innerLogJarFile = new JarFile(jarPath);
					// 读取第二层jar，即inner-logger内部的jlib的jar
					innerLogJarEntry = innerLogJarFile.getJarEntry(LOGBACK_LIB);
					copyInput2OutPut(innerLogJarFile.getInputStream(innerLogJarEntry), outLibFileOut);
				}
				if (LOGBACK_LIB_CHECK_LENGTH != outLogLibFile.length()) {
					outLogLibFile.delete();
					throw new ClassNotFoundException("out lib file error, will remove lib, correct length : "
							+ LOGBACK_LIB_CHECK_LENGTH + ", error length : "
							+ outLogLibFile.length() + ", outPath : "
							+ outLogLibFile.getAbsolutePath());
				}
				return outLogLibFile;
			} catch (IOException e) {
				throw new ClassNotFoundException("export innerlogger to local error, jarPath : " + jarPath, e);
			} finally {
				if (null != lock) {
					try {
						lock.release();
					} catch (IOException e) {
						throw new ClassNotFoundException("lock.release occur error", e);
					}
				}
				if (null != lockFileOut) {
					try {
						lockFileOut.close();
					} catch (IOException e) {
						throw new ClassNotFoundException("close file occur error", e);
					}
				}
				if (null != innerLogJarFile) {
					try {
						innerLogJarFile.close();
					} catch (IOException e) {
						throw new ClassNotFoundException("close file occur error", e);
					}
				}
				if (null != outLibFileOut) {
					try {
						outLibFileOut.close();
					} catch (IOException e) {
						throw new ClassNotFoundException("close file occur error", e);
					}
				}
			}
		}
	}

	private Class<?> getClassFromJarEntry(String name, JarFile jarFile) throws ClassNotFoundException {
		Class<?> clazz = null;
		DataInputStream dis = null;
		try {
			JarEntry innerClassEntry = jarFile.getJarEntry(StringUtils.replace(name, ".", "/") + ".class");
			if (null == innerClassEntry) {
				// 如果找不到任何的inner-logger.jar的话直接报错
				throw new ClassNotFoundException("Inner Logger ClassName: " + name);
			}
			dis = new DataInputStream(jarFile.getInputStream(innerClassEntry));
			byte[] classBytes = new byte[(int) innerClassEntry.getSize()];
			dis.readFully(classBytes);
			clazz = defineClass(name, classBytes, 0, classBytes.length);
		} catch (IOException e) {
			// 如果加载出错,可能是加载到损坏包尝试删除掉导出的外部LIB文件
			File outLogLibFile = new File(OUT_LIB_PATH);
			outLogLibFile.delete();
			throw new ClassNotFoundException("Inner Logger ClassName: " + name, e);
		} finally {
			if (null != jarFile) {
				try {
					jarFile.close();
				} catch (IOException e) {
				}
			}
			if (null != dis) {
				try {
					dis.close();
				} catch (IOException e) {
				}
			}
		}
		return clazz;
	}

	private static int copyInput2OutPut(InputStream input, OutputStream output) throws IOException {
		long count = 0;
		byte[] buffer = new byte[1024 * 4];
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	protected Object getInnerLogger(String loggerName) {
		if (null == logMethod) {
			// add method cache
			logMethod = MethodUtils.getMatchingAccessibleMethod(sl4jLogFactoryClass,
					"getLogger",
					new Class[] { String.class });
			// 关闭安全检查加速反射调用
			logMethod.setAccessible(true);
		}
		try {
			return logMethod.invoke(null, loggerName);
		} catch (Exception e) {
			throw new RuntimeException("invoke get inner logger Error! logName: " + loggerName, e);
		}
	}

	protected boolean isConfigure() {
		return configure;
	}

	protected void setConfigure(boolean configure) {
		this.configure = configure;
	}

	protected Object getInnerFactory() {
		return innerFactory;
	}

	protected Class<?> getSl4jLogFactoryClass() {
		return sl4jLogFactoryClass;
	}

	protected void setInnerFactory(Object innerFactory) {
		this.innerFactory = innerFactory;
	}

	protected void setSl4jLogFactoryClass(Class<?> sl4jLogFactoryClass) {
		this.sl4jLogFactoryClass = sl4jLogFactoryClass;
	}

	protected Integer getId() {
		return id;
	}

	public static void main(String[] args) throws IOException {
		printPath();
	}

	private static void printPath() {
		System.out.println("====");
		URL jarUrl2 = Thread.currentThread().getContextClassLoader().getResource(LOGBACK_LIB);
		System.out.println(jarUrl2);
		String relativelyPath = System.getProperty("user.dir");
		System.out.println(relativelyPath);
		URL resource = Thread.currentThread().getContextClassLoader().getResource("");
		System.out.println(resource);
		URL p2 = LoggerClassLoader.class.getResource("");
		System.out.println(p2);
		URL p3 = LoggerClassLoader.class.getResource("/");
		System.out.println(p3);
		System.out.println(ClassLoader.getSystemResource(""));
		System.out.println("====");
	}
}