package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.*;
import java.util.*;
import java.util.regex.*;

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

public class LabelPauseDeleter extends VoiceImportComponent {
        
        private DatabaseLayout db;        
        private File ehmm;
        private String outputDir;
        protected String labExt = ".lab";
        
        private int progress;
        private String locale;
        
        public final String EDIR = "LabelPauseDeleter.eDir";
        public final String OUTLABDIR = "LabelPauseDeleter.outputLabDir";
        public final String PAUSETHR = "LabelPauseDeleter.pauseDurationThreshold";
        
        public final String getName(){
            return "LabelPauseDeleter";
        }
        
       public SortedMap getDefaultProps(DatabaseLayout db){
           this.db = db;
           if (props == null){
               props = new TreeMap();
               String ehmmdir = System.getProperty("EHMMDIR");
               if ( ehmmdir == null ) {
                   ehmmdir = "/project/mary/Festival/festvox/src/ehmm/";
               }
               
               props.put(EDIR,db.getProp(db.ROOTDIR)
                            +"ehmm"
                            +System.getProperty("file.separator"));
               props.put(OUTLABDIR, db.getProp(db.ROOTDIR)
                       +"lab"
                       +System.getProperty("file.separator"));
               props.put(PAUSETHR, "100");
               
           }
           return props;
       }
       
       protected void setupHelp(){
           props2Help = new TreeMap();
           props2Help.put(EDIR,"directory containing all files used for training and labeling.");
           props2Help.put(OUTLABDIR, "Directory to store generated lebels from EHMM.");
           props2Help.put(PAUSETHR, "Threshold for deleting pauses from label files");
        }
        
        public void initialiseComp()
        {
           locale = db.getProp(db.LOCALE);
        }
        
        /**
         * Do the computations required by this component.
         * 
         * @return true on success, false on failure
         */
        public boolean compute() throws Exception{
            
                                    
            ehmm = new File(getProp(EDIR));
            System.out.println("Copying label files into lab directory ...");
            getProperLabelFormat();
            System.out.println(" ... done.");
            
            return true;
        }
        
         
        /**
         * Post processing Step to convert Label files
         * to MARY supportable format
         * @throws Exception
         */        
        private void getProperLabelFormat() throws Exception {
            
            
            for (int i=0; i<bnl.getLength(); i++) {
                progress = 100*i/bnl.getLength();
                convertSingleLabelFile(bnl.getName(i));               
                System.out.println( "    " + bnl.getName(i) );
                
            }
        }
        
       
        
        /**
         * Post Processing single Label file
         * @param basename
         * @throws Exception
         */
        private void convertSingleLabelFile(String basename) throws Exception {
            
            String line;
            String previous, current;
            String regexp = "\\spau|\\sssil";

            //Compile regular expression
            Pattern pattern = Pattern.compile(regexp);

            File labDir = new File(getProp(OUTLABDIR));
            if(!labDir.exists()){
                labDir.mkdir();
            }
            
            PrintWriter labelOut = new PrintWriter(
                    new FileOutputStream (new File(labDir+"/"+basename+labExt)));
            
            ArrayList<String> labelList = new ArrayList<String>(); 
            
            BufferedReader labelIn = new BufferedReader(
                    new InputStreamReader(new FileInputStream(getProp(EDIR)+"/lab/"+basename+labExt)));
            
            previous = labelIn.readLine();
                                  
            while((line = labelIn.readLine()) != null){

                //Replace all occurrences of pattern in input
                Matcher matcher = pattern.matcher(line);
                current = matcher.replaceAll(" _");
                
                if(previous.endsWith("_") && current.endsWith("_")){
                    previous = current;
                    continue;
                }
                labelList.add(previous);                             
                previous = current;
                
            }
            
            double startTimeStamp = 0.0;
            double endTimeStamp = 0.0;
            boolean correct = true;
            String labelUnit = null;
            
            labelList.add(previous);
            
         int n = labelList.size();
            
//            for(int i = 0; i < n ; i++)
//              System.out.println( labelList.get( i ) );
            
         for(int i = 0; i < n ; i++) { 
            line = labelList.get( i );
            
            if(line.equals("#")){
                labelOut.println(line);
                continue;
            }
            
            if (line != null){
                List labelUnitData = getLabelUnitData(line);
                labelUnit = (String)labelUnitData.get(2);
                endTimeStamp = Double.parseDouble((String)labelUnitData.get(0)); 
            }
            double phoneDuration =  endTimeStamp - startTimeStamp;
            
            if(labelUnit.equals("_")){
                if(isRealPause(phoneDuration)){
                    labelOut.println(line);
                }
                
            }
            else {
                labelOut.println(line);
            }
            
            startTimeStamp = endTimeStamp;
            
          }
            
           labelOut.flush();
           labelOut.close();
           labelIn.close();
           labelList.clear();
                        
        }
      
      private boolean isRealPause(double phoneDuration){
          /*TODO:
           * Here we need to modify thresholds
           */
          
          double threshold = Double.parseDouble(getProp(PAUSETHR)) ;
          if(phoneDuration > (threshold / (double) 1000.0))
          return true;
          else return false;
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
         * Provide the progress of computation, in percent, or -1 if
         * that feature is not implemented.
         * @return -1 if not implemented, or an integer between 0 and 100.
         */
        public int getProgress()
        {
            return progress;
        }

}
