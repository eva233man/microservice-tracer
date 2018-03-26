package com.jcfc.microservice.tracer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * 日志跟踪管理器
 * 1.加载一些环境变量
 * 2.存放tracing上下文
 *
 * @author zhangjinpeng
 * @version 1.0
 */
class TracerProperties {
	private static final Logger logger = LoggerFactory.getLogger(TracerProperties.class);

	static private final String DEFAULT_CONFIGURATION_FILE = "tracer.properties";

	private static Properties props;
	static{
		loadProps();
	}

	synchronized static private void loadProps(){
		props = new Properties();
		InputStream in = null;
		try {
					in = TracerProperties.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIGURATION_FILE);
					props.load(in);
		} catch (FileNotFoundException e) {
			logger.error("tracer.properties文件未找到");
		} catch (IOException e) {
			logger.error("出现IOException");
		} finally {
			try {
				if(null != in) {
					in.close();
				}
			} catch (IOException e) {
				logger.error("tracer.properties文件流关闭出现异常");
			}
		}
		logger.info("加载tracer.properties文件内容完成...........");
	}

	static String getProperty(String key){
		if(null == props) {
			loadProps();
		}
		return props.getProperty(key);
	}

	static String getProperty(String key, String defaultValue) {
		if(null == props) {
			loadProps();
		}
		return props.getProperty(key, defaultValue);
	}

}
