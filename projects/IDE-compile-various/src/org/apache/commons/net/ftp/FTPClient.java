package org.apache.commons.net.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FTPClient extends FTP implements Configurable {
	public String printWorkingDirectory() throws IOException {
		return null;
	}
	public boolean changeWorkingDirectory(String cwd) throws IOException {
		return false;
	}
	public FTPFile[] listFiles() throws IOException {
		return null;
	}
	public boolean makeDirectory(String name) throws IOException {
		return false;
	}
	public boolean sendSiteCommand(String theCMD) throws IOException {
		return false;
	}
	public String[] getReplyStrings() throws IOException {
		return null;
	}
	public boolean deleteFile(String resolveFile) throws IOException {
		return false;
	}
	public boolean removeDirectory(String resolveFile) throws IOException {
		return false;
	}
	public boolean changeToParentDirectory() throws IOException {
		return false;
	}
	public String getReplyString() {
		return null;
	}
	public boolean storeFile(String remote, InputStream local) throws IOException {
		return false;
	}
	public int getReplyCode() {
		return 0;
	}
	public FTPFile[] listFiles(String name) throws IOException {
		return null;
	}
	public boolean retrieveFile(String resolveFile, OutputStream outstream) throws IOException {
		return false;
	}
	public void setRemoteVerificationEnabled(boolean enableRemoteVerification) {}
	public boolean login(String userid, String password, String account) throws IOException {
		return false;
	}
	public boolean login(String userid, String password) throws IOException {
		return false;
	}
	public boolean setFileType(int binaryFileType) throws IOException {
		return false;
	}
	public void enterLocalPassiveMode() {}
	@Override
	public void configure(FTPClientConfig config) {}
	public boolean logout() throws IOException {
		return false;
	}
	public void disconnect() throws IOException {}
}
