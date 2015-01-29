package javax.jms;

public interface TopicConnectionFactory {
	TopicConnection createTopicConnection(String userName, String password) throws JMSException;
	TopicConnection createTopicConnection() throws JMSException;
}
