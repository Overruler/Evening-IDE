package org.apache.xalan.trace;

import java.io.PrintWriter;
import javax.xml.transform.TransformerException;

public class PrintTraceListener implements TraceListenerEx3 {
	public boolean m_traceElements;
	public boolean m_traceGeneration;
	public boolean m_traceExtension;
	public boolean m_traceSelection;
	public boolean m_traceTemplates;

	public PrintTraceListener(PrintWriter w) {}
	@Override
	public void traceEnd(TracerEvent ev) {}
	@Override
	public void selectEnd(EndSelectionEvent ev) throws TransformerException {}
	@Override
	public void extension(ExtensionEvent ee) {}
	@Override
	public void extensionEnd(ExtensionEvent ee) {}
}
