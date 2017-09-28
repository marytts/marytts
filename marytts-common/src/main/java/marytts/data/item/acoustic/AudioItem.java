package marytts.data.item.acoustic;

import marytts.data.item.Item;

import java.io.File;
import java.nio.file.Files;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.util.Base64;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class AudioItem extends Item {
    private AudioInputStream ais;
    public AudioItem() {
        ais = null;
    }

    public AudioItem(String filename) throws UnsupportedAudioFileException, IOException {
        setAudio(filename);
    }

    public AudioItem(AudioInputStream ais) {
        setAudio(ais);
    }

    public void setAudio(String filename) throws UnsupportedAudioFileException, IOException {

        AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(Files.readAllBytes(new File(
                                      filename).toPath())));
        setAudio(stream);
    }

    public void setAudio(AudioInputStream ais) {
        this.ais = ais;

    }

    public AudioInputStream getAudioStream() {
        return ais;
    }

    public String getAis() throws IOException {
        return getAudioStringEncoded();
    }

    public String getAudioStringEncoded() throws IOException {

        // FIXME: what to do with multiple audio, merge them
        AudioInputStream ais = getAudioStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        AudioSystem.write(ais,
                          AudioFileFormat.Type.WAVE,
                          baos);

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

}


/* AudioItem.java ends here */
