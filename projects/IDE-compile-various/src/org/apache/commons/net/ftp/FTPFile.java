package org.apache.commons.net.ftp;

import java.util.Calendar;

public class FTPFile {
	public String getName() {
		return null;
	}
	public boolean isSymbolicLink() {
		return false;
	}
	public boolean isDirectory() {
		return false;
	}
	public boolean isFile() {
		return false;
	}
	public String getLink() {
		return null;
	}
	public Calendar getTimestamp() {
		return null;
	}
	public long getSize() {
		return 0;
	}
}
