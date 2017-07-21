package marytts.data.item.acoustic;

import marytts.data.item.Item;

import java.io.File;
import java.nio.file.Files;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class AudioItem extends Item
{
    private AudioInputStream ais;
    public AudioItem()
    {
	ais = null;
    }

    public AudioItem(String filename) throws UnsupportedAudioFileException, IOException {
	setAudio(filename);
    }

    public AudioItem(AudioInputStream ais) {
	setAudio(ais);
    }

    public void setAudio(String filename) throws UnsupportedAudioFileException, IOException {

	AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(Files.readAllBytes(new File(filename).toPath())));
	setAudio(stream);
    }

    public void setAudio(AudioInputStream ais) {
	this.ais = ais;

    }

    public AudioInputStream getAudio() {
	return ais;
    }
}


/* AudioItem.java ends here */
