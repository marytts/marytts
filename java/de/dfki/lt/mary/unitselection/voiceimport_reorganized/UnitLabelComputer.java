package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import de.dfki.lt.mary.util.FileUtils;

/**
 * Compute unit labels from phone labels.
 * @author schroed
 *
 */
public class UnitLabelComputer
{
    protected File phonelabelDir;
    protected File unitlabelDir;
    
    public UnitLabelComputer(File phonelabelDir, File unitlabelDir)
    throws IOException
    {
        if (!phonelabelDir.exists()) throw new IOException("No such directory: "+ phonelabelDir);
        if (!unitlabelDir.exists()) unitlabelDir.mkdir();
        this.phonelabelDir = phonelabelDir;
        this.unitlabelDir = unitlabelDir;
    }
    
    public void compute() throws IOException
    {
        String[] basenames = FileUtils.listBasenames(phonelabelDir, ".lab");
        System.out.println("Computing unit labels for "+basenames.length+" files");
        for (int i=0; i<basenames.length; i++) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(phonelabelDir, basenames[i]+".lab")), "UTF-8"));
            PrintWriter out = new PrintWriter(new File(unitlabelDir, basenames[i]+".unitlab"), "UTF-8");
            // TODO: do something meaningful here
            String line;
            while ((line = in.readLine())!= null) {
                out.println(line);
            }
            out.flush();
            out.close();
            System.out.println("    "+basenames[i]);
        }
        System.out.println("Finished computing unit labels");
    }

    public static void main(String[] args) throws IOException
    {
        String phonelab = System.getProperty("phonelab.dir", "lab");
        String unitlab = System.getProperty("unitlab.dir", "unitlab");
        UnitLabelComputer ulc = new UnitLabelComputer(new File(phonelab), new File(unitlab));
        ulc.compute();
    }

}
