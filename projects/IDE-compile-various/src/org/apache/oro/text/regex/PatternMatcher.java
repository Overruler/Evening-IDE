package org.apache.oro.text.regex;

public interface PatternMatcher {
	public boolean contains(String input, Pattern p);
	public MatchResult getMatch();
}
