package javax.jms;

public interface QueueConnection extends Connection {
	public QueueSession createQueueSession(boolean b, int autoAcknowledge);
	public void start();
	public void close();
}
