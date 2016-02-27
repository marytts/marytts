/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.util.data.audio;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.BaseDoubleDataSource;
import marytts.util.data.DoubleDataSource;

/**
 * @author Marc Schr&ouml;der A Double Data Source reading doubles from a Reader, in their string representation. The Reader is
 *         expected to contain the text representation of exactly one double per line.
 */
public class AudioDoubleDataSource extends BaseDoubleDataSource {
	public static final int BYTEBUFFER_LENGTH = 65532; // multiple of 4 and 6, to allow for 16 and 24 bit
	protected AudioInputStream ais;
	protected byte[] byteBuf;
	protected int samplingRate;
	protected int bytesPerSample;
	protected boolean bigEndian;
	protected boolean hasMoreData;
	protected boolean bAutomaticClippingControl;
	protected double[] scales;
	protected int scaleInd;

	/**
	 * Initialise this double data source with the AudioInputStream from which samples can be read.
	 * 
	 * @param ais
	 *            ais
	 * @param isAutomaticClippingControl
	 *            isAutomaticClippingControl
	 * @throws IllegalArgumentException
	 *             if the audio input stream does not have 8, 16 or 24 bits per sample.
	 */
	public AudioDoubleDataSource(AudioInputStream ais, boolean isAutomaticClippingControl) {
		this.ais = ais;
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono signals");
		}
		int bitsPerSample = ais.getFormat().getSampleSizeInBits();
		if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24) {
			throw new IllegalArgumentException("Can deal with sample size 8, 16 or 24, but not " + bitsPerSample);
		}
		this.bytesPerSample = bitsPerSample / 8;
		this.bigEndian = ais.getFormat().isBigEndian();
		this.samplingRate = (int) ais.getFormat().getSampleRate();
		this.byteBuf = new byte[BYTEBUFFER_LENGTH];
		this.hasMoreData = true;

		this.scaleInd = -1;
		this.bAutomaticClippingControl = isAutomaticClippingControl;
		if (bAutomaticClippingControl) {
			scales = new double[20];
			for (int i = 0; i < scales.length; i++)
				scales[i] = 1.0;
		} else
			scales = null;
	}

	public AudioDoubleDataSource(AudioInputStream ais) {
		this(ais, false);
	}

	/**
	 * Get the sampling rate of the audio data.
	 * 
	 * @return the sampling rate
	 */
	public int getSamplingRate() {
		return samplingRate;
	}

	public AudioFormat getAudioFormat() {
		return ais.getFormat();
	}

	/**
	 * Try to get length doubles from this DoubleDataSource, and copy them into target, starting from targetPos. This is the core
	 * method getting the data. Subclasses may want to override this method. If an exception occurs reading from the underlying
	 * reader, or converting data to double, the method will print a stack trace to standard error, but otherwise will silently
	 * stop and behave as if all data was read.
	 * 
	 * @param target
	 *            the double array to write into
	 * @param targetPos
	 *            position in target where to start writing
	 * @param length
	 *            the amount of data requested
	 * @return the amount of data actually delivered. If the returned value is less than length, only that many data items have
	 *         been copied into target; further calls will return 0 and not copy anything.
	 */
	public int getData(double[] target, int targetPos, int length) {
		int currentPos = targetPos;
		int totalCopied = 0;
		int nTimesRead0 = 0;
		while (hasMoreData() && totalCopied < length) {
			int nSamplesToCopy = length - totalCopied;
			if (nSamplesToCopy > byteBuf.length / bytesPerSample) {
				nSamplesToCopy = byteBuf.length / bytesPerSample;
			}
			int nBytesRead = 0;
			try {
				nBytesRead = ais.read(byteBuf, 0, bytesPerSample * nSamplesToCopy);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return totalCopied;
			}
			if (nBytesRead == -1) { // end of stream
				hasMoreData = false;
				return totalCopied;
			} else if (nBytesRead == 0) { // prevent deadlock
				nTimesRead0++;
				if (nTimesRead0 > 10) {
					hasMoreData = false;
					return totalCopied;
				}
			} else { // nBytesRead > 0
				nTimesRead0 = 0;
				// Now we have nBytesRead/bytesPerSample samples in byteBuf.
				if (bytesPerSample == 1) {
					for (int i = 0; i < nBytesRead; i++, currentPos++) {
						target[currentPos] = (byteBuf[i] << 8) / 128.0; // normalise to range [-1, 1];
					}
					totalCopied += nBytesRead;
				} else if (bytesPerSample == 2) { // 16 bit
					for (int i = 0; i < nBytesRead; i += 2, currentPos++) {
						int sample;
						byte lobyte;
						byte hibyte;
						if (!bigEndian) {
							lobyte = byteBuf[i];
							hibyte = byteBuf[i + 1];
						} else {
							lobyte = byteBuf[i + 1];
							hibyte = byteBuf[i];
						}
						sample = hibyte << 8 | lobyte & 0xFF;
						target[currentPos] = sample / 32768.0;// normalise to range [-1, 1];
					}
					totalCopied += nBytesRead / bytesPerSample;
				} else { // bytesPerSample == 3, i.e. 24 bit
					for (int i = 0; i < nBytesRead; i += 3, currentPos++) {
						int sample;
						byte lobyte;
						byte midbyte;
						byte hibyte;
						if (!bigEndian) {
							lobyte = byteBuf[i];
							midbyte = byteBuf[i + 1];
							hibyte = byteBuf[i + 2];
						} else {
							lobyte = byteBuf[i + 2];
							midbyte = byteBuf[i + 1];
							hibyte = byteBuf[i];
						}
						sample = hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
						target[currentPos] = sample / 8388606.0; // normalise to range [-1, 1]
					}
					totalCopied += nBytesRead / bytesPerSample;
				}

			}
		}
		assert totalCopied <= length;
		return totalCopied;
	}

	/**
	 * Whether or not any more data can be read from this data source.
	 * 
	 * @return true if another call to getData() will return data, false otherwise.
	 */
	public boolean hasMoreData() {
		return hasMoreData;
	}

	/**
	 * The number of doubles that can currently be read from this double data source without blocking. This number can change over
	 * time.
	 * 
	 * @return the number of doubles that can currently be read without blocking
	 */
	public int available() {
		try {
			int bytes = ais.available();
			return bytes / bytesPerSample;
		} catch (IOException e) {
			return 0;
		}
	}

	public long getDataLength() {
		long frameLength = ais.getFrameLength();
		if (frameLength == AudioSystem.NOT_SPECIFIED)
			return DoubleDataSource.NOT_SPECIFIED;
		else
			return frameLength;
	}

}
