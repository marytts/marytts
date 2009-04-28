/**
 * Copyright 2000-2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
 * Translates phone names used in HTS-HTK
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
        String mary_phone, mary_prev_phone, mary_prev_prev_phone, mary_next_phone, mary_next_next_phone;
        
        if (featureList == null) {
            featureList = new Vector<String>(Arrays.asList(def.getFeatureNames().split("\\s+")));
        }
            
        feaAsInt = featureVector.getFeatureAsInt(iPhoneme);
        mary_phone = replaceTrickyPhones(def.getFeatureValueAsString(iPhoneme, feaAsInt));
              
        feaAsInt = featureVector.getFeatureAsInt(iPrevPhoneme);        
        if(feaAsInt > 0)
          mary_prev_phone = replaceTrickyPhones(def.getFeatureValueAsString(iPrevPhoneme, feaAsInt));
        else
          mary_prev_phone = mary_phone;
        //System.out.println("iPrevPhoneme=" + iPrevPhoneme +  "  val=" + feaAsInt);
        
        feaAsInt = featureVector.getFeatureAsInt(iPrevPrevPhoneme);    
        if ( feaAsInt > 0 )
          mary_prev_prev_phone = replaceTrickyPhones(def.getFeatureValueAsString(iPrevPrevPhoneme, feaAsInt));
        else
          mary_prev_prev_phone = mary_prev_phone;
        //System.out.println("iPrevPrevPhoneme=" + iPrevPrevPhoneme + "  val=" + feaAsInt);
        
        
        feaAsInt = featureVector.getFeatureAsInt(iNextPhoneme);
        if(feaAsInt > 0)
          mary_next_phone = replaceTrickyPhones(def.getFeatureValueAsString(iNextPhoneme, feaAsInt));
        else
          mary_next_phone = mary_phone;  
        //System.out.println("iNextPhoneme=" + iNextPhoneme + "  val=" + feaAsInt);
      
        feaAsInt = featureVector.getFeatureAsInt(iNextNextPhoneme);
        if (feaAsInt > 0) 
          mary_next_next_phone = replaceTrickyPhones(def.getFeatureValueAsString(iNextNextPhoneme, feaAsInt));         
        else
          mary_next_next_phone = mary_next_phone;
        //System.out.println("iNextNextPhoneme=" + iNextNextPhoneme + "  val=" + feaAsInt + "\n");
      
   
        StringBuffer contextName = new StringBuffer();
        contextName.append("prev_prev_phone=" + mary_prev_prev_phone);
        contextName.append("|prev_phone=" + mary_prev_phone);
        contextName.append("|phone=" + mary_phone);
        contextName.append("|next_phone=" + mary_next_phone);
        contextName.append("|next_next_phone=" + mary_next_next_phone);
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
            if (f.endsWith("phone")) 
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
    public static String replaceTrickyPhones(String lab){
      String s = lab;
      
      /** the replace is done for the labels: phone, prev_phone and next_phone */
      
      /** DE (replacements in German phone set) */     
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
      /** EN (replacements in English phone set) */
      else if (lab.contentEquals("r=") )
          s = "rr"; 
      
      //System.out.println("LAB=" + s);
      
      return s;
        
    }
    
    
    /** Translation table for labels which are incompatible with HTK or shell filenames
     * See common_routines.pl in HTS training.
     * In this function the phones as used internally in HTSEngine are changed
     * back to the Mary TTS set, this function is necessary when correcting the 
     * actual durations of AcousticPhonemes.
     * @param lab
     * @return String
     */
    public static String replaceBackTrickyPhones(String lab){
      String s = lab;
      /** DE (replacements in German phone set) */     
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
      /** EN (replacements in English phone set) */
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
      s = s.replace("phone","phn");
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
