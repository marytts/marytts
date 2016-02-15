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
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.mockito.Mockito;

public class UnitSelectionIT {

	MaryInterface mary;

	@Before
	public void setUp() throws Exception {
		mary = new LocalMaryInterface();
	}

	@Test
	public void canReadUnitSelectionConfig() throws Exception {
		assertThat(marytts.config.MaryConfig.getConfigs(), hasItem(isA(UnitSelectionConfig.class)));
	}
	
	@Test
	public void canReadUnitSelectionConfig2() throws Exception {	
		assertNotNull(MaryConfig.getSynthesisConfig("unitselection"));
	}
	
	/**
	@Test
	public void canLoadVoice() throws Exception {
		Voice voice = new UnitSelectionVoice(mockedVoiceConfig.getName(), null);
		assertNotNull(voice);
	}
	**/
	
	/**
	@Test
	public void canSetVoice() throws Exception {
		UnitSelectionVoice mockedVoice = Mockito.mock(UnitSelectionVoice.class);
		when(mockedVoice.getName()).thenReturn("fnord");
		
		mary.setVoice(mockedVoice.getName());
		assertEquals("fnord", mary.getVoice());
		
	}
	**/
	
	/**
	@Test
	public void canProcessTextToSpeech() throws Exception {
		mary.setVoice(mockedVoiceConfig.getName());
		AudioInputStream audio = mary.generateAudio("Hello world");
		assertNotNull(audio);
	}
	**/
}
