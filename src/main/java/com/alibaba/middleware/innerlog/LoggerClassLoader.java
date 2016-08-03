package com.alibaba.middleware.innerlog;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.alibaba.middleware.innerlog.util.MethodUtils;
import com.alibaba.middleware.innerlog.util.StringUtils;

/**
 * 内置log的lib加载classLoader,使用时根据appKey来划分
 *
 * User: <a href="mailto:qihao@taobao.com">qihao</a> Date: 14-7-2 Time: 12:52
 * version 1.0
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
	private final static String SYSTEM_INNER_JAR=System.getProperty("innerLoggerJar");

	/**
	 * 将JAR内的innerLib导出到文件系统需要的常量
	 */
	private final static String SYSTEM_TMP_DIR = System.getProperty("user.home");
	private final static String SYSTEM_FILE_SEP = System.getProperty("file.separator");
	private final static String OUT_LIB_DIR_NAME = ".inner-logger";
	private final static String OUT_LIB_DIR_PATH = SYSTEM_TMP_DIR+ SYSTEM_FILE_SEP + OUT_LIB_DIR_NAME;
	private final static String OUT_LIB_PATH = OUT_LIB_DIR_PATH+ SYSTEM_FILE_SEP + LOGBACK_LIB;
	private final static String LOCK_PATH = OUT_LIB_DIR_PATH + SYSTEM_FILE_SEP+ LOCK_FILE;

	/**
	 * method缓存部分,用来加速反射调用
	 * */
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
		JarFile libJarFile;
		URL logLibUrl = Thread.currentThread().getContextClassLoader().getResource(LOGBACK_LIB);
		if(StringUtils.isNotBlank(SYSTEM_INNER_JAR)){
			String libPath= "file:"+SYSTEM_INNER_JAR+"!/"+LOGBACK_LIB;
			try{
				logLibUrl=new URL("jar","",libPath);
			}catch (MalformedURLException e){
				throw  new ClassNotFoundException("load -DinnerLoggerLib error! path: "+SYSTEM_INNER_JAR +" className: "+ name, e);
			}
		}
		String libProtocol = logLibUrl.getProtocol();
		if ("file".equals(libProtocol)) {
			// 在inner-logger工程内部运行,由于加载的是main/
			File libFile = new File(logLibUrl.getFile());
			try {
				libJarFile = new JarFile(libFile);
			} catch (IOException e) {
				throw new ClassNotFoundException("inner Logger className: "
						+ name, e);
			}
			return getClassFromJarEntry(name, libJarFile);
		} else if ("jar".equals(libProtocol)) {
			// 获取inner-loger的jar 的外部File 绝对路径
			String innerLoggerPath = StringUtils.substringBeforeLast(
					StringUtils.substringAfter(logLibUrl.toString(),"jar:file:"), "!");
			return getaClassFromOutLib(name, innerLoggerPath);
		} else if ("vfs".equals(libProtocol)) {
			//这里可能是jboss 的VFS加载,File的路径就是绝对路径,直接获取到INNER的JAR并且导出内部logback
			String innerLoggerPath=StringUtils.substringBeforeLast(logLibUrl.getFile(), SYSTEM_FILE_SEP);;
			return getaClassFromOutLib(name, innerLoggerPath);
		} else {
			// 考虑到lib加载方式要么是本地class文件,要么是本地jar方式,其他如ftp, http, nntp等网络的方式不支持
			throw new ClassNotFoundException("Not Supported Lib Protocol: "
					+ libProtocol + " ClassName: " + name);
		}
	}

	private Class<?> getaClassFromOutLib(String name, String libPath)
			throws ClassNotFoundException {
		JarFile libJarFile = null;
		File outLogLibFile = exportInnerLib2Local(libPath);
		long outFileLength = outLogLibFile.length();
		// 文件大小检查
		if (LOGBACK_LIB_CHECK_LENGTH != outFileLength) {
			if (null != outLogLibFile) {
				outLogLibFile.delete();
			}
			// 第一次检查文件损坏,尝试重新再导出一次
			outLogLibFile = exportInnerLib2Local(libPath);
			outFileLength = outLogLibFile.length();
			// 连续两次导出失败,抛异常!
			if (LOGBACK_LIB_CHECK_LENGTH != outFileLength) {
				throw new ClassNotFoundException(
						"Out Lib File Error remove Lib ! correct length: "
								+ LOGBACK_LIB_CHECK_LENGTH + " error length: "
								+ outFileLength + " outPath: "
								+ outLogLibFile.getAbsolutePath());
			}
		}
		try {
			libJarFile = new JarFile(outLogLibFile);
			return getClassFromJarEntry(name, libJarFile);
		} catch (Exception e) {
			// 加载失败删除失败的文件,可能是加载到损坏的jar
			if (null != outLogLibFile) {
				outLogLibFile.delete();
			}
			throw new ClassNotFoundException("Load Class From OutLib: "
					+ outLogLibFile.getAbsolutePath() + " ClassName: " + name,
					e);
		} finally {
			if (null != libJarFile) {
				try {
					libJarFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private File exportInnerLib2Local(String libPath)
			throws ClassNotFoundException {
		// 拼装outLib的目录路径
		File outLogDirFile = new File(OUT_LIB_DIR_PATH);
		File outLogLibFile = new File(OUT_LIB_PATH);
		File lockFile = new File(LOCK_PATH);
		// JVM内Class同步,目的优化JVM内多线程同时调用时导致outLibFile判断不准确
		synchronized (LoggerClassLoader.class) {
			// 尝试创建下父目录
			if (!outLogDirFile.exists()) {
				outLogDirFile.mkdir();
			}
			// 如果外部outlib文件不存在则写一个
			if (!outLogLibFile.exists()) {
				FileLock lock = null;
				JarFile innerLogJarFile = null;
				FileOutputStream outLibFileOut = null;
				FileOutputStream lockFileOut = null;
				try {
					// 读取第一层jar,即inner-loger.jar
					innerLogJarFile = new JarFile(libPath);
					// 读取第二层jar,即inner-logger内部的jlib的jar
					JarEntry entry = innerLogJarFile.getJarEntry(LOGBACK_LIB);
					/*
					 * 对将要导出写入文件系统的log的lib文件加锁,防止同一时刻其他进程也在入该文件导致
					 * 从而导致可能其他进程中的线程读取未写完整的lib文件
					 */
					lockFileOut = new FileOutputStream(lockFile);
					lock = lockFileOut.getChannel().lock();
					outLibFileOut = new FileOutputStream(outLogLibFile);
					copyInput2OutPut(innerLogJarFile.getInputStream(entry),
							outLibFileOut);
				} catch (IOException e) {
					throw new ClassNotFoundException(
							"Inner Logger Write outLib: "
									+ outLogLibFile.getAbsolutePath(), e);
				} finally {
					if (null != lock) {
						try {
							lock.release();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (null != lockFileOut) {
						try {
							lockFileOut.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (null != innerLogJarFile) {
						try {
							innerLogJarFile.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (null != outLibFileOut) {
						try {
							outLibFileOut.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return outLogLibFile;
	}

	private Class<?> getClassFromJarEntry(String name, JarFile jarFile)
			throws ClassNotFoundException {
		Class<?> clazz;
		DataInputStream dis = null;
		try {
			JarEntry innerClassEntry = jarFile.getJarEntry(StringUtils.replace(
					name, ".", "/") + ".class");
			if (null == innerClassEntry) {
				// 如果找不到任何的inner-logger.jar的话直接报错
				throw new ClassNotFoundException("Inner Logger ClassName: "
						+ name);
			}
			dis = new DataInputStream(jarFile.getInputStream(innerClassEntry));
			byte[] classBytes = new byte[(int) innerClassEntry.getSize()];
			dis.readFully(classBytes);
			clazz = defineClass(name, classBytes, 0, classBytes.length);
		} catch (IOException e) {
			// 如果加载出错,可能是加载到损坏包尝试删除掉导出的外部LIB文件
			File outLogLibFile = new File(OUT_LIB_PATH);
			outLogLibFile.delete();
			throw new ClassNotFoundException("Inner Logger ClassName: " + name,
					e);
		} finally {
			if (null != jarFile) {
				try {
					jarFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != dis) {
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return clazz;
	}

	private int copyInput2OutPut(InputStream input, OutputStream output)
			throws IOException {
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
			logMethod = MethodUtils.getMatchingAccessibleMethod(
					sl4jLogFactoryClass, "getLogger",
					new Class[] { String.class });
			// 关闭安全检查加速反射调用
			logMethod.setAccessible(true);
		}
		try {
			return logMethod.invoke(null, loggerName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					"invoke get inner logger Error! logName: " + loggerName, e);
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
}