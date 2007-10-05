package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.FFTMixedRadix;
import de.dfki.lt.signalproc.filter.BandPassFilter;

/**
 * Quality Control Component for Voice Import Tool to perform 'Sensibility check' on Data. 
 * It identifies some suspicious labels in label files generated from Automatic Labeling
 *  
 * @author Sathish Chandra Pammi
 *
 */

public class QualityControl extends VoiceImportComponent {
   
    private DatabaseLayout db;
    private int progress;
    
    protected String featsExt = ".pfeats";
    protected String labExt = ".lab";
    private PrintWriter outFileWriter;
    
    public final String FEATUREDIR = "QualityControl.featureDir";
    public final String LABELDIR = "QualityControl.labelDir";
    public final String FRICCUTENERGY = "QualityControl.fricativeHighFreqCutofEnegy";
    public final String SILCUTENERGY = "QualityControl.silenceCutofEnergy";
    public final String OUTFILE = "QualityControl.outputFile";
    public final String MLONGPHN = "QualityControl.markUnusuallyLongPhone";
    public final String MHSILEGY = "QualityControl.markHighSILEnergy";
    public final String MHFREQEGY = "QualityControl.markFricativeHighFreqEnergy";
    public final String MUNVOICEDVOWEL = "QualityControl.markUnvoicedVowel";
    
    public final String getName(){
        return "QualityControl";
    }
    
    public void initialiseComp()
    {
        File unitfeatureDir = new File(getProp(FEATUREDIR));
        if (!unitfeatureDir.exists()){
            throw new Error(FEATUREDIR+" "+getProp(FEATUREDIR)
                    +" does not exist; ");
        } 
        File unitlabelDir = new File(getProp(LABELDIR));
        if (!unitlabelDir.exists()){
            throw new Error(LABELDIR+" "+getProp(LABELDIR)
                    +" does not exist; ");
        }
        
    }
    
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(FEATUREDIR, db.getProp(db.ROOTDIR)
                        +"phonefeatures"
                        +System.getProperty("file.separator"));
           props.put(LABELDIR, db.getProp(db.ROOTDIR)
                        +"phonelab"
                        +System.getProperty("file.separator"));
           props.put(FRICCUTENERGY,"0.344");
           props.put(SILCUTENERGY,"0.124");
           props.put(OUTFILE,db.getProp(db.ROOTDIR)+"QualityControl.out");
           props.put(MLONGPHN,"true");
           props.put(MHSILEGY,"true");
           props.put(MHFREQEGY,"true");
           props.put(MUNVOICEDVOWEL,"true");
           
       }
       return props;
    }

    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(FEATUREDIR, "directory containing the phone features.");
        props2Help.put(LABELDIR, "directory containing the phone labels");
        props2Help.put(FRICCUTENERGY, "Higher Frequency Cutof Energy for Fricatives");
        props2Help.put(SILCUTENERGY, "Cutof Energy for Silence");
        props2Help.put(OUTFILE,"Output file which shows suspicious alignments");
        props2Help.put(MLONGPHN,"if true, Mark Un usually long Phone");
        props2Help.put(MHSILEGY,"if true, Mark Higher Silence Energy");
        props2Help.put(MHFREQEGY,"if true, Mark Higher Frequency Energy for a Fricative");   
        props2Help.put(MUNVOICEDVOWEL,"if true, Unvoiced Vowels");
                                
    }
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    
    public boolean compute() throws Exception{
        
        String wavDir    =  db.getProp(db.WAVDIR);
        String voiceName =  db.getProp(db.VOICENAME);
        progress = 1;
        int bnlLengthIn = bnl.getLength();
        System.out.println( "Searching for Suspicious Alignments (Labels) in "+ bnlLengthIn + " utterances...." );
        TreeMap problems = new TreeMap();
        outFileWriter = new PrintWriter(new FileWriter(new File(getProp(OUTFILE))));
        for (int i=0; i<bnl.getLength(); i++) {
            progress = 100*i/bnl.getLength();
     
            findSuspiciousAlignments(bnl.getName(i));
            
        }
        outFileWriter.flush();
        outFileWriter.close();
        
        System.out.println( "Identified Suspicious Alignments (Labels) written into "+ getProp(OUTFILE) + " file." );
        System.out.println( ".... Done."); 
        return true;
    }
    
    
