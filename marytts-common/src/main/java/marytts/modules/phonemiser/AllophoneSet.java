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
package marytts.modules.phonemiser;

/**
 * @author ingmar
 */
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

public class AllophoneSet {
	private static Map<String, AllophoneSet> allophoneSets = new HashMap<String, AllophoneSet>();

	/**
	 * Return the allophone set specified by the given filename. It will only be loaded if it was not loaded before.
	 * 
	 * @param filename
	 *            filename
	 * @return the allophone set, if one can be created. This method will never return null.
	 * @throws MaryConfigurationException
	 *             if no allophone set can be loaded from the given file.
	 */
	public static AllophoneSet getAllophoneSet(String filename) throws MaryConfigurationException {
		InputStream fis = null;
		try {
			fis = new FileInputStream(filename);
		} catch (IOException e) {
			throw new MaryConfigurationException("Problem reading allophone file " + filename, e);
		}
		assert fis != null;
		return getAllophoneSet(fis, filename);
	}

	/**
	 * Determine whether the registry of previously loaded allophone sets already contains an allophone set with the given
	 * identifier. If this returns true, then a call to {@link #getAllophoneSetById(String)} with the same identifier will return
	 * a non-null Allophone set.
	 * 
	 * @param identifier
	 *            the identifier of the allophone set to test.
	 * @return true if the registry already contains the given allophone set, false otherwise.
	 */
	public static boolean hasAllophoneSet(String identifier) {
		return allophoneSets.containsKey(identifier);
	}

	/**
	 * Get a previously loaded allophone set by its identifier. The method will make no attempt to load the allophone set if it is
	 * not yet available.
	 * 
	 * @param identifier
	 *            the identifier of the allophone set
	 * @return the allophone set if available, null otherwise.
	 */
	public static AllophoneSet getAllophoneSetById(String identifier) {
		return allophoneSets.get(identifier);
	}

	/**
	 * Return the allophone set that can be read from the given input stream, identified by the given identifier. It will only be
	 * loaded if it was not loaded before.
	 * 
	 * @param inStream
	 *            an open stream from which the allophone set can be loaded. it will be closed when this method returns.
	 * @param identifier
	 *            a unique identifier for this allophone set.
	 * @return the allophone set, if one can be created. This method will never return null.
	 * @throws MaryConfigurationException
	 *             if no allophone set can be loaded from the given file.
	 */

	public static AllophoneSet getAllophoneSet(InputStream inStream, String identifier) throws MaryConfigurationException {
		AllophoneSet as = allophoneSets.get(identifier);
		if (as == null) {
			// Need to load it:
			try {
				as = new AllophoneSet(inStream);
			} catch (MaryConfigurationException e) {
				throw new MaryConfigurationException("Problem loading allophone set from " + identifier, e);
			}
			allophoneSets.put(identifier, as);
		} else {
			try {
				inStream.close();
			} catch (IOException e) {
				// ignore
			}
		}
		assert as != null;
		return as;
	}

	// //////////////////////////////////////////////////////////////////

	private String name; // the name of the allophone set
	private Locale locale; // the locale of the allophone set, e.g. US English
	// The map of segment objects, indexed by their phonetic symbol:
	private Map<String, Allophone> allophones = null;
	// Map feature names to the list of possible values in this AllophoneSet
	private Map<String, String[]> featureValueMap = null;

	private Allophone silence = null;
	private String ignore_chars = null;
	// The number of characters in the longest Allophone symbol
	private int maxAllophoneSymbolLength = 1;

