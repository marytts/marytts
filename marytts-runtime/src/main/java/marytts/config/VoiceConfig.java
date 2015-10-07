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

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;

/**
 * @author marc
 *
 */
public class VoiceConfig extends MaryConfig {

	public VoiceConfig(InputStream propertyStream) throws MaryConfigurationException {
		super(propertyStream);
		if (getName() == null) {
			throw new MaryConfigurationException("Voice does not have a name");
		}
		if (getLocale() == null) {
			throw new MaryConfigurationException("Voice '" + getName() + "' does not have a locale");
		}
	}

	@Override
	public boolean isVoiceConfig() {
		return true;
	}

	/**
	 * The voice's name. Guaranteed not to be null.
	 * 
	 * @return getProperties().getProperty("name")
	 */
	public String getName() {
		return getProperties().getProperty("name");
	}

	/**
	 * The voice's locale. Guaranteed not to be null.
	 * 
	 * @return null if localeString is null, return MaryUtils.string2locale(localeString) otherwise
	 */
	public Locale getLocale() {
		String localeString = getProperties().getProperty("locale");
		if (localeString == null) {
			return null;
		}
		return MaryUtils.string2locale(localeString);
	}
}
