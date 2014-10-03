package javax.mail;

public class SendFailedException extends MessagingException {
	public Address[] getValidSentAddresses() {
		return null;
	}
	public Address[] getValidUnsentAddresses() {
		return null;
	}
	public Address[] getInvalidAddresses() {
		return null;
	}
}
