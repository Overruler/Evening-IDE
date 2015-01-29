package javax.mail.internet;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;

public class MimeBodyPart extends BodyPart implements MimePart {
	@Override
	public void setDataHandler(DataHandler dataHandler) throws MessagingException {}
	@Override
	public void addHeader(String name, String value) throws MessagingException {}
	@Override
	public void setFileName(String filename) throws MessagingException {}
	@Override
	public void setContent(Multipart mp) throws MessagingException {}
	public void setText(String string, String charsetEncoding, String subType) {}
	public void setContent(String string, String contentType) {}
}
