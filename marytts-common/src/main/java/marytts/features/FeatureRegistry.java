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

package marytts.features;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;

import marytts.modules.synthesis.Voice;

import org.apache.commons.collections.map.MultiKeyMap;

/**
 * @author marc
 *
 */
public class FeatureRegistry {
	/**
	 * No instances of this class.
	 */
	private FeatureRegistry() {
	}

	private static Map<Locale, FeatureProcessorManager> managersByLocale = new HashMap<Locale, FeatureProcessorManager>();
	private static Map<Voice, FeatureProcessorManager> managersByVoice = new HashMap<Voice, FeatureProcessorManager>();
	private static FeatureProcessorManager fallbackManager = null;
	private static MultiKeyMap/* <Locale+String listing features, TargetFeatureComputer> */computers = new MultiKeyMap();

	/**
	 * Set the given feature processor manager as the one to use for the given locale.
	 * 
	 * @param locale
	 *            locale
	 * @param mgr
	 *            mgr
	 */
	public static void setFeatureProcessorManager(Locale locale, FeatureProcessorManager mgr) {
		managersByLocale.put(locale, mgr);
	}

	/**
	 * Set the given feature processor manager as the one to use when no voice- or locale-specific feature processor manager can
	 * be found.
	 * 
	 * @param mgr
	 *            mgr
	 */
	public static void setFallbackFeatureProcessorManager(FeatureProcessorManager mgr) {
		fallbackManager = mgr;
	}

	/**
	 * Set the given feature processor manager as the one to use for the given voice.
	 * 
	 * @param voice
	 *            voice
	 * @param mgr
	 *            mgr
	 */
	public static void setFeatureProcessorManager(Voice voice, FeatureProcessorManager mgr) {
		managersByVoice.put(voice, mgr);
	}

	/**
	 * Get the feature processor manager associated with the given voice, if any.
	 * 
	 * @param voice
	 *            voice
	 * @return the feature processor manager, or null if there is no voice-specific feature processor manager.
	 */
	public static FeatureProcessorManager getFeatureProcessorManager(Voice voice) {
		return managersByVoice.get(voice);
	}

	/**
	 * Get the feature processor manager associated with the given locale, if any.
	 * 
	 * @param locale
	 *            locale
	 * @return the feature processor manager, or null if there is no locale-specific feature processor manager.
	 */
	public static FeatureProcessorManager getFeatureProcessorManager(Locale locale) {
		FeatureProcessorManager m = managersByLocale.get(locale);
		if (m != null)
			return m;
		// Maybe locale is language_COUNTRY, so look up by language also:
		Locale lang = new Locale(locale.getLanguage());
		return managersByLocale.get(lang);
	}

	/**
	 * Get the fallback feature processor manager which should be used if there is no voice- or locale-specific feature processor
	 * manager.
	 * 
	 * @return fallbackManager
	 */
	public static FeatureProcessorManager getFallbackFeatureProcessorManager() {
		return fallbackManager;
	}

	/**
	 * For the given voice, return the best feature manager. That is either the voice-specific feature manager, if any, or the
	 * locale-specific feature manager, if any, or the language-specific feature manager, if any, or the fallback feature manager.
	 * 
	 * @param voice
	 *            voice
	 * @return a feature processor manager object. If this returns null, something is broken.
	 */
	public static FeatureProcessorManager determineBestFeatureProcessorManager(Voice voice) {
		FeatureProcessorManager mgr = getFeatureProcessorManager(voice);
		if (mgr == null) {
			mgr = determineBestFeatureProcessorManager(voice.getLocale());
		}
		return mgr;
	}

	/**
	 * For the given locale, return the best feature manager. That is either the locale-specific feature manager, if any, or the
	 * language-specific feature manager, if any, or the fallback feature manager.
	 * 
	 * @param locale
	 *            locale
	 * @return a feature processor manager object. If this returns null, something is broken.
	 */
	public static FeatureProcessorManager determineBestFeatureProcessorManager(Locale locale) {
		FeatureProcessorManager mgr = getFeatureProcessorManager(locale);
		// Locale can have been en_US etc, i.e. language + country; let's try
		// language only as well.
		if (mgr == null) {
			Locale lang = new Locale(locale.getLanguage());
			mgr = getFeatureProcessorManager(lang);
		}
		if (mgr == null) {
			mgr = getFallbackFeatureProcessorManager();
		}
		assert mgr != null;
		return mgr;
	}

