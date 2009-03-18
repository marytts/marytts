/**   
*           The HMM-Based Speech Synthesis System (HTS)             
*                       HTS Working Group                           
*                                                                   
*                  Department of Computer Science                   
*                  Nagoya Institute of Technology                   
*                               and                                 
*   Interdisciplinary Graduate School of Science and Engineering    
*                  Tokyo Institute of Technology                    
*                                                                   
*                Portions Copyright (c) 2001-2006                       
*                       All Rights Reserved.
*                         
*              Portions Copyright 2000-2007 DFKI GmbH.
*                      All Rights Reserved.                  
*                                                                   
*  Permission is hereby granted, free of charge, to use and         
*  distribute this software and its documentation without           
*  restriction, including without limitation the rights to use,     
*  copy, modify, merge, publish, distribute, sublicense, and/or     
*  sell copies of this work, and to permit persons to whom this     
*  work is furnished to do so, subject to the following conditions: 
*                                                                   
*    1. The source code must retain the above copyright notice,     
*       this list of conditions and the following disclaimer.       
*                                                                   
*    2. Any modifications to the source code must be clearly        
*       marked as such.                                             
*                                                                   
*    3. Redistributions in binary form must reproduce the above     
*       copyright notice, this list of conditions and the           
*       following disclaimer in the documentation and/or other      
*       materials provided with the distribution.  Otherwise, one   
*       must contact the HTS working group.                         
*                                                                   
*  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
*  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
*  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
*  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
*  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
*  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
*  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
*  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
*  PERFORMANCE OF THIS SOFTWARE.                                    
*                                                                   
*/
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import marytts.features.FeatureDefinition;
import marytts.htsengine.PhoneTranslator;
import marytts.modules.phonemiser.AllophoneSet;

