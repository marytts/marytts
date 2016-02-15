/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.unitselection.concat;

import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.unitselection.data.UnitDatabase;
import marytts.unitselection.select.SelectedUnit;

/**
 * Concatenates the units of an utterance and returns an audio stream
 * 
 * @author Marc Schr&ouml;der, Anna Hunecke
 *
 */
public interface UnitConcatenator {
	/**
	 * Initialise the unit concatenator from the database.
	 * 
	 * @param database
	 *            database
	 */
	public void load(UnitDatabase database);

	/**
	 * Build the audio stream from the units
	 * 
	 * @param units
	 *            the units
	 * @throws IOException
	 *             IOException
	 * @return the resulting audio stream
	 */
	public AudioInputStream getAudio(List<SelectedUnit> units) throws IOException;

	/**
	 * Provide the audio format which will be produced by this unit concatenator.
	 * 
	 * @return the audio format
	 */
	public AudioFormat getAudioFormat();

}