	public static Collection<Locale> getSupportedLocales() {
		Collection<Locale> locales = new TreeSet<Locale>(new Comparator<Locale>() {
			public int compare(Locale o1, Locale o2) {
				if (o1 == null) {
					if (o2 == null)
						return 0;
					return -1;
				}
				if (o2 == null) {
					return 1;
				}
				return o1.toString().compareTo(o2.toString());
			}
		});
		locales.addAll(managersByLocale.keySet());
		return locales;
	}

	/**
	 * Obtain a TargetFeatureComputer that knows how to compute features for a Target using the given set of feature processor
	 * names. These names must be known to the given Feature processor manager.
	 * 
	 * @param mgr
	 *            mgr
	 * @param features
	 *            a String containing the names of the feature processors to use, separated by white space, and in the right order
	 *            (byte-valued discrete feature processors first, then short-valued, then continuous). If features is null, use
	 *            all available features processors.
	 * @return a target feature computer
	 * @throws IllegalArgumentException
	 *             if one of the features is not known to the manager
	 */
	public static TargetFeatureComputer getTargetFeatureComputer(FeatureProcessorManager mgr, String features) {
		if (features == null) {
			features = mgr.listFeatureProcessorNames();
		} else {
			// verify that each feature is known to the mgr
			StringTokenizer st = new StringTokenizer(features);
			while (st.hasMoreTokens()) {
				String feature = st.nextToken();
				if (mgr.getFeatureProcessor(feature) == null) {
					throw new IllegalArgumentException("Feature processor manager '" + mgr.getClass().toString()
							+ "' does not know the feature '" + feature + "'");
				}

			}
		}
		TargetFeatureComputer tfc = (TargetFeatureComputer) computers.get(mgr, features);
		if (tfc == null) {
			tfc = new TargetFeatureComputer(mgr, features);
		}
		return tfc;
	}

	/**
	 * Convenience method for getting a suitable target feature computer for the given locale and list of features. A feature
	 * processor for the given locale is looked up using {@link #getFeatureProcessorManager(Locale)} or, if that fails, using
	 * {@link #getFallbackFeatureProcessorManager()}.
	 * 
	 * @see #getTargetFeatureComputer(FeatureProcessorManager, String)
	 * @param locale
	 *            locale
	 * @param features
	 *            a String containing the names of the feature processors to use, separated by white space, and in the right order
	 *            (byte-valued discrete feature processors first, then short-valued, then continuous)
	 * @return a target feature computer
	 */
	public static TargetFeatureComputer getTargetFeatureComputer(Locale locale, String features) {
		FeatureProcessorManager mgr = determineBestFeatureProcessorManager(locale);
		return getTargetFeatureComputer(mgr, features);
	}

	/**
	 * Convenience method for getting a suitable target feature computer for the given voice and list of features. A feature
	 * processor for the given voice is looked up using {@link #getFeatureProcessorManager(Voice)} or, if that fails, using
	 * {@link #getFeatureProcessorManager(Locale)} using the voice locale or, if that also fails, using
	 * {@link #getFallbackFeatureProcessorManager()}.
	 * 
	 * @see #getTargetFeatureComputer(FeatureProcessorManager, String)
	 * @param voice
	 *            voice
	 * @param features
	 *            a String containing the names of the feature processors to use, separated by white space, and in the right order
	 *            (byte-valued discrete feature processors first, then short-valued, then continuous)
	 * @return a target feature computer
	 */
	public static TargetFeatureComputer getTargetFeatureComputer(Voice voice, String features) {
		FeatureProcessorManager mgr = determineBestFeatureProcessorManager(voice);
		return getTargetFeatureComputer(mgr, features);
	}

}
