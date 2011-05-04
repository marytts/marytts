/**
 * 
 */
package marytts.config;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;

/**
 * @author marc
 *
 */
public class LanguageConfig extends MaryConfig {

	private Set<Locale> locales = new HashSet<Locale>();
	
	public LanguageConfig(InputStream propertyStream) throws MaryConfigurationException {
		super(propertyStream);
		String localeProp = getProperties().getProperty("locale");
		if (localeProp == null) {
			throw new MaryConfigurationException("property stream does not contain a locale property");
		}
		for (StringTokenizer st = new StringTokenizer(localeProp); st.hasMoreTokens() ; ) {
			String localeString = st.nextToken();
			locales.add(MaryUtils.string2locale(localeString));
		}
		if (locales.isEmpty()) {
			throw new MaryConfigurationException("property stream does not define any locale");
		}
	}
	
	@Override
	public boolean isLanguageConfig() {
		return true;
	}
	
	public Set<Locale> getLocales() {
		return locales;
	}
}
