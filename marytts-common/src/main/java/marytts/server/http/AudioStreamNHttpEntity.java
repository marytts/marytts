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

package marytts.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.server.Request;
import marytts.util.MaryUtils;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentOutputStream;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SharedOutputBuffer;
import org.apache.log4j.Logger;

/**
 * @author marc
 * 
 */
public class AudioStreamNHttpEntity extends AbstractHttpEntity implements ProducingNHttpEntity, Runnable {
	private Request maryRequest;
	private AudioInputStream audio;
	private AudioFileFormat.Type audioType;
	private Logger logger;
	private Object mutex;
	private SharedOutputBuffer out;

	public AudioStreamNHttpEntity(Request maryRequest) {
		this.maryRequest = maryRequest;
		this.audio = maryRequest.getAudio();
		this.audioType = maryRequest.getAudioFileFormat().getType();
		setContentType(MaryHttpServerUtils.getMimeType(audioType));
		this.mutex = new Object();
	}

	public void finish() {
		assert logger != null : "we should never be able to write if run() is not called";
		logger.info("Completed sending streaming audio");
		maryRequest = null;
		audio = null;
		audioType = null;
		logger = null;
	}

	public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
		if (out == null) {
			synchronized (mutex) {
				out = new SharedOutputBuffer(8192, ioctrl, new HeapByteBufferAllocator());
				mutex.notify();
			}
		}
		while (!encoder.isCompleted())
			out.produceContent(encoder);
	}

	public long getContentLength() {
		return -1;
	}

	public boolean isRepeatable() {
		return false;
	}

	public boolean isStreaming() {
		return true;
	}

	public InputStream getContent() {
		return null;
	}

	public void writeTo(final OutputStream outstream) throws IOException {
		throw new RuntimeException("Should not be called");
	}

	/**
	 * Wait for the SharedOutputBuffer to become available, write audio data to it.
	 */
	public void run() {
		this.logger = MaryUtils.getLogger(Thread.currentThread().getName());
		// We must wait until produceContent() is called:
		while (out == null) {
			synchronized (mutex) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		assert out != null;
		ContentOutputStream outStream = new ContentOutputStream(out);
		try {
			AudioSystem.write(audio, audioType, outStream);
			outStream.flush();
			outStream.close();
			logger.info("Finished writing output");
		} catch (IOException ioe) {
			logger.info("Cannot write output, client seems to have disconnected. ", ioe);
			maryRequest.abort();
		}
	}
}
