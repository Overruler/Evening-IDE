package org.apache.xalan.trace;

public interface TraceListenerEx3 extends TraceListenerEx2 {
	public void extension(ExtensionEvent ee);
	public void extensionEnd(ExtensionEvent ee);
}
