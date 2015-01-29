package javax.jms;

public interface TopicConnection extends Connection {
	TopicSession createTopicSession(boolean b, int autoAcknowledge) throws JMSException;
}
