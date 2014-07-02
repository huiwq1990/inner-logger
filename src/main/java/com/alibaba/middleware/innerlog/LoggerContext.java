package com.alibaba.middleware.innerlog;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.MethodUtils;

import java.io.DataInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 内置log的lib加载classLoader,使用时根据appKey来划分
 *
 * User: <a href="mailto:qihao@taobao.com">qihao</a>
 * Date: 14-7-2
 * Time: 12:52
 * version 1.0
 */
public class LoggerContext extends ClassLoader {
	private final static String SL4J_API_LIB = "loglib/slf4j-api-1.7.6.jlib";
	private final static String LOGBACK_CLASSIC_LIB = "loglib/logback-classic-1.1.2.jlib";
	private final static String LOGBACK_CORE_LIB = "loglib/logback-core-1.1.2.jlib";
	private final static String[] LOGBACK_LIBS = new String[] {
			SL4J_API_LIB, LOGBACK_CLASSIC_LIB, LOGBACK_CORE_LIB };
	/**
	 * method缓存部分,用来加速反射调用
	 * */
	private Method logMethod;

	/**
	 * SL4绑定和configure用到的变量
	 */
	private Object innerFactory;
	private Class<?> sl4jLogFactoryClass;

	// 标记该classLoader对应的log系统是否已经configure过
	private boolean configure = false;

	protected LoggerContext() {
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

	protected Object getInnerLogger(String loggerName) {
		if (null == logMethod) {
			// add method cache
			logMethod = MethodUtils.getMatchingAccessibleMethod(
					sl4jLogFactoryClass, "getLogger", new Class[] { String.class });
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
}