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

package marytts.htsengine;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

import org.apache.log4j.Logger;


/**
 * Translates phoneme names used in HTS-HTK
 */
public class PhoneTranslator  {
    private Logger logger = Logger.getLogger("PhoneTranslator");
    private String contextFeatureFile;
    private int iPhoneme, iPrevPhoneme, iPrevPrevPhoneme, iNextPhoneme, iNextNextPhoneme;
    private Map<String,String> feat2shortFeat = new HashMap<String, String>();
 
    public PhoneTranslator()
    {

    }
    
    
    public void setContextFeatureFile(String str){ contextFeatureFile = str; }

       
    /**
     * Convert the feature vector into a context model name to be used by HTS/HTK.
     * @param def a feature definition
     * @param featureVector a feature vector which must be consistent with the Feature definition
     * @param featureList a list of features to use in constructing the context model name. If missing, all features in the feature definition are used.
     * @return the string representation of one context name.
     */
    public String features2context(FeatureDefinition def, FeatureVector featureVector, Vector<String> featureList)
    {
         
        int feaAsInt;
        String mary_phoneme, mary_prev_phoneme, mary_prev_prev_phoneme, mary_next_phoneme, mary_next_next_phoneme;
        
        if (featureList == null) {
            featureList = new Vector<String>(Arrays.asList(def.getFeatureNames().split("\\s+")));
        }
            
        feaAsInt = featureVector.getFeatureAsInt(iPhoneme);
        mary_phoneme = replaceTrickyPhones(def.getFeatureValueAsString(iPhoneme, feaAsInt));
              
        feaAsInt = featureVector.getFeatureAsInt(iPrevPhoneme);        
        if(feaAsInt > 0)
          mary_prev_phoneme = replaceTrickyPhones(def.getFeatureValueAsString(iPrevPhoneme, feaAsInt));
        else
          mary_prev_phoneme = mary_phoneme;
        //System.out.println("iPrevPhoneme=" + iPrevPhoneme +  "  val=" + feaAsInt);
        
        feaAsInt = featureVector.getFeatureAsInt(iPrevPrevPhoneme);    
        if ( feaAsInt > 0 )
          mary_prev_prev_phoneme = replaceTrickyPhones(def.getFeatureValueAsString(iPrevPrevPhoneme, feaAsInt));
        else
          mary_prev_prev_phoneme = mary_prev_phoneme;
        //System.out.println("iPrevPrevPhoneme=" + iPrevPrevPhoneme + "  val=" + feaAsInt);
        
        
        feaAsInt = featureVector.getFeatureAsInt(iNextPhoneme);
        if(feaAsInt > 0)
          mary_next_phoneme = replaceTrickyPhones(def.getFeatureValueAsString(iNextPhoneme, feaAsInt));
        else
          mary_next_phoneme = mary_phoneme;  
        //System.out.println("iNextPhoneme=" + iNextPhoneme + "  val=" + feaAsInt);
      
        feaAsInt = featureVector.getFeatureAsInt(iNextNextPhoneme);
        if (feaAsInt > 0) 
          mary_next_next_phoneme = replaceTrickyPhones(def.getFeatureValueAsString(iNextNextPhoneme, feaAsInt));         
        else
          mary_next_next_phoneme = mary_next_phoneme;
        //System.out.println("iNextNextPhoneme=" + iNextNextPhoneme + "  val=" + feaAsInt + "\n");
      
   
        StringBuffer contextName = new StringBuffer();
        contextName.append("prev_prev_phoneme=" + mary_prev_prev_phoneme);
        contextName.append("|prev_phoneme=" + mary_prev_phoneme);
        contextName.append("|phoneme=" + mary_phoneme);
        contextName.append("|next_phoneme=" + mary_next_phoneme);
        contextName.append("|next_next_phoneme=" + mary_next_next_phoneme);
        contextName.append("||");
        /* append the other context features included in the featureList */
        for (String f : featureList) {
            if (!def.hasFeature(f)) {
                throw new IllegalArgumentException("Feature '"+f+"' is not known in the feature definition. Valid features are: "+def.getFeatureNames());
            }
            //String shortF = shortenPfeat(f);
            //contextName.append(shortF);
            contextName.append(f);
            contextName.append("=");
            String value = def.getFeatureValueAsString(f, featureVector);
            if (f.contains("sentence_punc") || f.contains("punctuation"))
                value = replacePunc(value);
            else if (f.contains("tobi"))
                value = replaceToBI(value);
            contextName.append(value);
            contextName.append("|");
        }
        
        return contextName.toString();
    } /* method features2context */

