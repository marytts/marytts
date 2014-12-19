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
package marytts.language.en;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.datatypes.MaryData;
import marytts.language.en_US.datatypes.USEnglishDataTypes;
import marytts.modules.InternalModule;
import marytts.modules.synthesis.FreeTTSVoices;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

/**
 * Use an individual FreeTTS module for English synthesis.
 * 
 * @author Marc Schr&ouml;der
 */

public class FreeTTSPartOfSpeechTagger extends InternalModule {
	// private UtteranceProcessor processor;
	private Logger logger;

	private Map posMap;

	public FreeTTSPartOfSpeechTagger() {
		super("PartOfSpeechTagger", USEnglishDataTypes.FREETTS_WORDS, USEnglishDataTypes.FREETTS_POS, Locale.ENGLISH);

	}

	public void startup() throws Exception {
		super.startup();
		this.logger = MaryUtils.getLogger("FreeTTSPOSTagger");
		buildPosMap();
		// Initialise FreeTTS
		FreeTTSVoices.load();
		// processor = new PartOfSpeechTagger();
	}

	public MaryData process(MaryData d) throws Exception {
		List utterances = d.getUtterances();
		Iterator it = utterances.iterator();
		while (it.hasNext()) {
			Utterance utterance = (Utterance) it.next();
			// processor.processUtterance(utterance);
			processUtterance(utterance);
		}
		MaryData output = new MaryData(outputType(), d.getLocale());
		output.setUtterances(utterances);
		return output;
	}

	private void buildPosMap() {
		posMap = new HashMap();
		try {
			String posFile = MaryProperties.getFilename("english.freetts.posfile");
			BufferedReader reader = new BufferedReader(new FileReader(new File(posFile)));
			String line = reader.readLine();
			while (line != null) {
				if (!(line.startsWith("***"))) {
					// System.out.println(line);
					StringTokenizer st = new StringTokenizer(line, " ");
					String word = st.nextToken();
					String pos = st.nextToken();
					posMap.put(word, pos);
				}
				line = reader.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Unable to build PoS-Map");
		}
	}

	private void processUtterance(Utterance utt) {
		logger.debug("Tagging part of speech...");
		for (Item word = utt.getRelation(Relation.WORD).getHead(); word != null; word = word.getNext()) {
			String pos = null;
			if (posMap.containsKey(word.toString())) {
				pos = (String) posMap.get(word.toString());
				logger.debug("Assigning pos \"" + pos + "\" to word \"" + word.toString() + "\"");
			} else {
				pos = "content";
			}
			word.getFeatures().setString("pos", pos);

		}

	}

}
