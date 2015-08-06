/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.signalproc.effects;

import java.util.ArrayList;

import marytts.server.MaryProperties;

/**
 * @author marc
 *
 */
public class AudioEffects {
	private static ArrayList<AudioEffect> effects = initialiseEffects();

	private static ArrayList<AudioEffect> initialiseEffects() {
		ArrayList<AudioEffect> effs = new ArrayList<AudioEffect>();
		for (String className : MaryProperties.effectClasses()) {
			try {
				effs.add((AudioEffect) Class.forName(className).newInstance());
			} catch (Exception e) {
				throw new Error("Cannot set up effect class '" + className + "'", e);
			}
		}
		return effs;
	}

	public static Iterable<AudioEffect> getEffects() {
		return effects;
	}

	public static int countEffects() {
		return effects.size();
	}

	public static AudioEffect getEffect(String name) {
		for (AudioEffect effect : getEffects()) {
			if (effect.getName().equals(name)) {
				return effect;
			}
		}
		return null;
	}
}
