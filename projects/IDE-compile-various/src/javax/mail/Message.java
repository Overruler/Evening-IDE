package javax.mail;

import java.io.Serializable;

public abstract class Message implements Part {
	public static class RecipientType implements Serializable {
		public static final Message.RecipientType TO = null;
		public static final Message.RecipientType CC = null;
		public static final Message.RecipientType BCC = null;
	}

	public void setFrom(Address address) throws MessagingException {}
	public void setReplyTo(Address[] internetAddresses) throws MessagingException {}
	public abstract void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException;
	public abstract void setSubject(String subject) throws MessagingException;
	public Address[] getAllRecipients() throws MessagingException {
		return null;
	}
}
