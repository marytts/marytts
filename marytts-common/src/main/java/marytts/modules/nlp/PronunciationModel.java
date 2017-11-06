/**
 * Copyright 2008 DFKI GmbH.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.config.MaryProperties;
import marytts.modules.MaryModule;

import marytts.data.Utterance;
import marytts.modeling.features.FeatureDefinition;
import marytts.modules.nlp.phonemiser.Allophone;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.config.MaryProperties;
import marytts.util.MaryRuntimeUtils;

import marytts.data.item.phonology.Syllable;
import marytts.data.item.phonology.Accent;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.linguistic.Word;
import marytts.data.item.prosody.Phrase;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.utils.IntegerPair;


import org.apache.logging.log4j.core.Appender;
/**
 *
 * This module serves as a post-lexical pronunciation model. Its appropriate
 * place in the module chain is after intonisation. The target features are
 * taken and fed into decision trees that predict the new pronunciation. A new
 * mary xml is output, with the difference being that the old pronunciation is
 * replaced by the newly predicted one, and a finer grained xml structure.
 *
 * @author ben
 *
 */
public class PronunciationModel extends MaryModule {

    // used in startup() and later for convenience
    private FeatureDefinition featDef;

    /**
     * Constructor, stating that the input is of type INTONATION, the output of
     * type ALLOPHONES.
     *
     */
    public PronunciationModel() {
        this(null);
    }

    public PronunciationModel(Locale locale) {
        super("PronunciationModel", locale);
    }

    public void startup() throws Exception {
        super.startup();

        // TODO: pronunciation model tree and feature definition should be
        // voice-specific
        // get featureDefinition used for trees - just to tell the tree that the
        // features are discrete
        String fdFilename = null;
        if (getLocale() != null) {
            fdFilename = MaryProperties
                         .getFilename(MaryProperties.localePrefix(getLocale()) + ".pronunciation.featuredefinition");
        }
        if (fdFilename != null) {
            File fdFile = new File(fdFilename);
            // reader for file, readweights = false
            featDef = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), false);

            logger.debug("Reading in feature definition finished.");

        }
        logger.debug("Building feature computer finished.");
    }

    /**
     * Optionally, a language-specific subclass can implement any postlexical
     * rules on the document.
     *
     * @param token
     *            a &lt;t&gt; element with a syllable and &lt;ph&gt;
     *            substructure.
     * @param allophoneSet
     *            allophoneSet
     * @return true if something was changed, false otherwise
     */
    protected boolean postlexicalRules(Word token, AllophoneSet allophoneSet) {
        return false;
    }

    /**
     * This computes a new pronunciation for the elements of some Utterance, that
     * is phonemised.
     *
     * @param d
     *            d
     * @throws Exception
     *             Exception
     */
    public Utterance process(Utterance utt, MaryProperties configuration, Appender app) throws Exception {
        return utt;
    }
}
