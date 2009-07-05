/**
 * Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */

package marytts.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import marytts.tests.language.de.JTokeniserTest;
import marytts.tests.language.de.PreprocessorTest;

/**
 * @author Marc Schr&ouml;der
 *
 * Group all the test cases in this package.

 */
public class AllGermanTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for German de.dfki.lt.mary.tests");
        suite.addTest(new TestSuite(GermanMaryTest.class));

        // TestCases in other packages:
        suite.addTest(new TestSuite(JTokeniserTest.class));
        suite.addTest(new TestSuite(PreprocessorTest.class));
        return suite;
    }
}
