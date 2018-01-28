/**
 * Copyright 2002 DFKI GmbH.
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

package marytts.language.de;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;

import marytts.data.Utterance;
import marytts.modules.MaryModule;
import marytts.MaryException;

import org.apache.logging.log4j.core.Appender;
/**
 * The preprocessing module.
 *
 * @author Marc Schr&ouml;der
 */

public class Preprocess extends MaryModule {


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
    }

    public Utterance process(Utterance utt, MaryConfiguration configuration) throws Exception {
        return utt;
    }
}
