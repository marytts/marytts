package marytts.io.serializer;

import marytts.io.serializer.Serializer;
import marytts.io.MaryIOException;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.item.acoustic.AudioItem;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class AudioSerializer implements Serializer {
    public AudioSerializer() {
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

        if (seq_au == null) {
            throw new MaryIOException("There is no audio to serialize (no sequence available)", null);
        }

        if (seq_au.size() == 0) {
            throw new MaryIOException("There is no audio to serialize (sequence is empty)", null);
        }

        // FIXME: what to do with multiple audio, merge them
        return ((AudioItem) seq_au.get(0)).getAudio();

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
