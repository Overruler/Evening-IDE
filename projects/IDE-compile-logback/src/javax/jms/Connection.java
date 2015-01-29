package javax.jms;

public interface Connection {
	void start() throws JMSException;
	void close() throws JMSException;
}
