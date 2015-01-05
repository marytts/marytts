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
package marytts.modules.synthesis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import marytts.exceptions.NoSuchPropertyException;
import marytts.modules.DummyFreeTTSVoice;
import marytts.server.MaryProperties;
import marytts.unitselection.UnitSelectionVoice;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.en.us.CMULexicon;
import com.sun.speech.freetts.lexicon.Lexicon;

import de.dfki.lt.freetts.de.GermanLexicon;

/**
 * Instantiate and manage FreeTTS voices.
 *
 * @author Marc Schr&ouml;der
 */
public class FreeTTSVoices {
	private FreeTTSVoices() {
	} // no instances of this class

	/**
	 * The keys in this map are instances of <code>marytts.modules.synthesis.Voice</code>; the values are instances of
	 * <code>com.sun.speech.freetts.Voice</code>.
	 */
	private static Map<marytts.modules.synthesis.Voice, com.sun.speech.freetts.Voice> mary2freettsVoices = null;
	/**
	 * The keys in this map are instances of <code>com.sun.speech.freetts.Voice</code>; the values are instances of
	 * <code>marytts.modules.synthesis.Voice</code>.
	 */
	private static Map<com.sun.speech.freetts.Voice, marytts.modules.synthesis.Voice> freetts2maryVoices = null;
	private static Lexicon usenLexicon = null;
	private static Lexicon deLexicon = null;

	protected static Logger logger = MaryUtils.getLogger("FreeTTSVoices");

	/**
	 * Ascertain that the FreeTTS voices are loaded. Whether the resources for them will also be allocated depends on the mary
	 * property "freetts.lexicon.preload" setting -- if that results to false, no resources will be loaded; if the setting is
	 * missing, a NoSuchPropertyException will be thrown. This method can safely be called more than once; any subsequent calls
	 * will have no effect.
	 */
	public static void load() throws NoSuchPropertyException {
		if (mary2freettsVoices == null) {
			logger.info("Loading US English FreeTTS voices...");
			// create all voices at startup time
			Collection maryVoices = marytts.modules.synthesis.Voice.getAvailableVoices(Locale.US);
			Iterator it = maryVoices.iterator();
			while (it.hasNext()) {
				marytts.modules.synthesis.Voice maryVoice = (marytts.modules.synthesis.Voice) it.next();
				load(maryVoice);
			}
			logger.info("done.");
		}
	}

	/**
	 * Add a freetts voice for a given mary voice. This is used by load(), but can also be called separately. Repeated
	 * registration of the same voice will be ignored, so it is safe to call this several times with the same maryVoice. This
	 * depends on the mary property "freetts.lexicon.preload" setting -- if that results to false, nothing will be loaded; if the
	 * setting is missing, a NoSuchPropertyException will be thrown.
	 * 
	 * @param maryVoice
	 *            the maryVoice object to register a freetts voice for.
	 */
	public static void load(marytts.modules.synthesis.Voice maryVoice) throws NoSuchPropertyException {
		if (mary2freettsVoices == null)
			mary2freettsVoices = new HashMap<marytts.modules.synthesis.Voice, com.sun.speech.freetts.Voice>();
		if (freetts2maryVoices == null)
			freetts2maryVoices = new HashMap<com.sun.speech.freetts.Voice, marytts.modules.synthesis.Voice>();
		if (mary2freettsVoices.containsKey(maryVoice))
			return; // already known
		load(maryVoice, createFreeTTSVoice(maryVoice));
	}

