/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.modules;

import java.util.Arrays;

import opennlp.maxent.MaxentModel;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.postag.POSContextGenerator;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.postag.TagDictionary;
import opennlp.tools.util.BeamSearch;

/**
 * Extend POSTaggerME to support deterministic symbols dictionary.
 * It permit to add constraints on the only tokens allowed to have a specific label.
 * It is useful to force only the punctuation symbols to have a specific label.
 * The following line is an example of a row in the deterministic dictionary file:
 * $PUNCT , . ! ? ;
 * 
 * @author Fabio Tesser
 * 
 */
public class PosTaggerMEDetSymSupport extends POSTaggerME {

    /**
     * Inverse Tag dictionary used for deterministic token like punctuation symbols
     * The dictionary file format is inverted to respect the tagdictionary the first column is for the POS label, 
     * the following are a list of ONLY tokens admitted to have the previous POS label     
     */
    protected TagDictionary deterministicSymbolsTagDictionary;

    
    /**
     * @param model
     * @param cg
     * @param deterministic_symbols_tagdict
     */
    public PosTaggerMEDetSymSupport(MaxentModel model, POSContextGenerator cg, TagDictionary deterministic_symbols_tagdict) {
        super(model, cg, null);
        deterministicSymbolsTagDictionary = deterministic_symbols_tagdict;
        // new PosBeamSearch that take into account deterministicSymbolsTagDictionary
        beam = new PosBeamSearchDeterministicSymbolsDict(size, cg, model);
    }
    
    
    /**
     * @param model
     * @param cg
     * @param tagdict
     * @param deterministic_symbols_tagdict
     */
    public PosTaggerMEDetSymSupport(MaxentModel model, POSContextGenerator cg, TagDictionary tagdict, TagDictionary deterministic_symbols_tagdict) {
        super(model, cg, tagdict);
        deterministicSymbolsTagDictionary = deterministic_symbols_tagdict;
        // new PosBeamSearch that take into account both tagDictionary and deterministicSymbolsTagDictionary
        beam = new PosBeamSearchBothDicts(size, cg, model);
    }

    /**
     * BeamSearch class to support constraints given by the deterministic symbols dictionary
     * 
     */
    private class PosBeamSearchDeterministicSymbolsDict extends BeamSearch {

        PosBeamSearchDeterministicSymbolsDict(int size, POSContextGenerator cg, MaxentModel model) {
            super(size, cg, model);
        }

        PosBeamSearchDeterministicSymbolsDict(int size, POSContextGenerator cg, MaxentModel model, int cacheSize) {
            super(size, cg, model, cacheSize);
        }

        protected boolean validSequence(int i, Object[] inputSequence, String[] outcomesSequence, String outcome) {
            String[] tags = deterministicSymbolsTagDictionary.getTags(outcome);
            if (tags == null) {
                return true;
            } else {
                return Arrays.asList(tags).contains(inputSequence[i].toString());
            }
        }
    }

    /**
     * BeamSearch class to support both the constraints given by the probabilistic dictionary and the deterministic symbols dictionary
     * 
     */
    private class PosBeamSearchBothDicts extends BeamSearch {

        PosBeamSearchBothDicts(int size, POSContextGenerator cg, MaxentModel model) {
            super(size, cg, model);
        }

        PosBeamSearchBothDicts(int size, POSContextGenerator cg, MaxentModel model, int cacheSize) {
            super(size, cg, model, cacheSize);
        }

        protected boolean validSequence(int i, Object[] inputSequence, String[] outcomesSequence, String outcome) {
            String[] tags = null;
            boolean tmp = false;
            tags = deterministicSymbolsTagDictionary.getTags(outcome);

            if (tags != null) {
                // oK we are talking about FS...
                tmp = Arrays.asList(tags).contains(inputSequence[i].toString());
                if (!tmp) {
                    // if det_tagDictionary FS does not contain the input sequence ","
                    return false;
                }
            }
            // Ok the det_tagDictionary contains the correct  input sequence "," or tags == null
            // check for normal tag dict
            tags = tagDictionary.getTags(inputSequence[i].toString());
            if (tags == null) {
                // we are not talinkg about ancora
                return true;
            } else {
                // we are talinkg about "ancora"
                // retrurn treue if the outcome is OK with that
                return Arrays.asList(tags).contains(outcome);
            }
        }
    }

    
}
