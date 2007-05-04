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
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitFileReader;
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
    protected int percent = 0;
    protected boolean useStepwiseTraining = false;
    
    
    
    /**/
    public DurationCARTTrainer( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;
        this.unitlabelDir = new File( db.phoneUnitLabDirName() );
        this.unitfeatureDir = new File( db.phoneUnitFeaDirName() );
        String rootDir = db.rootDirName();
        this.durationDir = new File(rootDir, System.getProperty("db.duration.dir", "dur"));
        if (!durationDir.exists()) durationDir.mkdir();
        this.durationFeaturesFile = new File(durationDir, System.getProperty("db.duration.featuresfile", "dur.feats"));
        this.wagonDescFile = new File(durationDir, System.getProperty("db.dur.wagondescfile", "dur.desc"));
        this.useStepwiseTraining = Boolean.valueOf(System.getProperty("DurationCARTTrainer.useStepwiseTraining", "false")).booleanValue();
    }
    
    /**/
    public boolean compute() throws IOException
    {
        FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(db.phoneFeaturesFileName());
        UnitFileReader unitFile = new UnitFileReader( db.phoneUnitFileName() );
        TimelineReader waveTimeline = new TimelineReader( db.waveTimelineFileName() );

        PrintWriter toFeaturesFile = new PrintWriter(new FileOutputStream(durationFeaturesFile));
        System.out.println("Duration CART trainer: exporting duration features");

        FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
        int nUnits = 0;
        for (int i=0, len=unitFile.getNumberOfUnits(); i<len; i++) {
            // We estimate that feature extraction takes 1/10 of the total time
            // (that's probably wrong, but never mind)
            percent = 10*i/len;
            Unit u = unitFile.getUnit(i);
            float dur = u.getDuration() / (float) unitFile.getSampleRate();
            if (dur >= 0.01) { // enforce a minimum duration for training data
                toFeaturesFile.println(dur + " " + featureDefinition.toFeatureString(featureFile.getFeatureVector(i)));
                nUnits++;
            }
        }
        if (useStepwiseTraining) percent = 1;
        else percent = 10;
        toFeaturesFile.close();
        System.out.println("Duration features extracted for "+nUnits+" units");
        
        PrintWriter toDesc = new PrintWriter(new FileOutputStream(wagonDescFile));
        generateFeatureDescriptionForWagon(featureDefinition, toDesc);
        toDesc.close();
        
        boolean ok = false;
        // Now, call wagon
        WagonCaller wagonCaller = new WagonCaller(null);
        File wagonTreeFile = new File(durationDir, "dur.tree");
        if (useStepwiseTraining) {
            // Split the data set in training and test part:
            Process traintest = Runtime.getRuntime().exec("/project/mary/Festival/festvox/src/general/traintest "+durationFeaturesFile.getAbsolutePath());
             try {
                traintest.waitFor();
             } catch (InterruptedException ie) {}
             ok = wagonCaller.callWagon("-data "+durationFeaturesFile.getAbsolutePath()+".train"
                     +" -test "+durationFeaturesFile.getAbsolutePath()+".test -stepwise"
                     +" -desc "+wagonDescFile.getAbsolutePath()
                     +" -stop 10 "
                     +" -output "+wagonTreeFile.getAbsolutePath());
        } else {
            ok = wagonCaller.callWagon("-data "+durationFeaturesFile.getAbsolutePath()
                    +" -desc "+wagonDescFile.getAbsolutePath()
                    +" -stop 10 "
                    +" -output "+wagonTreeFile.getAbsolutePath());            
        }
        percent = 100;
        return ok;

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
        return percent;
    }


    public static void main(String[] args) throws IOException
    {
        DatabaseLayout db = DatabaseImportMain.getDatabaseLayout();
        BasenameList bnl = DatabaseImportMain.getBasenameList(db);
        new DurationCARTTrainer(db, bnl).compute();
    }


}
