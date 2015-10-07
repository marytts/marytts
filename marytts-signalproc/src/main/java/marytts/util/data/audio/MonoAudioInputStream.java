/**
 * Copyright 2000-2009 DFKI GmbH.
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

public class MonoAudioInputStream extends AudioInputStream {
	protected int inputChannels;
	protected int inputMode;
	protected AudioFormat newFormat;

	public MonoAudioInputStream(AudioInputStream input) {
		this(input, AudioPlayer.STEREO);
	}

	/**
	 * 
	 * @param input
	 *            input
	 * @param inputMode
	 *            if AudioPlayer.STEREO, average both input streams; if AudioPlayer.LEFT_ONLY, use only the left channel; if
	 *            AudioPlayer.RIGHT_ONLY, use only the right channel.
	 */
	public MonoAudioInputStream(AudioInputStream input, int inputMode) {
		super(input, input.getFormat(), input.getFrameLength());
		this.newFormat = new AudioFormat(input.getFormat().getEncoding(), input.getFormat().getSampleRate(), input.getFormat()
				.getSampleSizeInBits(), 1, input.getFormat().getFrameSize() / input.getFormat().getChannels(), input.getFormat()
				.getFrameRate(), input.getFormat().isBigEndian());
		this.inputChannels = input.getFormat().getChannels();
		if (inputChannels < 2)
			throw new IllegalArgumentException("expected more than one input channel!");
		this.inputMode = inputMode;
		if (inputMode == AudioPlayer.MONO)
			throw new IllegalArgumentException("expected non-mono input mode");
	}

	/**
	 * Reads up to a specified maximum number of bytes of data from the audio stream, putting them into the given byte array.
	 * <p>
	 * This method will always read an integral number of frames. If <code>len</code> does not specify an integral number of
	 * frames, a maximum of <code>len - (len % frameSize)
	 * </code> bytes will be read.
	 * 
	 * @param b
	 *            the buffer into which the data is read
	 * @param off
	 *            the offset, from the beginning of array <code>b</code>, at which the data will be written
	 * @param len
	 *            the maximum number of bytes to read
	 * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has
	 *         been reached
	 * @throws IOException
	 *             if an input or output error occurs
	 * @see #read(byte[])
	 * @see #read()
	 * @see #skip
	 * @see #available
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int sampleSizeInBytes = frameSize / inputChannels;
		int outputFrameSize = sampleSizeInBytes; // mono output
		int nFrames = len / outputFrameSize;
		boolean bigEndian = getFormat().isBigEndian();
		byte[] inputBytes = new byte[nFrames * frameSize];
		int nInputBytes = super.read(inputBytes, 0, inputBytes.length);
		if (nInputBytes <= 0)
			return nInputBytes;

		if (inputMode == AudioPlayer.STEREO) {
			for (int i = 0, j = off; i < nInputBytes; i += frameSize, j += outputFrameSize) {
				int sample = 0;
				for (int c = 0; c < inputChannels; c++) {
					if (sampleSizeInBytes == 1) {
						sample += inputBytes[i] << 8;
					} else if (sampleSizeInBytes == 2) { // 16 bit
						byte lobyte;
						byte hibyte;
						if (!bigEndian) {
							lobyte = inputBytes[i];
							hibyte = inputBytes[i + 1];
						} else {
							lobyte = inputBytes[i + 1];
							hibyte = inputBytes[i];
						}
						sample += hibyte << 8 | lobyte & 0xFF;
					} else { // bytesPerSample == 3, i.e. 24 bit
						assert sampleSizeInBytes == 3 : "Unsupported sample size in bytes: " + sampleSizeInBytes;
						byte lobyte;
						byte midbyte;
						byte hibyte;
						if (!bigEndian) {
							lobyte = inputBytes[i];
							midbyte = inputBytes[i + 1];
							hibyte = inputBytes[i + 2];
						} else {
							lobyte = inputBytes[i + 2];
							midbyte = inputBytes[i + 1];
							hibyte = inputBytes[i];
						}
						sample += hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
					}
				}
				sample /= inputChannels; // here is where we average the three samples
				if (sampleSizeInBytes == 1) {
					b[j] = (byte) ((sample >> 8) & 0xFF);
				} else if (sampleSizeInBytes == 2) { // 16 bit
					byte lobyte = (byte) (sample & 0xFF);
					byte hibyte = (byte) (sample >> 8);
					if (!bigEndian) {
						b[j] = lobyte;
						b[j + 1] = hibyte;
					} else {
						b[j] = hibyte;
						b[j + 1] = lobyte;
					}
				} else { // bytesPerSample == 3, i.e. 24 bit
					assert sampleSizeInBytes == 3 : "Unsupported sample size in bytes: " + sampleSizeInBytes;
					byte lobyte = (byte) (sample & 0xFF);
					byte midbyte = (byte) ((sample >> 8) & 0xFF);
					byte hibyte = (byte) (sample >> 16);
					if (!bigEndian) {
						b[j] = lobyte;
						b[j + 1] = midbyte;
						b[j + 2] = hibyte;
					} else {
						b[j] = hibyte;
						b[j + 1] = midbyte;
						b[j + 2] = lobyte;
					}
				}
			} // for all frames
		} else if (inputMode == AudioPlayer.LEFT_ONLY) {
			for (int i = 0, j = off; i < nInputBytes; i += frameSize, j += outputFrameSize) {
				for (int k = 0; k < sampleSizeInBytes; k++) {
					b[j + k] = inputBytes[i + k];
				}
			}
		} else {
			assert inputMode == AudioPlayer.RIGHT_ONLY : "unexpected input mode: " + inputMode;
			for (int i = 0, j = off; i < nInputBytes; i += frameSize, j += outputFrameSize) {
				for (int k = 0; k < sampleSizeInBytes; k++) {
					b[j + k] = inputBytes[i + k + sampleSizeInBytes];
				}
			}

		}

		return nInputBytes / inputChannels;
	}

	/**
	 * Skips over and discards a specified number of bytes from this audio input stream.
	 * 
	 * @param n
	 *            the requested number of bytes to be skipped
	 * @return the actual number of bytes skipped
	 * @throws IOException
	 *             if an input or output error occurs
	 * @see #read
	 * @see #available
	 */
	public long skip(long n) throws IOException {
		return super.skip(n * inputChannels) / inputChannels;
	}

	/**
	 * Returns the maximum number of bytes that can be read (or skipped over) from this audio input stream without blocking. This
	 * limit applies only to the next invocation of a <code>read</code> or <code>skip</code> method for this audio input stream;
	 * the limit can vary each time these methods are invoked. Depending on the underlying stream,an IOException may be thrown if
	 * this stream is closed.
	 * 
	 * @return the number of bytes that can be read from this audio input stream without blocking
	 * @throws IOException
	 *             if an input or output error occurs
	 * @see #read(byte[], int, int)
	 * @see #read(byte[])
	 * @see #read()
	 * @see #skip
	 */
	public int available() throws IOException {
		int av = super.available();
		if (av <= 0)
			return av;
		return av / inputChannels;
	}

	public AudioFormat getFormat() {
		return newFormat;
	}
}
