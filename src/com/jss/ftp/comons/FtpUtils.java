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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ftp批量上传下载
 * 
 * @author zoupeiyang
 *
 */
public class FtpUtils {
	private static String DEAFULT_REMOTE_CHARSET = "UTF-8";
	private static int DEAFULT_REMOTE_PORT = 21;
	private static String SEPARATOR = File.separator;
	private String host;
	private String ftpUserName;
	private String ftpPassword;
	private String ftpPort;
	private FTPClient ftpClient;

	private static Logger logger = LogManager.getLogger();

	public FtpUtils(String host, String ftpUserName, String ftpPassword,
			String ftpPort) throws IOException {
		this.host = host;
		this.ftpUserName = ftpUserName;
		this.ftpPassword = ftpPassword;
		this.ftpPort = ftpPort;
		this.ftpClient = initFtpClient();
	}

	public Map<String, Collection<String>> uploadFiles(String localPath,
			String remotePath, Map<String, Collection<String>> messages)
			throws Exception {
		if (messages == null) {
			messages = new HashMap<String, Collection<String>>();
			messages.put("success", new ArrayList<String>());
			messages.put("fail", new ArrayList<String>());
		}
		FtpStatus ftpStatus = createDirectory(remotePath, ftpClient);
		if (ftpStatus == FtpStatus.CREATE_DIRECTORY_FAIL) {
			logger.error("创建远程目录失败" + remotePath);
			return messages;
		} else if (ftpStatus == FtpStatus.CREATE_DIRECTORY_SUCCESS) {
			logger.info("创建远程目录成功" + remotePath);
		}
		File file = new File(localPath);
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isFile()) {
				String fileName = f.getName();
				String uploadMessage;
				ftpStatus = uploadFile(fileName, f, 0);
				if (ftpStatus == FtpStatus.UPLOAD_FILE_SUCCESS) {
					uploadMessage = "上传文件成功：" + localPath + SEPARATOR
							+ fileName + " -->> " + remotePath + "/" + fileName;
					messages.get("success").add(uploadMessage);
				}

				else {
					uploadMessage = "上传文件失败：" + localPath + SEPARATOR
							+ fileName + " -->> " + remotePath + "/" + fileName;
					messages.get("fail").add(uploadMessage);
				}
			} else {
				this.uploadFiles(localPath + SEPARATOR + f.getName(),
						remotePath + "/" + f.getName(), messages);
				ftpClient.changeWorkingDirectory(remotePath);
			}
		}

		return messages;
	}

	/**
	 * 批量上传
	 * @param localPath
	 * @param remotePath
	 * @return
	 * @throws Exception
	 */
	public String uploadFiles(String localPath,String remotePath) throws Exception {
		Map<String, Collection<String>> uploadMessages = uploadFiles(localPath, remotePath, null);
		String result="共上传0个文件";
		if(uploadMessages!=null)
		{
			int successCount = uploadMessages.get("success").size();
			int failCount = uploadMessages.get("fail").size();
			result = "共上传文件" + (successCount + failCount) + "个，其中成功上传文件"
					+ successCount + "个，失败上传文件" + failCount + "个";
		}
		logger.info(result);
		return result;
	}
	
	/**
	 * 从服务器批量下载文件
	 * 
	 * @param localPath
	 *            本地路径
	 * @param remotePath
	 *            远程路径
	 * @param messages
	 *            下载状态信息
	 * @return 下载状态信息
	 * @throws IOException
	 */
	public Map<String, Collection<String>> downloadFiles(String localPath,
			String remotePath, Map<String, Collection<String>> messages)
			throws IOException {
		if (messages == null) {
			messages = new HashMap<String, Collection<String>>();
			messages.put("success", new ArrayList<String>());
			messages.put("fail", new ArrayList<String>());
		}

		this.ftpClient.changeWorkingDirectory(remotePath);
		FTPFile[] ftpFiles = this.ftpClient.listFiles();
		if (!ArrayUtils.isEmpty(ftpFiles)) {
			FtpStatus downStatus = FtpStatus.DOWNLOAD_FILE_FAIL;
			for (FTPFile ftpFile : ftpFiles) {
				String fileName = ftpFile.getName();
				String newRemotePath = remotePath + "/" + fileName;
				String newLocalPath = localPath + SEPARATOR + fileName;
				if (ftpFile.isFile()) {
					downStatus = downloadFile(newLocalPath, newRemotePath);
					String downloadMessage = newLocalPath + "<<--"
							+ newRemotePath;
					if (downStatus == FtpStatus.DOWNLOAD_FILE_SUCCESS) {
						messages.get("success").add(downloadMessage);

					} else {
						messages.get("fail").add(downloadMessage);

					}
				} else {
					File file = new File(newLocalPath);
					// 如果本地不存在远程对应的目录，则创建目录
					if (file != null && !file.exists())
						file.mkdirs();
					// 通过递归下载该目录下所有文件
					downloadFiles(newLocalPath, newRemotePath, messages);
					this.ftpClient.changeWorkingDirectory(remotePath);
				}
			}
		}
		return messages;
	}

	/**
	 * 从服务器批量下载文件
	 * 
	 * @param localPath
	 * @param remotePath
	 * @return
	 * @throws IOException
	 */
	public String downloadFiles(String localPath, String remotePath)
			throws IOException {
		Map<String, Collection<String>> downloadMessages = downloadFiles(
				localPath, remotePath, null);
		String result = "共下载文件0个";
		if (downloadMessages != null) {
			int successCount = downloadMessages.get("success").size();
			int failCount = downloadMessages.get("fail").size();
			result = "共下载文件" + (successCount + failCount) + "个，其中成功下载文件"
					+ successCount + "个，失败下载文件" + failCount + "个";
		}
		logger.info(result);
		return result;

	}

	/**
	 * 下载文件
	 * 
	 * @param localFilePath
	 * @param remoteFilePath
	 * @return
	 * @throws IOException
	 */
	public FtpStatus downloadFile(String localFilePath, String remoteFilePath)
			throws IOException {
		// logger.info(remoteFilePath+" 当前正在下载");
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
			long process = localSize * 100 / lRemoteSize;
			int c;
			while ((c = in.read(bytes)) != -1) {
				out.write(bytes, 0, c);
				localSize += c;
				long nowProcess = localSize * 100 / lRemoteSize;
				if (nowProcess > process) {
					process = nowProcess;
					if (process % 10 == 0) {
						logger.info(remoteFilePath + " 文件下载进度" + process + "%");
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
				long nowProcess = localSize * 100 / lRemoteSize;
				if (nowProcess > process) {
					process = nowProcess;
					if (process % 10 == 0) {
						logger.info(remoteFilePath + " 文件下载进度" + process + "%");
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
	 * 
	 * @param remoteFile
	 * @param localFile
	 * @param ftpClient
	 * @param remoteSize
	 * @return
	 * @throws IOException
	 */
	public FtpStatus uploadFile(String remoteFile, File localFile,
			long remoteSize) throws Exception {
		FtpStatus status;

		long process = 0;
		long localreadbytes = 0L;
		RandomAccessFile raf = new RandomAccessFile(localFile, "r");
		OutputStream out = ftpClient.appendFileStream(remoteFile);
		if (remoteSize > 0) {
			ftpClient.setRestartOffset(remoteSize);
			process = remoteSize * 100 / localFile.length();
			raf.seek(remoteSize);
			localreadbytes = remoteSize;
		}
		byte[] bytes = new byte[1024];
		int c;
		while ((c = raf.read(bytes)) != -1) {
			out.write(bytes, 0, c);
			localreadbytes += c;
			if (localreadbytes * 100 / localFile.length() != process) {
				process = localreadbytes * 100 / localFile.length();
				logger.info(localFile + " 文件上传进度" + process + "%");
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
	 * 
	 * @param remote
	 * @param ftpClient
	 * @return
	 * @throws IOException
	 */
	public FtpStatus createDirectory(String remote, FTPClient ftpClient)
			throws IOException {
		FtpStatus resutlStatus = FtpStatus.DIRECTORY_EXIST;
		// 如果当前远程路径的最后一个目录不存在，则创建该目录
		if (!ftpClient.changeWorkingDirectory(remote)) {
			int start = 0;
			int end = remote.lastIndexOf("/");
			String parentPath = remote.substring(start, end);
			ftpClient.changeWorkingDirectory(parentPath);
			String newDir = remote.substring(end + 1);
			if (ftpClient.makeDirectory(newDir)) {
				ftpClient.changeWorkingDirectory(remote);
				resutlStatus = FtpStatus.CREATE_DIRECTORY_SUCCESS;
			} else {
				resutlStatus = FtpStatus.CREATE_DIRECTORY_FAIL;
			}

		}
		return resutlStatus;
	}

	/**
	 * 初始化FtpClient对象
	 * 
	 * @return
	 * @throws IOException
	 */
	public FTPClient initFtpClient() throws IOException {
		FTPClient ftp = new FTPClient();
		try {
			ftp.setDataTimeout(7200);
			ftp.setControlEncoding(DEAFULT_REMOTE_CHARSET);
			ftp.setDefaultPort(DEAFULT_REMOTE_PORT);
			ftp.setListHiddenFiles(false);
			if (StringUtils.isNotEmpty(ftpPort)
					&& NumberUtils.isDigits(ftpPort)) {
				ftp.connect(host, Integer.valueOf(ftpPort));
			} else {
				ftp.connect(host);
			}
		} catch (ConnectException e) {
			logger.error("ftp连接失败：" + ftp.getReplyString() + ftp.getReplyCode());
			throw new IOException("Problem connecting the FTP-server fail", e);
		}
		int reply = ftp.getReplyCode();

		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
		}
		if (!ftp.login(this.ftpUserName, this.ftpPassword)) {
			ftp.quit();
			ftp.disconnect();
			logger.error("ftp登陆失败" + ftp.getReplyString());
			throw new IOException("Cant Authentificate to FTP-Server");
		}

		ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		return ftp;

	}

}
