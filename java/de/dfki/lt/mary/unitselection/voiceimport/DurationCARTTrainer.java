package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.util.PrintfFormat;

/**
 * A class which converts a text file in festvox format
 * into a one-file-per-utterance format in a given directory.
 * @author schroed
 *
 */
public class DurationCARTTrainer implements VoiceImportComponent
{
    protected File unitlabelDir;
    protected File unitfeatureDir;
    protected File durationDir;
    protected File durationFeaturesFile;
    protected File wagonDescFile;
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    
    
    
    /**/
    public DurationCARTTrainer( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;
        this.unitlabelDir = new File( db.phoneUnitLabDirName() );
        this.unitfeatureDir = new File( db.phoneUnitFeaDirName() );
        String rootDir = db.rootDirName();
        this.durationDir = new File(rootDir, System.getProperty("db.durationdir", "dur"));
        if (!durationDir.exists()) durationDir.mkdir();
        this.durationFeaturesFile = new File(durationDir, System.getProperty("db.durationfeaturesfile", "dur.feats"));
        this.wagonDescFile = new File(durationDir, System.getProperty("db.wagondescfile", "dur.desc"));
    }
    
    /**/
    public boolean compute() throws IOException
    {
        PrintWriter toFeaturesFile = new PrintWriter(new FileOutputStream(durationFeaturesFile));
        int nOK = 0;
        for (int i=0, len=bnl.getLength(); i<len; i++) {
            String[] aligned;
            try {
                aligned = align(bnl.getName(i));
            } catch (IOException e) {
                e.printStackTrace();
                aligned = null;
            }
            if (aligned == null) {
                System.err.println("Cannot align "+bnl.getName(i)+" -- skipping");
            } else {
                for (int l=0, lMax=aligned.length; l<lMax; l++) {
                    toFeaturesFile.println(aligned[l]);
                }
                nOK++;
            }
        }
        toFeaturesFile.close();
        System.out.println("Duration features extracted for "+nOK+" out of "+bnl.getLength()+ " files");
        if (nOK == 0) return false;

        FeatureFileReader features = FeatureFileReader.getFeatureFileReader(db.targetFeaturesFileName());
        FeatureDefinition fd = features.getFeatureDefinition();
        PrintWriter toDesc = new PrintWriter(new FileOutputStream(wagonDescFile));
        generateFeatureDescriptionForWagon(fd, toDesc);
        toDesc.close();
        
        // Now, call wagon
        WagonCaller wagonCaller = new WagonCaller(null);
        File wagonTreeFile = new File(durationDir, "dur.tree");
        boolean ok = wagonCaller.callWagon("-data "+durationFeaturesFile.getAbsolutePath()
                +"-desc "+wagonDescFile.getAbsolutePath()
                +"-stop 10 "
                +"-output "+wagonTreeFile.getAbsolutePath());
        return ok;

    }
    
    private String[] align(String basename) throws IOException
    {
        BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.phoneUnitLabDirName() + basename + db.phoneUnitLabExt() )), "UTF-8"));
        BufferedReader features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.phoneUnitFeaDirName() + basename + db.phoneUnitFeaExt() )), "UTF-8")); 
        String line;
        // Skip label file header:
        while ((line = labels.readLine()) != null) {
            if (line.startsWith("#")) break; // line starting with "#" marks end of header
        }
        // Skip features file header:
        while ((line = features.readLine()) != null) {
            if (line.trim().equals("")) break; // empty line marks end of header
        }
        
        // Now go through all feature file units
        boolean correct = true;
        int unitIndex = -1;
        float prevEnd = 0;
        List aligned = new ArrayList();
        while (correct) {
            unitIndex++;
            String labelLine = labels.readLine();
            String featureLine = features.readLine();
            if (featureLine == null) { // incomplete feature file
                return null;
            } else if (featureLine.trim().equals("")) { // end of features
                if (labelLine == null) break; // normal end found
                else // label file is longer than feature file
                    return null;
            }
            // Verify that the two labels are the same:
            StringTokenizer st = new StringTokenizer(labelLine.trim());
            // The third token in each line is the label
            float end = Float.parseFloat(st.nextToken());
            st.nextToken(); // skip
            String labelUnit = st.nextToken();

            st = new StringTokenizer(featureLine.trim());
            // The expect that the first token in each line is the label
            String featureUnit = st.nextToken();
            if (!featureUnit.equals(labelUnit)) {
                // Non-matching units found
                return null;
            }
            // OK, now we assume we have two matching lines.
            if (!featureUnit.startsWith("_")) { // discard all silences
                // Output format: unit duration, followed by the feature line
                aligned.add(new PrintfFormat(Locale.ENGLISH, "%.3f").sprintf(end-prevEnd)+" "+featureLine.trim());
            }
            prevEnd = end;
        }
        return (String[]) aligned.toArray(new String[0]);
    }
    
    private void generateFeatureDescriptionForWagon(FeatureDefinition fd, PrintWriter out)
    {
        out.println("(");
        out.println("(segment_duration float)");
        int nDiscreteFeatures = fd.getNumberOfByteFeatures()+fd.getNumberOfShortFeatures();
        for (int i=0, n=fd.getNumberOfFeatures(); i<n; i++) {            
            out.print("( ");
            out.print(fd.getFeatureName(i));
            if (i<nDiscreteFeatures) { // list values
                if (fd.getNumberOfValues(i) == 20 && fd.getFeatureValueAsString(i, 19).equals("19")) {
                    // one of our pseudo-floats
                    out.println(" float )");
                } else { // list the values
                    for (int v=0, vmax=fd.getNumberOfValues(i); v<vmax; v++) {
                        out.print("  ");
                        String val = fd.getFeatureValueAsString(i, v);
                        if (val.indexOf('"') != -1) {
                            StringBuffer buf = new StringBuffer();
                            for (int c=0; c<val.length(); c++) {
                                char ch = val.charAt(c);
                                if (ch == '"') buf.append("\\\"");
                                else buf.append(ch);
                            }
                            val = buf.toString();
                        }
                        out.print("\""+val+"\"");
                    }
                    out.println(" )");
                }
            } else { // float feature
                    out.println(" float )");
            }

        }
        out.println(")");
    }

    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return -1;
    }


}
