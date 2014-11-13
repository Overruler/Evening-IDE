package org.apache.xml.resolver;

public class Version {
	public static String getVersion() {
		return getProduct() + " " + getVersionNum();
	}
	public static String getProduct() {
		return "XmlResolver";
	}
	public static java.lang.String getVersionNum() {
		return "1.2";
	}
	public static void main(java.lang.String[] argv) {
		System.out.println(getVersion());
	}
}