    /**
     * Convert the feature vector into a context model name to be used by HTS/HTK.
     * @param def a feature definition
     * @param featureVector a feature vector which must be consistent with the Feature definition
     * @param featureList a list of features to use in constructing the context model name. If missing, all features in the feature definition are used.
     * @return the string representation of one context name.
     */
    public String features2LongContext(FeatureDefinition def, FeatureVector featureVector, Vector<String> featureList)
    {
        if (featureList == null) {
            featureList = new Vector<String>(Arrays.asList(def.getFeatureNames().split("\\s+")));
        }
        StringBuffer contextName = new StringBuffer();
        contextName.append("|");
        for (String f : featureList) {
            if (!def.hasFeature(f)) {
                throw new IllegalArgumentException("Feature '"+f+"' is not known in the feature definition. Valid features are: "+def.getFeatureNames());
            }
            contextName.append(f);
            contextName.append("=");
            String value = def.getFeatureValueAsString(f, featureVector);
            if (f.endsWith("phoneme")) 
                value = replaceTrickyPhones(value);
            else if (f.contains("sentence_punc") || f.contains("punctuation"))
                value = replacePunc(value);
            else if (f.contains("tobi"))
                value = replaceToBI(value);
            contextName.append(value);
            contextName.append("|");
        }
        
        return contextName.toString();
    } /* method features2context */
    
    
    /** Translation table for labels which are incompatible with HTK or shell filenames
     * See common_routines.pl in HTS training.
     * @param lab
     * @return String
     */
    public String replaceTrickyPhones(String lab){
      String s = lab;
      
      /** the replace is done for the labels: phoneme, prev_phoneme and next_phoneme */
      
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
    public static String replaceBackTrickyPhones(String lab){
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
      
      // look up the feature in a table:
      String s = feat2shortFeat.get(fea);
      if (s!=null) return s;
      
      // First time: need to do the shortening:
      
     // s = s.replace("^pos$/POS/g;  /* ??? */
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
      
      feat2shortFeat.put(fea, s);
      return s;
    }
    
    
    public static String replacePunc(String lab){
        String s = lab;
           
        if(lab.contentEquals(".") )
          s = "pt";
        else if (lab.contentEquals(",") )
          s = "cm";
        else if (lab.contentEquals("(") )
            s = "op";
        else if (lab.contentEquals(")") )
            s = "cp";
        else if (lab.contentEquals("?") )
            s = "in";
        else if (lab.contentEquals("\"") )
            s = "qt";
        
        return s;
          
      }
    
    public static String replaceBackPunc(String lab){
        String s = lab;
           
        if(lab.contentEquals("pt") )
          s = ".";
        else if (lab.contentEquals("cm") )
          s = ",";
        else if (lab.contentEquals("op") )
            s = "(";
        else if (lab.contentEquals("cp") )
            s = ")";
        else if (lab.contentEquals("in") )
            s = "?";
        else if (lab.contentEquals("qt") )
            s = "\"";
        
        return s;
          
      }
   
    
    private String replaceToBI(String lab){
        String s = lab;
        
        if(lab.contains("*") )  
          s = s.replace("*", "st");
        
        if(lab.contains("%") )
          s = s.replace("%", "pc");
        
        if(lab.contains("^") )    
          s = s.replace("^", "ht");
        
        return s;
          
      }
    

} /* class StringTranslator*/