package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.FileUtils;

/**
 * For the given texts, compute unit features and align them
 * with the given unit labels.
 * @author schroed
 *
 */
public class UnitFeatureComputer {

    protected File textDir;
    protected File unitfeatureDir;
    protected String locale;
    protected MaryClient mary;
    
    public UnitFeatureComputer(File textDir, File unitfeatureDir, String locale)
    throws IOException
    {
        if (!textDir.exists()) throw new IOException("No such directory: "+ textDir);
        if (!unitfeatureDir.exists()) unitfeatureDir.mkdir();
        this.textDir = textDir;
        this.unitfeatureDir = unitfeatureDir;
        this.locale = locale;
        this.mary = new MaryClient();
    }

    public void compute() throws IOException
    {
        String[] basenames = FileUtils.listBasenames(textDir, ".txt");
        System.out.println("Computing unit features for "+basenames.length+" files");
        for (int i=0; i<basenames.length; i++) {
            computeFeaturesFor(basenames[i]);
            System.out.println("    "+basenames[i]);
        }
        System.out.println("Finished computing unit features");
    }

    public void computeFeaturesFor(String basename) throws IOException
    {
        String text;
        String inputFormat;
        // First, test if there is a corresponding .rawmaryxml file in textdir:
        File rawmaryxmlFile = new File(textDir, basename+".rawmaryxml");
        if (rawmaryxmlFile.exists()) {
            text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
            inputFormat = "RAWMARYXML";
        } else {
            text = FileUtils.getFileAsString(new File(textDir, basename+".txt"), "UTF-8");
            inputFormat = "TEXT_"+locale.toUpperCase();
        }
        String outputFormat = "TARGETFEATURES";
        OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(unitfeatureDir, basename+".feats")));
        mary.process(text, inputFormat, outputFormat, null, null, os);
        os.flush();
        os.close();
    }

    public static void main(String[] args) throws IOException
    {
        String text = System.getProperty("text.dir", "text");
        String unitfeatures = System.getProperty("unitfeatures.dir", "unitfeatures");
        String locale = System.getProperty("locale", "en");
        UnitFeatureComputer ufc = new UnitFeatureComputer(new File(text), new File(unitfeatures), locale);
        ufc.compute();
    }
}
