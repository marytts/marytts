package marytts.modules;

import marytts.MaryException;

// Configuration
import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;

// Module
import marytts.modules.MaryModule;

// DNN part
import marytts.dnn.normaliser.QuinphoneNormaliser;
import org.tensorflow.Tensor;


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

    public FeatureNormalisation() throws Exception
    {
	super("normalisation");
	this.output_sequence_type = SupportedSequenceType.NORMALISED_FEATURES;
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
	    // FIXME: hard coded
	    QuinphoneNormaliser tn = new QuinphoneNormaliser();

	    // Encode (FIXME: assume)
	    Tensor<Float> encoded_input =
		tn.normalise((Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES));

	    encoded_vectors.add(new FeatureChunk(encoded_input, tn));
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
