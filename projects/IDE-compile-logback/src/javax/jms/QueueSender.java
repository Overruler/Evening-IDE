package javax.jms;

public interface QueueSender {
	void send(Message msg) throws JMSException;
}
