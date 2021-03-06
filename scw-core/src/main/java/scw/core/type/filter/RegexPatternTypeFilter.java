package scw.core.type.filter;

import java.util.regex.Pattern;

import scw.core.Assert;
import scw.core.type.ClassMetadata;

/**
 * A simple filter for matching a fully-qualified class name with a regex {@link Pattern}.
 */
public class RegexPatternTypeFilter extends AbstractClassTestingTypeFilter {

	private final Pattern pattern;


	public RegexPatternTypeFilter(Pattern pattern) {
		Assert.notNull(pattern, "Pattern must not be null");
		this.pattern = pattern;
	}


	@Override
	protected boolean match(ClassMetadata metadata) {
		return this.pattern.matcher(metadata.getClassName()).matches();
	}

}
