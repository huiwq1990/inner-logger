package com.alibaba.middleware.innerlog;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.reflect.MethodUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志工厂类,采用日志框架sl4j和日志系统logback
 *
 * 解决以下问题：
 *
 * 1.日志冲突问题: 基于classLoader隔离日志系统,用自己的classLoader加载内置的sl4j和logback
 * 不会跟同JVM中其他的日志框架和日志系统冲突
 *
 * 2.相同JVM多个innerLogger通过appKey隔离支持按照appKey进行configure,且互不干扰。
 *
 * 3.日志重新configure 定制日志输出配置:
 *
 * 使用initByClassResource 或者initByFileSystem 方法指定具体的logback的配置 文件进行configure
 *
 * 使用方式:
 *
 * setp 1: 如果需要重新configure 日志系统,则需要在最开始调用
 *
 * LogFactory.doConfigure(LogConfigure logConfigure,String appKey)
 *
 * 或者使用以下的方式
 *
 * 一、指定配置文件初始化: LogFactory.initByClassResource(final String classResource,String appKey)
 *
 * 二、LogFactory.initByFileSystem(final String configFilePath,String appKey)
 *
 *
 * setp 2: 获取日志方法
 *
 * LogFactory.getLogger(String name,String appKey),LogFactory.getLogger(Class<?> clazz,String appKey)
 *
 * @author: <a href="mailto:qihao@taobao.com">qihao</a>
 *
 *          Date: 14-6-17 Time: 15:47 version 1.0
 */

public class LoggerFactory {

	private static final String SYSTEM_LONGBACK_CONFIG_FILE_KEY = "com.alibaba.inner.logback.config.file";
	private static final Map<String/* appKey */, LoggerContext> LOGGER_CONTEXT_LOADERS = new HashMap<String, LoggerContext>();

	/**
	 * 该方法适用于直接给定classPath下logback配置文件classpath路径名称 配置文件来对日志做Configure
	 *
	 * @param classResource
	 */
	public static void initByClassResource(final String classResource,
			String appKey) {
		doConfigure(new DefaultLogConfigure(classResource), appKey);
	}

	/**
	 * 该方法适用于直接给定文件系统下logback配置文件绝对路径 配置文件来对日志做Configure
	 *
	 * @param configFilePath
	 */
	public static void initByFileSystem(final String configFilePath,
			String appKey) {
		doConfigure(new DefaultLogConfigure(configFilePath, Boolean.TRUE),
				appKey);
	}

	/**
	 * 利用LogConfigure类来Configure日志系统, 需要在LogConfigure
	 * 中实现根据给出logback配置文件的InputStream
	 *
	 * @param logConfigure
	 */
	public synchronized static void doConfigure(LogConfigure logConfigure,
			String appKey) {
		appKey = StringUtils.defaultIfBlank(appKey, StringUtils.EMPTY);
		if (null == LOGGER_CONTEXT_LOADERS.get(appKey)) {
			bindSl4j(appKey);
		}
		LoggerContext loggerContext = LOGGER_CONTEXT_LOADERS.get(appKey);
		if (loggerContext.isConfigure()) {
			// 已经配置configure过
			return;
		}
		String systemConfPath;
		InputStream inputStream = null;
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
				Class<?> JoranConfClass = ClassUtils.getClass(loggerContext,
						"ch.qos.logback.classic.joran.JoranConfigurator");
				Object JoranConfObj = ConstructorUtils.invokeConstructor(
						JoranConfClass, ArrayUtils.EMPTY_OBJECT_ARRAY);
				// 设置logContext
				MethodUtils.invokeMethod(JoranConfObj, "setContext",
						loggerContext.getInnerFactory());
				// 进行Configure
				MethodUtils.invokeMethod(JoranConfObj, "doConfigure",
						inputStream);
				loggerContext.setConfigure(true);
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

	/**
	 * 根据logName和appKey获取logger
	 * @param logName 日志名称
	 * @param appKey  对应的appKey
	 * @return
	 */
	public static Logger getLogger(String logName, String appKey) {
		return getWrapperLogger(logName, appKey);
	}

	/**
	 * 根据class和appKey获取logger
	 * @param clazz 需要获取logger对应的class
	 * @param appKey 对应的appKey
	 * @return
	 */
	public static Logger getLogger(Class<?> clazz, String appKey) {
		return getWrapperLogger(clazz.getName(), appKey);
	}

	/*
	 * SL4J的初始化与log的绑定方法。
	 */
	private synchronized static void bindSl4j(String appKey) {
		try {
			LoggerContext loggerContext = new LoggerContext();
			Class<?> sl4jLogFactoryClass = ClassUtils.getClass(loggerContext,
					"org.slf4j.LoggerFactory");
			Object innerFactory = MethodUtils.invokeExactStaticMethod(
					sl4jLogFactoryClass, "getILoggerFactory",
					ArrayUtils.EMPTY_OBJECT_ARRAY);
			loggerContext.setSl4jLogFactoryClass(sl4jLogFactoryClass);
			loggerContext.setInnerFactory(innerFactory);
			LOGGER_CONTEXT_LOADERS.put(appKey, loggerContext);
		} catch (Exception e) {
			throw new RuntimeException("binding inner sl4j Error! ", e);
		}
	}

	/*
	 * 根据给定的对象获取对应的SL4J的包装Logger对象
	 *
	 * @param loggerName
	 *            logger名称
	 *
	 * @param appKey
	 *            日志系统的appKey
	 * @return
	 */
	private synchronized static Logger getWrapperLogger(
			String loggerName, String appKey) {
		appKey = StringUtils.defaultIfBlank(appKey, StringUtils.EMPTY);
		LoggerContext loggerContext = LOGGER_CONTEXT_LOADERS.get(appKey);
		if (null == loggerContext) {
			// 如果sl4j没有进行绑定过,先尝试绑定
			bindSl4j(appKey);
		}
		return new Logger(loggerContext.getInnerLogger(loggerName), loggerContext);
	}

	public static void main(String[] args) {
		String metaqAppKey = "metaq";
		String jwAppKey = "jingwei";
		LoggerFactory.doConfigure(null, jwAppKey);
		LoggerFactory.doConfigure(null, metaqAppKey);
		Logger jwLogger = LoggerFactory.getLogger(LoggerFactory.class, jwAppKey);
		Logger metaqLogger = LoggerFactory.getLogger(LoggerFactory.class, metaqAppKey);
		jwLogger.setLevel(LogLevel.ERROR);
		jwLogger.error("error!!");
		metaqLogger.setLevel(LogLevel.WARN);
		metaqLogger.warn("warn!!");
	}
}