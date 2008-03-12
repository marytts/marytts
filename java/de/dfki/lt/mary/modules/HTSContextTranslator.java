/**
 * Copyright 2000-2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.modules;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.htsengine.HMMVoice;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;
import de.dfki.lt.mary.modules.synthesis.Voice;


/**
 * Translates TARGETFEATURES --> HTSCONTEXT used in HMM synthesis.
 * 
 * Java port of make_labels_full.pl and common_routines.pl developed in Perl
 * by Sacha Krstulovic. 
 * @author Marc Schr&ouml;der, Marcela Charfuelan 
 */
public class HTSContextTranslator extends InternalModule {

    private String contextFeatureFile;
 
    public HTSContextTranslator()
    {
        super("HTSContextTranslator",
              MaryDataType.get("TARGETFEATURES"),
              MaryDataType.get("HTSCONTEXT")
              );
    }
    
    /**
     * This module is actually tested as part of the HMMSynthesizer test,
     * for which reason this method does nothing.
     */
    public synchronized void powerOnSelfTest() throws Error
    {
    }


    /**
     * Translate TARGETFEATURES to HTSCONTEXT
     * @param MaryData type.
     */
    public MaryData process(MaryData d)
    throws Exception
    {
 
        MaryData output = new MaryData(outputType());

        Voice v = d.getDefaultVoice();  /* This is the way of getting a Voice through a MaryData type */
        assert v instanceof HMMVoice : v.getName()+" is not an HMM voice!";
        HMMVoice hmmv = (HMMVoice)v;  
        
        String lab;   
        
        lab = _process(d.getPlainText(), hmmv.getFeatureList());       
        output.setPlainText(lab);
        
        return output;
    }
    
    public void setContextFeatureFile(String str){ contextFeatureFile = str; }

