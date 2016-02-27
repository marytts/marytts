/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.signalproc.sinusoidal.hntm.analysis;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import marytts.util.math.ArrayUtils;

/**
 * @author oytun.turk
 * 
 */

public class FrameNoisePartWaveform implements FrameNoisePart {
	protected short[] waveform;

	public FrameNoisePartWaveform() {
		super();

		waveform = null;
	}

	public FrameNoisePartWaveform(FrameNoisePartWaveform existing) {
		this();

		waveform = ArrayUtils.copy(existing.waveform);
	}

	public FrameNoisePartWaveform(DataInputStream dis, int waveLen) {
		this();

		if (waveLen > 0) {
			waveform = new short[waveLen];
			for (int i = 0; i < waveLen; i++) {
				try {
					waveform[i] = dis.readShort();
				} catch (IOException e) {
					System.out.println("Error! At least " + String.valueOf(waveLen) + " samples required!");
				}
			}
		}
	}

	public FrameNoisePartWaveform(ByteBuffer bb, int waveLen) {
		this();

		if (waveLen > 0) {
			waveform = new short[waveLen];
			for (int i = 0; i < waveLen; i++) {
				try {
					waveform[i] = bb.getShort();
				} catch (Exception e) {
					throw new IllegalArgumentException("At least " + String.valueOf(waveLen) + " samples required!", e);
				}
			}
		}
	}

	public void write(DataOutput out) throws IOException {
		int waveLen = 0;
		if (waveform != null && waveform.length > 0)
			waveLen = waveform.length;

		if (waveLen > 0) {
			for (int i = 0; i < waveform.length; i++)
				out.writeShort(waveform[i]);
		}
	}

	public FrameNoisePartWaveform(float[] x) {
		this();

		setWaveform(x);
	}

	public FrameNoisePartWaveform(double[] x) {
		this();

		setWaveform(x);
	}

	public FrameNoisePartWaveform(short[] x) {
		this();

		setWaveform(x);
	}

	public boolean equals(FrameNoisePartWaveform other) {
		if (waveform != null || other.waveform != null) {
			if (waveform != null && other.waveform == null)
				return false;
			if (waveform == null && other.waveform != null)
				return false;
			if (waveform.length != other.waveform.length)
				return false;

			if (waveform != null) {
				for (int i = 0; i < waveform.length; i++)
					if (waveform[i] != other.waveform[i])
						return false;
			}
		}

		return true;
	}

	public int getVectorSize() {
		int waveLen = 0;
		if (waveform != null && waveform.length > 0)
			waveLen = waveform.length;

		return waveLen;
	}

	public int getLength() {
		return 2 * getVectorSize();
	}

	public void setWaveform(float[] x) {
		if (x != null)
			waveform = ArrayUtils.copyFloat2Short(x);
		else
			waveform = null;
	}

	public void setWaveform(double[] x) {
		if (x != null)
			waveform = ArrayUtils.copyDouble2Short(x);
		else
			waveform = null;
	}

	public void setWaveform(short[] x) {
		if (x != null)
			waveform = ArrayUtils.copy(x);
		else
			waveform = null;
	}

	public double[] waveform2Doubles() {
		if (waveform != null) {
			return ArrayUtils.copyShort2Double(waveform);
		} else
			return null;
	}

	public float[] waveform2Floats() {
		if (waveform != null)
			return ArrayUtils.copyShort2Float(waveform);
		else
			return null;
	}

	public short[] waveform() {
		return waveform;
	}
}