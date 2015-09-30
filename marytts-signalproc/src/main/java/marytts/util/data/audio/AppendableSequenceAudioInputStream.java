/**
 * Copyright (C) 2005 DFKI GmbH. All rights reserved.
 */

package marytts.util.data.audio;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class AppendableSequenceAudioInputStream extends SequenceAudioInputStream {
	protected boolean doneAppending = false;

	/**
	 * Create a sequence audio input stream to which more AudioInputStreams can be appended after creation. When the currently
	 * available audio input streams have been read, calls to read() will block until new audio data is appended or
	 * doneAppending() is called. After doneAppending() is called, read() will return -1 when running out of data.
	 * 
	 * @param audioFormat
	 *            audio format
	 * @param audioInputStreams
	 *            the list of initial audio input streams, or null if initially the stream is empty.
	 */
	public AppendableSequenceAudioInputStream(AudioFormat audioFormat, Collection<AudioInputStream> audioInputStreams) {
		super(audioFormat, audioInputStreams != null ? audioInputStreams : Arrays.asList(new AudioInputStream[0]));
	}

	/**
	 * Append the new audio input stream to the end of the list of audio input streams.
	 * 
	 * @param ais
	 *            ais
	 * @throws IllegalArgumentException
	 *             if this method is called after doneAppending() was called.
	 */
	public synchronized void append(AudioInputStream ais) {
		if (ais == this)
			throw new IllegalArgumentException("Cannot append me to myself");
		if (doneAppending)
			throw new IllegalArgumentException("Cannot append after doneAppending() was called!");
		m_audioInputStreamList.add(ais);
		// System.err.println("Appending audio");
		notifyAll();
	}

	/**
	 * Inform this audio input stream not to expect any further calls to append(), and report end-of-stream when all data has been
	 * read.
	 */
	public synchronized void doneAppending() {
		doneAppending = true;
		// System.err.println("Done appending");
		notifyAll();
	}

	public synchronized int read() throws IOException {
		while (m_audioInputStreamList.size() == 0) {
			if (doneAppending) // never had any data, no more to come
				return -1;
			// no data yet, wait
			try {
				wait();
			} catch (InterruptedException ie) {
			}
		}
		int n = -1;
		// Try to read data
		while ((n = super.read()) <= 0) { // no data, but more expected
			if (n == -1 && doneAppending) // finished reading
				return -1;
			// wait and try again
			try {
				wait();
			} catch (InterruptedException ie) {
			}
		}
		return n;
	}

	public synchronized int read(byte[] buf, int off, int len) throws IOException {
		int n = -1;
		while (m_audioInputStreamList.size() == 0) {
			if (doneAppending) // never had any data, no more to come
				return -1;
			// no data yet, wait
			try {
				wait();
			} catch (InterruptedException ie) {
			}
		}
		// Try to read data
		while (m_nCurrentStream >= m_audioInputStreamList.size() || (n = super.read(buf, off, len)) <= 0) { // no data, but more
																											// expected
			if (n == -1 && doneAppending) // finished reading
				return -1;
			// wait and try again
			try {
				wait();
			} catch (InterruptedException ie) {
			}
		}
		// System.err.println("Read "+ n + " bytes");
		return n;
	}

	/**
	 * Return the frame length of this appendable sequence audio input stream. As long as <code>doneAppending()</code> has not
	 * been called, returns <code>AudioSystem.NOT_SPECIFIED</code>; after that, the frame length is the sum of the frame lengths
	 * of individual frame lengths.
	 * 
	 * @return total
	 */
	public long getFrameLength() {
		if (!doneAppending) {
			return AudioSystem.NOT_SPECIFIED;
		} else {
			long total = 0;
			for (int i = 0, n = m_audioInputStreamList.size(); i < n; i++) {
				long length = ((AudioInputStream) m_audioInputStreamList.get(i)).getFrameLength();
				if (length == AudioSystem.NOT_SPECIFIED) {
					// If one is not specified, all are not specified
					return AudioSystem.NOT_SPECIFIED;
				}
				total += length;
			}
			return total;
		}

	}
}
