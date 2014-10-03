package org.apache.avalon.framework.logger;

public class Logger {
	public Logger getChildLogger(String name) {
		return null;
	}
	public boolean isDebugEnabled() {
		return false;
	}
	public void debug(String valueOf, Throwable t) {}
	public void debug(String valueOf) {}
	public boolean isErrorEnabled() {
		return false;
	}
	public void error(String valueOf, Throwable t) {}
	public void error(String valueOf) {}
	public boolean isFatalErrorEnabled() {
		return false;
	}
	public void fatalError(String valueOf, Throwable t) {}
	public void fatalError(String valueOf) {}
	public boolean isInfoEnabled() {
		return false;
	}
	public void info(String valueOf, Throwable t) {}
	public void info(String valueOf) {}
	public boolean isWarnEnabled() {
		return false;
	}
	public void warn(String valueOf, Throwable t) {}
	public void warn(String valueOf) {}
}
