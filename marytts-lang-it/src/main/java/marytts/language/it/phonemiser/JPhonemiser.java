package marytts.language.it.phonemiser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;


public class JPhonemiser extends marytts.modules.JPhonemiser {

	public JPhonemiser(String propertyPrefix) throws IOException,
			MaryConfigurationException, SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		super(propertyPrefix);
	}

	public JPhonemiser(String componentName, MaryDataType inputType,
			MaryDataType outputType, String allophonesProperty,
			String userdictProperty, String lexiconProperty,
			String ltsProperty, String removetrailingonefromphonesProperty,
			String syllabifierClassProperty) throws IOException,
			MaryConfigurationException, SecurityException,
			NoSuchMethodException, ClassNotFoundException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		super(componentName, inputType, outputType, allophonesProperty,
				userdictProperty, lexiconProperty, ltsProperty,
				removetrailingonefromphonesProperty, syllabifierClassProperty);
	}


	public JPhonemiser(String componentName, MaryDataType inputType,
			MaryDataType outputType, String allophonesProperty,
			String userdictProperty, String lexiconProperty, String ltsProperty)
			throws IOException, MaryConfigurationException, SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		super(componentName, inputType, outputType, allophonesProperty,
				userdictProperty, lexiconProperty, ltsProperty);
	}


	/**
	 * Phonemise the word text. This starts with a simple lexicon lookup,
	 * followed by some heuristics.
	 * 
     * @param privatedict an additional lexicon for lookups (can be null).
	 * @param text
	 *            the textual (graphemic) form of a word.
	 * @param pos
	 *            the part-of-speech of the word
	 * @param g2pMethod
	 *            This is an awkward way to return a second String parameter via
	 *            a StringBuilder. If a phonemisation of the text is found, this
	 *            parameter will be filled with the method of phonemisation
	 *            ("lexicon", ... "rules").
	 * @return a phonemisation of the text if one can be generated, or null if
	 *         no phonemisation method was successful.
	 */
	public String phonemiseLookupOnly(Map<String, List<String>> privatedict, String text, String pos,
 StringBuilder g2pMethod) {
		if ((pos != null) && pos.length() != 0) {
			switch (pos.charAt(0)) {
			case 'V':
				pos = pos.replaceAll("\\d.*$", "").replaceAll("^V[AM]", "V");
				break;
			case 'T':
				pos = "D";
				break;
			case 'E':
				if ((pos.length() > 1) && (pos.charAt(1) == 'A')) {
					pos = "EA";
				} else {
					pos = "E";
				}
				break;
			case 'S':
				if ((pos.length() > 1) && ( (pos.charAt(1) == 'A') || (pos.charAt(1) == 'W') || (pos.charAt(1) == 'P') )) {
					pos = pos.substring(0, 2);
				} else {
					pos = "S";
				}
				break;
			default:
				pos = pos.substring(0, 1);
				break;
			}
		}
		return super.phonemiseLookupOnly(privatedict, text, pos, g2pMethod);
	}

}
