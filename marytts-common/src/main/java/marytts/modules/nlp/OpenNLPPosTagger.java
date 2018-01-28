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
package marytts.modules.nlp;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.modules.MaryModule;

import marytts.data.Utterance;
import marytts.util.MaryUtils;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.Paragraph;
import marytts.data.item.linguistic.Word;

import org.w3c.dom.Document;

import marytts.MaryException;

import org.apache.logging.log4j.core.Appender;

/**
 * Part-of-speech tagger using OpenNLP.
 *
 * @author Marc Schr&ouml;der
 */

public abstract class OpenNLPPosTagger extends MaryModule {
    private String propertyPrefix;
    private POSTaggerME tagger;
    private Map<String, String> posMapper = null;

    /**
     * Constructor which can be directly called from init info in the config
     * file. Different languages can call this code with different settings.
     *
     * @param locale
     *            a locale string, e.g. "en"
     * @param propertyPrefix
     *            propertyPrefix
     * @throws Exception
     *             Exception
     */
    protected OpenNLPPosTagger() throws Exception {
        super();
    }

    public void startup() throws Exception {
	getDefaultConfiguration().applyConfiguration(this);
        super.startup();
    }

    public void checkStartup() throws MaryConfigurationException {
	if (tagger == null)
	    throw new MaryConfigurationException("The tagger is null and should not be");

    }

    public void setModel(InputStream model_stream) throws IOException {
        tagger = new POSTaggerME(new POSModel(model_stream));
        model_stream.close();
    }

    public void setPosmap(InputStream pos_map_stream) throws IOException {
	posMapper = new HashMap<String, String>();
	BufferedReader br = new BufferedReader(new InputStreamReader(pos_map_stream, "UTF-8"));
	String line;
	while ((line = br.readLine()) != null) {
	    // skip comments and empty lines
	    if (line.startsWith("#") || line.trim().equals("")) {
		continue;
	    }
	    // Entry format: POS GPOS, i.e. two space-separated entries per
	    // line
	    StringTokenizer st = new StringTokenizer(line);
	    String pos = st.nextToken();
	    String gpos = st.nextToken();
	    posMapper.put(pos, gpos);
	}
	pos_map_stream.close();
    }


    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.WORD)) {
            throw new MaryException("Word sequence is missing", null);
        }
    }

    @SuppressWarnings("unchecked")
    public Utterance process(Utterance utt, MaryConfiguration configuration) throws Exception {

        // Generate the list of word in the sentence
        List<String> tokens = new ArrayList<String>();
        for (Word w : (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD)) {
            tokens.add(w.getText());
        }

        // Trick the system in case of one ==> add a punctuation
        if (tokens.size() == 1) {
            tokens.add(".");
        }

        // POS Tagging
        List<String> partsOfSpeech = null;

        String[] tokensArr = new String[tokens.size()];
        tokensArr = tokens.toArray(tokensArr);
        synchronized (this) {
            partsOfSpeech = Arrays.asList(tagger.tag(tokensArr));
        }

        // Associate POS to words
        Iterator<String> posIt = partsOfSpeech.iterator();
        for (Word w : (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD)) {
            assert posIt.hasNext();
            String pos = posIt.next();

            if (w.getPOS() != null) {
                continue;
            }

            if (posMapper != null) {
                String gpos = posMapper.get(pos);
                if (gpos == null) {
                    logger.warn("POS map file incomplete: do not know how to map '" + pos + "'");
                } else {
                    pos = gpos;
                }
            }
            w.setPOS(pos);
        }
        return utt;
    }

}
