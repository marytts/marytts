package marytts.io.serializer;

import marytts.data.Utterance;
import marytts.io.MaryIOException;


/**
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class UtteranceSerializer implements Serializer {

    /**
     * Constructor
     *
     */
    public UtteranceSerializer() {
    }

    public Object export(Utterance utt) throws MaryIOException {
	return utt;
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

