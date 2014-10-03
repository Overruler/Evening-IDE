package javax.mail;

import javax.activation.DataHandler;

public interface Part {
	void addHeader(String name, String value) throws MessagingException;
	void setDataHandler(DataHandler dataHandler) throws MessagingException;
	void setFileName(String filename) throws MessagingException;
	void setContent(Multipart mp) throws MessagingException;
}
