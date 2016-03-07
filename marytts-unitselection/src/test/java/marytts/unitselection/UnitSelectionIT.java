package marytts.unitselection;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.modules.synthesis.Voice;
import marytts.config.LanguageConfig;
import marytts.config.SynthesisConfig;
import marytts.config.VoiceConfig;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.DomUtils;
import marytts.config.MaryConfig;
import static org.hamcrest.CoreMatchers.*;

import org.testng.Assert;
import org.testng.annotations.*;

import org.mockito.Mockito;

public class UnitSelectionIT {

	MaryInterface mary;

	@BeforeMethod
	public void setUp() throws Exception {
		mary = new LocalMaryInterface();
	}

	@Test
	public void canReadUnitSelectionConfig() throws Exception {
        boolean us_config_found = false;
        for (Object o:marytts.config.MaryConfig.getConfigs())
        {
            if (o instanceof UnitSelectionConfig)
            {
                us_config_found = true;
                break;
            }
        }

        Assert.assertTrue(us_config_found);
    }

    @Test
public void canReadUnitSelectionConfig2() throws Exception {
    Assert.assertNotNull(MaryConfig.getSynthesisConfig("unitselection"));
	}

	/**
       @Test
       public void canLoadVoice() throws Exception {
       Voice voice = new UnitSelectionVoice(mockedVoiceConfig.getName(), null);
       Assert.assertNotNull(voice);
       }
	**/

	/**
       @Test
       public void canSetVoice() throws Exception {
       UnitSelectionVoice mockedVoice = Mockito.mock(UnitSelectionVoice.class);
       when(mockedVoice.getName()).thenReturn("fnord");

       mary.setVoice(mockedVoice.getName());
       Assert.assertEquals("fnord", mary.getVoice());

       }
	**/

	/**
       @Test
       public void canProcessTextToSpeech() throws Exception {
       mary.setVoice(mockedVoiceConfig.getName());
       AudioInputStream audio = mary.generateAudio("Hello world");
       Assert.assertNotNull(audio);
       }
	**/
}
