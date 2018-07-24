package marytts.dnn.modules;

import marytts.MaryException;

// Reflection
import java.lang.reflect.Constructor;

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
import java.io.InputStream;

// Data
import marytts.features.FeatureMap;
import marytts.dnn.features.FeatureChunk;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;

// Utils
import java.util.ArrayList;

/**
 *  Feature normalisation module used to generate binary input vectors for the DNN prediction module
 *  laster called.
 *
 *  @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class FeatureNormalisation extends MaryModule
{
    /** The name of the output sequence type */
    protected String output_sequence_type;

    /** The normaliser used */
    private FeatureNormaliser normaliser;

    /** The normaliser input path */
    private String normaliser_input_path;

    /** The normaliser input stream loaded from the resource */
    private InputStream normaliser_input_stream;

    /**
     *  Default Constructor.
     *
     */
    public FeatureNormalisation()
    {
	super("normalisation");
	this.output_sequence_type = SupportedSequenceType.NORMALISED_FEATURES;
	this.normaliser = null;
        this.normaliser_input_path = null;
        this.normaliser_input_stream = null;
    }

    /**
     *  Method to get the normaliser input path (file or resource)
     *
     *  @return the normaliser input path
     */
    public String getInputNormaliserPath() {
	return normaliser_input_path;
    }

    /**
     *  Method to get the normaliser
     *
     *  @return the normaliser
     */
    public FeatureNormaliser getNormaliser() {
	return normaliser;
    }

    /**
     *  Method to set the input normaliser filename
     *
     *  @param input_filename the input filename
     */
    public void setInputNormaliserFilename(String input_filename) {
	this.normaliser_input_path = input_filename;
    }

    /**
     *  Method to set the input normaliser resource path
     *
     *  @param input_resource_path the input resource path of the normaliser
     */
    public void setInputNormaliserResourcePath(String input_resource_path) {
	this.normaliser_input_path = input_resource_path;
        normaliser_input_stream =  FeatureNormalisation.class.getResourceAsStream(input_resource_path);
    }

    /**
     *  Method to set the normaliser based on its name
     *
     *  @param normaliser_name the normaliser name
     *  @throws MaryIOException if the normaliser can't be set for any reason
     */
    public void setNormaliser(String normaliser_name) throws MaryIOException {
	try {

            Class<?> clazz = Class.forName(normaliser_name);
            Constructor<?> ctor;
            if (normaliser_input_path == null) {
                ctor = clazz.getConstructor();
                this.normaliser = (FeatureNormaliser) ctor.newInstance(new Object[] {});

            } else if (normaliser_input_stream == null) {
                ctor = clazz.getConstructor(new Class[]{String.class});
                this.normaliser = (FeatureNormaliser) ctor.newInstance(new Object[] {this.normaliser_input_path});
            } else {
                ctor = clazz.getConstructor(new Class[]{InputStream.class});
                this.normaliser = (FeatureNormaliser) ctor.newInstance(new Object[] {this.normaliser_input_stream});
            }

	} catch (Exception ex) {
	    throw new MaryIOException("Cannot set normaliser \"" + normaliser_name + "\"", ex);
	}
    }

    /**
     *  The startup checking method.
     *
     *  @throws MaryConfigurationException never done here!
     */
    @Override
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
    @Override
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


    /**
     *  Method to set the description of the object
     *
     */
    @Override
    public void setDescription() {
	this.description = "Dummy duration prediction which sets each phone at 1s.";
    }
}
