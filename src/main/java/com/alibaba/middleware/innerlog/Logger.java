package com.alibaba.middleware.innerlog;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

	private final Object innerlogback;

	private final ClassLoader innerLoader;

	private final static MethodCache methodCache = new MethodCache(LoggerMethodEnum.values().length);

	public Logger(Object innerLogger, ClassLoader innerLoader) {
		this.innerlogback = innerLogger;
		this.innerLoader = innerLoader;
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
		this.invokeMethodNoCache(LoggerMethodEnum.TRACE_FORMAT_MANY, format, arguments);
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
		this.invokeMethodNoCache(LoggerMethodEnum.DEBUG_FORMAT_MANY, format, arguments);
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
		this.invokeMethodNoCache(LoggerMethodEnum.INFO_FORMAT_MANY, format, arguments);
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
		this.invokeMethodNoCache(LoggerMethodEnum.WARN_FORMAT_MANY, format, arguments);
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
		this.invokeMethodNoCache(LoggerMethodEnum.ERROR_FORMAT_MANY, format, arguments);
	}

	public void error(String msg, Throwable t) {
		this.invokeMethod(LoggerMethodEnum.ERROR_THROWABLE, msg, t);
	}

	public void setLevel(LogLevel level) {
		try {
			Class<?> levelClass = ClassUtils
					.getClass(innerLoader, "ch.qos.logback.classic.Level");
			Object logBackLevel = MethodUtils.invokeStaticMethod(levelClass, "toLevel", level.name());
			MethodUtils.invokeMethod(innerlogback, "setLevel", logBackLevel);
		} catch (Exception e) {
			throw new RuntimeException("setLevel to inner logback Error! ", e);
		}
	}

	/*
	 * 日志方法枚举
	 */
	static enum LoggerMethodEnum {
		GET_NAME("getName()"),
		IS_TRACE_ENABLED("isTraceEnabled()"),
		TRACE("trace(String msg)", MethodCache.ONE_STRING_ARRAY),
		TRACE_FORMAT_ONE("trace(String format, Object arg)", MethodCache.FORMAT_ONE_ARRAY),
		TRACE_FORMAT_TWO("trace(String format, Object arg1, Object arg2)", MethodCache.FORMAT_TWO_ARRAY),
		TRACE_THROWABLE("trace(String msg, Throwable t)", MethodCache.THROWABLE_ARRAY),

		IS_DEBUG_ENABLED("isDebugEnabled()"),
		DEBUG("debug(String msg)", MethodCache.ONE_STRING_ARRAY),
		DEBUG_FORMAT_ONE("debug(String format, Object arg)", MethodCache.FORMAT_ONE_ARRAY),
		DEBUG_FORMAT_TWO("debug(String format, Object arg1, Object arg2)", MethodCache.FORMAT_TWO_ARRAY),
		DEBUG_THROWABLE("debug(String msg, Throwable t)", MethodCache.THROWABLE_ARRAY),

		IS_INFO_ENABLED("isInfoEnabled()"),
		INFO("info(String msg)", MethodCache.ONE_STRING_ARRAY),
		INFO_FORMAT_ONE("info(String format, Object arg)", MethodCache.FORMAT_ONE_ARRAY),
		INFO_FORMAT_TWO("info(String format, Object arg1, Object arg2)", MethodCache.FORMAT_TWO_ARRAY),
		INFO_THROWABLE("info(String msg, Throwable t)", MethodCache.THROWABLE_ARRAY),

		IS_WARN_ENABLED("isWarnEnabled()"),
		WARN("warn(String msg)", MethodCache.ONE_STRING_ARRAY),
		WARN_FORMAT_ONE("warn(String format, Object arg)", MethodCache.FORMAT_ONE_ARRAY),
		WARN_FORMAT_TWO("warn(String format, Object arg1, Object arg2)", MethodCache.FORMAT_TWO_ARRAY),
		WARN_THROWABLE("warn(String msg, Throwable t)", MethodCache.THROWABLE_ARRAY),

		IS_ERROR_ENABLED("isErrorEnabled()"),
		ERROR("error(String msg)", MethodCache.ONE_STRING_ARRAY),
		ERROR_FORMAT_ONE("error(String format, Object arg)", MethodCache.FORMAT_ONE_ARRAY),
		ERROR_FORMAT_TWO("error(String format, Object arg1, Object arg2)", MethodCache.FORMAT_TWO_ARRAY),
		ERROR_THROWABLE("error(String msg, Throwable t)", MethodCache.THROWABLE_ARRAY),

		//no cache methods
		TRACE_FORMAT_MANY("trace(String format, Object... arguments)"),
		DEBUG_FORMAT_MANY("debug(String format, Object... arguments)"),
		INFO_FORMAT_MANY("info(String format, Object... arguments)"),
		WARN_FORMAT_MANY("warn(String format, Object... arguments)"),
		ERROR_FORMAT_MANY("error(String format, Object... arguments)");

		private final String mKey;
		private final String mName;
		private final Class<?>[] parmClassTypes;

		LoggerMethodEnum(String methodKey) {
			this.mKey = methodKey;
			this.parmClassTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
			this.mName = StringUtils.substringBeforeLast(methodKey, "(");
		}

		LoggerMethodEnum(String methodKey, Class<?>[] parmClassTypes) {
			this.mKey = methodKey;
			this.parmClassTypes = parmClassTypes;
			this.mName = StringUtils.substringBeforeLast(methodKey, "(");
		}
	}

	/*
	 * 日志方法的缓存,缓存method来加速反射调用
	 */
	static class MethodCache {

		final static Class<?>[] ONE_STRING_ARRAY = new Class[] { String.class };
		final static Class<?>[] FORMAT_ONE_ARRAY = new Class[] { String.class, Object.class };
		final static Class<?>[] FORMAT_TWO_ARRAY = new Class[] { String.class, Object.class, Object.class };
		final static Class<?>[] THROWABLE_ARRAY = new Class[] { String.class, Throwable.class };

		private Map<String, Method> methodCache;

		MethodCache(int cacheSize) {
			methodCache = new HashMap<String, Method>(cacheSize);
		}

		void put(String cachKey, Method method) {
			methodCache.put(cachKey, method);
		}

		void put(String cachKey, Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
			Method method = MethodUtils.getMatchingAccessibleMethod(clazz, methodName, parameterTypes);
			methodCache.put(cachKey, method);
		}

		Object invoke(String cachKey, Object obj) {
			return invoke(cachKey, obj, ArrayUtils.EMPTY_OBJECT_ARRAY);
		}

		Object invokeStatic(String cachKey) {
			return invoke(cachKey, null, ArrayUtils.EMPTY_OBJECT_ARRAY);
		}

		Object invokeStatic(String cachKey, Object... args) {
			return invoke(cachKey, null, args);
		}

		boolean containsKey(String cacheKey) {
			return methodCache.containsKey(cacheKey);
		}

		Object invoke(String cachKey, Object obj, Object... args) {
			Method method = methodCache.get(cachKey);
			if (null == method) {
				throw new RuntimeException("not find method cache maybe not init put cacheKey: " + cachKey);
			}
			try {
				return method.invoke(obj, args);
			} catch (Throwable e) {
				throw new RuntimeException("invoke method Error ! cacheKey: " + cachKey, e);
			}
		}
	}

	private Object invokeMethod(LoggerMethodEnum methodEnum, Object... arguments) {
		try {
			if (!methodCache.containsKey(methodEnum.mKey)) {
				methodCache.put(methodEnum.mKey, this.innerlogback.getClass(), methodEnum.mName,
						methodEnum.parmClassTypes);
			}
			return methodCache.invoke(methodEnum.mKey, this.innerlogback, arguments);
		} catch (Exception e) {
			e.printStackTrace();
			StringBuilder sb = new StringBuilder("invokeMethod Error! ");
			sb.append(" class: ").append(this.innerlogback.getClass());
			sb.append(" method: ").append(methodEnum.mKey);
			sb.append(" arguments: ").append(Arrays.toString(arguments));
			throw new RuntimeException(sb.toString(), e);
		}
	}

	private Object invokeMethodNoCache(LoggerMethodEnum methodEnum, Object... arguments) {
		try {
			return MethodUtils.invokeMethod(this.innerlogback, methodEnum.mName, arguments);
		} catch (Exception e) {
			e.printStackTrace();
			StringBuilder sb = new StringBuilder("invokeMethodNoCache Error! ");
			sb.append(" class: ").append(this.innerlogback.getClass());
			sb.append(" method: ").append(methodEnum.mKey);
			sb.append(" arguments: ").append(Arrays.toString(arguments));
			throw new RuntimeException(sb.toString(), e);
		}
	}
}