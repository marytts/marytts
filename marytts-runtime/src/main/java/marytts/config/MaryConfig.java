/**
 * 
 */
package marytts.config;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.ServiceLoader;

import marytts.exceptions.MaryConfigurationException;

/**
 * @author marc
 *
 */
public abstract class MaryConfig {
	private static final ServiceLoader<MaryConfig> configLoader = ServiceLoader.load(MaryConfig.class);
	
	public static int countConfigs() {
		int num = 0;
		for (@SuppressWarnings("unused") MaryConfig mc : configLoader) {
			num++;
		}
		return num;
	}
	
	public static int countLanguageConfigs() {
		int num = 0;
		for (MaryConfig mc : configLoader) {
			if (mc.isLanguageConfig()) {
				num++;
			}
		}
		return num;
	}

	public static MaryConfig getMainConfig() {
		for (MaryConfig mc : configLoader) {
			if (mc.isMainConfig()) {
				return mc;
			}
		}
		return null;
	}
	
	
	public static MaryConfig getLanguageConfig(Locale locale) {
		for (MaryConfig mc : configLoader) {
			if (mc.isLanguageConfig()) {
				LanguageConfig lc = (LanguageConfig) mc;
				if (lc.getLocales().contains(locale)) {
					return mc;
				}
			}
		}
		return null;
	}
	
	
	
	
	//////////// Non-static / base class methods //////////////
	
	private Properties props;
	
	protected MaryConfig(InputStream propertyStream) throws MaryConfigurationException {
		props = new Properties();
		try {
			props.load(propertyStream);
		} catch (Exception e) {
			throw new MaryConfigurationException("cannot load properties", e);
		}
	}
	
	public boolean isMainConfig() {
		return false;
	}
	
	public boolean isLanguageConfig() {
		return false;
	}
	
	public Properties getProperties() {
		return props;
	}
	
}
