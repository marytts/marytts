package marytts.modules;

import marytts.MaryException;

// Configuration
import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;

// Module
import marytts.modules.MaryModule;

// DNN part
import marytts.dnn.FeatureNormaliser;
import marytts.dnn.normaliser.*;
import org.tensorflow.Tensor;

// IO
import marytts.io.MaryIOException;

// Data
import marytts.features.FeatureMap;
import marytts.features.FeatureChunk;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;

// Utils
import java.util.ArrayList;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class FeatureNormalisation extends MaryModule
{

    protected String output_sequence_type;

    private FeatureNormaliser normaliser;

    private String dict_filename;

    public FeatureNormalisation() throws Exception
    {
	super("normalisation");
	this.output_sequence_type = SupportedSequenceType.NORMALISED_FEATURES;
	this.normaliser = null;
    }



    public String getDictFilename() {
	return dict_filename;
    }

    public FeatureNormaliser getNormaliser() {
	return normaliser;
    }


    public void setDictFilename(String dict_filename) {
	this.dict_filename = dict_filename;
    }

    public void setNormaliser(String normaliser) throws MaryIOException {
	try {
	    if (normaliser.equals("QuinphoneNormaliser")) {
		this.normaliser = new QuinphoneNormaliser();
	    } else if (normaliser.equals("QuinphoneWithDictNormaliser")) {
		if (getDictFilename() != null) {
		    this.normaliser = new QuinphoneWithDictNormaliser(getDictFilename());
		} else {
		    throw new MaryIOException("QuinphoneWithDictNormaliser needs a dict filename");
		}
	    } else {
		throw new MaryIOException("Unknown normaliser: " + normaliser);
	    }
	} catch (Exception ex) {
	    throw new MaryIOException("Cannot set normaliser", ex);
	}
    }

    public void checkStartup() throws MaryConfigurationException {
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
            throw new MaryException("Feature sequence is missing", null);
        }
    }


    /**
     * The process method which take a Utterance object in parameter, compute the
     * features of the utterance referenced in the parameter and return a new
     * Utterance object which contains the reference to the updated utterance.
     *
     * @param d
     *            the input Utterance object
     * @return the Utterance object with the updated reference
     * @throws Exception
     *             [TODO]
     */
    public Utterance process(Utterance utt, MaryConfiguration configuration) throws MaryException {
	Sequence<FeatureChunk> encoded_vectors = new Sequence<FeatureChunk>();
	try {
	    configuration.applyConfiguration(this);

	    Sequence<FeatureMap> seq_feat = (Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES);
	    Tensor<Float> encoded_input = getNormaliser().normalise(seq_feat);

	    encoded_vectors.add(new FeatureChunk(encoded_input, normaliser));
	    utt.addSequence(this.output_sequence_type, encoded_vectors);

	    return utt;
	} catch (Exception ex) {
	    throw new MaryException("Cannot predict duration", ex);
	}
    }


    public void setDescription() {
	this.description = "Dummy duration prediction which sets each phone at 1s.";
    }
}


/* DurationPrediction.java ends here */
