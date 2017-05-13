package marytts.features;

import java.util.Hashtable;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class FeatureProcessorFactory {
	private Hashtable<String, FeatureProcessor> feature_processors;

	public FeatureProcessorFactory() {
		feature_processors = new Hashtable<String, FeatureProcessor>();
	}

	public void addFeatureProcessor(String name, String classname)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		feature_processors.put(name, (FeatureProcessor) Class.forName(classname).newInstance());
	}

	public FeatureProcessor createFeatureProcessor(String name) {
		return feature_processors.get(name);
	}
}
