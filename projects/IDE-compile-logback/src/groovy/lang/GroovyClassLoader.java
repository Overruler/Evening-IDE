package groovy.lang;

import org.codehaus.groovy.control.CompilationFailedException;

public class GroovyClassLoader {
	public GroovyClassLoader(ClassLoader classLoader) {}
	public Class parseClass(String scriptText) throws CompilationFailedException {
		throw new CompilationFailedException();
	}
}
