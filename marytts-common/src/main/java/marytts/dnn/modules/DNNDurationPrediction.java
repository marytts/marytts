package marytts.dnn.modules;

import marytts.MaryException;
import marytts.dnn.FeatureNormaliser;
import marytts.io.MaryIOException;

// Configuration
import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;

// Module
import marytts.modules.MaryModule;

// DNN part
import marytts.dnn.DNNPredictor;
import marytts.dnn.normaliser.*;
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

    private FeatureNormaliser normaliser;

    private String dict_filename;



    public DNNDurationPrediction() throws Exception
    {
	super("duration");
	dict_filename = null;
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
	    configuration.applyConfiguration(this);

	    double start = 15.0;
	    Sequence<Phoneme> ph_seq = (Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE);

	    // Encode (FIXME: assume)
	    Tensor<Float> encoded_input =
		normaliser.normalise((Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES));

	    // Predict
	    Tensor<Float> dur_t = dnn_pred.predict(encoded_input);
	    float[][] dur_a = new float[ph_seq.size()][1];
	    dur_t.copyTo(dur_a);

	    for (int i=0; i<ph_seq.size(); i++) {
		double dur = dur_a[i][0];

		// Replace phoneme by phone
		Phone tmp = new Phone(ph_seq.get(i), start, dur);
		ph_seq.set(i, tmp);

		// Move to the next one !
		start += dur;
	    }


	    Phone tmp = new Phone("_", 0, 15.0);
	    ph_seq.add(0, tmp);

	    tmp = new Phone("_", start, start+15.0);
	    ph_seq.add(tmp);

	    return utt;
	} catch (Exception ex) {
	    throw new MaryException("Cannot predict duration", ex);
	}
    }


    public void setDescription() {
	this.description = "Dummy duration prediction which sets each phone at 1s.";
    }
}
