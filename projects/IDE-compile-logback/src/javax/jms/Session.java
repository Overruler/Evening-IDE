package javax.jms;

import java.io.Serializable;

public interface Session extends Runnable {
	public static final int AUTO_ACKNOWLEDGE = 1;
	public static final int CLIENT_ACKNOWLEDGE = 2;
	public static final int DUPS_OK_ACKNOWLEDGE = 3;
	public static final int SESSION_TRANSACTED = 0;

	public MessageConsumer createConsumer(Queue queue) throws JMSException;
	public ObjectMessage createObjectMessage(Serializable object) throws JMSException;
	public ObjectMessage createObjectMessage() throws JMSException;
	public void close() throws JMSException;
}
