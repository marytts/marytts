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
package de.dfki.lt.mary;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.traversal.DocumentTraversal;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class EnvironmentChecks {

    /**
     * Check central requirements in the runtime environment of the system. 
     * @throws Error if any requirement is violated.
     */
    public static void check() {
        // Java version
        String javaVersion = System.getProperty("java.version");
		if (Float.parseFloat(javaVersion.substring(0, 3)) < 1.499) {
			// 1.499 instead of 1.5 because of rounding error
            	throw new Error("Wrong java version: Required 1.5, found " + javaVersion);
        	}
        // XML code
        if (!(MaryXML.newDocument() instanceof DocumentTraversal)) {
            throw new Error("XML handling code " +
                DocumentBuilderFactory.newInstance().getClass() +
                " does not support DocumentTraversal.\n" +
                "Please update your java XML handling code as described in " +
                MaryProperties.getProperty("mary.base") + File.separator + "README.");
        }

        // Try to find a suitable XSLT transformer
        /*
        tFactory = javax.xml.transform.TransformerFactory.newInstance();
        if (tFactory instanceof org.apache.xalan.processor.TransformerFactoryImpl) {
            Hashtable xalanEnv = (new org.apache.xalan.xslt.EnvironmentCheck()).getEnvironmentHash();
            String xalan2Version = (String) xalanEnv.get("version.xalan2x");
            if (xalan2Version == null || xalan2Version.equals(""))
                xalan2Version = (String) xalanEnv.get("version.xalan2");
            if (xalan2Version != null && !xalan2Version.equals(""))
                System.err.println("Using " + xalan2Version);
        } else {
            System.err.println("Using XSL processor " + tFactory.getClass().getName());
        }
        */
    }
}
