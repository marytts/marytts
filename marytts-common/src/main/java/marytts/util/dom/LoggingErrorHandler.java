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
package marytts.util.dom;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Implements an ErrorHandler for XML parsing that provides error and warning messages to the log4j logger.
 * 
 * @author Marc Schr&ouml;der
 */

public class LoggingErrorHandler implements ErrorHandler, ErrorListener {
	Logger logger;

	public LoggingErrorHandler(String name) {
		logger = MaryUtils.getLogger(name);
	}

	public void error(SAXParseException e) throws SAXParseException {
		logger.warn(e.getMessage());
		throw e;
	}

	public void error(TransformerException e) throws TransformerException {
		logger.warn(e.getMessageAndLocation());
		throw e;
	}

	public void warning(SAXParseException e) throws SAXParseException {
		logger.warn(e.getMessage());
		throw e;
	}

	public void warning(TransformerException e) throws TransformerException {
		logger.warn(e.getMessageAndLocation());
		throw e;
	}

	public void fatalError(SAXParseException e) throws SAXParseException {
		logger.warn(e.getMessage());
		throw e;
	}

	public void fatalError(TransformerException e) throws TransformerException {
		logger.warn(e.getMessageAndLocation());
		throw e;
	}

}
