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
package marytts.unitselection.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.data.Datagram;

public class MCepTimelineReader extends TimelineReader {
	protected int order;

	public MCepTimelineReader(String fileName) throws IOException, MaryConfigurationException {
		super();
		load(fileName);
	}

	@Override
	protected void load(String fileName) throws IOException, MaryConfigurationException {
		super.load(fileName);
		// Now make sense of the processing header
		Properties props = new Properties();
		ByteArrayInputStream bais = new ByteArrayInputStream(procHdr.getString().getBytes("latin1"));
		props.load(bais);
		ensurePresent(props, "mcep.order");
		order = Integer.parseInt(props.getProperty("mcep.order"));
	}

	private void ensurePresent(Properties props, String key) throws IOException {
		if (!props.containsKey(key))
			throw new IOException("Processing header does not contain required field '" + key + "'");

	}

	public int getOrder() {
		return order;
	}

	/**
	 * Read and return the upcoming datagram.
	 * 
	 * @param bb
	 *            bb
	 * @return the current datagram, or null if EOF was encountered; internally updates the time pointer.
	 * 
	 */
	@Override
	protected Datagram getNextDatagram(ByteBuffer bb) {

		Datagram d = null;

		/* If the end of the datagram zone is reached, gracefully refuse to read */
		if (bb.position() == timeIdxBytePos)
			return (null);
		/* Else, pop the datagram out of the file */
		try {
			d = new MCepDatagram(bb, order);
		}
		/* Detect a possible EOF encounter */
		catch (IOException e) {
			return null;
		}

		return (d);
	}

}
