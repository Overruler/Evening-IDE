package org.apache.commons.logging;

public interface Log {
	boolean isTraceEnabled();
	void error(String message);
	void error(String message, Throwable t);
	void warn(String message);
	void warn(String message, Throwable t);
	void info(String message, Throwable t);
	void debug(String message);
	void info(String message);
}
