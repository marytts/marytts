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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * Read from a stream and log.
 * 
 * @author Marc Schr&ouml;der
 */

public class StreamLogger extends Thread {
	private InputStream is;
	private PrintStream ps;
	private Logger logger;
	private Pattern ignorePattern = null;

	/**
	 * Read from an input stream, logging to category <code>logCategory</code>, ignoring lines matching the regular expression
	 * specified in <code>ignorePattern</code>. If <code>logCategory</code> is <code>null</code>, "unnamed" will be used. If
	 * <code>ignorePattern</code> is <code>null</code>, no filtering will be performed. The thread will silently die when it
	 * reaches end-of-file from the input stream.
	 * 
	 * @param is
	 *            is
	 * @param logCategory
	 *            logCategory
	 * @param ignorePattern
	 *            ignorePattern
	 */
	public StreamLogger(InputStream is, String logCategory, String ignorePattern) {
		this.is = is;
		if (logCategory == null)
			logger = MaryUtils.getLogger("unnamed");
		else
			logger = MaryUtils.getLogger(logCategory);
		if (ignorePattern != null) {
			try {
				this.ignorePattern = Pattern.compile(ignorePattern);
			} catch (PatternSyntaxException e) {
				logger.warn("Problem with regular expression pattern", e);
				this.ignorePattern = null;
			}
		}
	}

	public StreamLogger(InputStream is, PrintStream ps) {
		this.is = is;
		this.ps = ps;
	}

	public void run() {
		String line = null;
		try {
			BufferedReader b = new BufferedReader(new InputStreamReader(is));
			while ((line = b.readLine()) != null) {
				if (ignorePattern != null && ignorePattern.matcher(line).matches())
					continue; // do not log
				if (ps != null) {
					ps.println(line);
				} else {
					logger.info(line);
				}
			}
		} catch (IOException e) {
			try {
				logger.warn("Cannot read from stream", e);
			} catch (NullPointerException npe) {
				e.printStackTrace();
			}
		}
	}
}
