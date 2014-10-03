package org.apache.xalan.trace;

import javax.xml.transform.TransformerException;

public interface TraceListenerEx extends TraceListener {
	public void selectEnd(EndSelectionEvent ev) throws TransformerException;
}
