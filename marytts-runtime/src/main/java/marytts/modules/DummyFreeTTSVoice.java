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
package marytts.modules;

import java.io.IOException;
import java.util.Locale;


import com.sun.speech.freetts.Tokenizer;
import com.sun.speech.freetts.UtteranceProcessor;
import com.sun.speech.freetts.lexicon.Lexicon;
import com.sun.speech.freetts.util.BulkTimer;

/**
 * Defines a dummy voice allowing to use FreeTTS UtteranceProcessors from "outside". Differing from the FreeTTS philosophy, this
 * "voice" does *not* actually call the UtteranceProcessors. It only serves as a collection of data, such as the lexicon, and some
 * reference values.
 */
public class DummyFreeTTSVoice extends com.sun.speech.freetts.Voice {
	protected marytts.modules.synthesis.Voice maryVoice;
	protected boolean useBinaryIO = System.getProperty("com.sun.speech.freetts.useBinaryIO", "true").equals("true");

	/**
	 * Creates a simple voice containing a reference to a <code>marytts.modules.synthesis.Voice</code>.
	 * 
	 * This default constructor must be followed by a meaningful call to initialise().
	 */
	public DummyFreeTTSVoice(Locale locale) {
		super.setLocale(locale);
	}

	/**
	 * Creates a simple voice containing a reference to a <code>marytts.modules.synthesis.Voice</code>.
	 * 
	 * @param lexiconClassName
	 *            if not null, automatically load up the specified lexicon; otherwise, don't load any lexicon.
	 * @throws RuntimeException
	 *             if the class specified by lexiconClassName cannot be instantiated.
	 */
	public DummyFreeTTSVoice(marytts.modules.synthesis.Voice maryVoice, String lexiconClassName) {
		initialise(maryVoice, lexiconClassName);
	}

	/**
	 * Creates a simple voice containing a reference to a <code>marytts.modules.synthesis.Voice</code>.
	 * 
	 * @param lexiconClassName
	 *            if not null, automatically load up the specified lexicon; otherwise, don't load any lexicon.
	 * @throws RuntimeException
	 *             if the class specified by lexiconClassName cannot be instantiated.
	 */
	public void initialise(marytts.modules.synthesis.Voice aMaryVoice, String lexiconClassName) {
		this.maryVoice = aMaryVoice;
		if (maryVoice != null) {
			super.setLocale(maryVoice.getLocale());
		}
		if (lexiconClassName != null) {
			try {
				Lexicon lex = (Lexicon) Class.forName(lexiconClassName).newInstance();
				setLexicon(lex);
			} catch (IllegalAccessException iae) {
				throw new RuntimeException("Illegal access trying to instantiate " + lexiconClassName);
			} catch (ClassNotFoundException iae) {
				throw new RuntimeException("Class not found trying to instantiate " + lexiconClassName);
			} catch (InstantiationException iae) {
				throw new RuntimeException("Instantiation exception trying to instantiate " + lexiconClassName);
			}
		}
		setRate(135f);

		setPitch(100);
		setPitchRange(30);
	}

	/**
	 * Called by <code> load() </code> during loading, derived voices should override this to provide customized loading.
	 */
	protected void loader() {
		try {
			setupFeatureSet();
			// do not set up any utterance processors
			setupFeatureProcessors();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads this Voice. It loads the lexicon and the audio output handler, but does not create audio output and output thread. It
	 * then calls the <code>loader()</code> method to load Voice-specific data, which include utterance processors.
	 */
	public void allocate() {
		if (isLoaded()) {
			return;
		}
		BulkTimer.LOAD.start();

		Lexicon lexicon = getLexicon();
		if (lexicon != null && !lexicon.isLoaded()) {
			try {
				lexicon.load();
			} catch (IOException ioe) {
				throw new Error("Can't load voice", ioe);
			}
		}

		loader();
		BulkTimer.LOAD.stop();
		if (isMetrics()) {
			BulkTimer.LOAD.show("loading " + toString() + " for " + getRunTitle());
		}
		setLoaded(true);
	}

	/**
	 * Sets up the FeatureSet for this Voice.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void setupFeatureSet() {
	}

	/**
	 * Sets up the FeatureProcessors for this Voice. Not used for this default implementation.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void setupFeatureProcessors() throws IOException {
	}

	/**
	 * Given a phone and a feature name, return the feature
	 * 
	 * @param phone
	 *            the phone of interest
	 * @param featureName
	 *            the name of the feature of interest
	 * 
	 * @return the feature with the given name, or null if it cannot be found.
	 */
	public String getPhoneFeature(String phone, String featureName) {
		return null;
	}

	/**
	 * Returns the AudioOutput processor to be used by this voice Derived voices typically override this to customize behaviors.
	 * 
	 * @return the audio output processor
	 * 
	 * @throws IOException
	 *             if an IO error occurs while getting processor
	 */
	protected UtteranceProcessor getAudioOutput() throws IOException {
		return null; // no audio output needed in dummy voice
	}

	/**
	 * Gets a tokenizer for this voice (not used here)
	 * 
	 * @return null
	 */
	public Tokenizer getTokenizer() {
		return null;
	}

	/**
	 * Gets the marytts.modules.synthesis.Voice associated with this voice.
	 */
	public marytts.modules.synthesis.Voice getMaryVoice() {
		return maryVoice;
	}

	/**
	 * Converts this object to a string
	 * 
	 * @return a string representation of this object
	 */
	public String toString() {
		return "DummyFreeTTSVoice";
	}
}
