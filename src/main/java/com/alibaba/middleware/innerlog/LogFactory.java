package com.alibaba.middleware.innerlog;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.reflect.MethodUtils;

/**
 * 日志工厂类,采用日志框架sl4j和日志系统logback
 *
 * 解决以下问题：
 *
 * 1.日志冲突问题: 基于classLoader隔离日志系统,用自己的classLoader加载内置的sl4j和logback
 * 不会跟同JVM中其他的日志框架和日志系统冲突
 *
 * 2.日志重新configure 定制日志输出配置:
 *
 * 使用initByClassResource 或者initByFileSystem 方法指定具体的logback的配置 文件进行configure
 *
 * 使用方式:
 *
 * setp 1: 如果需要重新configure 日志系统,则需要在最开始调用
 *
 * LogFactory.doConfigure(LogConfigure logConfigure)
 *
 * 或者使用以下的方式
 *
 * 一、指定配置文件初始化: LogFactory.initByClassResource(final String classResource)
 * 
 * 二、LogFactory.initByFileSystem(final String configFilePath)
 *
 *
 * setp 2: 获取日志方法
 *
 * LogFactory.getLogger(String name),LogFactory.getLogger(Class<?> clazz)
 *
 * @author: <a href="mailto:qihao@taobao.com">qihao</a>
 *
 *          Date: 14-6-17 Time: 15:47 version 1.0
 */

public class LogFactory {

	private static final LoggerClassLoader innerLoader = new LoggerClassLoader();
	private static final String SYSTEM_LONGBACK_CONFIG_FILE_KEY = "com.alibaba.inner.logback.config.file";

	/**
	 * method缓存部分,用来加速反射调用
	 * */
	private static Method logMethod;

	/**
	 * SL4绑定和configure用到的变量
	 */
	private static Object innerFactory;
	private static Class<?> sl4jLogFactoryClass;

	/**
	 * 该方法适用于直接给定classPath下logback配置文件classpath路径名称 配置文件来对日志做Configure
	 * 
	 * @param classResource
	 */
	public static void initByClassResource(final String classResource) {
		doConfigure(new DefaultLogConfigure(classResource));
	}

	/**
	 * 该方法适用于直接给定文件系统下logback配置文件绝对路径 配置文件来对日志做Configure
	 * 
	 * @param classResource
	 */
	public static void initByFileSystem(final String configFilePath) {
		doConfigure(new DefaultLogConfigure(configFilePath, Boolean.TRUE));
	}

