/**
 * Copyright 2003 DFKI GmbH.
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
package marytts.language.it;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import marytts.config.MaryConfiguration;
import marytts.MaryException;
import marytts.exceptions.MaryConfigurationException;
import marytts.data.Utterance;

import org.apache.logging.log4j.core.Appender;

/**
 * Italian Tokenizer using JTok for Italian
 *
 * @author Fabio Tesser
 */
public class JTokenizer extends marytts.modules.nlp.JTokenizer {

    public JTokenizer() throws MaryConfigurationException {
	super();
    }

    public void checkStartup() throws MaryConfigurationException {
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        super.checkInput(utt);
    }

    public Utterance process(Utterance utt, MaryConfiguration configuration) throws Exception {
        return super.process(utt);
    }
}
