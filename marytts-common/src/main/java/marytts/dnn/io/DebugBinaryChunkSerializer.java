package marytts.dnn.io;


import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.dnn.features.FeatureChunk;
import marytts.io.MaryIOException;
import marytts.io.serializer.Serializer;

/**
 * Feature serializer to generate TSV format output. There is not import from it
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class DebugBinaryChunkSerializer implements Serializer {

    private String selected_sequence;

    /**
     * Constructor
     *
     */
    public DebugBinaryChunkSerializer() {
	setSelectedSequence(SupportedSequenceType.NORMALISED_FEATURES);
    }

    public String getSelectedSequence() {
	return selected_sequence;
    }

    public void setSelectedSequence(String selected_sequence) {
	this.selected_sequence = selected_sequence;
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
        if (!utt.hasSequence(getSelectedSequence())) {
            throw new MaryIOException("Current utterance doesn't have the desired sequence",
                                      null);
        }

	Sequence<FeatureChunk> cur_seq = (Sequence<FeatureChunk>) utt.getSequence(selected_sequence);
	String tsv = "";

	// Header
	for (String col: cur_seq.get(0).getNormaliser().getHeader())
	    tsv += col + "\t";
	tsv += "\n";

	for (FeatureChunk fc: cur_seq) {
	    double[][] ar = fc.getValues().toArray();
	    for (int i=0; i<ar.length; i++) {
		for (int j=0; j<ar[i].length; j++)
		    tsv += ar[i][j] + "\t";

		tsv += "\n";
	    }
	}
	return tsv;
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
