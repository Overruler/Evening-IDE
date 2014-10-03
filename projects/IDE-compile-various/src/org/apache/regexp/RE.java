package org.apache.regexp;

public class RE {
	public static final int MATCH_NORMAL = 0;
	public static final int MATCH_CASEINDEPENDENT = 1;
	public static final int MATCH_MULTILINE = 2;
	public static final int MATCH_SINGLELINE = 4;
	public static final int REPLACE_ALL = 0;
	public static final int REPLACE_FIRSTONLY = 1;
	public static final int REPLACE_BACKREFERENCES = 2;

	public RE(String pattern) throws RESyntaxException {}
	public void setMatchFlags(int cOptions) {}
	public boolean match(String input) {
		return false;
	}
	public int getParenCount() {
		return 0;
	}
	public String getParen(int i) {
		return null;
	}
	public String subst(String input, String argument, int sOptions) {
		return null;
	}
}
