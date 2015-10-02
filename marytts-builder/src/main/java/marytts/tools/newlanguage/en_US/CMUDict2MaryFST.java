/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.newlanguage.en_US;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.Syllabifier;
import marytts.tools.newlanguage.LexiconCreator;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

/**
 * This class does a one-time, offline conversion from the CMUDict in Festival format (cmudict-0.4.scm and cmudict_extensions.scm)
 * into MARY format. Specifically, the following steps are performed:
 * <ol>
 * <li>conversion to a text format without brackets, using '|' as the delimiter between three fields:
 * <code>graphemes | allophones | part-of-speech(optional)</code></li>
 * <li>conversion of the phonetic alphabet used from MRPA to SAMPA</li>
 * <li>creation of a compact FST representing the lexicon</li>
 * <li>training of Letter-to-sound rules from the data</li>
 * </ol>
 * 
 * @author marc
 *
 */
public class CMUDict2MaryFST extends LexiconCreator {
	private static final String LEXPATH = "lib/modules/en/us/lexicon/";

	public CMUDict2MaryFST() throws Exception {
		super(AllophoneSet.getAllophoneSet(LEXPATH + "allophones.en_US.xml"), LEXPATH + "cmudictSampa.txt", LEXPATH
				+ "cmudict.fst", LEXPATH + "cmudict.lts", true, // convert to lowercase
				true, // predict stress
				3 // number of characters to the left and to the right to use for prediction
		);
	}

	@Override
	protected void prepareLexicon() throws IOException {
		File cmudict = new File(LEXPATH + "cmu/cmudict-0.4.scm");
		if (!cmudict.exists())
			throw new IllegalStateException("This program should be called from the MARY base directory.");
		File extensions = new File(LEXPATH + "cmu/cmudict_extensions.scm");
		File cmudictSampa = new File(lexiconFilename);

		// Convert to SAMPA text dictionary
		logger.info("Converting dictionary to MARY text format...");
		mrpa2sampa = new HashMap<String, String>();
		fillSampaMap();

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(cmudict), "ASCII"));
		PrintWriter toSampa = new PrintWriter(cmudictSampa, "UTF-8");
		convertToSampa(br, toSampa);
		br.close();
		br = new BufferedReader(new InputStreamReader(new FileInputStream(extensions), "ASCII"));
		convertToSampa(br, toSampa);
		br.close();
		toSampa.close();
		logger.info("...done!\n");

	}

	private Map<String, String> mrpa2sampa;

	private void fillSampaMap() {
		// Any phone inventory mappings?
		String sampamapFilename = "lib/modules/en/synthesis/sampa2mrpa_en.map";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sampamapFilename), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.equals("") || line.startsWith("#")) {
					continue; // ignore empty and comment lines
				}
				try {
					addSampaMapEntry(line);
				} catch (IllegalArgumentException iae) {
					throw new IllegalArgumentException("Ignoring invalid entry in sampa map file " + sampamapFilename, iae);
				}
			}
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Cannot open file '" + sampamapFilename + "'", ioe);
		}
	}

	private void addSampaMapEntry(String entry) throws IllegalArgumentException {
		boolean s2v = false;
		boolean v2s = false;
		String[] parts = null;
		// For one-to-many mappings, '+' can be used to group phone symbols.
		// E.g., the line "EI->E:+I" would map "EI" to "E:" and "I"
		entry = entry.replace('+', ' ');
		if (entry.indexOf("<->") != -1) {
			parts = entry.split("<->");
			s2v = true;
			v2s = true;
		} else if (entry.indexOf("->") != -1) {
			parts = entry.split("->");
			s2v = true;
		} else if (entry.indexOf("<-") != -1) {
			parts = entry.split("<-");
			v2s = true;
		}
		if (parts == null || parts.length != 2) { // invalid entry
			throw new IllegalArgumentException();
		}
		if (v2s) {
			mrpa2sampa.put(parts[1].trim(), parts[0].trim());
		}
	}

	/**
	 * Converts a single phonetic symbol in MRPA representation representation into its equivalent in MARY sampa representation.
	 * 
	 * @param voicePhoneme
	 *            voicePhoneme
	 * @return the converted phone, or the input string if no known conversion exists.
	 */
	private String mrpa2sampa(String voicePhoneme) {
		if (mrpa2sampa.containsKey(voicePhoneme))
			return mrpa2sampa.get(voicePhoneme);
		else
			return voicePhoneme;
	}

	private String mrpaString2sampaString(String mrpaString) {
		StringTokenizer st = new StringTokenizer(mrpaString);
		LinkedList<String> sampaList = new LinkedList<String>();
		while (st.hasMoreTokens()) {
			String mrpa = st.nextToken();
			String sampa;
			if (mrpa.endsWith("1")) {
				sampa = mrpa2sampa(mrpa.substring(0, mrpa.length() - 1)) + "1";
			} else if (mrpa.endsWith("0")) {
				sampa = mrpa2sampa(mrpa.substring(0, mrpa.length() - 1));
			} else {
				sampa = mrpa2sampa(mrpa);
			}
			sampaList.add(sampa);
		}
		new Syllabifier(allophoneSet).syllabify(sampaList);
		StringBuilder sb = new StringBuilder();
		for (String s : sampaList) {
			if (sb.length() > 0)
				sb.append(" ");
			sb.append(s);
		}
		return sb.toString();
	}

	private void convertToSampa(BufferedReader br, PrintWriter toSampa) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			// skip comments:
			if (line.startsWith(";") || line.equals(""))
				continue;
			// expected line format:
			// ("acquirer" nil (ax k w ay1 er0 er0))
			int firstQuote = line.indexOf('"');
			if (!(firstQuote >= 0)) {
				System.err.println("Skipping strange line (no first quote): " + line);
			}
			int secondQuote = line.indexOf('"', firstQuote + 1);
			if (!(secondQuote > firstQuote)) {
				System.err.println("Skipping strange line (no second quote): " + line);
			}
			int firstSpace = secondQuote + 1;
			if (!(line.charAt(firstSpace) == ' ')) {
				System.err.println("Skipping strange line (no first space): " + line);
			}
			int secondSpace = line.indexOf(' ', firstSpace + 1);
			if (!(secondSpace > firstSpace)) {
				System.err.println("Skipping strange line (no second space): " + line);
			}
			int firstBracket = secondSpace + 1;
			if (!(line.charAt(firstBracket) == '(')) {
				System.err.println("Skipping strange line (no first bracket): " + line);
			}
			int secondBracket = line.indexOf(')', firstBracket + 1);
			if (!(secondBracket > firstBracket)) {
				System.err.println("Skipping strange line (no second bracket): " + line);
			}
			String graphemes = line.substring(firstQuote + 1, secondQuote);
			String pos = line.substring(firstSpace + 1, secondSpace);
			if (pos.equals("nil"))
				pos = "";
			else
				pos = "(" + pos + ")";
			String allophones = line.substring(firstBracket + 1, secondBracket);
			String sampaString = mrpaString2sampaString(allophones);
			toSampa.println(graphemes + " | " + sampaString + " | " + pos);
		}
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		PatternLayout layout = new PatternLayout("%d %m\n");
		BasicConfigurator.configure(new ConsoleAppender(layout));

		CMUDict2MaryFST c2m = new CMUDict2MaryFST();
		c2m.createLexicon();
	}

}