	private AllophoneSet(InputStream inputStream) throws MaryConfigurationException {
		allophones = new TreeMap<String, Allophone>();
		// parse the xml file:
		Document document;
		try {
			document = DomUtils.parseDocument(inputStream);
		} catch (Exception e) {
			throw new MaryConfigurationException("Cannot parse allophone file", e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException ioe) {
				// ignore
			}
		}
		Element root = document.getDocumentElement();
		name = root.getAttribute("name");
		String xmlLang = root.getAttribute("xml:lang");
		locale = MaryUtils.string2locale(xmlLang);
		String[] featureNames = root.getAttribute("features").split(" ");

		if (root.hasAttribute("ignore_chars")) {
			ignore_chars = root.getAttribute("ignore_chars");
		}

		NodeIterator ni = DomUtils.createNodeIterator(document, root, "vowel", "consonant", "silence", "tone");
		Element a;
		while ((a = (Element) ni.nextNode()) != null) {
			Allophone ap = new Allophone(a, featureNames);
			if (allophones.containsKey(ap.name()))
				throw new MaryConfigurationException("File contains duplicate definition of allophone '" + ap.name() + "'!");
			allophones.put(ap.name(), ap);
			if (ap.isPause()) {
				if (silence != null)
					throw new MaryConfigurationException("File contains more than one silence symbol: '" + silence.name()
							+ "' and '" + ap.name() + "'!");
				silence = ap;
			}
			int len = ap.name().length();
			if (len > maxAllophoneSymbolLength) {
				maxAllophoneSymbolLength = len;
			}
		}
		if (silence == null)
			throw new MaryConfigurationException("File does not contain a silence symbol");
		// Fill the list of possible values for all features
		// such that "0" comes first and all other values are sorted alphabetically
		featureValueMap = new TreeMap<String, String[]>();
		for (String feature : featureNames) {
			Set<String> featureValueSet = new TreeSet<String>();
			for (Allophone ap : allophones.values()) {
				featureValueSet.add(ap.getFeature(feature));
			}
			if (featureValueSet.contains("0"))
				featureValueSet.remove("0");
			String[] featureValues = new String[featureValueSet.size() + 1];
			featureValues[0] = "0";
			int i = 1;
			for (String f : featureValueSet) {
				featureValues[i++] = f;
			}
			featureValueMap.put(feature, featureValues);
		}
		// Special "vc" feature:
		featureValueMap.put("vc", new String[] { "0", "+", "-" });
	}

	public Locale getLocale() {
		return locale;
	}

	/**
	 * Get the Allophone with the given name
	 * 
	 * @param ph
	 *            name of Allophone to get
	 * @return the Allophone
	 * @throws IllegalArgumentException
	 *             if the Allophone is not found in the AllophoneSet
	 */
	public Allophone getAllophone(String ph) {
		Allophone allophone = allophones.get(ph);
		if (allophone == null) {
			throw new IllegalArgumentException(String.format(
					"Allophone `%s' could not be found in AllophoneSet `%s' (Locale: %s)", ph, name, locale));
		}
		return allophone;
	}

	/**
	 * Obtain the silence allophone in this AllophoneSet
	 * 
	 * @return silence
	 */
	public Allophone getSilence() {
		return silence;
	}

	/**
	 * Obtain the ignore chars in this AllophoneSet Default: "',-"
	 * 
	 * @return ignore_chars
	 */
	public String getIgnoreChars() {
		if (ignore_chars == null) {
			return "',-";
		} else {
			return ignore_chars;
		}
	}

	/**
	 * For the Allophone with name ph, return the value of the named feature.
	 * 
	 * @param ph
	 *            ph
	 * @param featureName
	 *            feature name
	 * @return the allophone feature, or null if either the allophone or the feature does not exist.
	 */
	public String getPhoneFeature(String ph, String featureName) {
		if (ph == null)
			return null;
		Allophone a = allophones.get(ph);
		if (a == null)
			return null;
		return a.getFeature(featureName);
	}

	/**
	 * Get the list of available phone features for this allophone set.
	 * 
	 * @return Collections.unmodifiableSet(featureValueMap.keySet())
	 */
	public Set<String> getPhoneFeatures() {
		return Collections.unmodifiableSet(featureValueMap.keySet());
	}

	/**
	 * For the given feature name, get the list of all possible values that the feature can take in this allophone set.
	 * 
	 * @param featureName
	 *            featureName
	 * @throws IllegalArgumentException
	 *             if featureName is not a known feature name.
	 * @return the list of values, "0" first.
	 */
	public String[] getPossibleFeatureValues(String featureName) {
		String[] vals = featureValueMap.get(featureName);
		if (vals == null)
			throw new IllegalArgumentException("No such feature: " + featureName);
		return vals;
	}

	/**
	 * This returns the names of all allophones contained in this AllophoneSet, as a Set of Strings
	 * 
	 * @return allophoneKeySet
	 */
	public Set<String> getAllophoneNames() {
		Iterator<String> it = allophones.keySet().iterator();
		Set<String> allophoneKeySet = new TreeSet<String>();
		while (it.hasNext()) {
			String keyString = it.next();
			if (!allophones.get(keyString).isTone()) {
				allophoneKeySet.add(keyString);
			}
		}
		return allophoneKeySet;
	}

