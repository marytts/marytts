package marytts.io.serializer;

import java.util.ArrayList;

import marytts.io.MaryIOException;

import marytts.data.Utterance;
import org.json.simple.JSONObject;


/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class TextGridAudioSerializer extends TextGridSerializer {
    private AudioSerializer au_ser;

    public TextGridAudioSerializer() {
	au_ser = new AudioSerializer();
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
        try {
            JSONObject ob = (JSONObject) au_ser.export(utt);

            // Add the textgrid
            Object tgt_part = super.export(utt);
            ob.put("textgrid", tgt_part.toString());

            return ob;
        } catch (Exception ex) {
            throw new MaryIOException("Cannot serialize utterance", ex);
        }
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
