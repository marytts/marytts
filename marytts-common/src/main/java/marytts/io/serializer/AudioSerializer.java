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
public class AudioSerializer
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
    public String toString(Utterance utt) throws MaryIOException {
        throw new UnsupportedOperationException();
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
    public Utterance fromString(String content) throws MaryIOException {
        throw new UnsupportedOperationException();
    }

}


/* AudioSerializer.java ends here */
