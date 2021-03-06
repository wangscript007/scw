package scw.util.placeholder;

public interface ConfigurablePlaceholderReplacer extends PlaceholderReplacer{
	void addPlaceholderReplacer(PlaceholderReplacer placeholderReplacer);
	
	String replaceRequiredPlaceholders(String value, PlaceholderResolver placeholderResolver) throws IllegalArgumentException;
}
