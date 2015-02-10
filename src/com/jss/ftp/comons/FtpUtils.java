package com.jss.ftp.comons;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;





public class FtpUtils {
	private static String DEAFULT_REMOTE_CHARSET="UTF-8";
	private static int DEAFULT_REMOTE_PORT=21;
	private static String SEPARATOR  = File.separator;
	private String host;
	private String ftpUserName;
	private String ftpPassword;
	private String ftpPort;
	private FTPClient ftpClient;

	private static Logger logger = LogManager.getLogger();
	public FtpUtils(String host,String ftpUserName,String ftpPassword,String ftpPort) throws IOException {
		this.host=host;
		this.ftpUserName=ftpUserName;
		this.ftpPassword=ftpPassword;
		this.ftpPort=ftpPort;
		this.ftpClient = initFtpClient();
	}

	public Collection<String> uploadFiles(String localPath,
			String remotePath,Collection<String> uploadStatusMessages) throws Exception {
		if(uploadStatusMessages==null) uploadStatusMessages=new ArrayList<String>();
		FtpStatus ftpStatus = createDirectory(remotePath,ftpClient);
		if(ftpStatus==FtpStatus.CREATE_DIRECTORY_FAIL)
		{
			uploadStatusMessages.add("创建远程目录失败"+remotePath);
			return uploadStatusMessages;
		}
		else if(ftpStatus==FtpStatus.CREATE_DIRECTORY_SUCCESS)
		{
			uploadStatusMessages.add("创建远程目录成功"+remotePath);
		}
		File file = new File(localPath);
		File[] files = file.listFiles();
		for(File f:files)
		{
			if(f.isFile())
			{
				String fileName=f.getName();
				ftpStatus = uploadFile(fileName, f, 0);
				if(ftpStatus==FtpStatus.UPLOAD_FILE_SUCCESS)
				uploadStatusMessages.add("上传文件成功："+localPath+SEPARATOR+fileName+" -->> "+remotePath+"/"+fileName);
				else {
					uploadStatusMessages.add("上传文件失败："+localPath+SEPARATOR+fileName+" -->> "+remotePath+"/"+fileName);
				}
			}
			else {
				this.uploadFiles(localPath+SEPARATOR+f.getName(), remotePath+SEPARATOR+f.getName(),uploadStatusMessages);
				ftpClient.changeWorkingDirectory(remotePath);
			}
		}
		
		return uploadStatusMessages;
	}
	
	public Collection<String> downloadFiles(String localPath,String remotePath,Collection<String> messages) throws IOException
	{
		if(messages==null) messages=new ArrayList<String>();
		this.ftpClient.changeWorkingDirectory(remotePath);
		FTPFile[] ftpFiles=this.ftpClient.listFiles();	
		
		if(!ArrayUtils.isEmpty(ftpFiles))
		{
			FtpStatus downStatus=FtpStatus.DOWNLOAD_FILE_FAIL;
			for(FTPFile ftpFile : ftpFiles)
			{
				String fileName=ftpFile.getName();
				String newRemotePath =remotePath+"/"+fileName;
				String newLocalPath =localPath+SEPARATOR+fileName;
				if(ftpFile.isFile())
				{
					downStatus=downloadFile(newLocalPath, newRemotePath);
					if(downStatus==FtpStatus.DOWNLOAD_FILE_SUCCESS)
					{
						String succString=newLocalPath+"<<--"+newRemotePath+"\r\n";
						messages.add(succString);
					}
				}
				else
				{
					File file = new File(newLocalPath);
					if(file!=null&&!file.exists())
						file.mkdirs();
					downloadFiles(newLocalPath,newRemotePath,messages);
					this.ftpClient.changeWorkingDirectory(remotePath);
				}
			}
		}
		return messages;
	}
	
	public FtpStatus downloadFile(String localFilePath,String remoteFilePath)
			throws IOException {
				ftpClient.enterLocalPassiveMode();
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				FtpStatus result;
				FTPFile[] files = ftpClient.listFiles(remoteFilePath);
				if (files.length != 1) {
					return FtpStatus.REMOTE_FILE_NOEXIST;
				}
				long lRemoteSize = files[0].getSize();
				File f = new File(localFilePath);
				if (f.exists()) {
					long localSize = f.length();
					if (localSize >= lRemoteSize) {
						return FtpStatus.REMOTE_FILE_NOEXIST;
					}
					FileOutputStream out = new FileOutputStream(f, true);
					ftpClient.setRestartOffset(localSize);
					InputStream in = ftpClient.retrieveFileStream(remoteFilePath);
					byte[] bytes = new byte[1024];
					long process = localSize*100/lRemoteSize;
					int c;
					while ((c = in.read(bytes)) != -1) {
						out.write(bytes, 0, c);
						localSize += c;
						long nowProcess = localSize*100/lRemoteSize;
						if (nowProcess > process) {
							process = nowProcess;
							if (process % 10 == 0){
								System.out.println("下载进度：" + process+"%");
							}
						}
					}
					in.close();
					out.close();
					boolean isDo = ftpClient.completePendingCommand();
					if (isDo) {
						result = FtpStatus.DOWNLOAD_FILE_SUCCESS;
					} else {
						result = FtpStatus.DOWNLOAD_FILE_FAIL;
					}
				} else {
					OutputStream out = new FileOutputStream(f);
					InputStream in = ftpClient.retrieveFileStream(remoteFilePath);
					byte[] bytes = new byte[1024];
					long process = 0;
					long localSize = 0L;
					int c;
					while ((c = in.read(bytes)) != -1) {
						out.write(bytes, 0, c);
						localSize += c;
						long nowProcess = localSize *100/lRemoteSize;
						if (nowProcess > process) {
							process = nowProcess;
							if (process % 10 == 0){
								System.out.println("下载进度" + process+"%");
							}
						}
					}
					in.close();
					out.close();
					boolean upNewStatus = ftpClient.completePendingCommand();
					if (upNewStatus) {
						result = FtpStatus.DOWNLOAD_FILE_SUCCESS;
					} else {
						result = FtpStatus.DOWNLOAD_FILE_FAIL;
					}
				}
				
				return result;
			}
	
