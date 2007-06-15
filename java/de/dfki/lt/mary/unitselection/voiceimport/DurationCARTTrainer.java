package de.dfki.lt.mary.unitselection.voiceimport;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

/**
 * A class which converts a text file in festvox format
 * into a one-file-per-utterance format in a given directory.
 * @author schroed
 *
 */
public class DurationCARTTrainer extends VoiceImportComponent
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
    private final String name = "durationCARTTrainer";
    
    public final String LABELDIR = name+".labelDir";
    public final String FEATUREDIR = name+".featureDir";
    public final String DURDIR = name+".durDir";
    public final String DURFEATSFILE = name+".durFeatsFile";
    public final String DURDESCFILE = name+".durDescFile";
    public final String STEPWISETRAINING = name+".stepwiseTraining";
    public final String FEATUREFILE = name+".featureFile";
    public final String UNITFILE = name+".unitFile";
    public final String WAVETIMELINE = name+".waveTimeline";
    public final String DURTREEFILE = name+".durTreeFile";
    
    public String getName(){
        return name;
    }
    
    /**/
     public void initialise( BasenameList setbnl, SortedMap newProps )
    {       
        this.props = newProps;
        this.bnl = setbnl;
        this.unitlabelDir = new File(getProp(LABELDIR));
        this.unitfeatureDir = new File(getProp(FEATUREDIR));
        String rootDir = db.getProp(db.ROOTDIR);
        this.durationDir = new File(getProp(DURDIR));
        if (!durationDir.exists()){
            System.out.print(DURDIR+" "+getProp(DURDIR)
                    +" does not exist; ");
            if (!durationDir.mkdir()){
                throw new Error("Could not create DURDIR");
            }
            System.out.print("Created successfully.\n");
        }         
        this.durationFeaturesFile = new File(getProp(DURFEATSFILE));
        this.wagonDescFile = new File(getProp(DURDESCFILE));
        this.useStepwiseTraining = Boolean.valueOf(getProp(STEPWISETRAINING)).booleanValue();
    }
    
     public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
        if (props == null){
            props = new TreeMap();
            String fileSeparator = System.getProperty("file.separator");
            props.put(FEATUREDIR, db.getProp(db.ROOTDIR)                       
                    +"phonefeatures"
                    +fileSeparator);
            props.put(LABELDIR, db.getProp(db.ROOTDIR)
                    +"phonelab"
                    +fileSeparator);
            props.put(DURDIR,db.getProp(db.TEMPDIR));
            props.put(DURFEATSFILE,getProp(DURDIR)
                    +"dur.feats");
            props.put(DURDESCFILE,getProp(DURDIR)
                    +"dur.desc");
            props.put(STEPWISETRAINING,"false");
            props.put(FEATUREFILE, db.getProp(db.FILEDIR)
                    +"phoneFeatures"+db.getProp(db.MARYEXT));
            props.put(UNITFILE, db.getProp(db.FILEDIR)
                    +"phoneUnits"+db.getProp(db.MARYEXT));
            props.put(WAVETIMELINE, db.getProp(db.FILEDIR)
                    +"timeline_waveforms"+db.getProp(db.MARYEXT));
            props.put(DURTREEFILE, db.getProp(db.FILEDIR)
                    +"dur.tree");
        }
       return props;
    }
    
    /**/
    public boolean compute() throws IOException
    {
        FeatureFileReader featureFile = 
            FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
        UnitFileReader unitFile = new UnitFileReader(getProp(UNITFILE));
        TimelineReader waveTimeline = new TimelineReader(getProp(WAVETIMELINE));

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
        File wagonTreeFile = new File(getProp(DURTREEFILE));
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
        DurationCARTTrainer dct = new DurationCARTTrainer();
         DatabaseLayout db = new DatabaseLayout(dct);
         dct.compute();
    }


}
