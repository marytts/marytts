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
package marytts.server;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import marytts.datatypes.MaryXML;

import org.w3c.dom.traversal.DocumentTraversal;

/**
 * @author Marc Schr&ouml;der
 * 
 * 
 */
public class EnvironmentChecks {

	/**
	 * Check central requirements in the runtime environment of the system.
	 * 
	 * @throws Error
	 *             if any requirement is violated.
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
			throw new Error("XML handling code " + DocumentBuilderFactory.newInstance().getClass()
					+ " does not support DocumentTraversal.\n" + "Please update your java XML handling code as described in "
					+ MaryProperties.getProperty("mary.base") + File.separator + "README.");
		}

		// Try to find a suitable XSLT transformer
		/*
		 * tFactory = javax.xml.transform.TransformerFactory.newInstance(); if (tFactory instanceof
		 * org.apache.xalan.processor.TransformerFactoryImpl) { Hashtable xalanEnv = (new
		 * org.apache.xalan.xslt.EnvironmentCheck()).getEnvironmentHash(); String xalan2Version = (String)
		 * xalanEnv.get("version.xalan2x"); if (xalan2Version == null || xalan2Version.equals("")) xalan2Version = (String)
		 * xalanEnv.get("version.xalan2"); if (xalan2Version != null && !xalan2Version.equals("")) System.err.println("Using " +
		 * xalan2Version); } else { System.err.println("Using XSL processor " + tFactory.getClass().getName()); }
		 */
	}
}
