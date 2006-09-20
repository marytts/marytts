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
import java.util.StringTokenizer;
import java.util.Vector;

import de.dfki.lt.mary.util.FileUtils;

/**
 * Compute unit labels from phone labels.
 * @author schroed
 *
 */
public class UnitLabelComputer implements VoiceImportComponent
{
    protected File phonelabelDir;
    protected File unitlabelDir;
    protected String pauseSymbol;
    
    public UnitLabelComputer() throws IOException
    {
        phonelabelDir = new File(System.getProperty("phonelab.dir", "lab"));
        if (!phonelabelDir.exists()) throw new IOException("No such directory: "+ phonelabelDir);
        unitlabelDir = new File(System.getProperty("unitlab.dir", "unitlab"));
        if (!unitlabelDir.exists()) unitlabelDir.mkdir();
        pauseSymbol = System.getProperty("pause.symbol", "pau");

    }
    
    public boolean compute() throws IOException
    {
        String[] basenames = FileUtils.listBasenames(phonelabelDir, ".lab");
        System.out.println("Computing unit labels for "+basenames.length+" files");
        for (int i=0; i<basenames.length; i++) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(phonelabelDir, basenames[i]+".lab")), "UTF-8"));
            PrintWriter out = new PrintWriter(new File(unitlabelDir, basenames[i]+".unitlab"), "UTF-8");
            // Merge adjacent pauses into one: In a sequence of pauses,
            // only remember the last one.
            String pauseLine = null;
            String line;
            while ((line = in.readLine())!= null) {
                // Verify if this is a pause unit
                String unit = getUnit(line);
                if (pauseSymbol.equals(unit)) {
                    pauseLine = line; // remember only the latest pause line
                } else { // a non-pause symbol
                    if (pauseLine != null) {
                        out.println(pauseLine);
                        pauseLine = null;
                    }
                    out.println(line);
                }
            }
            if (pauseLine != null) {
                out.println(pauseLine);
            }
            out.flush();
            out.close();
            System.out.println("    "+basenames[i]);
        }
        System.out.println("Finished computing unit labels");
        return true;
    }
    
    protected String getUnit(String line)
    {
        StringTokenizer st = new StringTokenizer(line.trim());
        // The third token in each line is the label
        if (st.hasMoreTokens()) st.nextToken();
        if (st.hasMoreTokens()) st.nextToken();
        if (st.hasMoreTokens()) return st.nextToken();
        return null;
    }
    
    public static void main(String[] args) throws IOException
    {
        new UnitLabelComputer().compute();
    }

}
