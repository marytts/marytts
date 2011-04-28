/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.tests.modules;

import java.util.Locale;

import marytts.modules.OpenNLPPosTagger;
import marytts.tests.modules.MaryModuleTestCase;

import org.junit.Test;

/**
 * @author marc
 *
 */
public class OpenNLPPosTaggerIT extends MaryModuleTestCase {

	/**
	 * @param needMaryStarted
	 * @throws Exception
	 */
	public OpenNLPPosTaggerIT() throws Exception {
		super(true); // start MARY
		module = new OpenNLPPosTagger("de", "de.pos");
		module.startup();
		
	}
	
	@Override
	protected String inputEnding() {
		return "words";
	}
	
	@Override
	protected String outputEnding() {
		return "partsofspeech";
	}
	
	@Test
	public void posExample1() throws Exception {
		processAndCompare("example1-de", Locale.GERMAN);
	}

}
