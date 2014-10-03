package org.apache.oro.text.regex;

public interface Substitution {
	public void appendSubstitution(
		java.lang.StringBuffer appendBuffer,
		MatchResult match,
		int substitutionCount,
		PatternMatcherInput originalInput,
		PatternMatcher matcher,
		Pattern pattern);
}
