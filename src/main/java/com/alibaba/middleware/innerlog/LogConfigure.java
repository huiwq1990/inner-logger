package com.alibaba.middleware.innerlog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * 日志系统configure基础类，由具体实现类根据日志系统类型
 * 和是否是内置日志系统来给出不同的配置文件的inputStream来
 * 对日志系统进行configure
 *
 * User: <a href="mailto:qihao@taobao.com">qihao</a>
 * Date: 14-6-26
 * Time: 10:21
 * version 1.0
 */
public abstract class LogConfigure {

	/**
	 * 返回日志系统配置文件的InputStream,来configure日志系统。
	 *
	 * @return InputStream Log配置文件的的InputStream
	 */
	public abstract InputStream configure();

	public static InputStream getResourceFromClasPath(String resourceName) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
	}

	public static InputStream getResourceFromFileSystem(String filePath) {
		InputStream inputStream = null;
		File systemConfigFile = new File(filePath);
		if (systemConfigFile.exists()) {
			try {
				inputStream = new FileInputStream(systemConfigFile);
			} catch (FileNotFoundException e) {
				//已经做过exists判断,基本不会进入这里。及时进入到这里返回null的inputStream也没问题。
			}
		}
		return inputStream;
	}

	/**
	 * 从inputStream对象获取String的内容
	 * @param is InputStream
	 * @return String内容
	 */
	public static String getStringFromInputStream(InputStream is) {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			throw new RuntimeException("BufferedReader readLine Error! ", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}
}
