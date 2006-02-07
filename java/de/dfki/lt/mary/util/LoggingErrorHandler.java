/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Implements an ErrorHandler for XML parsing
 *          that provides error and warning messages to the log4j logger.
 *
 * @author Marc Schr&ouml;der
 */

public class LoggingErrorHandler implements ErrorHandler, ErrorListener
{
    Logger logger;
    public LoggingErrorHandler(String name)
    {
        logger = Logger.getLogger(name);
    }

    public void error(SAXParseException e)
        throws SAXParseException
    {
        logger.warn(e.getMessage());
        throw e;
    }

    public void error(TransformerException e)
        throws TransformerException
    {
        logger.warn(e.getMessageAndLocation());
        throw e;
    }

    public void warning(SAXParseException e)
        throws SAXParseException
    {
        logger.warn(e.getMessage());
        throw e;
    }

    public void warning(TransformerException e)
        throws TransformerException
    {
        logger.warn(e.getMessageAndLocation());
        throw e;
    }

    public void fatalError(SAXParseException e)
        throws SAXParseException
    {
        logger.warn(e.getMessage());
        throw e;
    }

    public void fatalError(TransformerException e)
        throws TransformerException
    {
        logger.warn(e.getMessageAndLocation());
        throw e;
    }

    

}
