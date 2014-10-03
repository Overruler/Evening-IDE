package org.apache.commons.net.bsd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.SocketClient;

public class RExecClient extends SocketClient {
	public static final int DEFAULT_PORT = 512;
	protected static final char NULL_CHAR = 0;

	public InputStream getInputStream() {
		return null;
	}
	public OutputStream getOutputStream() {
		return null;
	}
	public void rexec(String username, String password, String command) throws IOException {}
}
