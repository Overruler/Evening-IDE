package javax.mail.internet;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;

public class MimeMessage extends Message {
	public MimeMessage(Session sesh) {}
	public void setFrom(Address address) throws MessagingException {}
	public void setReplyTo(Address[] internetAddresses) throws MessagingException {}
	public void setRecipients(RecipientType to, Address[] internetAddresses) throws MessagingException {}
	public void setSubject(String subject, String charset) throws MessagingException {}
	@Override
	public void setSubject(String subject) throws MessagingException {}
	public void addHeader(String string, String date) throws MessagingException {}
	@Override
	public void setDataHandler(DataHandler dataHandler) throws MessagingException {}
	@Override
	public void setFileName(String filename) throws MessagingException {}
	@Override
	public void setContent(Multipart mp) throws MessagingException {}
	@Override
	public Address[] getAllRecipients() throws MessagingException {
		return null;
	}
}
