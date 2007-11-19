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
import de.dfki.lt.mary.util.MaryUtils;

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
    private PrintWriter priorityFileWriter;
    private Map fricativeThresholds;
    private ArrayList silenceEnergyList;
    private double sileceThreshold;
    private TreeMap allProblems;
    private TreeMap priorityProblems;
    
    public final String FEATUREDIR = "QualityControl.featureDir";
    public final String LABELDIR = "QualityControl.labelDir";
    public final String OUTFILE = "QualityControl.outputFile";
    public final String PRIORFILE = "QualityControl.outPriorityFile";
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
           props.put(OUTFILE,db.getProp(db.ROOTDIR)+"QualityControl_Problems.out");
           props.put(PRIORFILE,db.getProp(db.ROOTDIR)+"QualityControl_Priority.out");
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
        props2Help.put(OUTFILE,"Output file which shows suspicious alignments");
        props2Help.put(PRIORFILE,"Output file which shows sorted suspicious aligned basenames according to a priority");
        props2Help.put(MLONGPHN,"if true, Mark Unusually long Phone");
        props2Help.put(MHSILEGY,"if true, Mark Higher Silence Energy");
        props2Help.put(MHFREQEGY,"if true, Mark High-Frequency Energy for a Fricative is very low");   
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
        progress = 0;
        int bnlLengthIn = bnl.getLength();
        System.out.println( "Searching for Suspicious Alignments (Labels) in "+ bnlLengthIn + " utterances...." );
        Map fricativeHash = createHashMaps();
        if(getProp(MHFREQEGY).equals("true")){
            fricativeThresholds = getFricativeThresholds(fricativeHash);
        }
        if(getProp(MHSILEGY).equals("true")){
            sileceThreshold = getSilenceThreshold();
        }

        allProblems = new TreeMap();
        priorityProblems = new TreeMap();
        
        for (int i=0; i<bnl.getLength(); i++) {
            progress = 50 + (50*i/bnl.getLength());
            findSuspiciousAlignments(bnl.getName(i));
        }
        writeProblemstoFile();
        writePrioritytoFile();
        
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
        int cost = 0;

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
        double phoneDuration =  endTimeStamp - startTimeStamp;
        String currentProblem = "";
        if( phoneDuration > 1 && !labelUnit.equals("_") && getProp(MLONGPHN).equals("true")){
            currentProblem = labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tUnusually Long Phone";
            cost = 4;
        }
        else if(isFricative(line,ph_Ctype_idx) && phoneDuration > 0 && getProp(MHFREQEGY).equals("true")){
            boolean isFHEnergy = isFricativeHighEnergy(signal, samplingRate, startTimeStamp, endTimeStamp, labelUnit);
            if(!isFHEnergy){
                currentProblem = labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tFricative High-Frequency Energy is very low";
                cost = 3;
            }
        }
        else if(labelUnit.equals("_") && phoneDuration > 0 && getProp(MHSILEGY).equals("true")){
            boolean isSILHEnergy = isSilenceHighEnergy(signal, samplingRate, startTimeStamp, endTimeStamp);
            if(isSILHEnergy){
                currentProblem = labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tHigherEnergy for a Silence";
                cost = 2;
            }
        }
        else if(isVowel(line,ph_VC_idx) && phoneDuration > 0 && getProp(MUNVOICEDVOWEL).equals("true")){
            boolean isVV = isVowelVoiced(signal, samplingRate, startTimeStamp, endTimeStamp);
            if(!isVV){
                currentProblem = labelUnit+"\t"+startTimeStamp+"\t"+endTimeStamp+"\tUn-Voiced Vowel";
                cost = 0;
            }
        }
        
        if(!"".equals(currentProblem)){
            if(allProblems.containsKey(basename)){
                ArrayList arrList = (ArrayList) allProblems.get(basename);
                arrList.add(currentProblem);
                allProblems.put(basename, arrList);
                }
                else {
                ArrayList arrList = new ArrayList();
                arrList.add(currentProblem);
                allProblems.put(basename, arrList);
                }
            if(priorityProblems.containsKey(basename)){
                Integer problemCost = (Integer) priorityProblems.get(basename);
                problemCost = problemCost + cost;
                priorityProblems.put(basename, problemCost);
                }
                else {
                Integer problemCost = new Integer(cost);
                //problemCost = 0;
                priorityProblems.put(basename, problemCost);
                }
        }
        
        startTimeStamp = endTimeStamp;
     }
        labels.close();
        features.close();
        return;
    }
    
    /**
     * It helps to calculate Thresholds by storing all Energy values in to hash.
     * @return
     * @throws IOException
     * @throws Exception
     */
    private Map createHashMaps() throws IOException, Exception{
        
        Map fricativeHash = new HashMap();
        silenceEnergyList = new  ArrayList();
        
        for (int baseCnt=0; baseCnt<bnl.getLength(); baseCnt++) {
          
          progress = (50*baseCnt/bnl.getLength());
          String baseName = bnl.getName(baseCnt);
          BufferedReader labels;
          BufferedReader features; 
          String wavDir    =  db.getProp(db.WAVDIR);
          String voiceName =  db.getProp(db.VOICENAME);

          AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wavDir+"/"+baseName+".wav"));
          
          if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
              ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
          }
   
          float samplingRate = ais.getFormat().getSampleRate();
          double[] signal = new AudioDoubleDataSource(ais).getAllData();
          
          labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File( getProp(LABELDIR)+ baseName + labExt )), "UTF-8"));
          features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( getProp(FEATUREDIR)+ baseName + featsExt )), "UTF-8"));
          
          String line;
          int  ph_VC_idx = -1; // Vowel-Consonent Index
          int ph_Ctype_idx = -1;
          
          // Skip label file header:
          while ((line = labels.readLine()) != null) {
              if (line.startsWith("#")) break; // line starting with "#" marks end of header
          }
          // Skip features file header:
          for(int lineCount = 0 ;(line = features.readLine()) != null; lineCount++){   
             
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
               if (featureUnit == null) throw new IOException("Incomplete feature file: "+baseName);
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
                 
               if(isFricative(line,ph_Ctype_idx) && getProp(MHFREQEGY).equals("true")){
                   
                   double fricEnergy = getFricativeEnergy(signal, samplingRate, startTimeStamp, endTimeStamp, labelUnit);
                   if(fricativeHash.containsKey(featureUnit)){
                   ArrayList arrList = (ArrayList) fricativeHash.get(featureUnit);
                   arrList.add(new Double(fricEnergy));
                   fricativeHash.put(featureUnit, arrList);
                   }
                   else {
                   ArrayList arrList = new ArrayList();
                   arrList.add(new Double(fricEnergy));
                   fricativeHash.put(featureUnit, arrList);
                   }
               }
               double phoneDuration =  endTimeStamp - startTimeStamp;
               if(labelUnit.equals("_") && phoneDuration > 0 && getProp(MHSILEGY).equals("true")){
                   double silEnergy = getSilenceEnergy(signal, samplingRate, startTimeStamp, endTimeStamp);
                   silenceEnergyList.add(new Double(silEnergy));
               }
            }
              
            features.close();
            labels.close();
         }
       
        return fricativeHash;
     }
    
