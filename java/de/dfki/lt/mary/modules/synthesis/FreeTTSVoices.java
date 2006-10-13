/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.modules.synthesis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.en.us.CMULexicon;
import com.sun.speech.freetts.lexicon.Lexicon;

import de.dfki.lt.freetts.de.GermanLexicon;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.NoSuchPropertyException;
import de.dfki.lt.mary.modules.DummyFreeTTSVoice;
import de.dfki.lt.mary.unitselection.UnitSelectionVoice;

/**
 * Instantiate and manage FreeTTS voices.
 *
 * @author Marc Schr&ouml;der
 */
public class FreeTTSVoices
{
    private FreeTTSVoices() {} // no instances of this class

    /**
     * The keys in this map are instances of
     * <code>de.dfki.lt.mary.modules.synthesis.Voice</code>; the values are
     * instances of <code>com.sun.speech.freetts.Voice</code>.
     */
    private static Map mary2freettsVoices = null;
    /**
     * The keys in this map are instances of
     * <code>com.sun.speech.freetts.Voice</code>;
     * the values are instances of
     * <code>de.dfki.lt.mary.modules.synthesis.Voice</code>.
     */
    private static Map freetts2maryVoices = null;
    private static Lexicon usenLexicon = null;
    private static Lexicon deLexicon = null;

    /**
     * Ascertain that the FreeTTS voices are loaded.
     * This depends on the mary property "freetts.lexicon.preload" setting --
     * if that results to false, nothing will be loaded; if the setting is missing,
     * a NoSuchPropertyException will be thrown.
     * This method can safely be called more than once; any subsequent calls
     * will have no effect.
     */
    public static void load() throws NoSuchPropertyException
    {
        if (mary2freettsVoices == null && MaryProperties.needAutoBoolean("freetts.lexicon.preload")) {
            Logger logger = Logger.getLogger("FreeTTSVoices");
            logger.info("Loading US English FreeTTS voices...");
            // create all voices at startup time
            Collection maryVoices = de.dfki.lt.mary.modules.synthesis.Voice.
                getAvailableVoices(Locale.US);
            Iterator it = maryVoices.iterator();
            while (it.hasNext()) {
                de.dfki.lt.mary.modules.synthesis.Voice maryVoice =
                    (de.dfki.lt.mary.modules.synthesis.Voice) it.next();
                load(maryVoice);
            }
            logger.info("done.");
        }
    }

