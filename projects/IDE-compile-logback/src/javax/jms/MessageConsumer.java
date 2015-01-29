package javax.jms;

public interface MessageConsumer {
	public void setMessageListener(MessageListener jmsQueueSink) throws JMSException;
}
