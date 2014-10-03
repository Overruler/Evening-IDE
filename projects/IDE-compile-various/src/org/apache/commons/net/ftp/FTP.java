package org.apache.commons.net.ftp;

import org.apache.commons.net.SocketClient;

public class FTP extends SocketClient {
	public static final int ASCII_FILE_TYPE = 0;
	public static final int BINARY_FILE_TYPE = 2;
	public static final int BLOCK_TRANSFER_MODE = 11;
	public static final int CARRIAGE_CONTROL_TEXT_FORMAT = 6;
	public static final int COMPRESSED_TRANSFER_MODE = 12;
	public static final String DEFAULT_CONTROL_ENCODING = "ISO-8859-1";
	public static final int DEFAULT_DATA_PORT = 20;
	public static final int DEFAULT_PORT = 21;
	public static final int EBCDIC_FILE_TYPE = 1;
	public static final int FILE_STRUCTURE = 7;
	public static final int LOCAL_FILE_TYPE = 3;
	public static final int NON_PRINT_TEXT_FORMAT = 4;
	public static final int PAGE_STRUCTURE = 9;
	public static final int RECORD_STRUCTURE = 8;
	public static final int REPLY_CODE_LEN = 3;
	public static final int STREAM_TRANSFER_MODE = 10;
	public static final int TELNET_TEXT_FORMAT = 5;
}