    /**
     * Add a freetts voice for a given mary voice.
     * This is used by load(), but can also be called separately. Repeated
     * registration of the same voice will be ignored, so it is safe to call
     * this several times with the same maryVoice.
     * This depends on the mary property "freetts.lexicon.preload" setting --
     * if that results to false, nothing will be loaded; if the setting is missing,
     * a NoSuchPropertyException will be thrown.
     * @param maryVoice the maryVoice object to register a freetts voice for.
     */
    public static void load(de.dfki.lt.mary.modules.synthesis.Voice maryVoice)
    throws NoSuchPropertyException
    {
        if (mary2freettsVoices == null) mary2freettsVoices = new HashMap();
        if (freetts2maryVoices == null) freetts2maryVoices = new HashMap();
        if (mary2freettsVoices.containsKey(maryVoice)) return; // already known
        Lexicon voiceLexicon = null;
        if (maryVoice instanceof UnitSelectionVoice) {
            voiceLexicon = ((UnitSelectionVoice)maryVoice).getLexicon();
        }
        DummyFreeTTSVoice freeTTSVoice;
        if (maryVoice.getLocale().equals(Locale.US)) {
            if (!MaryProperties.needAutoBoolean("freetts.lexicon.preload")) return;
            if (voiceLexicon == null) {
                if (usenLexicon == null) usenLexicon = new CMULexicon("cmudict04");
                voiceLexicon = usenLexicon;
            }
            try {
                freeTTSVoice = (DummyFreeTTSVoice) Class.forName("de.dfki.lt.mary.modules.en.DummyFreeTTSVoice").newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            freeTTSVoice.initialise(maryVoice, null);
        } else if (maryVoice.getLocale().equals(Locale.GERMAN)) {
            if (voiceLexicon == null) {
                if (deLexicon == null) deLexicon = new GermanLexicon();
                voiceLexicon = deLexicon;
            }
            freeTTSVoice = new DummyFreeTTSVoice(maryVoice, null);
        } else {
            freeTTSVoice = new DummyFreeTTSVoice(maryVoice, null);
        }
        freeTTSVoice.setLexicon(voiceLexicon);
        freeTTSVoice.allocate();
        mary2freettsVoices.put(maryVoice, freeTTSVoice);
        freetts2maryVoices.put(freeTTSVoice, maryVoice);
        Logger logger = Logger.getLogger("FreeTTSVoices");
        logger.debug("added freetts voice for mary voice " + maryVoice.toString());

    }
        
    /**
     * Add a given freetts voice for a given mary voice.
     * Repeated
     * registration of the same voice will be ignored, so it is safe to call
     * this several times with the same maryVoice/freettsVoice pair.
     * This depends on the mary property "freetts.lexicon.preload" setting --
     * if that results to false, nothing will be loaded; if the setting is missing,
     * a NoSuchPropertyException will be thrown.
     * @param maryVoice the maryVoice object to register the freetts voice for.
     * @param freeTTSVoice the freettsVoice object to register.
     */
    public static void load(de.dfki.lt.mary.modules.synthesis.Voice maryVoice, com.sun.speech.freetts.Voice freeTTSVoice)
    throws NoSuchPropertyException
    {
        if (!MaryProperties.needAutoBoolean("freetts.lexicon.preload")) return;
        if (mary2freettsVoices == null) mary2freettsVoices = new HashMap();
        if (freetts2maryVoices == null) freetts2maryVoices = new HashMap();
        if (mary2freettsVoices.containsKey(maryVoice)) return; // already known
        if (maryVoice.getLocale().equals(Locale.US)) {
            if (freeTTSVoice.getLexicon() == null) {
                if (usenLexicon == null) usenLexicon = new CMULexicon("cmudict04");
                freeTTSVoice.setLexicon(usenLexicon);            
            }
        } else if (maryVoice.getLocale().equals(Locale.GERMAN)) {
            if (deLexicon == null) deLexicon = new GermanLexicon();
            freeTTSVoice.setLexicon(deLexicon);
        }
        freeTTSVoice.allocate();
        mary2freettsVoices.put(maryVoice, freeTTSVoice);
        freetts2maryVoices.put(freeTTSVoice, maryVoice);
        Logger logger = Logger.getLogger("FreeTTSVoices");
        logger.debug("added freetts voice for mary voice " + maryVoice.toString());

    }

    /**
     * For a given MARY voice, get the corresponding FreeTTS voice.
     * @throws NoSuchPropertyException if the property <code>freetts.lexicon.preload</code>
     * is not defined in the MARY properties file.
     */
    public static com.sun.speech.freetts.Voice getFreeTTSVoice
        (de.dfki.lt.mary.modules.synthesis.Voice maryVoice)
    throws NoSuchPropertyException
    {
        if (maryVoice == null) {
            maryVoice = de.dfki.lt.mary.modules.synthesis.Voice.
                getDefaultVoice(Locale.US);
        }
        if (MaryProperties.needAutoBoolean("freetts.lexicon.preload")) {
            assert mary2freettsVoices != null; // called before startup()?
            return (com.sun.speech.freetts.Voice) mary2freettsVoices.get(maryVoice);
        } else {
            DummyFreeTTSVoice voice;
            // Special treatment for US English dummy voices,
            // because FreeTTS provides US English synthesis:
            if (maryVoice.getLocale().equals(Locale.US)) {
                try {
                    voice = (DummyFreeTTSVoice) Class.forName("de.dfki.lt.mary.modules.en.DummyFreeTTSVoice").newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                voice.initialise(maryVoice, "com.sun.speech.freetts.en.us.CMULexicon");
            } else {
                voice = new DummyFreeTTSVoice(maryVoice, null);
            }
            voice.allocate();
            return voice;
        }
    }

    /**
     * For a given MARY voice, get the corresponding FreeTTS voice.
     * @throws NoSuchPropertyException if the property <code>freetts.lexicon.preload</code>
     * is not defined in the MARY properties file.
     */
    public static de.dfki.lt.mary.modules.synthesis.Voice getMaryVoice
        (com.sun.speech.freetts.Voice freeTTSVoice)
    throws NoSuchPropertyException
    {
        if (freeTTSVoice == null) {
            throw new NullPointerException("Received null voice");
        }
        assert freetts2maryVoices != null; // called before startup()?
        return (de.dfki.lt.mary.modules.synthesis.Voice) freetts2maryVoices.get(freeTTSVoice);
    }

}
