package com.jss.ftp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.IOUtils;

import com.jss.ftp.comons.FtpConfig;
import com.jss.ftp.comons.FtpStatus;
import com.jss.ftp.comons.FtpUtils;


public class FtpUploadDownLoad {

	private static Logger logger = LogManager.getLogger();
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("hello");
		Collection<String> msgs = new ArrayList<String>();
		msgs.add("test1/r/n");
		msgs.add("hello/r/n");
		OutputStream output = new FileOutputStream("A.log",true);
		IOUtils.writeLines(msgs, "UTF-8", output);
		FtpStatus aFtpStatus= FtpStatus.UPLOAD_FILE_SUCCESS;
		System.out.println(aFtpStatus);
		System.out.println(FtpConfig.getProperties("src/ftpconfig.xml").getProperty("ftpUserName"));
		Properties properties = FtpConfig.getProperties("src/ftpconfig.xml");
		
		FtpUtils ftpUtils = new FtpUtils(properties.getProperty("host"), properties.getProperty("ftpUserName"), properties.getProperty("ftpPassword"), properties.getProperty("ftpPort"));
//		Collection<String> restult = ftpUtils.uploadFiles("/Users/zoupeiyang/Downloads/ftptest/", "/var/www/abc", null);
//		System.out.println("上传文件个数："+restult.size());
		
		Collection<String> resultCollection =ftpUtils.downloadFiles("/Users/zoupeiyang/Downloads/ftptest2", "/var/www/abc", null);
		System.out.println("下载文件个数："+resultCollection.size());
		
		Properties props=System.getProperties();
		String osname=props.getProperty("os.name");  
		System.out.println(osname);
		System.out.println("执行成功");

	}

}
