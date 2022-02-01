package com._4point.aem.formsfeeder.server.support;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class MultiLineMatchers {
	/**
	 * Define two custom matchers to test the list of strings that we get back as part of the Utils.js tests.
	 * 
	 * I originally designed these to take streams, however the Hamcrest matchers evaluate failed matches twice
	 * (once to see if it matches and, if it fails, once again to get the description.  This seems dumb to me,
	 * however it is what it is.  it is described here: https://github.com/hamcrest/JavaHamcrest/issues/230
	 * 
	 * The prescribed answer is to collect the Stream first (which defeats the reason for having a stream,
	 * but I digress).
	 * 
	 * My current approach is not very efficient (lots of stream creation) however I prefer to keep it the way it is
	 * in case the Hamcrest implementation changes so that we can switch back if that happens.  It's probably efficient 
	 * enough for tests though.
	 * 
	 */
	public static TypeSafeDiagnosingMatcher<List<String>> anyLineMatches(Matcher<String> matcher) {
		return new AnyLineMatches(matcher);
	}
	
	private static class AnyLineMatches extends TypeSafeDiagnosingMatcher<List<String>> {

		private final Matcher<String> expectedMatcher;
		
		public AnyLineMatches(Matcher<String> expectedMatcher) {
			this.expectedMatcher = expectedMatcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("One or more lines should match '")
						.appendDescriptionOf(expectedMatcher)
						.appendText("'");
			
		}

		@Override
		protected boolean matchesSafely(List<String> list, Description mismatchDescription) {
			Optional<String> first = list.stream().filter(expectedMatcher::matches).findAny();
			if (first.isEmpty()) {
				mismatchDescription.appendText("none matched.");
			}
			return first.isPresent();
		}
	}

	public static TypeSafeDiagnosingMatcher<List<String>> allLinesMatch(Matcher<String> matcher) {
		return new AllLineMatches(matcher);
	}
	
	private static class AllLineMatches extends TypeSafeDiagnosingMatcher<List<String>> {

		private final Matcher<String> expectedMatcher;
		
		public AllLineMatches(Matcher<String> expectedMatcher) {
			this.expectedMatcher = expectedMatcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("All lines should match '")
						.appendDescriptionOf(expectedMatcher)
						.appendText("'");
			
		}

		@Override
		protected boolean matchesSafely(List<String> list, Description mismatchDescription) {
			Optional<String> first = list.stream().filter(Predicate.not(expectedMatcher::matches)).findAny();
			if (first.isPresent()) {
				mismatchDescription.appendText("failed to match '")
								   .appendText(first.get())
								   .appendText("'");
			}
			return first.isEmpty();
		}
	}


}
