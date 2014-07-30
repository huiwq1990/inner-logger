package com.alibaba.middleware.innerlog;

import org.apache.commons.lang.StringUtils;

import java.io.InputStream;

/**
 * 已知LOG系统的Configure类,用该类的前提是确定JVM中的日志系统
 * 是什么。根据给定的日志配置文件,支持classPath和fileSystem的
 * 加载日志的config文件的InputStream
 *
 * User: <a href="mailto:qihao@taobao.com">qihao</a>
 * Date: 14-6-26
 * Time: 13:28
 * version 1.0
 */

public class DefaultLogConfigure extends LogConfigure {
	private final static String INNER_DEFAULT_CONF = "inner-default-logback.xml";
	private final String resource;
	private final boolean fileSystem;

	public DefaultLogConfigure(String resource) {
		this.resource = resource;
		this.fileSystem = false;
	}

	public DefaultLogConfigure(String resource, boolean fileSystem) {
		this.resource = resource;
		this.fileSystem = fileSystem;
	}

	@SuppressWarnings("resource")
	public InputStream configure() {
		InputStream inputStream = null;
		if (StringUtils.isNotBlank(resource)) {
			inputStream = fileSystem ? getResourceFromFileSystem(resource) : getResourceFromClasPath(resource);
		}
		if (null == inputStream) {
			inputStream = getResourceFromClasPath(INNER_DEFAULT_CONF);
		}
		return inputStream;
	}
}