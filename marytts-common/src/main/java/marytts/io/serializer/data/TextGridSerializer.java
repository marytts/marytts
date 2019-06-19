package marytts.io.serializer.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.m2ci.msp.jtgt.Annotation;
import org.m2ci.msp.jtgt.TextGrid;
import org.m2ci.msp.jtgt.Tier;
import org.m2ci.msp.jtgt.annotation.IntervalAnnotation;
import org.m2ci.msp.jtgt.tier.IntervalTier;

import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.acoustic.Segment;


import marytts.io.serializer.Serializer;
import marytts.io.MaryIOException;

/**
 * TextGrid serializer
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class TextGridSerializer implements Serializer {
    protected String ref_sequence;

    private Set<String> ignored_sequences;
    protected Logger logger;

    /**
     * Constructor
     *
     */
    public TextGridSerializer() {
	ref_sequence = SupportedSequenceType.SEGMENT;
        setIgnoredSequences(new ArrayList<String>());
        logger = LogManager.getLogger(this);
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
	    Sequence<Segment> ref_seq = (Sequence<Segment>) utt.getSequence(ref_sequence);

	    tiers.add(loadReferenceTier(ref_seq));

	    for (String seq_name: utt.listAvailableSequences()) {
		// We ignore the reference sequence as it as already been done
		if (seq_name.equals(ref_sequence))
		    continue;

                if (! isSequenceIgnored(seq_name)) {
                    logger.info(seq_name + " exported into the textgrid");
                    tiers.add(loadSequence(utt, seq_name, ref_seq));
                } else {
                    logger.info(seq_name + " is ignored as part of the ignored sequences");
                }
	    }

	    TextGrid tgt = new TextGrid(null, 0, getEnd(ref_seq)/1000, tiers);

	    // Serialize
	    org.m2ci.msp.jtgt.io.TextGridSerializer tgt_ser = new org.m2ci.msp.jtgt.io.TextGridSerializer();
	    return tgt_ser.toString(tgt);
	} catch (Exception ex) {
	    throw new MaryIOException("cannot serialize utterance to textgrid", ex);
	}
    }

    protected double getEnd(Sequence<Segment> ref_seq) {
	Segment seg = (Segment) ref_seq.get(ref_seq.size() - 1);
	return seg.getStart() + seg.getDuration();
    }


    protected IntervalTier loadSequence(Utterance utt, String seq_name, Sequence<Segment> ref_seq) throws Exception {
	Sequence<Item> cur_seq = (Sequence<Item>) utt.getSequence(seq_name);
	Relation rel_cur_ref = utt.getRelation(seq_name, ref_sequence);

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


            // Annotation starts at the first element start
	    double start_an = ref_seq.get(rel_indexes[start_i]).getStart();

            // Annotation ends at the last element end
	    end = ref_seq.get(rel_indexes[rel_indexes.length-1]).getStart();
	    end += ref_seq.get(rel_indexes[rel_indexes.length-1]).getDuration();
	    String text = cur_seq.get(i).toString();

	    Annotation annotation = new IntervalAnnotation(start_an/1000, end/1000, text);
	    annotations.add(annotation);

	    last_ref_index = rel_indexes[rel_indexes.length-1];
	}


        return new IntervalTier(seq_name, start/1000, end/1000, annotations);
    }

    protected IntervalTier loadReferenceTier(Sequence<Segment> ref_seq) {
	double start = 0;
	double end = 0;

        ArrayList<Annotation> annotations = new ArrayList<Annotation>();
	for (Segment seg: ref_seq) {
	    double start_an = seg.getStart();
	    end = start_an + seg.getDuration();
	    String text = seg.toString();

	    Annotation annotation = new IntervalAnnotation(start_an/1000, end/1000, text);

	    annotations.add(annotation);
	}


        return new IntervalTier(ref_sequence, start/1000, end/1000, annotations);
    }


    public Set<String> getIgnoredSequences() {
        return ignored_sequences;
    }

    public void setIgnoredSequences(ArrayList<String> ignored_sequences) {
        this.ignored_sequences = new HashSet<String>(ignored_sequences);
    }

    public boolean isSequenceIgnored(String seq_name) {
        return getIgnoredSequences().contains(seq_name);
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
