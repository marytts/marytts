/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.mary.recsessionmgr;

import de.dfki.lt.mary.recsessionmgr.debug.Test;
import de.dfki.lt.mary.recsessionmgr.gui.AdminWindow;
import de.dfki.lt.mary.recsessionmgr.gui.Splash;

public class Redstart {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        
        // Display splash screen

        Splash splash = null;
        try {
            splash = new Splash();
            splash.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Test.output("OS Name: " + System.getProperty("os.name"));
        Test.output("OS Architecture: " + System.getProperty("os.arch"));
        Test.output("OS Version: " + System.getProperty("os.version"));

        System.out.println("Welcome to Redstart, your recording session manager.");
        
        final String voiceFolderPathString;
        // Set string variable indicating voice folder location
        if (args.length > 0) {
            voiceFolderPathString = args[0]; // Take first command-line argument as the voice path
        }
        else { // default to current directory
            voiceFolderPathString = ".";
        }
        
        // TESTCODE
        Test.output("|Redstart.main| voiceFolderPath = " + voiceFolderPathString);
                        
        AdminWindow adminWindow = new AdminWindow(voiceFolderPathString);
        if (splash != null) splash.setVisible(false);
        adminWindow.setVisible(true);
    }

}
