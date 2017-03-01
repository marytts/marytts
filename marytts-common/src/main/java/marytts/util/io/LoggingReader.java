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
package marytts.util.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;

public class LoggingReader extends FilterReader {
	protected Logger logger;
	protected StringBuffer logText;

	public LoggingReader(Reader in, Logger logger) {
		super(in);
		this.logger = logger;
		logText = new StringBuffer();
	}

	public int read() throws IOException {
		int c = super.read();
		if (c == -1) {
			logRead();
		} else {
			logText.append((char) c);
		}
		return c;
	}

	public int read(char[] cbuf, int off, int len) throws IOException {
		int nr = super.read(cbuf, off, len);
		if (nr == -1) {
			logRead();
		} else {
			logText.append(new String(cbuf, off, nr));
		}
		return nr;
	}

	public void close() throws IOException {
		super.close();
		logRead();
	}

	public void logRead() {
		if (logText.length() > 0) {
			logger.info("Read:\n" + logText.toString());
			logText.setLength(0);
		}
	}
}
