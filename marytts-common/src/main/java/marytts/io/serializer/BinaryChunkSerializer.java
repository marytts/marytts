package marytts.io.serializer;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import marytts.io.serializer.Serializer;
import marytts.data.Sequence;
import marytts.data.item.global.DoubleMatrixItem;

import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.io.MaryIOException;

/**
 * Serializer to generate a binary (bytebuffer) representation of a given sequence
 *
 * The sequence should be a sequence of DoubleMatrixItem
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class BinaryChunkSerializer implements Serializer {

    /** The label of the selected sequence */
    private String selected_sequence;

    /** The default precision constant */
    private static final String DEFAULT_PRECISION = "double";

    /** The output precision */
    private String precision;

    /** The default endianness constant */
    private static final String DEFAULT_ENDIANNESS = "big";

    /** The output endianness */
    private String endianness;

    /**
     * Default Constructor
     *
     */
    public BinaryChunkSerializer() {
	setSelectedSequence(null);
        setPrecision(DEFAULT_PRECISION);
        setEndianness(DEFAULT_ENDIANNESS);
    }

    /**
     * Export the selected sequence from the given utterance to a byte biffer
     *
     * @param utt the utterance containing the feature sequence
     * @return the ByteBuffer filled with the values of the sequence
     * @throws MaryIOException  if anything is going wrong
     */
    public Object export(Utterance utt) throws MaryIOException {
        if (getSelectedSequence() == null) {
            throw new MaryIOException("The sequence label should be defined!",
                                      null);
        }
        if (!utt.hasSequence(getSelectedSequence())) {
            throw new MaryIOException("Current utterance doesn't have the desired sequence",
                                      null);
        }

        if ((!getPrecision().equals("float")) && (!getPrecision().equals("double"))) {
            throw new MaryIOException("precision should be the string \"float\" or \"double\"");
        }

        if ((!getEndianness().equals("little")) && (!getEndianness().equals("big"))) {
            throw new MaryIOException("endianness should be the string \"little\" or \"big\"");
        }

        int nb_bytes = Double.BYTES;
        if (getPrecision().equals("float")) {
           nb_bytes = Float.BYTES;
        }

	// Compute size
	int size = 0;
	for (DoubleMatrixItem chunk: (Sequence<DoubleMatrixItem>) utt.getSequence(selected_sequence)) {
	    size += chunk.getValues().size() * nb_bytes;
	}

	// Copy data information
	ByteBuffer dst = ByteBuffer.allocate(size);
        if (getEndianness().equals("little"))
            dst.order(ByteOrder.LITTLE_ENDIAN);

        if (getPrecision().equals("double")) {
            for (DoubleMatrixItem chunk: (Sequence<DoubleMatrixItem>) utt.getSequence(selected_sequence)) {
                double[][] ar = chunk.getValues().toArray();
                for (int t=0; t<ar.length; t++)
                    for (int i=0; i<ar[t].length; i++)
                        dst.putDouble(ar[t][i]);
            }
        } else if (getPrecision().equals("float")) {
            for (DoubleMatrixItem chunk: (Sequence<DoubleMatrixItem>) utt.getSequence(selected_sequence)) {
                double[][] ar = chunk.getValues().toArray();
                for (int t=0; t<ar.length; t++)
                    for (int i=0; i<ar[t].length; i++)
                        dst.putFloat((float) ar[t][i]);
            }
        }

	// Flip buffer to be able to validate header
	dst.flip();

	return dst; // Double check !
    }


    /**
     *  Getter of the label of the used sequence
     *
     *  @return the used sequence label
     */
    public String getSelectedSequence() {
	return selected_sequence;
    }

    /**
     *  Setter of the label of the used sequence
     *
     *  @param selected_sequence the used sequence label
     */
    public void setSelectedSequence(String selected_sequence) {
	this.selected_sequence = selected_sequence;
    }



    /**
     *  Setter of the precision.
     *
     *  @param precision the new precision (should be "float" or "double")
     *  @throws IllegalArgumentException if the parameter value is neither "float" nor "double"
     */
    public void setPrecision(String precision) {
        if ((!precision.equals("float")) && (!precision.equals("double"))) {
            throw new IllegalArgumentException("precision should be the string \"float\" or \"double\"");
        }

        this.precision = precision;
    }

    /**
     *  Getter of the precision
     *
     *  @return the used precision
     */
    public String getPrecision() {
        return precision;
    }

    /**
     *  Getter of the endianness
     *
     *  @return the used endianness
     */
    public String getEndianness() {
        return endianness;
    }

    /**
     *  Setter of the endianness.
     *
     *  @param endianness the new endianness (should be "little" or "big")
     *  @throws IllegalArgumentException if the parameter value is neither "little" nor "big"
     */
    public void setEndianness(String endianness) {
        if ((!endianness.equals("little")) && (!endianness.equals("big"))) {
            throw new IllegalArgumentException("endianness should be the string \"little\" or \"big\"");
        }

        this.endianness = endianness;
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