	/**
	 * Split a phonetic string into allophone symbols. Symbols representing primary and secondary stress, syllable boundaries, and
	 * spaces, will be silently skipped.
	 * 
	 * @param allophoneString
	 *            the phonetic string to split
	 * @return an array of Allophone objects corresponding to the string given as input
	 * @throws IllegalArgumentException
	 *             if the allophoneString contains unknown symbols.
	 */
	public Allophone[] splitIntoAllophones(String allophoneString) {
		List<String> phones = splitIntoAllophoneList(allophoneString, false);
		Allophone[] allos = new Allophone[phones.size()];
		for (int i = 0; i < phones.size(); i++) {
			try {
				allos[i] = getAllophone(phones.get(i));
			} catch (IllegalArgumentException e) {
				throw e;
			}
		}
		return allos;
	}

	/**
	 * Split allophone string into a list of allophone symbols. Include stress markers (',) and syllable boundaries (-), skip
	 * space characters.
	 * 
	 * @param allophoneString
	 *            allophoneString
	 * @throws IllegalArgumentException
	 *             if the string contains illegal symbols.
	 * @return a String containing allophones and stress markers / syllable boundaries, separated with spaces
	 */
	public String splitAllophoneString(String allophoneString) {
		List<String> phones = splitIntoAllophoneList(allophoneString, true);
		StringBuilder pronunciation = new StringBuilder();
		for (String a : phones) {
			if (pronunciation.length() > 0)
				pronunciation.append(" ");
			pronunciation.append(a);
		}
		return pronunciation.toString();
	}

	/**
	 * Split allophone string into a list of allophone symbols, preserving all stress and syllable boundaries that may be present
	 * 
	 * @param allophonesString
	 *            allophonesString
	 * @return a List of allophone Strings
	 * @throws IllegalArgumentException
	 *             if allophoneString contains a symbol for which no Allophone can be found
	 */
	public List<String> splitIntoAllophoneList(String allophonesString) {
		return splitIntoAllophoneList(allophonesString, true);
	}

	/**
	 * Split allophone string into a list of allophone symbols. Include (or ignore, depending on parameter
	 * 'includeStressAndSyllableMarkers') stress markers (',), syllable boundaries (-). Ignores space characters.
	 * 
	 * @param allophoneString
	 * @param includeStressAndSyllableMarkers
	 *            whether to skip stress markers and syllable boundaries. If true, will return each such marker as a separate
	 *            string in the list.
	 * @throws IllegalArgumentException
	 *             if the string contains illegal symbols.
	 * @return a list of allophone strings.
	 */
	private List<String> splitIntoAllophoneList(String allophoneString, boolean includeStressAndSyllableMarkers) {
		List<String> phones = new ArrayList<String>();
		for (int i = 0; i < allophoneString.length(); i++) {
			String one = allophoneString.substring(i, i + 1);

			// Allow modification of ignore characters in allophones.xml
			if (getIgnoreChars().contains(one)) {
				if (includeStressAndSyllableMarkers)
					phones.add(one);
				continue;
			} else if (one.equals(" ")) {
				continue;
			}
			// Try to cut off individual segments,
			// starting with the longest prefixes:
			String ph = null;
			for (int l = maxAllophoneSymbolLength; l >= 1; l--) {
				if (i + l <= allophoneString.length()) {
					ph = allophoneString.substring(i, i + l);
					// look up in allophone map:
					if (allophones.containsKey(ph)) {
						// OK, found a symbol of length l.
						i += l - 1; // together with the i++ in the for loop, move by l
						break;
					}
				}
			}
			if (ph != null && allophones.containsKey(ph)) {
				// have found a valid phone
				phones.add(ph);
			} else {
				// FIXME: temporarily handle digit suffix stress notation from legacy LTS CARTs until these are rebuilt
				String stress = null;
				switch (ph) {
				case "1":
					stress = Stress.PRIMARY;
					break;
				case "2":
					stress = Stress.SECONDARY;
					break;
				case "0":
					stress = Stress.NONE;
					break;
				}
				if (stress != null && phones.size() > 0) {
					phones.add(phones.size() - 1, stress);
				} else {
					throw new IllegalArgumentException("Found unknown symbol `" + allophoneString.charAt(i)
							+ "' in phonetic string `" + allophoneString + "' -- ignoring.");
				}
			}
		}
		return phones;
	}

