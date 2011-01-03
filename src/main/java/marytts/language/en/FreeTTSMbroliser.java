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

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.language.en_US.datatypes.USEnglishDataTypes;
import marytts.modules.InternalModule;
import marytts.modules.synthesis.FreeTTSVoices;

import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.UtteranceProcessor;

import de.dfki.lt.freetts.mbrola.ParametersToMbrolaConverter;


/**
 * Convert the timing data from FreeTTS into MBROLA format.
 *
 * @author Marc Schr&ouml;der
 */

public class FreeTTSMbroliser extends InternalModule
{
    private UtteranceProcessor processor;

    public FreeTTSMbroliser()
    {
        super("Parameter Mbroliser",
              USEnglishDataTypes.FREETTS_CONTOUR,
              MaryDataType.FREETTS_ACOUSTPARAMS,
              Locale.ENGLISH
              );
    }

    public void startup() throws Exception
    {
        super.startup();

        // Initialise FreeTTS
        FreeTTSVoices.load();
        processor = new ParametersToMbrolaConverter();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        List utterances = d.getUtterances();
        Iterator it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = (Utterance) it.next();
            processor.processUtterance(utterance);
        }
        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setUtterances(utterances);
        return output;
    }




}

