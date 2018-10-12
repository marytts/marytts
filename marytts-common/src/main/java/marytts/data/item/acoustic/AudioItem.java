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
 *  The audio item which contains an AudioInputStream as a value and provide an interface to
 *  play/transfer the sound.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class AudioItem extends Item {

    /** The audio input stream */
    private AudioInputStream ais;

    /**
     *  Default constructor (the stream is null)
     *
     */
    public AudioItem() {
        ais = null;
    }

    /**
     *  Constructor which loads the audio from a file
     *
     *  @param filename the name of the file containing the audio
     *  @throws UnsupportedAudioFileException if the file is not following a known format (wav, ..)
     *  @throws IOException if the file is corrupted or if there is a problem with the stream
     */
    public AudioItem(String filename) throws UnsupportedAudioFileException, IOException {
        setAudio(filename);
    }

    /**
     *  Constructor which loads the audio from binary data
     *
     *  @param bytes the binary data
     *  @throws UnsupportedAudioFileException if the data is not following a known format (wav, ..)
     *  @throws IOException if the data is corrupted or if there is a problem with the stream
     */
    public AudioItem(byte[] bytes)  throws UnsupportedAudioFileException, IOException {
	setAudio(bytes);
    }

    /**
     *  Constructor which reference a loaded stream
     *
     *  @param ais the loaded stream
     */
    public AudioItem(AudioInputStream ais) {
        setAudio(ais);
    }

    /**
     *  Method which loads the audio from a file
     *
     *  @param filename the name of the file containing the audio
     *  @throws UnsupportedAudioFileException if the file is not following a known format (wav, ..)
     *  @throws IOException if the file is corrupted or if there is a problem with the stream
     */
    public void setAudio(String filename) throws UnsupportedAudioFileException, IOException {
	setAudio(Files.readAllBytes(new File(filename).toPath()));
    }

    /**
     *  Method which loads the audio from binary data
     *
     *  @param bytes the binary data
     *  @throws UnsupportedAudioFileException if the data is not following a known format (wav, ..)
     *  @throws IOException if the data is corrupted or if there is a problem with the stream
     */
    public void setAudio(byte[] bytes) throws UnsupportedAudioFileException, IOException {

        AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
        setAudio(stream);
    }

    /**
     *  Method which sets the reference to a loaded stream
     *
     *  @param ais the loaded stream
     */
    public void setAudio(AudioInputStream ais) {
        this.ais = ais;

    }

    /**
     *  Method to get the reference to the audio stream
     *
     *  @return the audio stream
     */
    public AudioInputStream getAudioStream() {
        return ais;
    }

    /**
     *  This method is an alias of getAudioStringEncoded
     *
     *  @return the audio encoded in base64
     */
    public String getAis() throws IOException {
        return getAudioStringEncoded();
    }

    /**
     *  Method to get the stream but encoded in base64
     *
     *  @return the audio encoded in base64
     */
    public String getAudioStringEncoded() throws IOException {

        // FIXME: what to do with multiple audio, merge them
        AudioInputStream ais = getAudioStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        AudioSystem.write(ais,
                          AudioFileFormat.Type.WAVE,
                          baos);

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     *  Baseline to string. It will return just "audio". To get the encoded audio, use
     *  getAudioStringEncoded
     *
     *  @return "audio"
     */
    @Override
    public String toString() {
        return "audio";
    }
}
