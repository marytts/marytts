package marytts.voice.${PACKAGE};

import static org.junit.Assert.assertNotNull;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryRuntimeUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class LoadVoiceIT {
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		MaryRuntimeUtils.ensureMaryStarted();
	}
	
    @Test
    public void canLoadVoice() throws Exception {
    	Config config = new Config();
        Voice voice = new ${VOICECLASS}(config.getName(), null);
        assertNotNull(voice);
    }
}