	/**
	 * 利用LogConfigure类来Configure日志系统, 需要在LogConfigure
	 * 中实现根据给出logback配置文件的InputStream
	 *
	 * @param logConfigure
	 */
	public synchronized static void doConfigure(LogConfigure logConfigure) {
		if (null == innerFactory) {
			bindSl4j();
		}
		InputStream inputStream = null;
		String systemConfPath = null;
		if (null != logConfigure
				&& StringUtils.isNotBlank(logConfigure.getSystemPropertyKey())) {
			systemConfPath = logConfigure.getSystemPropertyKey();
		} else {
			systemConfPath = System.getProperty(
					SYSTEM_LONGBACK_CONFIG_FILE_KEY,
					System.getenv(SYSTEM_LONGBACK_CONFIG_FILE_KEY));
		}
		if (StringUtils.isNotBlank(systemConfPath)) {
			inputStream = LogConfigure
					.getResourceFromFileSystem(systemConfPath);
		}
		// 如果没有指定系统的LOG配置文件路径,用代码中的logConfigure
		if (null == inputStream && null != logConfigure) {
			inputStream = logConfigure.configure();
		}
		if (null != inputStream) {
			try {
				Class<?> JoranConfClass = ClassUtils.getClass(innerLoader,
						"ch.qos.logback.classic.joran.JoranConfigurator");
				Object JoranConfObj = ConstructorUtils.invokeConstructor(
						JoranConfClass, ArrayUtils.EMPTY_OBJECT_ARRAY);
				// 设置logContext
				MethodUtils.invokeMethod(JoranConfObj, "setContext",
						innerFactory);
				// 进行Configure
				MethodUtils.invokeMethod(JoranConfObj, "doConfigure",
						inputStream);
			} catch (Exception e) {
				throw new RuntimeException("doConfigure logback Error! ", e);
			} finally {
				if (null != inputStream) {
					try {
						inputStream.close();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

	public static Logger getLogger(String logName) {
		return getWrapperLogger(logName, logName);
	}

	public static Logger getLogger(Class<?> clazz) {
		return getWrapperLogger(clazz, clazz.getName());
	}

	/**
	 * SL4J的初始化与log的绑定方法。
	 */
	private synchronized static void bindSl4j() {
		try {
			sl4jLogFactoryClass = ClassUtils.getClass(innerLoader,
					"org.slf4j.LoggerFactory");
			innerFactory = MethodUtils.invokeExactStaticMethod(
					sl4jLogFactoryClass, "getILoggerFactory",
					ArrayUtils.EMPTY_OBJECT_ARRAY);
		} catch (Exception e) {
			throw new RuntimeException("binding inner sl4j Error! ", e);
		}
	}

	/**
	 * 根据给定的对象获取对应的SL4J的包装Logger对象
	 * 
	 * @param object
	 *            getLoger的对象
	 * @param cacheKey
	 *            methodCache对应的缓存key
	 * @return
	 */
	private synchronized static Logger getWrapperLogger(Object object,
			String loggerName) {
		if (null == innerFactory) {
			// 如果sl4j没有进行绑定过,先尝试绑定
			bindSl4j();
		}
		if (null == logMethod) {
			// add method cache
			logMethod = MethodUtils.getMatchingAccessibleMethod(
					sl4jLogFactoryClass, "getLogger",
					new Class[] { String.class });
		}
		try {
			Object innerLog = logMethod.invoke(null, loggerName);
			return new Logger(innerLog, innerLoader);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					"invoke get inner logger Error! logName: " + loggerName, e);
		}
	}

	private static class LoggerClassLoader extends ClassLoader {

		private final static String SL4J_API_LIB = "loglib/slf4j-api-1.7.6.jlib";
		private final static String LOGBACK_CLASSIC_LIB = "loglib/logback-classic-1.1.2.jlib";
		private final static String LOGBACK_CORE_LIB = "loglib/logback-core-1.1.2.jlib";
		private final static String[] LOGBACK_LIBS = new String[] {
				SL4J_API_LIB, LOGBACK_CLASSIC_LIB, LOGBACK_CORE_LIB };

		LoggerClassLoader() {
			// 去掉父的classLoader,防止干扰业务的classLoader
			super(null);
		}

		protected Class<?> findClass(String name) throws ClassNotFoundException {
			Class<?> clazz = null;
			for (String jarFilePath : LOGBACK_LIBS) {
				DataInputStream dis = null;
				try {
					URL jarUrl = ClassLoader.getSystemClassLoader()
							.getResource(jarFilePath);
					JarFile jarFile = new JarFile(new File(jarUrl.toURI()));
					JarEntry entry = jarFile.getJarEntry(StringUtils.replace(
							name, ".", "/") + ".class");
					// 当前JAR找不到重试到下一个
					if (null == entry) {
						continue;
					}
					dis = new DataInputStream(jarFile.getInputStream(entry));
					byte[] classBytes = new byte[(int) entry.getSize()];
					dis.readFully(classBytes);
					clazz = defineClass(name, classBytes, 0, classBytes.length);
					if (null != clazz) {
						break;
					}
				} catch (Exception e) {
					throw new ClassNotFoundException(name, e);
				} finally {
					if (dis != null) {
						try {
							dis.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			if (null == clazz) {
				throw new ClassNotFoundException(name);
			}
			return clazz;
		}
	}

	public static void main(String[] args)  {
		LogFactory.doConfigure(null);
		Logger logger = LogFactory.getLogger(LogFactory.class);
		logger.setLevel(LogLevel.ERROR);
		logger.error("error!!");
		logger.setLevel(LogLevel.WARN);
		logger.warn("warn!!");
		logger.setLevel(LogLevel.INFO);
		logger.info("info!!");
	}
}