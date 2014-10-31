package com.alibaba.middleware.innerlog;

import java.util.Arrays;

import com.alibaba.middleware.innerlog.cache.LoggerMethodCache;
import com.alibaba.middleware.innerlog.cache.LoggerMethodEnum;
import com.alibaba.middleware.innerlog.util.ClassUtils;
import com.alibaba.middleware.innerlog.util.MethodUtils;

/**
 *  内置日志类
 *
 *  通过com.alibaba.middleware.innerlog.LoggerFactory
 *  的getLogger方法获得。
 *
 *  注: 内置的innerLogger 其实就是SL4J的Logger
 *  由于使用者系统的可能没有SL4J的依赖需要通过内置的classLoader加载,
 *  直接显示暴露org.slf4j.Logger 类可能会找不到类,所以该类通过反射方
 *  法进行调用.
 *
 * @author: <a href="mailto:qihao@taobao.com">qihao</a>
 *
 *  Date: 14-6-17
 *  Time: 15:47
 *  version 1.0
 */

public class Logger {
	/**
	 * 加速反射调用的cache
	 */
	private final static LoggerMethodCache methodCache = new LoggerMethodCache();
	/**
	 * 加载这个Logger对象对应的classLoader
	 */
	private final LoggerClassLoader loggerClassLoader;
	/**
	 * 真实的sl4j的Logger对象
	 */
	private final Object innerlogback;

	public Logger(Object innerlogback, LoggerClassLoader loggerClassLoader) {
		this.loggerClassLoader = loggerClassLoader;
		this.innerlogback = innerlogback;
	}

	public String getName() {
		return (String) this.invokeMethod(LoggerMethodEnum.GET_NAME);
	}

	public boolean isTraceEnabled() {
		return (Boolean) this.invokeMethod(LoggerMethodEnum.IS_TRACE_ENABLED);
	}

	public void trace(String msg) {
		this.invokeMethod(LoggerMethodEnum.TRACE, msg);
	}

	public void trace(String format, Object arg) {
		this.invokeMethod(LoggerMethodEnum.TRACE_FORMAT_ONE, format, arg);
	}

	public void trace(String format, Object arg1, Object arg2) {
		this.invokeMethod(LoggerMethodEnum.TRACE_FORMAT_TWO, format, arg1, arg2);
	}

	public void trace(String format, Object... arguments) {
		this.invokeMethod(LoggerMethodEnum.TRACE_FORMAT_MANY, format, arguments);
	}

	public void trace(String msg, Throwable t) {
		this.invokeMethod(LoggerMethodEnum.TRACE_THROWABLE, msg, t);
	}

	public boolean isDebugEnabled() {
		return (Boolean) this.invokeMethod(LoggerMethodEnum.IS_DEBUG_ENABLED);
	}

	public void debug(String msg) {
		this.invokeMethod(LoggerMethodEnum.DEBUG, msg);
	}

	public void debug(String format, Object arg) {
		this.invokeMethod(LoggerMethodEnum.DEBUG_FORMAT_ONE, format, arg);
	}

	public void debug(String format, Object arg1, Object arg2) {
		this.invokeMethod(LoggerMethodEnum.DEBUG_FORMAT_TWO, format, arg1, arg2);
	}

	public void debug(String format, Object... arguments) {
		this.invokeMethod(LoggerMethodEnum.DEBUG_FORMAT_MANY, format, arguments);
	}

	public void debug(String msg, Throwable t) {
		this.invokeMethod(LoggerMethodEnum.DEBUG_THROWABLE, msg, t);
	}

	public boolean isInfoEnabled() {
		return (Boolean) this.invokeMethod(LoggerMethodEnum.IS_INFO_ENABLED);
	}

	public void info(String msg) {
		this.invokeMethod(LoggerMethodEnum.INFO, msg);
	}

	public void info(String format, Object arg) {
		this.invokeMethod(LoggerMethodEnum.INFO_FORMAT_ONE, format, arg);
	}

	public void info(String format, Object arg1, Object arg2) {
		this.invokeMethod(LoggerMethodEnum.INFO_FORMAT_TWO, format, arg1, arg2);
	}

	public void info(String format, Object... arguments) {
		this.invokeMethod(LoggerMethodEnum.INFO_FORMAT_MANY, format, arguments);
	}

	public void info(String msg, Throwable t) {
		this.invokeMethod(LoggerMethodEnum.INFO_THROWABLE, msg, t);
	}

	public boolean isWarnEnabled() {
		return (Boolean) this.invokeMethod(LoggerMethodEnum.IS_WARN_ENABLED);
	}

	public void warn(String msg) {
		this.invokeMethod(LoggerMethodEnum.WARN, msg);
	}

	public void warn(String format, Object arg) {
		this.invokeMethod(LoggerMethodEnum.WARN_FORMAT_ONE, format, arg);
	}

	public void warn(String format, Object arg1, Object arg2) {
		this.invokeMethod(LoggerMethodEnum.WARN_FORMAT_TWO, format, arg1, arg2);
	}

	public void warn(String format, Object... arguments) {
		this.invokeMethod(LoggerMethodEnum.WARN_FORMAT_MANY, format, arguments);
	}

	public void warn(String msg, Throwable t) {
		this.invokeMethod(LoggerMethodEnum.WARN_THROWABLE, msg, t);
	}

	public boolean isErrorEnabled() {
		return (Boolean) this.invokeMethod(LoggerMethodEnum.IS_ERROR_ENABLED);
	}

	public void error(String msg) {
		this.invokeMethod(LoggerMethodEnum.ERROR, msg);
	}

	public void error(String format, Object arg) {
		this.invokeMethod(LoggerMethodEnum.ERROR_FORMAT_ONE, format, arg);
	}

	public void error(String format, Object arg1, Object arg2) {
		this.invokeMethod(LoggerMethodEnum.ERROR_FORMAT_TWO, format, arg1, arg2);
	}

	public void error(String format, Object... arguments) {
		this.invokeMethod(LoggerMethodEnum.ERROR_FORMAT_MANY, format, arguments);
	}

	public void error(String msg, Throwable t) {
		this.invokeMethod(LoggerMethodEnum.ERROR_THROWABLE, msg, t);
	}

	public void setLevel(LogLevel level) {
		try {
			Class<?> levelClass = ClassUtils
					.getClass(loggerClassLoader, "ch.qos.logback.classic.Level");
			Object logBackLevel = MethodUtils.invokeStaticMethod(levelClass, "toLevel", level.name());
			MethodUtils.invokeMethod(innerlogback, "setLevel", logBackLevel);
		} catch (Exception e) {
			throw new RuntimeException("setLevel to inner logback Error! ", e);
		}
	}

	private Object invokeMethod(LoggerMethodEnum methodEnum, Object... arguments) {
		Integer classLoaderId = this.loggerClassLoader.getId();
		try {
			if (!methodCache.containsKey(classLoaderId, methodEnum.getmKey())) {
				methodCache.put(classLoaderId, methodEnum.getmKey(), this.innerlogback.getClass(),
						methodEnum.getmName(),
						methodEnum.getParmClassTypes());
			}
			return methodCache.invoke(classLoaderId, methodEnum.getmKey(), this.innerlogback, arguments);
		} catch (Exception e) {
			e.printStackTrace();
			StringBuilder sb = new StringBuilder("invokeMethod Error! ");
			sb.append(" class: ").append(this.innerlogback.getClass());
			sb.append(" method: ").append(methodEnum.getmKey());
			sb.append(" arguments: ").append(Arrays.toString(arguments));
			throw new RuntimeException(sb.toString(), e);
		}
	}
}