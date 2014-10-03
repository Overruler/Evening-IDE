package com.sun.media.jai.codec;

import java.io.File;
import java.io.IOException;

public class FileSeekableStream extends SeekableStream {
	public FileSeekableStream(File file) {}
	@Override
	public void readFully(byte[] b) throws IOException {}
	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {}
	@Override
	public int skipBytes(int n) throws IOException {
		return 0;
	}
	@Override
	public boolean readBoolean() throws IOException {
		return false;
	}
	@Override
	public byte readByte() throws IOException {
		return 0;
	}
	@Override
	public int readUnsignedByte() throws IOException {
		return 0;
	}
	@Override
	public short readShort() throws IOException {
		return 0;
	}
	@Override
	public int readUnsignedShort() throws IOException {
		return 0;
	}
	@Override
	public char readChar() throws IOException {
		return 0;
	}
	@Override
	public int readInt() throws IOException {
		return 0;
	}
	@Override
	public long readLong() throws IOException {
		return 0;
	}
	@Override
	public float readFloat() throws IOException {
		return 0;
	}
	@Override
	public double readDouble() throws IOException {
		return 0;
	}
	@Override
	public String readLine() throws IOException {
		return null;
	}
	@Override
	public String readUTF() throws IOException {
		return null;
	}
	@Override
	public int read() throws IOException {
		return 0;
	}
}
