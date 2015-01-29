package javax.jms;

public interface TopicSession extends Session {
	TopicPublisher createPublisher(Topic topic) throws JMSException;
	TopicSubscriber createSubscriber(Topic topic) throws JMSException;
}
