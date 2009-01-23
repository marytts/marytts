/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Marc Schr&ouml;der
 *
 * Group all the test cases in this package.
 * 
 */
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for marytts.signalproc.tests");
        //$JUnit-BEGIN$
        suite.addTest(new TestSuite(BufferedDoubleDataSourceTest.class));
        suite.addTest(new TestSuite(FFTTest.class));
        suite.addTest(new TestSuite(AudioDoubleDataSourceTest.class));
        suite.addTest(new TestSuite(FrameProviderTest.class));
        suite.addTest(new TestSuite(FrameOverlapAddTest.class));
        suite.addTest(new TestSuite(PhaseVocoderTest.class));
        suite.addTest(new TestSuite(PitchFrameProviderTest.class));
        //$JUnit-END$
        
        // TestCases in other packages:
        return suite;
    }
}


