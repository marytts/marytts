/**
 * 
 */
package marytts.config;

import java.util.Locale;
import java.util.ServiceLoader;

import marytts.util.MaryUtils;

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
				Locale general = lc.getLocale();
				Locale specific = locale;
				if (MaryUtils.subsumes(general, specific)) {
					return mc;
				}
			}
		}
		return null;
	}
	
	
	
	
	//////////// Non-static / base class methods //////////////
	
	public boolean isMainConfig() {
		return false;
	}
	
	public boolean isLanguageConfig() {
		return false;
	}
	
}