	/**
	 * Check whether the given allophone string has a correct syntax according to this allophone set.
	 * 
	 * @param allophoneString
	 *            allophoneString
	 * @return true if the syntax is correct, false otherwise.
	 */
	public boolean checkAllophoneSyntax(String allophoneString) {
		try {
			splitIntoAllophoneList(allophoneString, false);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Syllabify a string of allophones. If stress markers are provided, they are preserved; otherwise, primary stress will be
	 * assigned to the initial syllable.
	 * <p>
	 * The syllabification algorithm itself follows the <i>Core Syllabification Principle (CSP)</i> from <blockquote>G.N. Clements
	 * (1990) "The role of the sonority cycle in core syllabification." In: J. Kingston &amp; M.E. Beckman (Eds.),
	 * <em>Papers in Laboratory Phonology I: Between the Grammar and Physics of Speech</em>, Ch. 17, pp. 283-333, Cambridge
	 * University Press.</blockquote>
	 *
	 * @param phoneString
	 *            phoneString
	 * @return a syllabified string; individual allophones are separated by spaces, and syllables, by dashes.
	 * @throws IllegalArgumentException
	 *             if the <b>phoneString</b> is empty or contains a symbol that satisfies none of the following conditions:
	 *             <ol>
	 *             <li>the symbol corresponds to an Allophone, or</li> <li>the symbol is a stress symbol (cf. {@link Stress}), or
	 *             </li> <li>the symbol is a syllable boundary (<code>-</code>)</li>
	 *             </ol>
	 * 
	 */
	public String syllabify(String phoneString) throws IllegalArgumentException {
		// Before we process, a sanity check:
		if (phoneString.trim().isEmpty()) {
			throw new IllegalArgumentException("Cannot syllabify empty phone string");
		}

		// First, split phoneString into a List of allophone Strings...
		List<String> allophoneStrings = splitIntoAllophoneList(phoneString, true);
		// ...and create from it a List of generic Objects
		List<Object> phonesAndSyllables = new ArrayList<Object>(allophoneStrings);

		// Create an iterator
		ListIterator<Object> iterator = phonesAndSyllables.listIterator();

		// First iteration (left-to-right):
		// CSP (a): Associate each [+syllabic] segment to a syllable node.
		Syllable currentSyllable = null;
		while (iterator.hasNext()) {
			String phone = (String) iterator.next();
			try {
				// either it's an Allophone
				Allophone allophone = getAllophone(phone);
				if (allophone.isSyllabic()) {
					// if /6/ immediately follows a non-diphthong vowel, it should be appended instead of forming its own syllable
					boolean appendR = false;
					if (allophone.getFeature("ctype").equals("r")) {
						// it's an /6/
						if (iterator.previousIndex() > 1) {
							Object previousPhoneOrSyllable = phonesAndSyllables.get(iterator.previousIndex() - 1);
							if (previousPhoneOrSyllable == currentSyllable) {
								// the /6/ immediately follows the current syllable
								if (!currentSyllable.getLastAllophone().isDiphthong()) {
									// the vowel immediately preceding the /6/ is not a diphthong
									appendR = true;
								}
							}
						}
					}
					if (appendR) {
						iterator.remove();
						currentSyllable.appendAllophone(allophone);
					} else {
						currentSyllable = new Syllable(allophone);
						iterator.set(currentSyllable);
					}
				}
			} catch (IllegalArgumentException e) {
				// or a stress or boundary marker
				if (!getIgnoreChars().contains(phone)) {
					throw e;
				}
			}
		}

		// Second iteration (right-to-left):
		// CSP (b): Given P (an unsyllabified segment) preceding Q (a syllabified segment), adjoin P to the syllable containing Q
		// iff P has lower sonority rank than Q (iterative).
		currentSyllable = null;
		boolean foundPrimaryStress = false;
		iterator = phonesAndSyllables.listIterator(phonesAndSyllables.size());
		while (iterator.hasPrevious()) {
			Object phoneOrSyllable = iterator.previous();
			if (phoneOrSyllable instanceof Syllable) {
				currentSyllable = (Syllable) phoneOrSyllable;
			} else if (currentSyllable == null) {
				// haven't seen a Syllable yet in this iteration
				continue;
			} else {
				String phone = (String) phoneOrSyllable;
				try {
					// it's an Allophone -- prepend to the Syllable
					Allophone allophone = getAllophone(phone);
					if (allophone.sonority() < currentSyllable.getFirstAllophone().sonority()) {
						iterator.remove();
						currentSyllable.prependAllophone(allophone);
					}
				} catch (IllegalArgumentException e) {
					// it's a provided stress marker -- assign it to the Syllable
					switch (phone) {
					case Stress.PRIMARY:
						iterator.remove();
						currentSyllable.setStress(Stress.PRIMARY);
						foundPrimaryStress = true;
						break;
					case Stress.SECONDARY:
						iterator.remove();
						currentSyllable.setStress(Stress.SECONDARY);
						break;
					case "-":
						iterator.remove();
						// TODO handle syllable boundaries
						break;
					default:
						throw e;
					}
				}
			}
		}

		// Third iteration (left-to-right):
		// CSP (c): Given Q (a syllabified segment) followed by R (an unsyllabified segment), adjoin R to the syllable containing
		// Q iff has a lower sonority rank than Q (iterative).
		Syllable initialSyllable = currentSyllable;
		currentSyllable = null;
		iterator = phonesAndSyllables.listIterator();
		while (iterator.hasNext()) {
			Object phoneOrSyllable = iterator.next();
			if (phoneOrSyllable instanceof Syllable) {
				currentSyllable = (Syllable) phoneOrSyllable;
			} else {
				String phone = (String) phoneOrSyllable;
				try {
					// it's an Allophone -- append to the Syllable
					Allophone allophone;
					try {
						allophone = getAllophone(phone);
					} catch (IllegalArgumentException e) {
						// or a stress or boundary marker -- remove
						if (getIgnoreChars().contains(phone)) {
							iterator.remove();
							continue;
						} else {
							throw e;
						}
					}
					if (currentSyllable == null) {
						// haven't seen a Syllable yet in this iteration
						iterator.remove();
						if (initialSyllable == null) {
							// haven't seen any syllable at all
							initialSyllable = new Syllable(allophone);
							iterator.add(initialSyllable);
						} else {
							initialSyllable.prependAllophone(allophone);
						}
					} else {
						// append it to the last seen Syllable
						iterator.remove();
						currentSyllable.appendAllophone(allophone);
					}
				} catch (IllegalArgumentException e) {
					throw e;
				}
			}
		}

		// if primary stress was not provided, assign it to initial syllable
		if (!foundPrimaryStress) {
			initialSyllable.setStress(Stress.PRIMARY);
		}

		// join Syllables with dashes and return the String
		return StringUtils.join(phonesAndSyllables, " - ");
	}

	/**
	 * Helper class for OO syllabification. Wraps an ArrayList of Allophones and has a Stress property.
	 * 
	 * @author ingmar
	 *
	 */
	private class Syllable {
		private List<Allophone> allophones = new ArrayList<Allophone>();
		private String stress = Stress.NONE;

		public Syllable(Allophone... allophones) {
			Collections.addAll(this.allophones, allophones);
		}

		public Allophone getFirstAllophone() {
			return allophones.get(0);
		}

		public void prependAllophone(Allophone allophone) {
			allophones.add(0, allophone);
		}

		public Allophone getLastAllophone() {
			return allophones.get(allophones.size() - 1);
		}

		public void appendAllophone(Allophone allophone) {
			allophones.add(allophone);
		}

		public void setStress(String stress) {
			this.stress = stress;
		}

		/**
		 * @return The Stress, if not {@link Stress.NONE NONE}, followed by the Allophones, all separated by spaces
		 */
		public String toString() {
			return String.format("%s %s", stress, StringUtils.join(allophones, " ")).trim();
		}
	}

	/**
	 * Constants for Stress markers
	 * 
	 * @author ingmar
	 *
	 */
	public interface Stress {
		String NONE = "";
		String PRIMARY = "'";
		String SECONDARY = ",";
	}
}
