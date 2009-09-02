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
import java.io.FileInputStream;
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
import marytts.features.FeatureVector;
import marytts.htsengine.PhoneTranslator;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.string.*;

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
    public final String QUESTIONSMARY = name+".makeQUESTIONSMARY";
    public final String LIST          = name+".makeLIST";
    public final String SCP           = name+".makeSCP";
    public final String questionsFile    = name+".questionsFile";
    public final String contextFile      = name+".contextFile";
    public final String allophonesFile   = name+".allophonesFile";
    public final String featureListFile  = name+".featureListFile";
    public final String trickyPhonesFile = name+".trickyPhonesFile";
    
    
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
           props.put(QUESTIONSMARY, "1");
           props.put(LIST, "1");
           props.put(SCP, "1");
           props.put(questionsFile, "data/questions/questions_qst001.hed");
           props.put(contextFile, "phonefeatures/cmu_us_arctic_slt_a0001.pfeats");           
           props.put(allophonesFile, "/project/mary/marcela/openmary/lib/modules/en/us/lexicon/allophones.en_US.xml");
           props.put(featureListFile, "mary/featuresHmmVoice.txt");
           props.put(trickyPhonesFile, "mary/trickyPhones.txt");
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
        props2Help.put(QUESTIONSMARY, "Creating questions .hed file.");
        props2Help.put(LIST, "Generating a fullcontext model list occurred in the training data.");
        props2Help.put(SCP, "Generating a trainig data script.");
        props2Help.put(questionsFile, "Name of the file that will contain the questions.");
        props2Help.put(contextFile, "An example of context feature file used for training, this file will be used to extract" +
                " the FeatureDefinition.");
        props2Help.put(allophonesFile, "allophones set (language dependent, an example can be found in ../openmary/lib/modules/language/...)");
        props2Help.put(featureListFile, "A file that contains aditional context features used for training HMMs, normally it" +
        " should be a subset of mary/features.txt --> mary/featuresHmmVoice.txt");
        props2Help.put(trickyPhonesFile, "list of aliases for tricky phones, so HTK-HHEd command can handle them. (This file" +
                " will be created automatically if aliases are necessary, otherwise it will not be created.)");

    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
    
      
      String cmdLine;
      String voiceDir = db.getProp(db.ROOTDIR);
        
      if( Integer.parseInt(getProp(MGC)) == 1 ){
        cmdLine = "cd data\nmake mgc\n";
        launchBatchProc(cmdLine, "", voiceDir);
      }
      if( Integer.parseInt(getProp(LF0)) == 1 ){
          cmdLine = "cd data\nmake lf0\n";
          launchBatchProc(cmdLine, "", voiceDir);
      }
      if( Integer.parseInt(getProp(MAG)) == 1 ){
          cmdLine = "cd data\nmake mag\n";
          launchBatchProc(cmdLine, "", voiceDir);
      }
      if( Integer.parseInt(getProp(STR)) == 1 ){
          cmdLine = "cd data\nmake str\n";
          launchBatchProc(cmdLine, "", voiceDir);
      }
      if( Integer.parseInt(getProp(CMPMARY)) == 1 ){
          cmdLine = "cd data\nmake cmp-mary\n";
          launchBatchProc(cmdLine, "", voiceDir);
      }
      if( Integer.parseInt(getProp(GVMARY)) == 1 ){
          cmdLine = "cd data\nmake gv-mary\n";
          launchBatchProc(cmdLine, "", voiceDir);
          // also execute HTS gv to avoid problems when running the training script
          cmdLine = "cd data\nmake gv\n";
          launchBatchProc(cmdLine, "", voiceDir);
      }
      if( Integer.parseInt(getProp(LABELMARY)) == 1 ){
          //uses:  contextFile (example)
          //       featureListFile
          // make sure that checkTrickyPhones is run before
          makeLabels(voiceDir);   
      }
      if( Integer.parseInt(getProp(QUESTIONSMARY)) == 1 ){
          // uses: questionsFile
          //       contextFile (example)
          //       featureListFile
          // make sure that checkTrickyPhones is run before
          makeQuestions(voiceDir);          
      }
      if( Integer.parseInt(getProp(LIST)) == 1 ){
          cmdLine = "cd data\nmake list\n";
          launchBatchProc(cmdLine, "", voiceDir);
      }
      if( Integer.parseInt(getProp(SCP)) == 1 ){
          cmdLine = "cd data\nmake scp\n";
          launchBatchProc(cmdLine, "", voiceDir);
      }

    
      /* delete the temporary file*/
      File tmpBatch = new File(voiceDir+"tmp.bat");
      tmpBatch.delete();
 
      return true;
        
    }
    
 
     /*** This function checks if replacements or aliases for tricky phones are necessary (so HTK-HHEd can
     * handle the phone names), if so it will create a trickyFile containing the replacements. 
     * This file should be used afterwards to create the PhoneTranslator object used in makeLabels, makeQuestions and 
     * JoinModelller. Also it is necessary when loading the HTS trees to replace back the tricky phones.
     * If a trickyFile is created when training a voice, the tricky file name will be included in the configuration
     * file of the voice. 
     * CHECK not sure how/where to keep this file for the JoinModeller?
     * @param phoneXML allophonesFile for the voice or language (full path).
     * @param trickyFile name of the file where the tricky phone replacements are saved (full path).
     * @return true if trickyPhones.txt file is created, false otherwise.
     */
    public static boolean checkTrickyPhones(String phoneXML, String trickyFile){
        
        boolean trickyPhones = false;
        try {
            
            AllophoneSet allophoneSet;
          
            System.out.println("Reading allophones set from file: " + phoneXML);
            allophoneSet = AllophoneSet.getAllophoneSet(phoneXML);
            String lang = allophoneSet.getLocale().getLanguage();
            
            
            System.out.println("Checking if there are tricky phones (problematic phone names):");
            FileWriter outputStream=null;
            String phonOri;         
            Set<String> phonesList = allophoneSet.getAllophoneNames();
            // Left-righ phone ID context questions
            Iterator<String> it = phonesList.iterator();
            int numReplacements=0;
            while(it.hasNext()){
                phonOri = it.next();
                System.out.println("  phon=" + phonOri);
                for(int i=0; i<phonOri.length(); i++){
                   if(! (marytts.util.string.StringUtils.isLetterOrModifier(phonOri.codePointAt(i))) ){
                       if(numReplacements == 0 ){
                           // just if there is replacements to make then create the trickyPhones.txt file
                           outputStream = new FileWriter(trickyFile);
                           trickyPhones = true;
                       } 
                       numReplacements++;
                       System.out.println("     replace --> " + lang + numReplacements);
                       if(outputStream != null)
                         outputStream.write(phonOri + " " + lang + numReplacements + "\n");
                       break;
                   } 
                }
                
            }
            if(outputStream != null){
               outputStream.close();
               System.out.println("Created tricky phones file: " + trickyFile);
            }
            } catch (Exception e) {
                System.out.println(e.getMessage()); 
            }
                    
       return trickyPhones; 
    }
    
    
    /***
     * Java version of the makeQuestions script (data/scripts/make_questions.pl)
     * uses: questionsFile
     *       contextFile 
     *       featureListFile
     * @throws Exception
     */
    private void makeQuestions(String voiceDir) throws Exception {
        
        String hmmFeatureListFile = voiceDir + getProp(featureListFile);
        FileWriter out = new FileWriter(voiceDir + getProp(questionsFile));
        int i;
        String phon;
        
        // Check if there are tricky phones
        PhoneTranslator phTranslator;  
        if( checkTrickyPhones(getProp(allophonesFile), voiceDir+getProp(trickyPhonesFile)) )        
           phTranslator = new PhoneTranslator(voiceDir + getProp(trickyPhonesFile));
        else
           phTranslator = new PhoneTranslator(""); 
        
        System.out.println("Generating questions file: " + voiceDir + getProp(questionsFile));
                
        // Get feature definition, whatever context feature file used for training can be passed here.       
        Scanner context = new Scanner(new BufferedReader(new FileReader(voiceDir + getProp(contextFile))));
        String strContext="";
        System.out.println("FeatureDefinition extracted from context file example: " + voiceDir + getProp(contextFile));
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
        Scanner feaList = new Scanner(new BufferedReader(new FileReader(hmmFeatureListFile)));
        String line;
        System.out.println("The following are other context features used for training HMMs, they are extracted from file: " + hmmFeatureListFile);
        while (feaList.hasNext()) {
          line = feaList.nextLine();
              // Check if the feature exist
              if(feaDef.hasFeature(line) ){
                featureList.add(line);
                System.out.println("  Added to featureList = " + line);
              }
              else{
                throw new Exception("Error: feature \"" + line + "\" in feature list file: " + hmmFeatureListFile + " does not exist in FeatureDefinition.");
              }
        }
        feaList.close();
         
        // Get possible values of phonological features, and initialise a set of phones
        // that have that value (new HashSet<String>)
        // mary_vc
        String val_vc[]      = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vc"));
        HashMap<String, Set<String>> mary_vc = new HashMap<String, Set<String>>();
        for(i=0; i<val_vc.length; i++)
          mary_vc.put(val_vc[i], new HashSet<String>());   

        // mary_vlng
        /*
        String val_vlng[]    = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vlng"));
        HashMap<String, Set<String>> mary_vlng = new HashMap<String, Set<String>>();
        for(i=0; i<val_vlng.length; i++)
          mary_vlng.put(val_vlng[i], new HashSet<String>());
          */          

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
        //String phoneXML = "/project/mary/marcela/openmary/lib/modules/en/us/lexicon/allophones.en_US.xml";
        String phoneXML = getProp(allophonesFile);
        System.out.println("Reading allophones set from file: " + phoneXML);
        allophoneSet = AllophoneSet.getAllophoneSet(phoneXML);
        
        
        String phoneSeq, phonOri;         
        Set<String> phonesList = allophoneSet.getAllophoneNames();
        // Left-righ phone ID context questions
        Iterator<String> it = phonesList.iterator();
        while(it.hasNext()){
            phonOri = it.next();
            phon = phTranslator.replaceTrickyPhones(phonOri);
            out.write(""); 
            out.write("QS \"prev_prev_phone=" + phon + "\"\t{"   + phon + "^*}\n");
            out.write("QS \"prev_phone=" + phon    + "\"\t\t{*^" + phon + "-*}\n");
            out.write("QS \"phone=" + phon       + "\"\t\t\t{*-" + phon + "+*}\n");
            out.write("QS \"next_phone=" + phon    + "\"\t\t{*+" + phon + "=*}\n");
            out.write("QS \"next_next_phone=" + phon + "\"\t{*=" + phon + "||*}\n");
            out.write("\n");
            
            // Get the phonological value of each phone, and add it to the corresponding
            // set of phones that have that value.
            //System.out.println(phon + " vc = " + allophoneSet.getPhoneFeature(phonOri, "vc"));
            mary_vc.get(allophoneSet.getPhoneFeature(phonOri, "vc")).add(phon);
            
            //System.out.println(phon + " vlng = " + allophoneSet.getPhoneFeature(phonOri, "vlng"));
           // mary_vlng.get(allophoneSet.getPhoneFeature(phonOri, "vlng")).add(phon);
            
            //System.out.println(phon + " vheight = " + allophoneSet.getPhoneFeature(phonOri, "vheight"));  
            mary_vheight.get(allophoneSet.getPhoneFeature(phonOri, "vheight")).add(phon);
            
            //System.out.println(phon + " vfront = " + allophoneSet.getPhoneFeature(phonOri, "vfront"));
            mary_vfront.get(allophoneSet.getPhoneFeature(phonOri, "vfront")).add(phon);
            
            //System.out.println(phon + " vrnd = " + allophoneSet.getPhoneFeature(phonOri, "vrnd"));
            mary_vrnd.get(allophoneSet.getPhoneFeature(phonOri, "vrnd")).add(phon);
            
            //System.out.println(phon + " ctype = " + allophoneSet.getPhoneFeature(phonOri, "ctype"));
            mary_ctype.get(allophoneSet.getPhoneFeature(phonOri, "ctype")).add(phon);
            
            //System.out.println(phon + " cplace = " + allophoneSet.getPhoneFeature(phonOri, "cplace"));
            mary_cplace.get(allophoneSet.getPhoneFeature(phonOri, "cplace")).add(phon);
            
            //System.out.println(phon + " cvox = " + allophoneSet.getPhoneFeature(phonOri, "cvox"));
            mary_cvox.get(allophoneSet.getPhoneFeature(phonOri, "cvox")).add(phon);
             
        }

        // phonological questions
        //String val, prev_prev, prev, ph, next, next_next;
        out.write("\n"); 
        writePhonologicalFeatures("vc", val_vc, mary_vc, out);
       // writePhonologicalFeatures("vlng", val_vlng, mary_vlng, out);
        writePhonologicalFeatures("vheight", val_vheight, mary_vheight, out);
        writePhonologicalFeatures("vfront", val_vfront, mary_vfront, out);
        writePhonologicalFeatures("vrnd", val_vrnd, mary_vrnd, out);
        writePhonologicalFeatures("ctype", val_ctype, mary_ctype, out);
        writePhonologicalFeatures("cplace", val_cplace, mary_cplace, out);
        writePhonologicalFeatures("cvox", val_cvox, mary_cvox, out);
       
        // Questions for other features, the additional features used for trainning.
        it = featureList.iterator();
        String fea;
        while (it.hasNext() ){
            fea = it.next();
            String val_fea[] = feaDef.getPossibleValues(feaDef.getFeatureIndex(fea));
            // write the feature value as string
            for(i=0; i<val_fea.length; i++){
              if(fea.contains("sentence_punc") || fea.contains("prev_punctuation") || fea.contains("next_punctuation"))
                  out.write("QS \"" + fea + "=" + phTranslator.replacePunc(val_fea[i]) + 
                         "\" \t{*|" + fea + "=" + phTranslator.replacePunc(val_fea[i]) + "|*}\n");
              else if(fea.contains("tobi_"))
                  out.write("QS \"" + fea + "=" + phTranslator.replaceToBI(val_fea[i]) + 
                         "\" \t{*|" + fea + "=" + phTranslator.replaceToBI(val_fea[i]) + "|*}\n");  
              else
                  out.write("QS \"" + fea + "=" + val_fea[i] + "\" \t{*|" + fea + "=" + val_fea[i] + "|*}\n");
            }            
            out.write("\n");            
        }
        
        out.close();
        System.out.println("Created question file: " + voiceDir + getProp(questionsFile) + "\n");
    }
    
    
    private void writePhonologicalFeatures(String fea, String fval[], HashMap<String, Set<String>> mary_fea, FileWriter out)
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
    
    
    /***
     * Java version of the make labels script (data/scripts/make_labels.pl)
     * uses: 
     * @throws Exception
     */
    private void makeLabels(String voiceDir) throws Exception {
        
        String hmmFeatureListFile = voiceDir + getProp(featureListFile);
        File dirFea = new File(voiceDir + "/phonefeatures");
        File dirLab = new File(voiceDir + "/phonelab");
        
        String[] feaFiles;
        if(dirFea.exists() && dirFea.list().length > 0 && dirLab.exists() && dirLab.list().length > 0 ){ 
          feaFiles = dirFea.list();
        } else {            
            throw new Exception("Error: directories " + voiceDir + "phonefeatures and/or " + voiceDir + "phonelab do not contain files." );  
        }
        
        // Check if there are tricky phones
        PhoneTranslator phTranslator;  
        if( checkTrickyPhones(getProp(allophonesFile), voiceDir+getProp(trickyPhonesFile)) )        
           phTranslator = new PhoneTranslator(voiceDir + getProp(trickyPhonesFile));
        else
           phTranslator = new PhoneTranslator(""); 
                
        // Get feature definition, whatever context feature file used for training can be passed here.
        // here we take the first in the feaFiles list.
        Scanner context = new Scanner(new BufferedReader(new FileReader(voiceDir + "/phonefeatures/" + feaFiles[0])));
        String strContext="";
        System.out.println("FeatureDefinition extracted from context file: " + voiceDir + "/phonefeatures/" + feaFiles[0]);
        while (context.hasNext()) {
          strContext += context.nextLine(); 
          strContext += "\n";
        }
        context.close();
        FeatureDefinition feaDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);
        
        // list of context features used for creating HTS context features --> features used for training HMMs
        // Since the features are provided by the user, it should be checked that the features exist
        Set <String> hmmFeatureList = new HashSet<String>();        
        Scanner feaList = new Scanner(new BufferedReader(new FileReader(hmmFeatureListFile)));
        String fea;
        System.out.println("The following are other context features used for training Hmms: ");
        while (feaList.hasNext()) {
          fea = feaList.nextLine();
          // Check if the feature exist
          if( feaDef.hasFeature(fea)){
            hmmFeatureList.add(fea);
            System.out.println("  " + fea);
          }
          else
            throw new Exception("Error: feature \"" + fea + "\" in feature list file: " + hmmFeatureListFile + " does not exist in FeatureDefinition.");
        }
        feaList.close();
        System.out.println("The previous context features were extracted from file: " + hmmFeatureListFile);
        
        
        // Process all the files in phonefeatures and phonelab and create the directories:
        // data/labels/full
        // data/labels/mono
        // data/labels/gen  (contain some examples from full, here we copy 10 examples)
        // Create also the HTK master label files: full.mlf and mono.mlf
        File labelsDir = new File(voiceDir + "/data/labels");
        if(!labelsDir.exists())  
            labelsDir.mkdir();
        File monoDir = new File(voiceDir + "/data/labels/mono");
        if(!monoDir.exists()){
            System.out.println("\nCreating a /data/labels/mono directory");  
            monoDir.mkdir();
        }        
        File fullDir = new File(voiceDir + "/data/labels/full");
        if(!fullDir.exists()){
            System.out.println("\nCreating a /data/labels/full directory");  
            fullDir.mkdir();
        }        
        File genDir = new File(voiceDir + "/data/labels/gen");
        if(!genDir.exists()){
          System.out.println("\nCreating a /data/labels/gen directory, copying some HTS-HTK full context examples for testing");  
          genDir.mkdir();
        }
        
        // Process all the files in phonefeatures and phonelab and create HTS-HTK full context feature files and label files.
        String basename;
        for (int i=0; (i<feaFiles.length); i++) {
          basename = StringUtils.getFileName(feaFiles[i]);      
          System.out.println("Extracting monophone and context features (" + (i+1) + "): " + feaFiles[i] + " and " + basename + ".lab");
          extractMonophoneAndFullContextLabels( 
                  voiceDir + "/phonefeatures/" + feaFiles[i], 
                  voiceDir + "/phonelab/" + basename + ".lab",
                  voiceDir + "/data/labels/full/" + basename + ".lab",
                  voiceDir + "/data/labels/mono/" + basename + ".lab",
                  feaDef,
                  phTranslator,
                  hmmFeatureList);          
        }
        System.out.println("Processed " + feaFiles.length + " files.");     
        System.out.println("Created directories: \n  " + voiceDir + "data/labels/full/" + "\n  " + voiceDir + "data/labels/mono/");

        // creating Master label files:
        FileWriter fullMlf = new FileWriter(voiceDir + "/data/labels/full.mlf");        
        fullMlf.write("#!MLF!#\n");
        fullMlf.write("\"*/*.lab\" -> \"" + voiceDir + "data/labels/full\"\n");
        fullMlf.close();
        
        FileWriter monoMlf = new FileWriter(voiceDir + "/data/labels/mono.mlf");
        monoMlf.write("#!MLF!#\n");
        monoMlf.write("\"*/*.lab\" -> \"" + voiceDir + "data/labels/mono\"\n");
        monoMlf.close();
        
        System.out.println("Created Master Label Files: \n  " + voiceDir + "data/labels/full.mlf" + "\n  " + voiceDir + "data/labels/mono.mlf");
        
        // Copy 10 files in gen directory to test with htsengine
        System.out.println("Copying 10 context feature files in gen directory for testing with the HTS htsengine.");
        String cmdLine;
        for (int i=0; i<10; i++) {
            basename = StringUtils.getFileName(feaFiles[i]);
            cmdLine = "cp " + voiceDir + "/data/labels/full/" + basename + ".lab " + voiceDir + "/data/labels/gen/gen_" + basename + ".lab";
            launchProc(cmdLine, "file copy", voiceDir);
        }
        
    }
    
    
    /***
     * Java version of the make labels script (data/scripts/make_labels.pl)
     * uses: 
     * @throws Exception
     */
    private void extractMonophoneAndFullContextLabels(String feaFileName, String labFileName, 
            String outFeaFileName, String outLabFileName,  
            FeatureDefinition feaDef, PhoneTranslator phTranslator, Set <String> hmmFeatureList ) throws Exception {
      
      FileWriter outFea = new FileWriter(outFeaFileName);
      FileWriter outLab = new FileWriter(outLabFileName);
      
      //Read label file     
      UnitLabel ulab[] = UnitLabel.readLabFile(labFileName);
      //for(int i=0; i<ulab.length; i++){
      //    System.out.println("start=" + ulab[i].getStartTime() + " end=" + ulab[i].getEndTime() + " " + ulab[i].unitName);
      //}
      
      
      // Read context features
      String nextLine;
      FeatureVector fv;
      
      Scanner sFea = null;
      sFea = new Scanner(new BufferedReader(new FileReader(feaFileName)));
      
      /* Skip mary context features definition */
      while (sFea.hasNext()) {
        nextLine = sFea.nextLine(); 
        if (nextLine.trim().equals("")) break;
      }
      /* skip until byte values */
      int numFeaVectors = 0;
      while (sFea.hasNext()) {
        nextLine = sFea.nextLine();        
        if (nextLine.trim().equals("")) break;
        numFeaVectors++;
      }
      
      if(numFeaVectors != ulab.length) 
        throw new Exception("Error: Number of context features in: " + feaFileName + " is not the same as the number of labels in: " + labFileName);
      else {
        /* Parse byte values  */
        int i=0;  
        while (sFea.hasNext()) {
           nextLine = sFea.nextLine();
           fv = feaDef.toFeatureVector(0, nextLine);
           
           //System.out.println("STR: " + nextLine);
           // check if phone name in feature file is the same as in the lab file
           if(ulab[i].unitName.contentEquals(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef))){
              // We need here HTK time units, which are measured in hundreds of nanoseconds.  
              // write in label file HTK-HTS format 
              outLab.write(ulab[i].startTime*1E7 + "  " + ulab[i].endTime*1E7 + " " +
                phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef)) + "\n");
              
              // write in features file HTK-HTS format
              outFea.write(ulab[i].startTime*1E7 + "  " + ulab[i].endTime*1E7 + " " +
                phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("prev_prev_phone"), feaDef)) + "^" +
                phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("prev_phone"), feaDef))      + "-" +
                phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef))           + "+" +
                phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("next_phone"), feaDef))      + "=" +
                phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("next_next_phone"), feaDef)) + "|");         
          
              String hmmfea, hmmfeaVal;
              int hmmfeaValInt;
              Iterator<String> it = hmmFeatureList.iterator();
              while (it.hasNext() ){
                hmmfea = it.next();
                hmmfeaVal = fv.getFeatureAsString(feaDef.getFeatureIndex(hmmfea), feaDef);
                // If the string is longer than 2048 chars then the feature names need to be shorten (phTranslator.shortenPfeat
                // can be used, but then when reading the HMMs the names have to be lengthen back in the HTSCARTReader).
                // if using punctuation features like: sentence_punc, next_punctuation or prev_punctuation or tobi features
                // the values need to be mapped otherwise HTK-HHEd complains.
                if(hmmfea.contains("sentence_punc") || hmmfea.contains("prev_punctuation") || hmmfea.contains("next_punctuation"))
                  outFea.write("|" + hmmfea + "=" + phTranslator.replacePunc(hmmfeaVal));
                else if(hmmfea.contains("tobi_"))
                  outFea.write("|" + hmmfea + "=" + phTranslator.replaceToBI(hmmfeaVal));
                else
                  outFea.write("|" + hmmfea + "=" + hmmfeaVal);
              }
              outFea.write("||\n");    
              i++; // next label
           } else {
             throw new Exception("Phone name mismatch: feature File:" + fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef) 
                                                      + " lab file: " + ulab[i].unitName);
           }           
        }
      }
      outLab.close();
      outFea.close();
      
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


    
    /**
     * A general process launcher for the various tasks
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;
        System.out.println("Running: "+ cmdLine);
        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( cmdLine );
            
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

    
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }
    
    
    public static void main(String[] args)throws Exception {
        
        HMMVoiceMakeData data = new HMMVoiceMakeData();
        String voiceDir = "/project/mary/marcela/HMM-voices/turkish/";
        String featuresHmmVoice = "/project/mary/marcela/HMM-voices/turkish/mary/featuresHmmVoice.txt";
        data.makeLabels(voiceDir);
    }
    
}
