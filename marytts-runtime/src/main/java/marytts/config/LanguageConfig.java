/**
 * 
 */
package marytts.config;

import java.util.Locale;

/**
 * @author marc
 *
 */
public class LanguageConfig extends MaryConfig {

	private Locale locale;
	
	protected LanguageConfig(Locale locale) {
		this.locale = locale;
	}
	
	@Override
	public boolean isLanguageConfig() {
		return true;
	}
	
	public Locale getLocale() {
		return locale;
	}
}
