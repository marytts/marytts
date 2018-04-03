package marytts.modules.duration;

import marytts.MaryException;

// Configuration
import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;

// Module
import marytts.modules.MaryModule;

// DNN part
import marytts.dnn.DNNPredictor;
import marytts.dnn.normaliser.TriphoneNormaliser;
import org.tensorflow.Tensor;


// Data
import marytts.features.FeatureMap;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Phone;
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
public class DNNDurationPrediction extends MaryModule
{
    private DNNPredictor dnn_pred;

    public DNNDurationPrediction() throws Exception
    {
	super("duration");
    }

    public void setPredictorModel(String model_path) {

	// Initialize the prediction resource
	dnn_pred = new DNNPredictor(model_path); // FIXME: hardcoded
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
        if (!utt.hasSequence(SupportedSequenceType.PHONE)) {
            throw new MaryException("Phone sequence is missing", null);
        }
        if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
            throw new MaryException("Feature sequence is missing", null);
        }

	assert (utt.getSequence(SupportedSequenceType.PHONE).size() ==
		utt.getSequence(SupportedSequenceType.FEATURES).size());
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

	try {
	    double start = 0.0;
	    Sequence<Phoneme> ph_seq = (Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE);
	    TriphoneNormaliser tn = new TriphoneNormaliser();

	    // Encode (FIXME: assume)
	    Tensor<Float> encoded_input =
		tn.normalise((Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES));

	    // Predict
	    Tensor<Float> dur_t = dnn_pred.predict(encoded_input);
	    float[][] dur = new float[ph_seq.size()][1];
	    dur_t.copyTo(dur);

	    for (int i=0; i<ph_seq.size(); i++) {
		// Replace phoneme by phone
		Phone tmp = new Phone(ph_seq.get(i), start, dur[i][0]/1000);
		ph_seq.set(i, tmp);

		// Move to the next one !
		start += dur[i][0] / 1000;
	    }

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
