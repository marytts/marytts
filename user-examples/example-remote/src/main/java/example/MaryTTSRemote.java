package example;

import javax.sound.sampled.AudioInputStream;

import marytts.MaryInterface;
import marytts.client.RemoteMaryInterface;
import marytts.util.data.audio.AudioPlayer;

public class MaryTTSRemote {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		if (args.length < 2) {
			System.err.println("Usage: java example.MaryTTSRemote hostname portnumber");
			System.exit(1);
		}
		
		String hostname = args[0];
		int port = Integer.valueOf(args[1]);
		
		MaryInterface marytts = new RemoteMaryInterface(hostname, port);

		marytts.setVoice("cmu-slt-hsmm");
		AudioInputStream audio = marytts.generateAudio("Hello world.");
		AudioPlayer player = new AudioPlayer(audio);
		player.start();
		player.join();
		System.exit(0);
	}
}
