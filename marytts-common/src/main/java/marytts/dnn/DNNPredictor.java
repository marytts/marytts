package marytts.dnn;

import org.tensorflow.*;

/**
 *  A default DNN predictor class
 *
 */
public class DNNPredictor {

    /** The DNN graph model saved using by TensorFlow */
    protected SavedModelBundle model;

    /** The input layer name */
    protected String input_layer_name;

    /** The output layer name */
    protected String output_layer_name;

    /** The tag */
    protected String tag;

    /**
     *  Constructor. A model is mandatory.
     *
     *  @param model_path the model path
     */
    public DNNPredictor(String model_path) {
	// Load model
	model = SavedModelBundle.load(model_path, tag);

	// Default
	setInputLayerName("input");
	setOutputLayerName("output");
	setTag("serve");
    }

    /**
     *  The prediction function.
     *
     *  @param input the input vector
     *  @return the output vector
     *  @throws Exception if something bad happens
     */
    public Tensor<Float> predict(Tensor<Float> input) throws Exception {

	// Execute the "MyConst" operation in a Session.
	Session s = model.session();

	Tensor<Float> out =  (Tensor<Float>) s.runner().feed(input_layer_name, input).fetch(output_layer_name).run().get(0);

	s.close();
	return out;
    }

    /**
     *  Get the used tag.
     *
     *  @return the used tag
     */
    public String getTag() {
	return tag;
    }

    /**
     *  Get the used output layer name.
     *
     *  @return the used output layer name
     */
    public String getOutputLayerName() {
	return output_layer_name;
    }

    /**
     *  Get the used input layer name.
     *
     *  @return the used input layer name
     */
    public String getInputLayerName() {
	return input_layer_name;
    }

    /**
     *  Set the used tag
     *
     *  @param tag the new tag
     */
    public void setTag(String tag) {
	this.tag = tag;
    }

    /**
     *  Set the used output layer name
     *
     *  @param output_layer_name the new output layer name
     */
    public void setOutputLayerName(String output_layer_name) {
	this.output_layer_name = output_layer_name;
    }

    /**
     *  Set the used input layer name
     *
     *  @param input_layer_name the new input layer name
     */
    public void setInputLayerName(String input_layer_name) {
	this.input_layer_name = input_layer_name;
    }
}
