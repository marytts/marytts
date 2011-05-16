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
package marytts;

import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import marytts.datatypes.MaryDataType;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

/**
 * @author marc
 *
 */
public class MaryInterfaceIT {
	@Test
	public void canGetMaryInterface() throws Exception {
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		assertNotNull(mary);
		assertEquals(MaryDataType.TEXT, mary.getInputType());
		assertEquals(MaryDataType.AUDIO, mary.getOutputType());
		assertEquals(Locale.US, mary.getLocale());
	}
	
	@Test
	public void canSetInputType() throws Exception {
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		MaryDataType in = MaryDataType.RAWMARYXML;
		assertTrue(!in.equals(mary.getInputType()));
		mary.setInputType(in);
		assertEquals(in, mary.getInputType());
	}

	@Test
	public void canSetOutputType() throws Exception {
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		MaryDataType out = MaryDataType.TOKENS;
		assertTrue(!out.equals(mary.getOutputType()));
		mary.setOutputType(out);
		assertEquals(out, mary.getOutputType());
	}
	
	@Test
	public void canSetLocale() throws Exception {
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		Locale loc = Locale.GERMAN;
		assertTrue(!loc.equals(mary.getLocale()));
		mary.setLocale(loc);
		assertEquals(loc, mary.getLocale());
	}

	@Test
	public void canSetAudioFileFormat() throws Exception {
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		AudioFileFormat aff = new AudioFileFormat(AudioFileFormat.Type.SND, null, AudioSystem.NOT_SPECIFIED);
		assertTrue(!aff.equals(mary.getAudioFileFormat()));
		mary.setAudioFileFormat(aff);
		assertEquals(aff, mary.getAudioFileFormat());
	}

	@Test
	public void canProcessToTokens() throws Exception {
		// setup
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		mary.setOutputType(MaryDataType.TOKENS);
		// exercise
		Document tokens = mary.generateXML("Hello world");
		// verify
		assertNotNull(tokens);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void refuseWrongInput1() throws Exception {
		// setup
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		mary.setInputType(MaryDataType.RAWMARYXML);
		// method with string arg does not match declared input type:
		mary.generateXML("some text");
	}

	@Test(expected=IllegalArgumentException.class)
	public void refuseWrongOutput1() throws Exception {
		// setup
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		// requesting xml output but set to default output type AUDIO:
		mary.generateXML("some text");
	}

	@Test(expected=IllegalArgumentException.class)
	public void refuseWrongOutput2() throws Exception {
		// setup
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		mary.setOutputType(MaryDataType.TOKENS);
		// requesting audio putput but set to XML output type:
		mary.generateAudio("some text");
	}

	@Test(expected=IllegalArgumentException.class)
	public void refuseWrongOutput3() throws Exception {
		// setup
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		mary.setOutputType(MaryDataType.TOKENS);
		// requesting text putput but set to XML output type:
		mary.generateText("some text");
	}

}
