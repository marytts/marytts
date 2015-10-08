/**
 * Copyright 2010 DFKI GmbH. All Rights Reserved. Use is subject to license terms.
 * 
 * This file is part of MARY TTS.
 * 
 * MARY TTS is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 */

package marytts.unitselection.data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import marytts.util.data.Datagram;

/**
 * Extension of Datagram to provide a float array instead of (actually alongside) a byte array
 * 
 * @author steiner
 * 
 */
public class FloatArrayDatagram extends Datagram {

	private float[] floatData;

	public FloatArrayDatagram(long duration, float[] data) {
		super(duration);
		this.floatData = data;
	}

	public float[] getFloatData() {
		return floatData;
	}

	/**
	 * Write this datagram to a random access file or data output stream.
	 * 
	 * @param raf
	 *            raf
	 * @throws IOException
	 *             IOException
	 */
	@Override
	public void write(DataOutput raf) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		BufferedOutputStream bos = new BufferedOutputStream(dos);

		dos.writeLong(duration);
		dos.writeInt(floatData.length);
		for (float fl : floatData) {
			dos.writeFloat(fl);
		}

		raf.write(baos.toByteArray());
	}

}
