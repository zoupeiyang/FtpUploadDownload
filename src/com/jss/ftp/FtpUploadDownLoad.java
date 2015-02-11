package com.jss.ftp;
import java.util.Properties;
import com.jss.ftp.comons.FtpConfig;
import com.jss.ftp.comons.FtpUtils;


public class FtpUploadDownLoad {

	public static void main(String[] args) throws Exception {
		Properties properties = FtpConfig.getProperties("src/ftpconfig.xml");
		FtpUtils ftpUtils = new FtpUtils(properties.getProperty("host"), properties.getProperty("ftpUserName"), properties.getProperty("ftpPassword"), properties.getProperty("ftpPort"));
		String result =ftpUtils.uploadFiles("D:\\ftptest2\\zentao3", "/var/www/abc");
		System.out.println(result);

	}

}
