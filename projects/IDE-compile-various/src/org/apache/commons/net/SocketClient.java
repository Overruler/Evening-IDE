package org.apache.commons.net;

import java.io.IOException;
import java.net.SocketException;

public class SocketClient {
	public void connect(String server, int port) throws SocketException, IOException {}
	public void disconnect() throws IOException {}
	public boolean isConnected() {
		return false;
	}
}
