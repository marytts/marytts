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

package marytts.language.it ;


// IO
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.apache.commons.io.FileUtils;

// Collections
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

// Parsing
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.google.common.base.Splitter;

// Locale
import java.util.Locale;

// Configuration
import marytts.config.MaryConfiguration;
import marytts.config.MaryConfigurationFactory;
import marytts.exceptions.MaryConfigurationException;

// Main mary
import marytts.MaryException;
import marytts.fst.FSTLookup;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.modules.nlp.phonemiser.TrainedLTS;
import marytts.modules.MaryModule;
import marytts.modules.synthesis.PAConverter;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;

// Data
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.utils.IntegerPair;
import marytts.data.item.linguistic.Word;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Syllable;
import marytts.data.item.phonology.Accent;

// Logging
import org.apache.logging.log4j.core.Appender;


/**
 * The phonemiser module -- java implementation.
 *
 * @author Marc Schr&ouml;der
 */

public class JPhonemiser extends marytts.modules.nlp.JPhonemiser {

    public JPhonemiser() throws MaryConfigurationException {
        super(Locale.ITALIAN);
    }

    public void startup() throws MaryException {
        super.startup();

	setAllophoneSet(this.getClass().getResourceAsStream("/marytts/language/it/lexicon/allophones.it.xml"));
	setLexicon(this.getClass().getResourceAsStream("/marytts/language/it/lexicon/it_lexicon.fst"));
	setLetterToSound(this.getClass().getResourceAsStream("/marytts/language/it/lexicon/it.lts"));

    }
}
