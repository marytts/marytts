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
public class SynthesisConfig extends MaryConfig {

	public SynthesisConfig(InputStream propertyStream) throws MaryConfigurationException {
		super(propertyStream);
	}

	@Override
	public boolean isSynthesisConfig() {
		return true;
	}
}
