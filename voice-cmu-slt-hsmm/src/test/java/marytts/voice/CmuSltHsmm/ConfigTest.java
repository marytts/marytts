/**
 * Copyright 2011 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * <p>
 * This file is part of MARY TTS.
 * <p>
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package marytts.voice.CmuSltHsmm;

import marytts.config.MaryConfig;
import marytts.config.VoiceConfig;
import marytts.exceptions.MaryConfigurationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author marc
 */
public class ConfigTest {

    private static final String voiceName = "cmu-slt-hsmm";

    @Test
    public void isVoiceConfig() throws MaryConfigurationException {
        MaryConfig m = new CmuSltHsmmConfig();
        Assert.assertTrue(m.isVoiceConfig());
    }

    @Test
    public void hasRightName() throws MaryConfigurationException {
        VoiceConfig m = new CmuSltHsmmConfig();
        Assert.assertEquals(voiceName, m.getName());
    }

    @Test
    public void canGetByName() throws MaryConfigurationException {
        VoiceConfig m = MaryConfig.getVoiceConfig(voiceName);
        Assert.assertNotNull(m);
        Assert.assertEquals(voiceName, m.getName());
    }

    @Test
    public void hasVoiceConfigs() throws MaryConfigurationException {
        Assert.assertTrue(MaryConfig.countVoiceConfigs() > 0);
    }

    @Test
    public void hasVoiceConfigs2() throws MaryConfigurationException {
        Iterable<VoiceConfig> vcs = MaryConfig.getVoiceConfigs();
        Assert.assertNotNull(vcs);
        Assert.assertTrue(vcs.iterator().hasNext());
    }

}