/**
 * Take Each Base File and identifies any suspicious-alignments 
 * @param basename
 * @throws IOException
 * @throws Exception
 */

    private void findSuspiciousAlignments(String basename) throws IOException, Exception  {
             
        BufferedReader labels;
        BufferedReader features; 
        String wavDir    =  db.getProp(db.WAVDIR);
        String voiceName =  db.getProp(db.VOICENAME);

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wavDir+"/"+basename+".wav"));
        
        if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
        }
        float samplingRate = ais.getFormat().getSampleRate();
        double[] signal = new AudioDoubleDataSource(ais).getAllData();
        
        labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File( getProp(LABELDIR)+ basename + labExt )), "UTF-8"));
        features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( getProp(FEATUREDIR)+ basename + featsExt )), "UTF-8"));
        
        String line;
        int  ph_VC_idx = -1; // Vowel-Consonent Index
        int ph_Ctype_idx = -1;
        
        // Skip label file header:
        while ((line = labels.readLine()) != null) {
            if (line.startsWith("#")) break; // line starting with "#" marks end of header
        }
        // Skip features file header:
        for(int lineCount = 0 ;(line = features.readLine()) != null; lineCount++){   
           
            if(line.startsWith("mary_ph_vc")){
                ph_VC_idx = lineCount - 1;                 
            }
            if(line.startsWith("mary_ph_ctype")){
                ph_Ctype_idx = lineCount - 1;                 
            }
            
            if (line.trim().equals("")) break; // empty line marks end of header
        }
        
        boolean correct = true;
        
        double startTimeStamp = 0.0;
        double endTimeStamp = 0.0;
        boolean isFricative = false;
        int unitIndex= 0;
        String labelUnit;
        
        
        while (correct) {
            line = labels.readLine();
            labelUnit = null;
            if (line != null){
                List labelUnitData = getLabelUnitData(line);
                labelUnit = (String)labelUnitData.get(2);
                unitIndex = Integer.parseInt((String)labelUnitData.get(1));
                endTimeStamp = Double.parseDouble((String)labelUnitData.get(0)); 
            }
                  
            line = features.readLine();
            String featureUnit = getFeatureUnit(line);
            if (featureUnit == null) throw new IOException("Incomplete feature file: "+basename);
            // when featureUnit is the empty string, we have found an empty line == end of feature section
            if ("".equals(featureUnit) && labelUnit == null){
                 //we have reached the end in both labels and features
                   break;
               }
            if (!featureUnit.equals(labelUnit)) {
                //label and feature unit do not match
                System.err.println("Non-matching units found: feature file '"
                +featureUnit+"' vs. label file '"+labelUnit
                +"' (Unit "+unitIndex+")");
            }
        // System.out.println(basename +" "+labelUnit+" "+startTimeStamp+" "+endTimeStamp+" ---> Just Printing...");   
        double phoneDuration =  endTimeStamp - startTimeStamp;
        if( phoneDuration > 1 && !labelUnit.equals("_") && getProp(MLONGPHN).equals("true")){
              //System.out.println(basename +" "+labelUnit+" "+startTimeStamp+" "+endTimeStamp+" Unusually Long Phone");
                outFileWriter.println(basename +"\t"+labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tUnusually Long Phone");
                startTimeStamp = endTimeStamp;
                continue;
            }
            
        if(isVowel(line,ph_VC_idx) && phoneDuration > 0 && getProp(MUNVOICEDVOWEL).equals("true")){
            
           boolean isVV = isVowelVoiced(signal, samplingRate, startTimeStamp, endTimeStamp);
           if(!isVV){
               //System.out.println(basename +" "+labelUnit+" "+startTimeStamp+" "+endTimeStamp+" Non-Voiced Vowel");
               outFileWriter.println(basename +"\t"+labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tUn-Voiced Vowel");
               startTimeStamp = endTimeStamp;
               continue;
           }
         }
              
        if(isFricative(line,ph_Ctype_idx) && phoneDuration > 0 && getProp(MHFREQEGY).equals("true")){
                  
           boolean isFHEnergy = isFricativeHighEnergy(signal, samplingRate, startTimeStamp, endTimeStamp, labelUnit);
           if(isFHEnergy){
             //System.out.println(basename +" "+labelUnit+" "+startTimeStamp+" "+endTimeStamp+" Higher Frequency Energy for Fricative");
               outFileWriter.println(basename +"\t"+labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tHigher Frequency Energy for a Fricative");
               startTimeStamp = endTimeStamp;
               continue;
           }
         }
              
        if(labelUnit.equals("_") && phoneDuration > 0 && getProp(MHSILEGY).equals("true")){
                  
           boolean isSILHEnergy = isSilenceHighEnergy(signal, samplingRate, startTimeStamp, endTimeStamp);
           if(isSILHEnergy){
             //System.out.println(basename +" "+labelUnit+" "+startTimeStamp+" "+endTimeStamp+" HigherEnergy for a Silence");
               outFileWriter.println(basename +"\t"+labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tHigherEnergy for a Silence");
               startTimeStamp = endTimeStamp;
               continue;
           }
         }
              
            
        startTimeStamp = endTimeStamp;

       }
        
        return;
    }
    
    
    /**
     * Identifies If Silence has more Energy
     * @param signal
     * @param samplingRate
     * @param startTimeStamp
     * @param endTimeStamp
     * @return true if silence segment more energy, else false
     * @throws IOException
     * @throws Exception
     */
    
    private boolean isSilenceHighEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp)
    throws IOException, Exception
    {
        boolean isSILHEnergy = false;
        float duration = signal.length / samplingRate;
        double phoneDur = endTimeStamp - startTimeStamp;
        int segmentStartIndex = (int)(startTimeStamp * samplingRate);
        int segmentEndIndex = (int)(endTimeStamp * samplingRate);
        
        if(segmentEndIndex > signal.length){
            segmentEndIndex = signal.length;
        }
        int segmentSize = segmentEndIndex - segmentStartIndex;

        
        double[] phoneSegment = new double[segmentSize]; 
        // System.out.println(segmentStartIndex + " "+ segmentEndIndex + " "+ segmentSize +" "+signal.length);
        
        System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);
        
        
        double silenceEnergy = SignalProcUtils.getEnergy(phoneSegment);
        
        double cutofEnergy =  Double.parseDouble(getProp(SILCUTENERGY)); 
        
        //System.out.println(basename +" : Silence Energy :  "+ silenceEnergy);
        //System.out.println(silenceEnergy);
        
        if(silenceEnergy > cutofEnergy ) isSILHEnergy = true;
        
        return isSILHEnergy;
    }
    
    
    /**
     * Identifies if Fricative has more higher frequency Energy
     * @param signal
     * @param samplingRate
     * @param startTimeStamp
     * @param endTimeStamp
     * @return true if the segment more energy in higher freq. region, else false
     * @throws IOException
     * @throws Exception
     */
    
    private boolean isFricativeHighEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp, String unitName)
    throws IOException, Exception
    {
        
        boolean isFHighEnergy = false;
        float duration = signal.length / samplingRate;
        double phoneDur = endTimeStamp - startTimeStamp;
        int segmentStartIndex = (int)(startTimeStamp * samplingRate);
        int segmentEndIndex = (int)(endTimeStamp * samplingRate);
        
        if(segmentEndIndex > signal.length){
            segmentEndIndex = signal.length;
        }
        
        int segmentSize = segmentEndIndex - segmentStartIndex;
        
        double[] phoneSegment = new double[segmentSize]; 
        
        System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);
        
        BandPassFilter filter = new BandPassFilter(0.25, 0.49);
        double [] highFreqSamples = filter.apply(phoneSegment);
        
        double higherFreqEnergy = SignalProcUtils.getEnergy(highFreqSamples);
                       
        //System.out.println(basename +" : High Freq. Energy :  "+ phoneUnit + " Energy : "+ higherFreqEnergy);
        //System.out.println(unitName+" "+higherFreqEnergy);
        
        double cutofEnergy =  Double.parseDouble(getProp(FRICCUTENERGY));
        if(higherFreqEnergy > cutofEnergy ) isFHighEnergy = true;
        
        return isFHighEnergy;
    }
    
    
     /**
      * Identifies The Given Segment is Voiced or Non-Voiced 
      * @param signal
      * @param samplingRate
      * @param startTimeStamp
      * @param endTimeStamp
      * @return true if the segment is Voiced, else false
      * @throws IOException
      * @throws Exception
      */
    
    private boolean isVowelVoiced(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp)
    throws IOException, Exception
    {
        
        boolean isVoiced = true;
        float duration = signal.length / samplingRate;
        double phoneDur = endTimeStamp - startTimeStamp;
        int segmentStartIndex = (int)(startTimeStamp * samplingRate);
        int segmentEndIndex = (int)(endTimeStamp * samplingRate);
        
        if(segmentEndIndex > signal.length){
            segmentEndIndex = signal.length;
        }
        
        int segmentSize = segmentEndIndex - segmentStartIndex;
        
        double[] phoneSegment = new double[segmentSize]; 
        
        System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);
     
        isVoiced = SignalProcUtils.getVoicing(phoneSegment, (int)samplingRate);
         
        return isVoiced;
    }
    
    
    /**
     * To get Label Unit DATA (time stamp, index, phone unit)
     * @param line
     * @return ArrayList contains time stamp, index and phone unit
     * @throws IOException
     */
    private ArrayList getLabelUnitData(String line) throws IOException
    {
        if (line == null) return null;
        ArrayList unitData = new ArrayList();
        StringTokenizer st = new StringTokenizer(line.trim());
        //the first token is the time
        unitData.add(st.nextToken()); 
        //the second token is the unit index
        unitData.add(st.nextToken());
        //the third token is the phoneme
        unitData.add(st.nextToken());
        return unitData;
    }
    
    
    /**
     * To get Phone Unit from Feature Vector
     * @param line
     * @return String phone unit
     * @throws IOException
     */
    
    private String getFeatureUnit(String line) throws IOException
    {
        if (line == null) return null;
        if (line.trim().equals("")) return ""; // empty line -- signal end of section
        StringTokenizer st = new StringTokenizer(line.trim());
        // The expect that the first token in each line is the label
        String unit = st.nextToken();
        return unit;
        
    }
    
    
    /**
     * To know phone unit in Feature Vector is Vowel or not
     * @param line
     * @param ph_VC_idx
     * @return true if phone unit in Feature Vector is Vowel, else false
     */
    private boolean isVowel(String line, int ph_VC_idx){
        
        boolean isVowel = false;
        String[] feats = line.split(" ");
        
        if(feats[ph_VC_idx].equals("+")){
            isVowel = true;
             
        }
        return isVowel;
    }
    
    
    /**
     * To know phone unit in Feature Vector is Fricative or not
     * @param line
     * @param ph_Ctype_idx
     * @return true if phone unit in Feature Vector is Fricative, else false
     */
    private boolean isFricative(String line, int ph_Ctype_idx){
        
        boolean isFricative = false;
        String[] feats = line.split(" ");
        
        if(feats[ph_Ctype_idx].equals("f")){
            isFricative = true;
          }
        return isFricative;
        
    }

    
    public int getProgress()
    {
        return progress;
    }
    
}
