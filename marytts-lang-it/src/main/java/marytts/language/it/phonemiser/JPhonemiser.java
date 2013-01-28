package marytts.language.it.phonemiser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Element;

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

	protected String getPosTag(Element t) {
		String pos = null;
		if (t != null) {
			// use part-of-speech if available
			if (t.hasAttribute("pos_full")) {
				pos = t.getAttribute("pos_full");
				
				// simplify POS tagging in order to match POS tags in the lexicon
				if ((pos != null) && pos.length() != 0) {
					switch (pos.charAt(0)) {
					case 'V':
						pos = pos.replaceAll("(\\d.*|[mf][sp])$", "").replaceAll("^V[AM]", "V");
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
						if ((pos.length() > 2)
								&& ((pos.charAt(1) == 'm') || (pos.charAt(1) == 'f') || (pos
										.charAt(1) == 'n'))
								&& ((pos.charAt(2) == 'p') || (pos.charAt(2) == 's') || (pos
										.charAt(2) == 'n'))) {
							pos = pos.substring(0, 3);
						} else if ((pos.length() > 1)
								&& ((pos.charAt(1) == 'A') || (pos.charAt(1) == 'W') || (pos
										.charAt(1) == 'P'))) {
							pos = pos.substring(0, 2);
						} else {
							pos = "S";
						}
						break;
					case 'N':
						break;
					default:
						pos = pos.substring(0, 1);
						break;
					}
				}
			}
		}
		return pos;
	}

    /**
     * Look a given text up in the (standard) lexicon. part-of-speech is used 
     * in case of ambiguity.
     * 
     * @param text
     * @param pos
     * @return
     */
    public String lexiconLookup(String text, String pos)
    {
        if (text == null || text.length() == 0) return null;
        String[] entries;
        entries = lexiconLookupPrimitive(text, pos);
        // If entry is not found directly, try the following changes:
        // - lowercase the word
        // - all lowercase but first uppercase
        if (entries.length  == 0) {
            text = text.toLowerCase(getLocale());
            entries = lexiconLookupPrimitive(text, pos);
            if (entries.length  == 0) {
                // - lowercase the word and drop points
                entries = lexiconLookupPrimitive(text.replaceAll("[.]", ""), pos);
            }
        }
        if (entries.length  == 0) {
            text = text.substring(0,1).toUpperCase(getLocale()) + text.substring(1);
            entries = lexiconLookupPrimitive(text, pos);
         }
         
         if (entries.length  == 0) return null;
         return entries[0];
    }
	
}
