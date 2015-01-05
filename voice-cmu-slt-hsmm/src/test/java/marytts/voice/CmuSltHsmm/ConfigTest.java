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
package marytts.voice.CmuSltHsmm;

import static org.junit.Assert.*;

import java.util.Set;

import marytts.config.MaryConfig;
import marytts.config.VoiceConfig;
import marytts.exceptions.MaryConfigurationException;

import org.junit.Test;

/**
 * @author marc
 *
 */
public class ConfigTest {
	private static final String voiceName = "cmu-slt-hsmm";

	@Test
	public void isNotMainConfig() throws MaryConfigurationException {
		MaryConfig m = new Config();
		assertFalse(m.isMainConfig());
	}

	@Test
	public void isVoiceConfig() throws MaryConfigurationException {
		MaryConfig m = new Config();
		assertTrue(m.isVoiceConfig());
	}

	@Test
	public void hasRightName() throws MaryConfigurationException {
		VoiceConfig m = new Config();
		assertEquals(voiceName, m.getName());
	}

	@Test
	public void canGetByName() throws MaryConfigurationException {
		VoiceConfig m = MaryConfig.getVoiceConfig(voiceName);
		assertNotNull(m);
		assertEquals(voiceName, m.getName());
	}

	@Test
	public void hasVoiceConfigs() throws MaryConfigurationException {
		assertTrue(MaryConfig.countVoiceConfigs() > 0);
	}

	@Test
	public void hasVoiceConfigs2() throws MaryConfigurationException {
		Iterable<VoiceConfig> vcs = MaryConfig.getVoiceConfigs();
		assertNotNull(vcs);
		assertTrue(vcs.iterator().hasNext());
	}

}