    /**Translate TARGETFEATURES_EN to HTSCONTEXT_EN
     * (I have put this method public so I can use it from MaryClientHMM)
     * @param String d
     * @return String
     * @throws Exception
     */
    public String _process(String d, Vector<String> featureList)
    throws Exception
    {
      Hashtable<String,Integer> maryPfeats = new Hashtable<String,Integer>();
      ArrayList< Vector<String> > currentPfeats = new ArrayList< Vector<String> >();
      int i,j;
      int num_phoneme = 0;
      int num_mary_pfeats = 0;
      
      /* These .._phoneme variables indicate when to convert tricky phonemes */
      int index_mary_phoneme = 0;
      int index_mary_prev_phoneme = 0;
      int index_mary_next_phoneme = 0;
      
      Integer index;
      boolean first_blank_line = false;
      String pfeats, fea_out;
      String lab = "";
      Vector<String> v, n_v, nn_v;
      pfeats = d;

      Scanner s = null;
      String line;
          
      s = new Scanner(pfeats).useDelimiter("\n"); 

      /* Create a hash table with the mary_ pfeats context feature names and possible values. */
      while(s.hasNext()) {
        line = s.next();
        //System.out.println("length=" + line.length() + " line= " + line);
          
        if( line.contains("mary_") ) {
            
          String[] elem = line.split(" ");
          if( elem.length > 1) {        
            maryPfeats.put(elem[0], new Integer(num_mary_pfeats));
            
            if(elem[0].contentEquals("mary_phoneme"))
              index_mary_phoneme = num_mary_pfeats;
            else if(elem[0].contentEquals("mary_prev_phoneme"))
              index_mary_prev_phoneme = num_mary_pfeats;
            else if(elem[0].contentEquals("mary_next_phoneme"))
              index_mary_next_phoneme = num_mary_pfeats;
            
            num_mary_pfeats++;
            
          } 
        }
        /* after "mary_" lines there are two lines more containing: 
           ShortValuedFeatureProcessors
           ContinuousFeatureProcessors  and then an empty line */
        if( line.length()==0 && !first_blank_line) {
            first_blank_line = true;  
            //System.out.println("first blank line length=" + line.length());
        } else if( first_blank_line ) { 
          
          String[] elem = line.split(" ");
          if( elem.length > 1) { 
            currentPfeats.add(new Vector<String>()); 
            v = currentPfeats.get(num_phoneme);
        
            /* create a vector with the elements of this line */
            for(i=0; i<elem.length; i++) {
              //System.out.println("elem " + i + ": " + elem[i]);
              /* i need to convert tricky phonemes */               
              if( i == index_mary_phoneme || 
                  i == index_mary_prev_phoneme || 
                  i == index_mary_next_phoneme) {               
                  v.addElement(replaceTrickyPhones(elem[i]));
              }
              else        
                v.addElement(elem[i]);
            }
            num_phoneme++; 
            
          }else    /* just copy the block between the first and second blank line */
            break;    
        }
    } /* while scanner mary_ pfeats */
      
    if (s != null) 
          s.close();
   
   /* produce output with phonemes in context */
    String pp_phn, p_phn, cur_phn, n_phn, nn_phn;
    v    = currentPfeats.get(0);
    n_v  = currentPfeats.get(1);
    nn_v = currentPfeats.get(2);
      
    pp_phn  = v.elementAt(0);
    p_phn   = v.elementAt(0);
    cur_phn = v.elementAt(0);
    n_phn   = n_v.elementAt(0); 
    nn_phn  = nn_v.elementAt(0);
   
    for(i=0; i<currentPfeats.size(); i++){
      lab += pp_phn + "^" + p_phn + "-" + cur_phn + "+" + n_phn + "=" + nn_phn + "||";  
      //System.out.print(pp_phn + "^" + p_phn + "-" + cur_phn + "+" + n_phn + "=" + nn_phn + "||");
      pp_phn = p_phn;
      p_phn = cur_phn;
      cur_phn = n_phn;
      n_phn = nn_phn;
      if( (i+3) < currentPfeats.size() ) {
        nn_v = currentPfeats.get(i+3);
        nn_phn = nn_v.elementAt(0);
      } else {
          nn_v = currentPfeats.get(currentPfeats.size()-1);
          nn_phn = nn_v.elementAt(0); 
      }
            
      v = currentPfeats.get(i);
      
      for(j=0; j<featureList.size(); j++) {
        fea_out = featureList.elementAt(j);
        /* check if the feature is in maryPfeats list */  
        if( maryPfeats.containsKey(fea_out) ) {
           index = maryPfeats.get(fea_out);
          
          /* now I should look for this index in currentPfeats vector */
          /* maybe i need to check first if the value is allowed ??? in the hash table */
          /* that is the value should exist in mary_v   */
          fea_out = shortenPfeat(fea_out);
           
          lab += fea_out + "=" + v.get(index.intValue()) + "|";
          //System.out.print(fea_out + "=" + v.get(index.intValue()) + "|");
           
        } else {
            logger.debug("HTSContextTranslator: error featureList element " + fea_out + " is not in maryPfeats.");
            throw new Exception("HTSContextTranslator: error featureList element " + fea_out + "  is not in maryPfeats.");
        }
      }
      lab += "\n";
      //System.out.println();
           
    }
    
    //logger.debug("\nLAB:\n" + lab);
       
    return lab;
    
    } /* method _process */

    
    /**Translate TARGETFEATURES_EN to HTSCONTEXT_EN
     * (I have put this method public so I can use it from MaryClientHMM)
     * This implementation is based on the FeatureDefinition class.
     * @param String d text of a TARGETFEATURES data
     * @return String
     * @throws Exception
     */
    public String processTargetFeatures(String d, Vector<String> featureList)
    throws Exception
    {
        FeatureDefinition def = new FeatureDefinition(new BufferedReader(new StringReader(d)), false);

        Scanner lines = new Scanner(d).useDelimiter("\n");
        // skip up to the first empty line
        while (lines.hasNext()) {
            String line = lines.next();
            if (line.trim().equals("")) break;
        }
        // Now each of the following lines is a feature vector, up to the next empty line
        StringBuffer output = new StringBuffer();
        while (lines.hasNext()) {
            String line = lines.next();
            if (line.trim().equals("")) break;
            FeatureVector fv = def.toFeatureVector(0, line);
            String context = features2context(def, fv, featureList);
            output.append(context);
            output.append("\n");
        }
        return output.toString();
    }
    
    
    /**
     * Convert the feature vector into a context model name to be used by HTS/HTK.
     * @param def a feature definition
     * @param featureVector a feature vector which must be consistent with the Feature definition
     * @param featureList a list of features to use in constructing the context model name. If missing, all features in the feature definition are used.
     * @return the string representation of one context name.
     */
    public String features2context(FeatureDefinition def, FeatureVector featureVector, Vector<String> featureList)
    {
        if (featureList == null) {
            featureList = new Vector<String>(Arrays.asList(def.getFeatureNames().split("\\s+")));
        }
        
        // construct quint-phone models:
        int iPhoneme = def.getFeatureIndex("mary_phoneme");
        int iPrevPhoneme = def.getFeatureIndex("mary_prev_phoneme");
        int iNextPhoneme = def.getFeatureIndex("mary_next_phoneme");
        String mary_phoneme = replaceTrickyPhones(
                def.getFeatureValueAsString(iPhoneme, featureVector.getFeatureAsInt(iPhoneme))
                );
        String mary_prev_phoneme = replaceTrickyPhones(
                def.getFeatureValueAsString(iPrevPhoneme, featureVector.getFeatureAsInt(iPrevPhoneme))
                );
        String mary_next_phoneme = replaceTrickyPhones(
                def.getFeatureValueAsString(iNextPhoneme, featureVector.getFeatureAsInt(iNextPhoneme))
                );
        String mary_prev_prev_phoneme = "0";
        if (def.hasFeature("mary_prev_prev_phoneme")) {
            int ipp = def.getFeatureIndex("mary_prev_prev_phoneme");
            mary_prev_prev_phoneme = replaceTrickyPhones(
                    def.getFeatureValueAsString(ipp, featureVector.getFeatureAsInt(ipp))
                    );
        }
        String mary_next_next_phoneme = "0";
        if (def.hasFeature("mary_next_next_phoneme")) {
            int inn = def.getFeatureIndex("mary_next_next_phoneme");
            mary_next_next_phoneme = replaceTrickyPhones(
                    def.getFeatureValueAsString(inn, featureVector.getFeatureAsInt(inn))
                    );
        }
   
        StringBuffer contextName = new StringBuffer();
        contextName.append(mary_prev_prev_phoneme + "^" + mary_prev_phoneme + "-"
                + mary_phoneme + "+" + mary_next_phoneme + "=" + mary_next_next_phoneme + "||");
        for (String f : featureList) {
            if (!def.hasFeature(f)) {
                throw new IllegalArgumentException("Feature '"+f+"' is not known in the feature definition. Valid features are: "+def.getFeatureNames());
            }
            String shortF = shortenPfeat(f);
            contextName.append(shortF+"="+def.getFeatureValueAsString(f, featureVector)+"|");
        }
        
        return contextName.toString();
    } /* method features2context */

    
    
