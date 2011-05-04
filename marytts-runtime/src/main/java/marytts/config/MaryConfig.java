/**
 * Copyright 2011 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
	
	
	public static LanguageConfig getLanguageConfig(Locale locale) {
		for (MaryConfig mc : configLoader) {
			if (mc.isLanguageConfig()) {
				LanguageConfig lc = (LanguageConfig) mc;
				if (lc.getLocales().contains(locale)) {
					return lc;
				}
			}
		}
		return null;
	}
	
	public static VoiceConfig getVoiceConfig(String voiceName) {
		for (MaryConfig mc : configLoader) {
			if (mc.isVoiceConfig()) {
				VoiceConfig vc = (VoiceConfig) mc;
				if (vc.getName().equals(voiceName)) {
					return vc;
				}
			}
		}
		return null;
	}
	
	public static Iterable<MaryConfig> getConfigs() {
		return configLoader;
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
	
	public boolean isVoiceConfig() {
		return false;
	}
	
	public Properties getProperties() {
		return props;
	}
	
}
