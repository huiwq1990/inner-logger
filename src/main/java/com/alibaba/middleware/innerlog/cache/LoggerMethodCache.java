package com.alibaba.middleware.innerlog.cache;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logger的反射调用加速的Cache
 *
 * 每个Logger的ClassLoader都对应一套Logger Method的Cache
 *
 * User: <a href="mailto:qihao@taobao.com">qihao</a>
 * Date: 14-8-1
 * Time: 10:43
 * version 1.0
 */
public class LoggerMethodCache {

	final static Class<?>[] ONE_STRING_ARRAY = new Class[] { String.class };
	final static Class<?>[] FORMAT_ONE_ARRAY = new Class[] { String.class, Object.class };
	final static Class<?>[] FORMAT_TWO_ARRAY = new Class[] { String.class, Object.class, Object.class };
	final static Class<?>[] FORMAT_MANY_ARRAY = new Class[] { String.class, Object[].class };
	final static Class<?>[] THROWABLE_ARRAY = new Class[] { String.class, Throwable.class };

	private ConcurrentHashMap<Integer/*classLoaderId*/, Map<String, Method>/*methodCache*/> methodCache = new ConcurrentHashMap<Integer, Map<String, Method>>();

	public void put(Integer classLoaderId, String cachKey, Method method) {
		Map<String, Method> caches = getCacheWitchClassLoader(classLoaderId);
		//关闭安全检查,加速反射调用
		method.setAccessible(true);
		caches.put(cachKey, method);
	}

	public void put(Integer classLoaderId, String cachKey, Class<?> clazz, String methodName,
			Class<?>[] parameterTypes) {
		Map<String, Method> caches = getCacheWitchClassLoader(classLoaderId);
		Method method = MethodUtils.getMatchingAccessibleMethod(clazz, methodName, parameterTypes);
		//关闭安全检查
		method.setAccessible(true);
		caches.put(cachKey, method);
	}

	public Object invoke(Integer classLoaderId, String cachKey, Object obj) {
		return invoke(classLoaderId, cachKey, obj, ArrayUtils.EMPTY_OBJECT_ARRAY);
	}

	public Object invokeStatic(Integer classLoaderId, String cachKey) {
		return invoke(classLoaderId, cachKey, null, ArrayUtils.EMPTY_OBJECT_ARRAY);
	}

	public Object invokeStatic(Integer classLoaderId, String cachKey, Object... args) {
		return invoke(classLoaderId, cachKey, null, args);
	}

	public boolean containsKey(Integer classLoaderId, String cacheKey) {
		Map<String, Method> caches = getCacheWitchClassLoader(classLoaderId);
		return caches.containsKey(cacheKey);
	}

	public Map<String, Method> getCacheWitchClassLoader(Integer classLoaderId) {
		Map<String, Method> value = methodCache.get(classLoaderId);
		if (null == value) {
			value = new HashMap<String, Method>(LoggerMethodEnum.values().length);
			methodCache.put(classLoaderId, value);
		}
		return value;
	}

	public Object invoke(Integer classLoaderId, String cachKey, Object obj, Object... args) {
		Map<String, Method> caches = getCacheWitchClassLoader(classLoaderId);
		Method method = caches.get(cachKey);
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