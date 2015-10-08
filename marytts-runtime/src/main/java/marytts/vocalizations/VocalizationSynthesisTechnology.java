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
package marytts.vocalizations;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.exceptions.SynthesisException;

/**
 * An abstract class for vocalization syntehsis technology
 * 
 * @author Sathish Pammi
 */

public abstract class VocalizationSynthesisTechnology {

	/**
	 * Synthesize given vocalization
	 * 
	 * @param unitIndex
	 *            unit index
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	public abstract AudioInputStream synthesize(int unitIndex, AudioFileFormat aft) throws SynthesisException;

	/**
	 * Re-synthesize given vocalization
	 * 
	 * @param sourceIndex
	 *            source index
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	public abstract AudioInputStream reSynthesize(int sourceIndex, AudioFileFormat aft) throws SynthesisException;

	/**
	 * Impose target intonation contour on given vocalization
	 * 
	 * @param sourceIndex
	 *            unit index of vocalization
	 * @param targetIndex
	 *            unit index of target intonation
	 * @param aft
	 *            aft audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	public abstract AudioInputStream synthesizeUsingImposedF0(int sourceIndex, int targetIndex, AudioFileFormat aft)
			throws SynthesisException;

}