public class HMMVoiceMakeData extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceMakeData";
    
    /** Tree files and TreeSet object */
    public final String MGC           = name+".makeMGC";
    public final String LF0           = name+".makeLF0";
    public final String MAG           = name+".makeMAG";
    public final String STR           = name+".makeSTR";
    public final String CMPMARY       = name+".makeCMPMARY";
    public final String GVMARY        = name+".makeGV";
    public final String LABELMARY     = name+".makeLABELMARY";
    public final String COUNTMARY     = name+".makeCOUNTMARY";
    public final String QUESTIONSMARY = name+".makeQUESTIONSMARY";
    public final String MLF           = name+".makeMLF";
    public final String LIST          = name+".makeLIST";
    public final String SCP           = name+".makeSCP";
    public final String questionsFile   = name+".questionsFile";
    public final String contextFile     = name+".contextFile";
    public final String featureListFile = name+".featureListFile";
    
    public String getName(){
        return name;
    }
    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap<String,String> getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap<String,String>();
           
           props.put(MGC, "1");
           props.put(LF0, "1");
           props.put(MAG, "1");
           props.put(STR, "1");
           props.put(CMPMARY, "1");
           props.put(GVMARY, "1");
           props.put(LABELMARY, "1");
           props.put(COUNTMARY, "1");
           props.put(QUESTIONSMARY, "1");
           props.put(MLF, "1");
           props.put(LIST, "1");
           props.put(SCP, "1");
           props.put(questionsFile, "data/questions/questions_qst001.hed");
           props.put(contextFile, "data/phonefeatures/cmu_us_arctic_slt_a0001.pfeats");
           props.put(featureListFile, "data/feature_list_en.pl");
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(MGC, "Extracting MGC or MGC-LSP coefficients from raw audio.");
        props2Help.put(LF0, "Extracting log f0 sequence from raw audio.");
        props2Help.put(MAG, "Extracting Fourier magnitudes from raw audio.");
        props2Help.put(STR, "Extracting strengths from 5 filtered bands from raw audio.");
        props2Help.put(CMPMARY, "Composing training data files from mgc, lf0 and str files.");
        props2Help.put(GVMARY, "Calculating GV and saving in Mary format.");
        props2Help.put(LABELMARY, "Extracting monophone and fullcontext labels from phonelab and phonefeature files.");
        props2Help.put(COUNTMARY, "Counting the phone occurrences and extracting the phonesets.");
        props2Help.put(QUESTIONSMARY, "Creating questions .hed file.");
        props2Help.put(MLF, "Generating monophone and fullcontext Master Label Files (MLF).");
        props2Help.put(LIST, "Generating a fullcntext model list occurred in the training data.");
        props2Help.put(SCP, "Generating a trainig data script.");
        props2Help.put(questionsFile, "Name of the file that will contain the questions.");
        props2Help.put(contextFile, "An example of context feature file used for training, this file will be used to extract" +
                " the FeatureDefinition.");
        props2Help.put(featureListFile, "A (perl) file that contains the aditional context features used for training, normally it" +
                " should be in data/ for example data/feature_list_en.pl (language dependent)");

    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{

      
      String cmdLine;
      String filedir = db.getProp(db.ROOTDIR);
        
      if( Integer.parseInt(getProp(MGC)) == 1 ){
        cmdLine = "cd data\nmake mgc\n";
        launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(LF0)) == 1 ){
          cmdLine = "cd data\nmake lf0\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(MAG)) == 1 ){
          cmdLine = "cd data\nmake mag\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(STR)) == 1 ){
          cmdLine = "cd data\nmake str\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(CMPMARY)) == 1 ){
          cmdLine = "cd data\nmake cmp-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(GVMARY)) == 1 ){
          cmdLine = "cd data\nmake gv-mary\n";
          launchBatchProc(cmdLine, "", filedir);
          // also execute HTS gv to avoid problems when running the training script
          cmdLine = "cd data\nmake gv\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(LABELMARY)) == 1 ){
          cmdLine = "cd data\nmake label-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(COUNTMARY)) == 1 ){
          cmdLine = "cd data\nmake count-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(QUESTIONSMARY)) == 1 ){
          // uses: questionsFile
          //       contextFile 
          //       featureListFile
          makeQuestions();          
      }
      if( Integer.parseInt(getProp(MLF)) == 1 ){
          cmdLine = "cd data\nmake mlf\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(LIST)) == 1 ){
          cmdLine = "cd data\nmake list\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(SCP)) == 1 ){
          cmdLine = "cd data\nmake scp\n";
          launchBatchProc(cmdLine, "", filedir);
      }

    
      /* delete the temporary file*/
      File tmpBatch = new File(filedir+"tmp.bat");
      tmpBatch.delete();
 
      return true;
        
    }
    
 
    /**
     * A general process launcher for the various tasks but using an intermediate batch file
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchBatchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        Process proctmp = null;
        BufferedReader procStdout = null;
        String line = null;
        String filedir = db.getProp(db.ROOTDIR);
        String tmpFile = filedir+"tmp.bat";

        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            FileWriter tmp = new FileWriter(tmpFile);
            tmp.write(cmdLine);
            tmp.close();
            
            /* make it executable... */
            proctmp = Runtime.getRuntime().exec( "chmod +x "+tmpFile );
            proctmp.waitFor();
            
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( tmpFile );
            
            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            while( true ) {
                line = procStdout.readLine();
                if ( line == null ) break;
                System.out.println( line );
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if ( proc.exitValue() != 0 ) {
                throw new RuntimeException( task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "]." );
            }
            
            
        }
        catch ( IOException e ) {
            throw new RuntimeException( task + " computation provoked an IOException on file [" + baseName + "].", e );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException( task + " computation interrupted on file [" + baseName + "].", e );
        }
        
    }    

    /***
     * Java version of the makeQuestions script (data/scripts/make_questions.pl)
     * uses: questionsFile
     *       contextFile 
     *       featureListFile
     * @throws Exception
     */
    private void makeQuestions() throws Exception {
        
        // check if questions directory exis, if not create it
        File dirQuestions  = new File("data/questions");
        if( !(dirQuestions.exists()) )
          dirQuestions.mkdir();
                
        FileWriter out = new FileWriter(getProp(questionsFile));
        int i;
        String phon;
        System.out.println("Generating questions file: " + getProp(questionsFile));
                
        // Get feature definition, whatever context feature file used for training can be passed here.       
        Scanner context = new Scanner(new BufferedReader(new FileReader(getProp(contextFile))));
        String strContext="";
        System.out.println("FeatureDefinition extracted from context file example: " + getProp(contextFile));
        while (context.hasNext()) {
          strContext += context.nextLine(); 
          strContext += "\n";
        }
        context.close();
        FeatureDefinition feaDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);
       
        // list of additional context features used for training
        // The features indicated in: data/feature_list.pl (The modes indicated in this list are not used)
        // Since the features are provided by the user, it should be checked that the feature exist
        Set <String> featureList = new HashSet<String>();        
        Scanner feaList = new Scanner(new BufferedReader(new FileReader(getProp(featureListFile))));
        String line;
        System.out.println("The following are other context features used for training, they were extracted from file: " + getProp(featureListFile));
        while (feaList.hasNext()) {
          line = feaList.nextLine();
          if( !line.startsWith("#") && !line.startsWith("%") && !line.contentEquals("") && !line.contains(");") && !line.contains("1;")){
            String elem[] = line.split(",");
            if( !elem[0].contains("#") ){
              elem[0] = elem[0].substring(elem[0].indexOf("\"")+1, elem[0].lastIndexOf("\""));
              // Check if the feature exist
              if(feaDef.hasFeature(elem[0]) ){
                featureList.add(elem[0]);
                System.out.println("  Added to featureList = " + elem[0]);
              }
              else{
                throw new Exception("Error: feature \"" + elem[0] + "\" in feature list file: " + getProp(featureListFile) + " does not exist in FeatureDefinition.");
              }
            }
          }
        }
        feaList.close();
         
        // Get possible values of phonological features, and initialise a set of phonemes
        // that have that value (new HashSet<String>)
        // mary_vc
        String val_vc[]      = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vc"));
        HashMap<String, Set<String>> mary_vc = new HashMap<String, Set<String>>();
        for(i=0; i<val_vc.length; i++)
          mary_vc.put(val_vc[i], new HashSet<String>());   

        // mary_vlng
        String val_vlng[]    = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vlng"));
        HashMap<String, Set<String>> mary_vlng = new HashMap<String, Set<String>>();
        for(i=0; i<val_vlng.length; i++)
          mary_vlng.put(val_vlng[i], new HashSet<String>());          

        // mary_vheight
        String val_vheight[] = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vheight"));
        HashMap<String, Set<String>> mary_vheight = new HashMap<String, Set<String>>();
        for(i=0; i<val_vheight.length; i++)
          mary_vheight.put(val_vheight[i], new HashSet<String>());  

        // mary_vfront      
        String val_vfront[]  = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vfront"));
        HashMap<String, Set<String>> mary_vfront = new HashMap<String, Set<String>>();
        for(i=0; i<val_vfront.length; i++)
          mary_vfront.put(val_vfront[i], new HashSet<String>());  

        // mary_vrnd
        String val_vrnd[]    = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vrnd"));
        HashMap<String, Set<String>> mary_vrnd = new HashMap<String, Set<String>>();
        for(i=0; i<val_vrnd.length; i++)
          mary_vrnd.put(val_vrnd[i], new HashSet<String>());  

        // mary_ctype
        String val_ctype[]   = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_ctype"));
        HashMap<String, Set<String>> mary_ctype = new HashMap<String, Set<String>>();
        for(i=0; i<val_ctype.length; i++)
          mary_ctype.put(val_ctype[i], new HashSet<String>());  
        
        // mary_cplace
        String val_cplace[]  = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_cplace"));
        HashMap<String, Set<String>> mary_cplace = new HashMap<String, Set<String>>();
        for(i=0; i<val_cplace.length; i++)
          mary_cplace.put(val_cplace[i], new HashSet<String>());  
        
        // mary_cvox
        String val_cvox[]    = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_cvox"));
        HashMap<String, Set<String>> mary_cvox = new HashMap<String, Set<String>>();
        for(i=0; i<val_cvox.length; i++)
          mary_cvox.put(val_cvox[i], new HashSet<String>());  
        
        AllophoneSet allophoneSet;
        String phonemeXML = "/project/mary/marcela/openmary/lib/modules/en/us/lexicon/allophones.en_US.xml";
        allophoneSet = AllophoneSet.getAllophoneSet(phonemeXML);
        
        
        String phoneSeq;         
        Set<String> phonesList = allophoneSet.getAllophoneNames();
        // Left-righ phoneme ID context questions
        Iterator<String> it = phonesList.iterator();
        while(it.hasNext()){
            phon = it.next();
            out.write(""); 
            out.write("QS \"prev_prev_phoneme=" + phon + "\"\t{"   + phon + "^*}\n");
            out.write("QS \"prev_phoneme=" + phon    + "\"\t\t{*^" + phon + "-*}\n");
            out.write("QS \"phoneme=" + phon       + "\"\t\t\t{*-" + phon + "+*}\n");
            out.write("QS \"next_phoneme=" + phon    + "\"\t\t{*+" + phon + "=*}\n");
            out.write("QS \"next_next_phoneme=" + phon + "\"\t{*=" + phon + "||*}\n");
            out.write("\n");
            
            // Get the phonological value of each phoneme, and add it to the corresponding
            // set of phonemes that have that value.
            //System.out.println(phon + " vc = " + allophoneSet.getPhoneFeature(phon, "vc"));
            mary_vc.get(allophoneSet.getPhoneFeature(phon, "vc")).add(PhoneTranslator.replaceTrickyPhones(phon));
            
            //System.out.println(phon + " vlng = " + allophoneSet.getPhoneFeature(phon, "vlng"));
            mary_vlng.get(allophoneSet.getPhoneFeature(phon, "vlng")).add(PhoneTranslator.replaceTrickyPhones(phon));
            
            //System.out.println(phon + " vheight = " + allophoneSet.getPhoneFeature(phon, "vheight"));  
            mary_vheight.get(allophoneSet.getPhoneFeature(phon, "vheight")).add(PhoneTranslator.replaceTrickyPhones(phon));
            
            //System.out.println(phon + " vfront = " + allophoneSet.getPhoneFeature(phon, "vfront"));
            mary_vfront.get(allophoneSet.getPhoneFeature(phon, "vfront")).add(PhoneTranslator.replaceTrickyPhones(phon));
            
            //System.out.println(phon + " vrnd = " + allophoneSet.getPhoneFeature(phon, "vrnd"));
            mary_vrnd.get(allophoneSet.getPhoneFeature(phon, "vrnd")).add(PhoneTranslator.replaceTrickyPhones(phon));
            
            //System.out.println(phon + " ctype = " + allophoneSet.getPhoneFeature(phon, "ctype"));
            mary_ctype.get(allophoneSet.getPhoneFeature(phon, "ctype")).add(PhoneTranslator.replaceTrickyPhones(phon));
            
            //System.out.println(phon + " cplace = " + allophoneSet.getPhoneFeature(phon, "cplace"));
            mary_cplace.get(allophoneSet.getPhoneFeature(phon, "cplace")).add(PhoneTranslator.replaceTrickyPhones(phon));
            
            //System.out.println(phon + " cvox = " + allophoneSet.getPhoneFeature(phon, "cvox"));
            mary_cvox.get(allophoneSet.getPhoneFeature(phon, "cvox")).add(PhoneTranslator.replaceTrickyPhones(phon));
             
        }

        // phonological questions
        //String val, prev_prev, prev, ph, next, next_next;
        out.write("\n"); 
        writePhonologicalFeatures("vc", val_vc, mary_vc, out);
        writePhonologicalFeatures("vlng", val_vlng, mary_vlng, out);
        writePhonologicalFeatures("vheight", val_vheight, mary_vheight, out);
        writePhonologicalFeatures("vfront", val_vfront, mary_vfront, out);
        writePhonologicalFeatures("vrnd", val_vrnd, mary_vrnd, out);
        writePhonologicalFeatures("ctype", val_ctype, mary_ctype, out);
        writePhonologicalFeatures("cplace", val_cplace, mary_cplace, out);
        writePhonologicalFeatures("cvox", val_cvox, mary_cvox, out);
       
        // Questions for other features, the additional features used for trainning.
        it = featureList.iterator();
        String fea, mode;
        while (it.hasNext() ){
            fea = it.next();
            String val_fea[] = feaDef.getPossibleValues(feaDef.getFeatureIndex(fea)); 
            for(i=0; i<val_fea.length; i++)
                out.write("QS \"" + fea + "=" + val_fea[i] + "\" \t{*|" + fea + "=" + val_fea[i] + "|*}\n");
            out.write("\n");            
        }
        
        out.close();
    }
    
    public void writePhonologicalFeatures(String fea, String fval[], HashMap<String, Set<String>> mary_fea, FileWriter out)
    throws Exception{
        String val, prev_prev, prev, ph, next, next_next;
        for(int i=0; i<fval.length; i++){    
            prev_prev = "QS \"prev_prev_" + fea + "=" + fval[i] + "\"\t\t{";
            prev      = "QS \"prev_" + fea + "=" + fval[i]      + "\"\t\t{";
            ph        = "QS \"ph_" + fea + "=" + fval[i]        + "\"\t\t\t{";
            next      = "QS \"next_" + fea + "=" + fval[i]      + "\"\t\t{";
            next_next = "QS \"next_next_" + fea + "=" + fval[i] + "\"\t\t{";
            Iterator<String> it = mary_fea.get(fval[i]).iterator();
            while (it.hasNext()){
              val = it.next();  
              prev_prev +=        val + "^*,";  
              prev      += "*^" + val + "-*,";
              ph        += "*-" + val + "+*,";
              next      += "*+" + val + "=*,";
              next_next += "*=" + val + "||*,";
            }
            // remove last unnecessary comma, and close curly brackets
            out.write(prev_prev.substring(0,prev_prev.lastIndexOf(",")) + "}\n");
            out.write(prev.substring(0,prev.lastIndexOf(","))           + "}\n");
            out.write(ph.substring(0,ph.lastIndexOf(","))               + "}\n");
            out.write(next.substring(0,next.lastIndexOf(","))           + "}\n");
            out.write(next_next.substring(0,next_next.lastIndexOf(",")) + "}\n");
            out.write("\n");
        }  
        
    }
    
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }
    
    
}
