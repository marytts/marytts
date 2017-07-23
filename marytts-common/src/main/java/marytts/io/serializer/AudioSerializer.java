package marytts.io.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import marytts.io.serializer.Serializer;
import marytts.io.MaryIOException;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.item.acoustic.AudioItem;


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
public class AudioSerializer implements Serializer
{
    public AudioSerializer()
    {
    }

    /**
     * Generate the TSV output from the utterance. Only the feature sequence is
     * used !
     *
     * @param utt
     *            the utterance containing the feature sequence
     * @return the TSV formatted feature sequence
     * @throws MaryIOException
     *             if anything is going wrong
     */
    public Object export(Utterance utt) throws MaryIOException {
	Sequence<AudioItem> seq_au = (Sequence<AudioItem>) utt.getSequence(SupportedSequenceType.AUDIO);

	if (seq_au == null)
	    throw new MaryIOException("There is no audio to serialize (no sequence available)");

	if (seq_au.size() == 0)
	    throw new MaryIOException("There is no audio to serialize (sequence is empty)");

	// FIXME: what to do with multiple audio, merge them
	AudioInputStream ais = ((AudioItem) seq_au.get(0)).getAudio();
	ByteArrayOutputStream baos = new ByteArrayOutputStream();

	try {
	    AudioSystem.write(ais
			      ,AudioFileFormat.Type.WAVE
			      ,baos);
	} catch (IOException ex) {
	    throw new MaryIOException("Cannot serialize utterance", ex);
	}

	return "{ \"audio\": \"" + Base64.getEncoder().encodeToString(baos.toByteArray()) + "\"}";

    }



    /**
     * Unsupported operation ! We can't import from a TSV formatted input.
     *
     * @param content
     *            unused
     * @return nothing
     * @throws MaryIOException
     *             never done
     */
    public Utterance load(String content) throws MaryIOException {
        throw new UnsupportedOperationException();
    }
}


/* AudioSerializer.java ends here */