/**
 * Create a HashMap which contains indivisual fricative Thresholds
 * @param fricativeHash
 * @return HashMap which contains indivisual fricative Thresholds
 */    
private Map getFricativeThresholds(Map fricativeHash){
    
    Map hashThresholds = new HashMap();
    
    for ( Iterator it = fricativeHash.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry e = (Map.Entry) it.next();
        ArrayList arr = (ArrayList) e.getValue();
        double[] arrVal =  listToArray(arr);
        double meanVal = MaryUtils.mean(arrVal);
        double stDev = MaryUtils.stdDev(arrVal);
        Double threshold = (Double) (meanVal - (1.5 * stDev));
        /*if(threshold.doubleValue() < 0)
            threshold = (Double) (meanVal - (1.5 * stDev));*/
        
        hashThresholds.put((String) e.getKey(), (Double) threshold);
    }
    
    return hashThresholds;
}

/**
 * Calculating Silence Energy Threshold Level 
 * @return Silence Threshold
 */
private double getSilenceThreshold(){
    
    double[] arrVal =  listToArray(silenceEnergyList);
    double meanVal = MaryUtils.mean(arrVal);
    double stDev = MaryUtils.stdDev(arrVal);
         
    return (meanVal + (1.5 * stDev));
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
        System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);
        double silenceEnergy = SignalProcUtils.getEnergy(phoneSegment);
        if(silenceEnergy > sileceThreshold ) isSILHEnergy = true;
        
        return isSILHEnergy;
    }
    
    /**
     * Calculate Silence Energy
     * @param signal
     * @param samplingRate
     * @param startTimeStamp
     * @param endTimeStamp
     * @return
     * @throws IOException
     * @throws Exception
     */
    private double getSilenceEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp)
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
        System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);
        return SignalProcUtils.getEnergy(phoneSegment);
    }
    
    /**
     * Writing all suspicious labels to a File
     * @throws IOException
     */
    private void writeProblemstoFile() throws IOException {
        
        outFileWriter = new PrintWriter(new FileWriter(new File(getProp(OUTFILE))));
        
        for (int i=0; i<bnl.getLength(); i++) {
           String baseName = bnl.getName(i);
           if(allProblems.containsKey(baseName)){
               //outFileWriter.println(baseName);
               ArrayList arrList = (ArrayList) allProblems.get(baseName);
               for(Iterator it = arrList.iterator(); it.hasNext() ;){
                   String eachProblem = (String) it.next();
                   outFileWriter.println(baseName+"\t"+eachProblem);
                   
               }
               //outFileWriter.println("");
           }
        }
        outFileWriter.flush();
        outFileWriter.close();
    }
    
    /**
     * Writing all priority problems to a file 
     * @throws IOException
     */
    private void writePrioritytoFile() throws IOException {
        
        priorityFileWriter = new PrintWriter(new FileWriter(new File(getProp(PRIORFILE))));
        
        TreeSet set = new TreeSet(new Comparator() {
            public int compare(Object obj, Object obj1) {
                int vcomp = ((Comparable) ((Map.Entry) obj1).getValue()).compareTo(((Map.Entry) 
                        obj).getValue());
                if (vcomp != 0) return vcomp;
                else return ((Comparable) ((Map.Entry) obj).getKey()).compareTo(((Map.Entry) 
                        obj1).getKey());
            }
        });
        
        set.addAll(priorityProblems.entrySet());
        for (Iterator i = set.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            priorityFileWriter.println(entry.getKey() + "\t" + entry.getValue());
            //System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
        priorityFileWriter.flush();
        priorityFileWriter.close();
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
        
        boolean isFHighEnergy = true;
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
        
        Double cutofEnergy = (Double) fricativeThresholds.get(unitName);
        //System.out.println("High Freq. Energy :  "+ unitName + " Energy : "+ higherFreqEnergy + "-- Threshold : "+ cutofEnergy.doubleValue());
        if(higherFreqEnergy < cutofEnergy.doubleValue() ) isFHighEnergy = false;
        
        return isFHighEnergy;
    }
    
    /**
     * To get Fricative High-Freq Energy
     * @param signal
     * @param samplingRate
     * @param startTimeStamp
     * @param endTimeStamp
     * @param unitName
     * @return Fricative High-Freq Energy
     * @throws IOException
     * @throws Exception
     */
    private double getFricativeEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp, String unitName)
    throws IOException, Exception
    {
        
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
        
        return higherFreqEnergy;
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
     * Double ArrayList to double array conversion
     * @param Double ArrayList
     * @return double array
     */
    private double[] listToArray(ArrayList array){
        
        double[] doubleArray = new double[array.size()];
        Iterator it = array.iterator();
        for(int i=0; it.hasNext(); i++){
            Double tempDouble = (Double) it.next();
            doubleArray[i] = tempDouble.doubleValue();
        }
        return doubleArray;
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