	/**
	 * 上传文件
	 * @param remoteFile
	 * @param localFile
	 * @param ftpClient
	 * @param remoteSize
	 * @return
	 * @throws IOException
	 */
	public FtpStatus uploadFile(String remoteFile, File localFile, long remoteSize) throws Exception {
		FtpStatus status;
			
				long process = 0;
				long localreadbytes = 0L;
				RandomAccessFile raf = new RandomAccessFile(localFile, "r");
				OutputStream out = ftpClient.appendFileStream(remoteFile);
				if (out == null)
				{
					String message = ftpClient.getReplyString();
					throw new RuntimeException(message);
				}
				if (remoteSize > 0) {
					ftpClient.setRestartOffset(remoteSize);
					process = remoteSize*100 / localFile.length();
					raf.seek(remoteSize);
					localreadbytes = remoteSize;
				}
				byte[] bytes = new byte[1024];
				int c;
				while ((c = raf.read(bytes)) != -1) {
					out.write(bytes, 0, c);
					localreadbytes += c;
					if (localreadbytes *100/ localFile.length() != process) {
						process = localreadbytes *100/ localFile.length();
						System.out.println("上传进度:" + process+"%");
					}
				}
				out.flush();
				raf.close();
				out.close();
				boolean result = ftpClient.completePendingCommand();
				if (remoteSize > 0) {
					status = result ? FtpStatus.UPLOAD_FILE_SUCCESS
					: FtpStatus.UPLOAD_FILE_FAIL;
				} else {
					status = result ? FtpStatus.UPLOAD_FILE_SUCCESS
							: FtpStatus.UPLOAD_FILE_FAIL;
				}
				return status;
			}
	
	/**
	 * 创建远程目录
	 * @param remote
	 * @param ftpClient
	 * @return
	 * @throws IOException
	 */
	public FtpStatus createDirectory(String remote, FTPClient ftpClient)throws IOException
	{ 
		FtpStatus resutlStatus = FtpStatus.DIRECTORY_EXIST;
		//如果当前远程路径的最后一个目录不存在，则创建该目录
		if (!ftpClient.changeWorkingDirectory(remote))
		{
			int start=0;
			int end=remote.lastIndexOf("/");
			String parentPath=remote.substring(start,end);
			ftpClient.changeWorkingDirectory(parentPath);
			String newDir =remote.substring(end+1);
			if(ftpClient.makeDirectory(newDir))
			{
				ftpClient.changeWorkingDirectory(remote);
				resutlStatus = FtpStatus.CREATE_DIRECTORY_SUCCESS;
			}
			else {
				resutlStatus = FtpStatus.CREATE_DIRECTORY_FAIL;
			}
			
		}
		return resutlStatus;
	}
	
	public FTPClient initFtpClient() throws IOException
	{
		FTPClient ftp=new FTPClient();
	    try {
	    	ftp.setDataTimeout(7200);
	    	 ftp.setControlEncoding(DEAFULT_REMOTE_CHARSET);
	    	ftp.setDefaultPort(DEAFULT_REMOTE_PORT);
	    	ftp.setListHiddenFiles(false);
	    	if(StringUtils.isNotEmpty(ftpPort)&&NumberUtils.isDigits(ftpPort)){
	    			ftp.connect(host, Integer.valueOf(ftpPort));
	    	}else{
	    		ftp.connect(host);
	    	}
        } catch(ConnectException e) {
        	logger.error("ftp连接失败："+ftp.getReplyString()+ftp.getReplyCode());
            throw new IOException("Problem connecting the FTP-server fail",e);
        }
		int reply=ftp.getReplyCode();
		
		if(!FTPReply.isPositiveCompletion(reply)){
			ftp.disconnect();
		}
        if(!ftp.login(this.ftpUserName, this.ftpPassword)) {
            ftp.quit();
            ftp.disconnect();
        	logger.error("ftp登陆失败"+ftp.getReplyString());
            throw new IOException("Cant Authentificate to FTP-Server");
        }
    
        ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftp;
		
	}
	
}
