package marytts.language.en;

import java.io.IOException;
import java.util.Locale;

import marytts.config.LanguageConfig;
import marytts.exceptions.MaryConfigurationException;


public class EnglishConfig extends LanguageConfig {
	public EnglishConfig() throws MaryConfigurationException {
		super(EnglishConfig.class.getResourceAsStream("en.config"));
	}
}
