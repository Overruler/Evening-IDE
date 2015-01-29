package javax.mail;

import javax.mail.internet.MimeMessage;

public abstract class Transport extends Service {
	public abstract void sendMessage(Message msg, Address[] addresses) throws MessagingException;
	public static void send(MimeMessage mimeMsg) {}
}
