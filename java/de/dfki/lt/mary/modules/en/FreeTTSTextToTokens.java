/**
 * Portions Copyright 2002 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.modules.en;

import java.util.ArrayList;
import java.util.Locale;

import com.sun.speech.freetts.Token;
import com.sun.speech.freetts.Tokenizer;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.en.us.USEnglish;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;


/**
 * Use an individual FreeTTS module for English synthesis.
 *
 * @author Marc Schr&ouml;der
 */

public class FreeTTSTextToTokens extends InternalModule
{
    public FreeTTSTextToTokens()
    {
        super("TextToTokens",
              MaryDataType.get("TEXT_EN"),
              MaryDataType.get("FREETTS_TOKENS_EN")
              );
    }

    public void startup() throws Exception
    {
        super.startup();

        // Initialise FreeTTS
        FreeTTSVoices.load();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        String text = d.getPlainText();
        // create a basic utterance from text
	Tokenizer tokenizer = new com.sun.speech.freetts.en.TokenizerImpl();
	tokenizer.setWhitespaceSymbols(USEnglish.WHITESPACE_SYMBOLS);
	tokenizer.setSingleCharSymbols(USEnglish.SINGLE_CHAR_SYMBOLS);
	tokenizer.setPrepunctuationSymbols(USEnglish.PREPUNCTUATION_SYMBOLS);
	tokenizer.setPostpunctuationSymbols(USEnglish.PUNCTUATION_SYMBOLS);
        tokenizer.setInputText(text);
        ArrayList utteranceList = new ArrayList();
        Token savedToken = null;
        boolean first = true;
        while (tokenizer.hasMoreTokens()) {
            // Fill a new Utterance:
            ArrayList tokenList = new ArrayList();
            Utterance utterance = null;
            if (savedToken != null) {
                tokenList.add(savedToken);
                savedToken = null;
            }
            while (tokenizer.hasMoreTokens()) {
                Token token = tokenizer.getNextToken();
                if ((token.getWord().length() == 0) ||
                    (tokenList.size() > 500) ||
                    tokenizer.isBreak()) {
                    savedToken = token;
                    break;
                }
                tokenList.add(token);
            }
            de.dfki.lt.mary.modules.synthesis.Voice maryVoice =
                d.getDefaultVoice();
            if (maryVoice == null ||
                !maryVoice.getLocale().equals(Locale.US)) {
                maryVoice = de.dfki.lt.mary.modules.synthesis.Voice.
                    getDefaultVoice(Locale.US);
            }

            utterance =  new Utterance
                (FreeTTSVoices.getFreeTTSVoice(maryVoice), tokenList);
            //utterance.setSpeakable(speakable);
            utterance.setFirst(first);
            first = false;
            utterance.setLast(!tokenizer.hasMoreTokens());
            utteranceList.add(utterance);
        }
        MaryData output = new MaryData(outputType());
        output.setUtterances(utteranceList);
        return output;
    }




}
