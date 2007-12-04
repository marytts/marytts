/*
 * Test.java
 *
 * Created on June 24, 2007, 2:48 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.dfki.lt.mary.recsessionmgr.debug;

/**
 *
 * @author Mat Wilson <mat.wilson@dfki.de>
 */
public class Test {
    
    public static boolean isDebug = true;
    
    /**
     * Creates a new instance of Test
     */
    public Test() {
        Test.output("Test object created.");
        setDebugMode(false);
    }

    public static void output(String message) {
        if (Test.isDebug) { System.out.println(message); }
    }

    public static void setDebugMode(boolean isEnabled) {
        Test.isDebug = isEnabled;
    }
}
