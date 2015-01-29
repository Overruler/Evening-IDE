package javax.mail.internet;

import javax.mail.Address;

public class InternetAddress extends Address implements Cloneable {
	public InternetAddress(String address) throws AddressException {}
	public InternetAddress(String address, String name) throws AddressException {}
	public static InternetAddress[] parse(String emailAdrr, boolean b) throws AddressException {
		throw new AddressException();
	}
}