	/**
	 * Depending on the locale of the mary voice, create a suitable FreeTTS dummy voice.
	 * 
	 * @param maryVoice
	 * @return a newly created FreeTTS dummy voice
	 */
	private static DummyFreeTTSVoice createFreeTTSVoice(Voice maryVoice) {
		DummyFreeTTSVoice freeTTSVoice;
		if (maryVoice.getLocale() != null && maryVoice.getLocale().equals(Locale.US)) {
			try {
				freeTTSVoice = (DummyFreeTTSVoice) Class.forName("marytts.language.en.DummyFreeTTSVoice").newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			freeTTSVoice.initialise(maryVoice, null);
		} else {
			freeTTSVoice = new DummyFreeTTSVoice(maryVoice, null);
		}
		return freeTTSVoice;
	}

	/**
	 * Depending on the maryVoice and its locale, associate an existing or create a new lexicon.
	 * 
	 * @param maryVoice
	 * @return a Lexicon; if it was freshly created, it is not yet loaded.
	 */
	@Deprecated
	private static Lexicon getLexicon(Voice maryVoice) {
		if (maryVoice instanceof UnitSelectionVoice) {
			return ((UnitSelectionVoice) maryVoice).getLexicon();
		}
		if (maryVoice.getLocale() == null) {
			return null;
		} else if (maryVoice.getLocale().equals(Locale.US)) {
			if (usenLexicon == null)
				usenLexicon = new CMULexicon("cmudict04");
			return usenLexicon;
		} else if (maryVoice.getLocale().equals(Locale.GERMAN)) {
			if (deLexicon == null)
				deLexicon = new GermanLexicon();
			return deLexicon;
		}
		return null;
	}

	/**
	 * Add a given freetts voice for a given mary voice. Repeated registration of the same voice will be ignored, so it is safe to
	 * call this several times with the same maryVoice/freettsVoice pair. This depends on the mary property
	 * "freetts.lexicon.preload" setting -- if that results to false, nothing will be loaded; if the setting is missing, a
	 * NoSuchPropertyException will be thrown.
	 * 
	 * @param maryVoice
	 *            the maryVoice object to register the freetts voice for.
	 * @param freeTTSVoice
	 *            the freettsVoice object to register.
	 */
	public static void load(marytts.modules.synthesis.Voice maryVoice, com.sun.speech.freetts.Voice freeTTSVoice)
			throws NoSuchPropertyException {
		if (mary2freettsVoices == null)
			mary2freettsVoices = new HashMap<marytts.modules.synthesis.Voice, com.sun.speech.freetts.Voice>();
		if (freetts2maryVoices == null)
			freetts2maryVoices = new HashMap<com.sun.speech.freetts.Voice, marytts.modules.synthesis.Voice>();
		if (mary2freettsVoices.containsKey(maryVoice))
			return; // already known
		if (freeTTSVoice.getLexicon() == null) {
			Lexicon lex = maryVoice.getLexicon();
			freeTTSVoice.setLexicon(lex);
		}
		if (MaryProperties.needAutoBoolean("freetts.lexicon.preload") && !freeTTSVoice.isLoaded()) {
			logger.debug("Allocating resources for voice " + freeTTSVoice.getName() + "...");
			freeTTSVoice.allocate();
			logger.debug("... done.");
		}
		mary2freettsVoices.put(maryVoice, freeTTSVoice);
		freetts2maryVoices.put(freeTTSVoice, maryVoice);
		logger.debug("added freetts voice for mary voice " + maryVoice.toString());

	}

	/**
	 * For a given MARY voice, get the corresponding FreeTTS voice. This method will load/allocate a voice if it had not been
	 * loaded before.
	 * 
	 * @throws NoSuchPropertyException
	 *             if the property <code>freetts.lexicon.preload</code> is not defined in the MARY properties file.
	 */
	public static com.sun.speech.freetts.Voice getFreeTTSVoice(marytts.modules.synthesis.Voice maryVoice)
			throws NoSuchPropertyException {
		if (maryVoice == null) {
			maryVoice = marytts.modules.synthesis.Voice.getDefaultVoice(Locale.US);
		}
		assert mary2freettsVoices != null; // called before startup()?
		com.sun.speech.freetts.Voice freeTTSVoice = (com.sun.speech.freetts.Voice) mary2freettsVoices.get(maryVoice);
		if (freeTTSVoice == null) {
			// need to create dummy freetts voice for mary voice
			load(maryVoice);
			freeTTSVoice = (com.sun.speech.freetts.Voice) mary2freettsVoices.get(maryVoice);
		}
		assert freeTTSVoice != null;
		// At this stage, make sure the voice is loaded:
		if (!freeTTSVoice.isLoaded()) {
			logger.debug("Allocating resources for voice " + freeTTSVoice.getName() + "...");
			freeTTSVoice.allocate();
			logger.debug("... done.");
		}
		return freeTTSVoice;
	}

	/**
	 * For a given MARY voice, get the corresponding FreeTTS voice.
	 * 
	 * @throws NoSuchPropertyException
	 *             if the property <code>freetts.lexicon.preload</code> is not defined in the MARY properties file.
	 */
	public static marytts.modules.synthesis.Voice getMaryVoice(com.sun.speech.freetts.Voice freeTTSVoice)
			throws NoSuchPropertyException {
		if (freeTTSVoice == null) {
			throw new NullPointerException("Received null voice");
		}
		if (freetts2maryVoices != null) {
			marytts.modules.synthesis.Voice maryVoice = (marytts.modules.synthesis.Voice) freetts2maryVoices.get(freeTTSVoice);
			/*
			 * if (maryVoice == null) { throw new NoSuchPropertyException("No Mary voice available for the FreeTTS voice \"" +
			 * freeTTSVoice + "\""); }
			 */
			return maryVoice;
		}
		return null;
	}

}
