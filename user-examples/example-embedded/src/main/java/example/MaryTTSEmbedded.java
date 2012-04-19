package example;
import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.config.MaryConfig;
import marytts.config.VoiceConfig;
import marytts.util.data.audio.AudioPlayer;


public class MaryTTSEmbedded {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		MaryInterface marytts = new LocalMaryInterface();
		VoiceConfig voiceConfig = MaryConfig.getVoiceConfigs().iterator().next();
		marytts.setVoice(voiceConfig.getName());
		AudioInputStream audio = marytts.generateAudio("Hello world.");
		AudioPlayer player = new AudioPlayer(audio);
		player.start();
		player.join();
		System.exit(0);
	}

}
