/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.datatypes;

// General Java Classes
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryUtils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * A representation of the data types available as input/output of (partial) processing. List the data types available as
 * input/output of (partial) processing. Provide static interfaces for obtaining a given data type.
 * 
 * @author Marc Schr&ouml;der
 */

public class MaryDataType {
	public static final Traits PLAIN_TEXT = new Traits(true, false, false, false);
	public static final Traits EXTERNAL_MARKUP = new Traits(true, true, false, false);
	public static final Traits MARYXML = new Traits(true, true, true, false);
	public static final Traits UTTERANCES = new Traits(false, false, false, true);
	public static final Traits BINARY = new Traits(false, false, false, false);

	// ///////////////////////////////////////////////////////////////////
	// ////////// Definition of global MaryDataTypes /////////////////////
	// ///////////////////////////////////////////////////////////////////

	public static final MaryDataType ACOUSTPARAMS = new MaryDataType("ACOUSTPARAMS", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType ALLOPHONES = new MaryDataType("ALLOPHONES", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType APML = new MaryDataType("APML", true, false, EXTERNAL_MARKUP, "apml");
	public static final MaryDataType AUDIO = new MaryDataType("AUDIO", false, true, BINARY);
	public static final MaryDataType DURATIONS = new MaryDataType("DURATIONS", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType EMOTIONML = new MaryDataType("EMOTIONML", true, false, EXTERNAL_MARKUP, "emotionml");
	public static final MaryDataType FESTIVAL_UTT = new MaryDataType("FESTIVAL_UTT", true, true, PLAIN_TEXT);
	public static final MaryDataType HALFPHONE_TARGETFEATURES = new MaryDataType("HALFPHONE_TARGETFEATURES", false, true,
			PLAIN_TEXT);
	public static final MaryDataType HTSCONTEXT = new MaryDataType("HTSCONTEXT", true, true, PLAIN_TEXT);
	public static final MaryDataType INTONATION = new MaryDataType("INTONATION", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType PARTSOFSPEECH = new MaryDataType("PARTSOFSPEECH", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType PHONEMES = new MaryDataType("PHONEMES", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType PRAAT_TEXTGRID = new MaryDataType("PRAAT_TEXTGRID", false, true, PLAIN_TEXT);
	public static final MaryDataType RAWMARYXML = new MaryDataType("RAWMARYXML", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType REALISED_ACOUSTPARAMS = new MaryDataType("REALISED_ACOUSTPARAMS", false, true, MARYXML,
			MaryXML.MARYXML);
	public static final MaryDataType REALISED_DURATIONS = new MaryDataType("REALISED_DURATIONS", false, true, PLAIN_TEXT);
	public static final MaryDataType SABLE = new MaryDataType("SABLE", true, false, EXTERNAL_MARKUP, "SABLE");
	public static final MaryDataType SIMPLEPHONEMES = new MaryDataType("SIMPLEPHONEMES", true, false, PLAIN_TEXT);
	public static final MaryDataType SSML = new MaryDataType("SSML", true, false, EXTERNAL_MARKUP, "speak");
	public static final MaryDataType TARGETFEATURES = new MaryDataType("TARGETFEATURES", false, true, PLAIN_TEXT);
	public static final MaryDataType TEXT = new MaryDataType("TEXT", true, false, PLAIN_TEXT);
	public static final MaryDataType TOKENS = new MaryDataType("TOKENS", true, true, MARYXML, MaryXML.MARYXML);
	public static final MaryDataType WORDS = new MaryDataType("WORDS", true, true, MARYXML, MaryXML.MARYXML);

	// ///////////////////////////////////////////////////////////////////
	// //////////////////////// One MaryDataType /////////////////////////
	// ///////////////////////////////////////////////////////////////////

	private String name;
	private boolean isInputType;
	private boolean isOutputType;
	private Traits traits;
	private String rootElement; // for XML types
	private String endMarker;

	public MaryDataType(String name, boolean isInputType, boolean isOutputType, Traits traits) {
		this(name, isInputType, isOutputType, traits, null);
	}

	public MaryDataType(String name, boolean isInputType, boolean isOutputType, Traits traits, String rootElement) {
		this.name = name;
		this.isInputType = isInputType;
		this.isOutputType = isOutputType;
		this.traits = traits;
		this.rootElement = rootElement;
	}

	public String name() {
		return name;
	}

	public boolean isInputType() {
		return isInputType;
	}

	public boolean isOutputType() {
		return isOutputType;
	}

	public boolean isTextType() {
		return traits.isTextType();
	}

	public boolean isXMLType() {
		return traits.isXMLType();
	}

	public boolean isMaryXML() {
		return traits.isMaryXML();
	}

	public String rootElement() {
		return rootElement;
	}

	public String endMarker() {
		if (isXMLType())
			return "</" + rootElement() + ">";
		else
			return endMarker;
	}

	/**
	 * Provide an example text for this data type, for the given locale, if one is available.
	 * 
	 * @param locale
	 *            locale
	 * @return an example text string, or null if none could be obtained.
	 */
	public String exampleText(Locale locale) {
		return MaryDataType.getExampleText(this, locale);
	}

	public String toString() {
		return name;
	}

	protected Traits getTraits() {
		return traits;
	}

	// ///////////////////////////////////////////////////////////////////
	// //////////////////////// Static registry //////////////////////////
	// ///////////////////////////////////////////////////////////////////

	private static List<MaryDataType> knownDataTypes;
	private static Map<String, MaryDataType> dataTypesByName;
	private static boolean registrationComplete;
	private static Logger logger;

	/**
	 * Create a new, empty data type registry.
	 */
	static {
		knownDataTypes = new LinkedList<MaryDataType>();
		dataTypesByName = new HashMap<String, MaryDataType>();
		registrationComplete = false;
		logger = MaryUtils.getLogger("MaryDataType");
	}

	public static void registerDataType(MaryDataType type) {
		if (!dataTypesByName.containsKey(type.name())) {
			dataTypesByName.put(type.name(), type);
			knownDataTypes.add(type);
		}
	}

	/**
	 * Determine whether or not the registration is complete. When the registration is not (yet) complete, calls to
	 * 
	 * 
	 * @return false when the registration is still open, true when it is complete.
	 */
	public static boolean getRegistrationComplete() {
		return registrationComplete;
	}

	/**
	 * Indicate that the registration is now complete. No further calls to registerModules() will be possible.
	 * 
	 * @throws IllegalStateException
	 *             if called when registration was already completed before.
	 */
	public static void setRegistrationComplete() throws IllegalStateException {
		if (registrationComplete)
			throw new IllegalStateException("Registration has already completed, cannot do that a second time");

		sortDataTypes();
		registrationComplete = true;
	}

	private static void sortDataTypes() {
		Collections.sort(knownDataTypes, new Comparator<MaryDataType>() {
			public int compare(MaryDataType one, MaryDataType two) {
				// First, sort by input type / output type status:
				if (one.isInputType() && !two.isInputType()) {
					return -1; // one is first
				} else if (two.isInputType() && !one.isInputType()) {
					return 1; // two is first
					// Now, either both or none of them are input types
				} else if (one.isOutputType() && !two.isOutputType()) {
					return 1; // two is first
				} else if (two.isOutputType() && !one.isOutputType()) {
					return -1; // one is first
				} else if (one.isInputType() && two.isInputType() && !one.isOutputType() && !two.isOutputType()) {
					// Both are only input types -> if one is plain text, the other XML,
					// text goes first
					if (!one.isXMLType() && two.isXMLType()) {
						return -1; // one is first
					} else if (!two.isXMLType() && one.isXMLType()) {
						return 1; // two is first
					}
				} else if (!one.isInputType() && !two.isInputType() && !one.isOutputType() && !two.isOutputType()) {
					return 0; // both are equal
				}
				if (one.isMaryXML() && two.isXMLType() && !two.isMaryXML()) {
					return 1; // xml input format two is first
				} else if (two.isMaryXML() && one.isXMLType() && !one.isMaryXML()) {
					return -1; // xml input format one is first
				}
				if (one.name().equals("TEXT") && !two.name().equals("TEXT")) {
					return -1; // one is first
				} else if (two.name().equals("TEXT") && !one.name().equals("TEXT")) {
					return 1; // two is first
				}
				if (one.name().startsWith("TEXT") && !two.name().startsWith("TEXT")) {
					return -1; // one is first
				} else if (two.name().startsWith("TEXT") && !one.name().startsWith("TEXT")) {
					return 1; // two is first
				}
				if (ModuleRegistry.modulesRequiredForProcessing(one, two, null) == null) {
					// if (modulesRequiredForProcessing(two, one, null) == null)
					// System.err.println("cannot compare the datatypes " + one.name() + " and " + two.name());
					return 1; // two is first
				} else {
					return -1; // one is first
				}
			}
		});
		// A test said this was sorted in 12 ms, so no harm done.
	}

	/**
	 * Provide the names of all registered data types that can be used as input.
	 * 
	 * @return a vector containing the string names of data types.
	 * @throws IllegalStateException
	 *             if this method is called while registration is ongoing.
	 */
	public static Vector<String> getInputTypeStrings() {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about data types while registration is ongoing");
		Vector<String> result = new Vector<String>(10);
		for (MaryDataType t : knownDataTypes) {
			if (t.isInputType())
				result.add(t.name());
		}
		return result;
	}

	/**
	 * Provide the names of all registered data types that can be used as output.
	 * 
	 * @return a vector containing the string names of data types.
	 * @throws IllegalStateException
	 *             if this method is called while registration is ongoing.
	 */
	public static Vector<String> getOutputTypeStrings() {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about data types while registration is ongoing");
		Vector<String> result = new Vector<String>(10);
		for (MaryDataType t : knownDataTypes) {
			if (t.isOutputType())
				result.add(t.name());
		}
		return result;
	}

	/**
	 * Provide the list of all registered data types that can be used as input.
	 * 
	 * @return a list containing data types.
	 * @throws IllegalStateException
	 *             if this method is called while registration is ongoing.
	 */
	public static List<MaryDataType> getInputTypes() {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about data types while registration is ongoing");
		Vector<MaryDataType> result = new Vector<MaryDataType>(10);
		for (MaryDataType t : knownDataTypes) {
			if (t.isInputType())
				result.add(t);
		}
		return result;
	}

	/**
	 * Provide the list of all registered data types that can be used as output.
	 * 
	 * @return a list containing data types.
	 * @throws IllegalStateException
	 *             if this method is called while registration is ongoing.
	 */
	public static List<MaryDataType> getOutputTypes() {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about data types while registration is ongoing");
		Vector<MaryDataType> result = new Vector<MaryDataType>(10);
		for (MaryDataType t : knownDataTypes) {
			if (t.isOutputType())
				result.add(t);
		}
		return result;
	}

	/**
	 * Look up a data type by name.
	 * 
	 * @param name
	 *            the name of the data type
	 * @return the requested data type, or null if there is no known data type with the given name
	 */
	public static MaryDataType get(String name) {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about data types while registration is ongoing");
		return dataTypesByName.get(name);
	}

	/**
	 * Provide a list of known data types, i.e. of data types used by any of the known modules, partially sorted in the order of
	 * processing.
	 * 
	 * @return a list of all known data types
	 */
	public static List<MaryDataType> getDataTypes() {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about data types while registration is ongoing");
		return Collections.unmodifiableList(knownDataTypes);
	}

	// ///////////////////////////////////////////////////////////////////
	// //////////////////////// Example texts ////////////////////////////
	// ///////////////////////////////////////////////////////////////////

	/**
	 * Get an example text for the given type and the given locale.
	 * 
	 * @param type
	 *            type
	 * @param locale
	 *            locale
	 * @return an example text suitable for type and locale, or null if none is available.
	 */
	public static String getExampleText(MaryDataType type, Locale locale) {
		// Convention:
		// - example texts for locale null are Resources in same package as type;
		// - example texts for a given locale are in marytts.language.<locale>.datatypes.
		// - example texts are always encoded as UTF-8.
		InputStream exampleStream;
		if (locale == null) {
			exampleStream = type.getClass().getResourceAsStream(type.name() + ".example");
		} else {
			exampleStream = type.getClass()
					.getResourceAsStream(
							"/marytts/language/" + locale.toString() + "/datatypes/" + type.name() + "." + locale.toString()
									+ ".example");
		}
		if (exampleStream == null)
			return null;
		try {
			return IOUtils.toString(exampleStream, "UTF-8");
		} catch (IOException e) {
			logger.debug("Could not get example text for " + type.name() + " / locale " + locale);
			return null;
		}
	}

	// ///////////////////////////////////////////////////////////////////
	// //////////////////////// Helper: Traits ///////////////////////////
	// ///////////////////////////////////////////////////////////////////

	public static class Traits {
		private boolean isTextType; // alternative: binary data
		private boolean isXMLType;
		private boolean isMaryXML;
		private boolean isUtterances;

		public Traits(boolean isTextType, boolean isXMLType, boolean isMaryXML, boolean isUtterances) {
			// Some plausibility checks:
			if (isMaryXML)
				assert isXMLType;
			if (isXMLType)
				assert isTextType;
			if (isTextType)
				assert !isUtterances;

			this.isTextType = isTextType;
			this.isXMLType = isXMLType;
			this.isMaryXML = isMaryXML;
			this.isUtterances = isUtterances;
		}

		public boolean isTextType() {
			return isTextType;
		}

		public boolean isXMLType() {
			return isXMLType;
		}

		public boolean isMaryXML() {
			return isMaryXML;
		}

		public boolean isUtterances() {
			return isUtterances;
		}
	}
}
