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
