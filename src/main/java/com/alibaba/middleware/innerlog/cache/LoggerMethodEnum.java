package com.alibaba.middleware.innerlog.cache;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * sl4j对外暴露的方法名枚举类,关联加速反射调用的method的key
 *
 * User: <a href="mailto:qihao@taobao.com">qihao</a>
 * Date: 14-8-1
 * Time: 10:47
 * version 1.0
 */
public enum LoggerMethodEnum {

	GET_NAME("getName()"),
	IS_TRACE_ENABLED("isTraceEnabled()"),
	TRACE("trace(String msg)", LoggerMethodCache.ONE_STRING_ARRAY),
	TRACE_FORMAT_ONE("trace(String format, Object arg)", LoggerMethodCache.FORMAT_ONE_ARRAY),
	TRACE_FORMAT_TWO("trace(String format, Object arg1, Object arg2)", LoggerMethodCache.FORMAT_TWO_ARRAY),
	TRACE_FORMAT_MANY("trace(String format, Object... arguments)", LoggerMethodCache.FORMAT_MANY_ARRAY),
	TRACE_THROWABLE("trace(String msg, Throwable t)", LoggerMethodCache.THROWABLE_ARRAY),

	IS_DEBUG_ENABLED("isDebugEnabled()"),
	DEBUG("debug(String msg)", LoggerMethodCache.ONE_STRING_ARRAY),
	DEBUG_FORMAT_ONE("debug(String format, Object arg)", LoggerMethodCache.FORMAT_ONE_ARRAY),
	DEBUG_FORMAT_TWO("debug(String format, Object arg1, Object arg2)", LoggerMethodCache.FORMAT_TWO_ARRAY),
	DEBUG_FORMAT_MANY("debug(String format, Object... arguments))", LoggerMethodCache.FORMAT_MANY_ARRAY),
	DEBUG_THROWABLE("debug(String msg, Throwable t)", LoggerMethodCache.THROWABLE_ARRAY),

	IS_INFO_ENABLED("isInfoEnabled()"),
	INFO("info(String msg)", LoggerMethodCache.ONE_STRING_ARRAY),
	INFO_FORMAT_ONE("info(String format, Object arg)", LoggerMethodCache.FORMAT_ONE_ARRAY),
	INFO_FORMAT_TWO("info(String format, Object arg1, Object arg2)", LoggerMethodCache.FORMAT_TWO_ARRAY),
	INFO_FORMAT_MANY("info(String format, Object... arguments)", LoggerMethodCache.FORMAT_MANY_ARRAY),
	INFO_THROWABLE("info(String msg, Throwable t)", LoggerMethodCache.THROWABLE_ARRAY),

	IS_WARN_ENABLED("isWarnEnabled()"),
	WARN("warn(String msg)", LoggerMethodCache.ONE_STRING_ARRAY),
	WARN_FORMAT_ONE("warn(String format, Object arg)", LoggerMethodCache.FORMAT_ONE_ARRAY),
	WARN_FORMAT_TWO("warn(String format, Object arg1, Object arg2)", LoggerMethodCache.FORMAT_TWO_ARRAY),
	WARN_FORMAT_MANY("warn(String format, Object... arguments)", LoggerMethodCache.FORMAT_MANY_ARRAY),
	WARN_THROWABLE("warn(String msg, Throwable t)", LoggerMethodCache.THROWABLE_ARRAY),

	IS_ERROR_ENABLED("isErrorEnabled()"),
	ERROR("error(String msg)", LoggerMethodCache.ONE_STRING_ARRAY),
	ERROR_FORMAT_ONE("error(String format, Object arg)", LoggerMethodCache.FORMAT_ONE_ARRAY),
	ERROR_FORMAT_TWO("error(String format, Object arg1, Object arg2)", LoggerMethodCache.FORMAT_TWO_ARRAY),
	ERROR_FORMAT_MANY("error(String format, Object... arguments)", LoggerMethodCache.FORMAT_MANY_ARRAY),
	ERROR_THROWABLE("error(String msg, Throwable t)", LoggerMethodCache.THROWABLE_ARRAY);

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

	public String getmKey() {
		return mKey;
	}

	public String getmName() {
		return mName;
	}

	public Class<?>[] getParmClassTypes() {
		return parmClassTypes;
	}
}