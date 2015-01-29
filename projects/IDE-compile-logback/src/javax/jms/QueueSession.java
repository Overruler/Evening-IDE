package javax.jms;


public interface QueueSession extends Session {
	public QueueSender createSender(Queue queue);
}
