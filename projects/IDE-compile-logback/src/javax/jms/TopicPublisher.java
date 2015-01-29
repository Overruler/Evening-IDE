package javax.jms;

public interface TopicPublisher extends MessageProducer {
	void publish(Message msg) throws JMSException;
}
