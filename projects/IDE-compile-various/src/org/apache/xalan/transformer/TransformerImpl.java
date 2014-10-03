package org.apache.xalan.transformer;

import java.util.Properties;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.apache.xalan.trace.TraceManager;

public class TransformerImpl extends Transformer {
	@Override
	public void transform(Source xmlSource, Result outputTarget) throws TransformerException {}
	@Override
	public void setParameter(String name, Object value) {}
	@Override
	public Object getParameter(String name) {
		return null;
	}
	@Override
	public void clearParameters() {}
	@Override
	public void setURIResolver(URIResolver resolver) {}
	@Override
	public URIResolver getURIResolver() {
		return null;
	}
	@Override
	public void setOutputProperties(Properties oformat) {}
	@Override
	public Properties getOutputProperties() {
		return null;
	}
	@Override
	public void setOutputProperty(String name, String value) throws IllegalArgumentException {}
	@Override
	public String getOutputProperty(String name) throws IllegalArgumentException {
		return null;
	}
	@Override
	public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {}
	@Override
	public ErrorListener getErrorListener() {
		return null;
	}
	public TraceManager getTraceManager() {
		return null;
	}
}
