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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SharedOutputBuffer;

/**
 * @author marc
 * 
 */
public class TestProducingNHttpEntity extends AbstractHttpEntity implements ProducingNHttpEntity {

	public TestProducingNHttpEntity() {
	}

	public void finish() {
	}

	public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
		final SharedOutputBuffer ob = new SharedOutputBuffer(8192, ioctrl, new HeapByteBufferAllocator());
		new Thread() {
			public void run() {
				try {
					FileInputStream fis = new FileInputStream("/Users/marc/Music/enjoytheride_feat.judytzuke_.mp3");
					int nRead;
					byte[] bytes = new byte[4096];
					while ((nRead = fis.read(bytes)) != -1) {
						synchronized (this) {
							try {
								wait(1);
							} catch (InterruptedException ie) {
							}
						}
						ob.write(bytes, 0, nRead);
					}
					ob.writeCompleted();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		while (!encoder.isCompleted())
			ob.produceContent(encoder);
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

}
