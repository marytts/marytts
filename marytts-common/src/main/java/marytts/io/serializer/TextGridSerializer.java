package marytts.io.serializer;

import marytts.data.item.Item;
import marytts.data.item.phonology.Phone;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.Utterance;
import marytts.io.MaryIOException;
import marytts.data.SupportedSequenceType;
import org.m2ci.msp.jtgt.tier.IntervalTier;
import marytts.data.item.phonology.Phoneme;


import org.m2ci.msp.jtgt.*;
import org.m2ci.msp.jtgt.tier.*;
import org.m2ci.msp.jtgt.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * TextGrid serializer
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class TextGridSerializer implements Serializer {
    protected String ref_sequence;
    /**
     * Constructor
     *
     */
    public TextGridSerializer() {
	ref_sequence = SupportedSequenceType.PHONE;
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
	    // Only interval tiers for now
	    ArrayList<Tier> tiers = new ArrayList<Tier>();


	    // Load reference sequence
	    Sequence<Phoneme> seq_ph = (Sequence<Phoneme>) utt.getSequence(ref_sequence);

	    tiers.add(loadReferenceTier(seq_ph));

	    for (String seq_name: utt.listAvailableSequences()) {
		// We ignore the reference sequence as it as already been done
		if (seq_name.equals(ref_sequence))
		    continue;

		tiers.add(loadSequence(utt, seq_name, seq_ph));
	    }

	    TextGrid tgt = new TextGrid(null, 0, getEnd(seq_ph)/1000, tiers);

	    // Serialize
	    org.m2ci.msp.jtgt.io.TextGridSerializer tgt_ser = new org.m2ci.msp.jtgt.io.TextGridSerializer();
	    return tgt_ser.toString(tgt);
	} catch (Exception ex) {
	    throw new MaryIOException("cannot serialize utterance to textgrid", ex);
	}
    }

    protected double getEnd(Sequence<Phoneme> seq_ph) {
	Phone ph = (Phone) seq_ph.get(seq_ph.size() - 1);
	return ph.getStart() + ph.getDuration();
    }


    protected IntervalTier loadSequence(Utterance utt, String seq_name, Sequence<Phoneme> seq_ph) throws Exception {
	Sequence<Item> cur_seq = (Sequence<Item>) utt.getSequence(seq_name);
	Relation rel_cur_ref = utt.getRelation(cur_seq, seq_ph);


        ArrayList<Annotation> annotations = new ArrayList<Annotation>();
	double start = 0;
	double end = 0;
	int last_ref_index = -1;
	for (int i=0; i<cur_seq.size(); i++) {
	    int[] rel_indexes = rel_cur_ref.getRelatedIndexes(i);

	    if (rel_indexes.length <= 0) {
		continue;
	    }
	    int start_i = 0;
	    while ((start_i < rel_indexes.length) &&
		   (rel_indexes[start_i] <= last_ref_index)) {
		start_i++;
	    }

	    if (start_i >= rel_indexes.length)
		throw new Exception("The item " + i + " of the sequence " + seq_name + " is totally overlapping with the previous one ");


	    double start_an = ((Phone) seq_ph.get(rel_indexes[start_i])).getStart();
	    end = ((Phone) seq_ph.get(rel_indexes[rel_indexes.length-1])).getStart();
	    end += ((Phone) seq_ph.get(rel_indexes[rel_indexes.length-1])).getDuration();
	    String text = cur_seq.get(i).toString();

	    Annotation annotation = new IntervalAnnotation(start_an/1000, end/1000, text);

	    annotations.add(annotation);

	    last_ref_index = rel_indexes[rel_indexes.length-1];
	}


        return new IntervalTier(seq_name, start/1000, end/1000, annotations);
    }

    protected IntervalTier loadReferenceTier(Sequence<Phoneme> seq_ph) {
	double start = 0;
	double end = 0;

        ArrayList<Annotation> annotations = new ArrayList<Annotation>();
	for (Phoneme ph: seq_ph) {

	    double start_an = ((Phone) ph).getStart();
	    end = start_an + ((Phone) ph).getDuration();
	    String text = ph.toString();

	    Annotation annotation = new IntervalAnnotation(start_an/1000, end/1000, text);

	    annotations.add(annotation);
	}


        return new IntervalTier(ref_sequence, start/1000, end/1000, annotations);
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

/* TextgridSerializer.java ends here */