    /** Translation table for labels which are incompatible with HTK or shell filenames
     * See common_routines.pl in HTS training.
     * @param lab
     * @return String
     */
    private String replaceTrickyPhones(String lab){
      String s = lab;
      
      /** the replace is done for the labels: mary_phoneme, mary_prev_phoneme and mary_next_phoneme */
      
      /** DE (replacements in German phoneme set) */     
      if(lab.contentEquals("6") )
        s = "ER6";
      else if (lab.contentEquals("=6") )
        s = "ER6";
      else if (lab.contentEquals("2:") )
          s = "EU2";
      else if (lab.contentEquals("9") )
          s = "EU9";
      else if (lab.contentEquals("9~") )
          s = "UM9";
      else if (lab.contentEquals("e~") )
          s = "IMe";
      else if (lab.contentEquals("a~") )
          s = "ANa";
      else if (lab.contentEquals("o~") )
          s = "ONo";
      else if (lab.contentEquals("?") )
          s = "gstop";
      /** EN (replacements in English phoneme set) */
      else if (lab.contentEquals("r=") )
          s = "rr"; 
      
      //System.out.println("LAB=" + s);
      
      return s;
        
    }
    
    
    /** Translation table for labels which are incompatible with HTK or shell filenames
     * See common_routines.pl in HTS training.
     * In this function the phonemes as used internally in HTSEngine are changed
     * back to the Mary TTS set, this function is necessary when correcting the 
     * actual durations of AcousticPhonemes.
     * @param lab
     * @return String
     */
    public String replaceBackTrickyPhones(String lab){
      String s = lab;
      /** DE (replacements in German phoneme set) */     
      if(lab.contentEquals("ER6") )
        s = "6";
      //else if (lab.contentEquals("ER6") )   /* CHECK ??? */
      //  s = "6";
      else if (lab.contentEquals("EU2") )
          s = "2:";
      else if (lab.contentEquals("EU9") )
          s = "9";
      else if (lab.contentEquals("UM9") )
          s = "9~";
      else if (lab.contentEquals("IMe") )
          s = "e~";
      else if (lab.contentEquals("ANa") )
          s = "a~";
      else if (lab.contentEquals("ONo") )
          s = "o~";
      else if (lab.contentEquals("gstop") )
          s = "?";
      /** EN (replacements in English phoneme set) */
      else if (lab.contentEquals("rr") )
          s = "r="; 
      
      //System.out.println("LAB=" + s);
      
      return s;
        
    }
    
    /** Shorten the key name (to make the full context names shorter)
     * See common_routines.pl in HTS training.
     */
    private String shortenPfeat(String fea) {
      
      String s;
      
     // s = s.replace("^mary_pos$/POS/g;  /* ??? */
      s = fea.replace("mary_", "");
      s = s.replace("phoneme","phn");
      s = s.replace("prev","p");
      s = s.replace("next","n");
      s = s.replace("sentence","snt");
      s = s.replace("phrase","phr");
      s = s.replace("word","wrd");
      s = s.replace("from_","");
      s = s.replace("to_","");
      s = s.replace("in_","");
      s = s.replace("is_","");
      s = s.replace("break","brk");
      s = s.replace("start","stt");
      s = s.replace("accented","acc");
      s = s.replace("accent","acc");
      s = s.replace("stressed","str");
      s = s.replace("punctuation","punc");
      s = s.replace("frequency","freq");
      s = s.replace("position","pos");
      s = s.replace("halfphone_lr", "lr");
      
      return s;     
    }
    

} /* class HTSContextTranslator*/
