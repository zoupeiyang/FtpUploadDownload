package com.jss.ftp.comons;

import java.io.FileInputStream;
import java.util.Properties;

public  class FtpConfig {
	
	
	public static Properties getProperties(String path) throws Exception
	{
		Properties properties  = new Properties();
		FileInputStream fis =  new FileInputStream(path);
		properties.loadFromXML(fis);
		return properties;
	}

}
