package javax.mail;

public abstract class Transport extends Service {
	public abstract void sendMessage(Message msg, Address[] addresses) throws MessagingException;
}
