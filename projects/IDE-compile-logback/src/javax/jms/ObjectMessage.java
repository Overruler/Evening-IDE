package javax.jms;

import java.io.Serializable;

public interface ObjectMessage extends Message {
	public Serializable getObject() throws JMSException;
	public void setObject(Serializable so) throws JMSException;
}
