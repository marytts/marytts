/**
 * Portions Copyright 2002 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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

import java.io.IOException;
import java.util.Locale;

import com.sun.speech.freetts.PartOfSpeech;
import com.sun.speech.freetts.PartOfSpeechImpl;
import com.sun.speech.freetts.PhoneSetImpl;
import com.sun.speech.freetts.en.us.FeatureProcessors;
import com.sun.speech.freetts.util.BulkTimer;

/**
 * Defines a dummy voice allowing to use FreeTTS UtteranceProcessors from
 * "outside". Differing from the FreeTTS philosophy, this "voice" does *not*
 * actually call the UtteranceProcessors. It only serves as a collection of
 * data, such as the lexicon, and some reference values.
 */
public class DummyFreeTTSVoice extends
        de.dfki.lt.mary.modules.DummyFreeTTSVoice
{
    /**
     * Creates a simple voice containing a reference to a
     * <code>de.dfki.lt.mary.modules.synthesis.Voice</code>.
     *
     * This default constructor must be followed by a meaningful
     * call to initialise().
     */
    public DummyFreeTTSVoice()
    {
    }

    /**
     * Creates a simple voice containing a reference to a
     * <code>de.dfki.lt.mary.modules.synthesis.Voice</code>.
     * This version sets up US English feature processors. 
     *
     * @param lexiconClassName if not null, automatically load up
     * the specified lexicon; otherwise, don't load any lexicon.
     * @throws RuntimeException if the class specified by lexiconClassName
     * cannot be instantiated.
     */
    public DummyFreeTTSVoice(de.dfki.lt.mary.modules.synthesis.Voice maryVoice,
            String lexiconClassName)
    {
        super(maryVoice, lexiconClassName);
        if (!maryVoice.getLocale().equals(Locale.US)) {
            throw new IllegalArgumentException("This dummy freetts voice is meant for US English voices only!");
        }
    }

    /**
     * Creates a simple voice containing a reference to a
     * <code>de.dfki.lt.mary.modules.synthesis.Voice</code>.
     *
     * @param lexiconClassName if not null, automatically load up
     * the specified lexicon; otherwise, don't load any lexicon.
     * @throws RuntimeException if the class specified by lexiconClassName
     * cannot be instantiated.
     */
    public void initialise(de.dfki.lt.mary.modules.synthesis.Voice aMaryVoice,
            String lexiconClassName) {
        super.initialise(aMaryVoice, lexiconClassName);
        if (!aMaryVoice.getLocale().equals(Locale.US)) {
            throw new IllegalArgumentException("This dummy freetts voice is meant for US English voices only!");
        }
    }

    /**
     * Sets up the FeatureProcessors for this Voice.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void setupFeatureProcessors() throws IOException
    {
        BulkTimer.LOAD.start("FeatureProcessing");
        PartOfSpeech pos = new PartOfSpeechImpl(
                com.sun.speech.freetts.en.us.CMUVoice.class
                        .getResource("part_of_speech.txt"), "content");

        phoneSet = new PhoneSetImpl(com.sun.speech.freetts.en.us.CMUVoice.class
                .getResource("phoneset.txt"));

        addFeatureProcessor("word_break", new FeatureProcessors.WordBreak());
        addFeatureProcessor("word_punc", new FeatureProcessors.WordPunc());
        addFeatureProcessor("gpos", new FeatureProcessors.Gpos(pos));
        addFeatureProcessor("word_numsyls", new FeatureProcessors.WordNumSyls());
        addFeatureProcessor("ssyl_in", new FeatureProcessors.StressedSylIn());
        addFeatureProcessor("syl_in", new FeatureProcessors.SylIn());
        addFeatureProcessor("syl_out", new FeatureProcessors.SylOut());
        addFeatureProcessor("ssyl_out", new FeatureProcessors.StressedSylOut());
        addFeatureProcessor("syl_break", new FeatureProcessors.SylBreak());
        addFeatureProcessor("old_syl_break", new FeatureProcessors.SylBreak());
        addFeatureProcessor("num_digits", new FeatureProcessors.NumDigits());
        addFeatureProcessor("month_range", new FeatureProcessors.MonthRange());
        addFeatureProcessor("token_pos_guess",
                new FeatureProcessors.TokenPosGuess());
        addFeatureProcessor("segment_duration",
                new FeatureProcessors.SegmentDuration());
        addFeatureProcessor("sub_phrases", new FeatureProcessors.SubPhrases());
        addFeatureProcessor("asyl_in", new FeatureProcessors.AccentedSylIn());
        addFeatureProcessor("last_accent", new FeatureProcessors.LastAccent());
        addFeatureProcessor("pos_in_syl", new FeatureProcessors.PosInSyl());
        addFeatureProcessor("position_type",
                new FeatureProcessors.PositionType());

        addFeatureProcessor("ph_cplace", new FeatureProcessors.PH_CPlace());
        addFeatureProcessor("ph_ctype", new FeatureProcessors.PH_CType());
        addFeatureProcessor("ph_cvox", new FeatureProcessors.PH_CVox());
        addFeatureProcessor("ph_vc", new FeatureProcessors.PH_VC());
        addFeatureProcessor("ph_vfront", new FeatureProcessors.PH_VFront());
        addFeatureProcessor("ph_vheight", new FeatureProcessors.PH_VHeight());
        addFeatureProcessor("ph_vlng", new FeatureProcessors.PH_VLength());
        addFeatureProcessor("ph_vrnd", new FeatureProcessors.PH_VRnd());

        addFeatureProcessor("seg_coda_fric",
                new FeatureProcessors.SegCodaFric());
        addFeatureProcessor("seg_onset_fric",
                new FeatureProcessors.SegOnsetFric());

        addFeatureProcessor("seg_coda_stop",
                new FeatureProcessors.SegCodaStop());
        addFeatureProcessor("seg_onset_stop",
                new FeatureProcessors.SegOnsetStop());

        addFeatureProcessor("seg_coda_nasal",
                new FeatureProcessors.SegCodaNasal());
        addFeatureProcessor("seg_onset_nasal",
                new FeatureProcessors.SegOnsetNasal());

        addFeatureProcessor("seg_coda_glide",
                new FeatureProcessors.SegCodaGlide());
        addFeatureProcessor("seg_onset_glide",
                new FeatureProcessors.SegOnsetGlide());

        addFeatureProcessor("seg_onsetcoda",
                new FeatureProcessors.SegOnsetCoda());
        addFeatureProcessor("syl_codasize", new FeatureProcessors.SylCodaSize());
        addFeatureProcessor("syl_onsetsize",
                new FeatureProcessors.SylOnsetSize());
        addFeatureProcessor("accented", new FeatureProcessors.Accented());
        BulkTimer.LOAD.stop("FeatureProcessing");
    }

    /**
     * Converts this object to a string
     * 
     * @return a string representation of this object
     */
    public String toString()
    {
        return "DummyFreeTTSVoice US-English";
    }
}

