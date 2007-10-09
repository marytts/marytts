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

package de.dfki.lt.mary.modules.en;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
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
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;
import de.dfki.lt.mary.modules.synthesis.Voice;


/**
 * Translates TARGETFEATURES_EN --> HTSCONTEXT_EN used in HMM synthesis.
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
              MaryDataType.get("TARGETFEATURES_EN"),
              MaryDataType.get("HTSCONTEXT_EN")
              );
    }
    
    /**
     * Perform a power-on self test by processing some example input data.
     * @throws Error if the module does not work properly.
     */
    public synchronized void powerOnSelfTest() throws Error
    {
        // TODO: add meaningful power-on self test
       logger.info("..........TODO: TO-BE DONE HTSContextTranslator powerOnSelfTest()");
    }


    /**
     * Translate TARGETFEATURES_EN to HTSCONTEXT_EN
     * @param MaryData type.
     */
    public MaryData process(MaryData d)
    throws Exception
    {
 
        MaryData output = new MaryData(outputType());

        Voice v = d.getDefaultVoice();  /* This is the way of getting a Voice through a MaryData type */
        assert v instanceof HMMVoice;
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
    public String _process(String d, Vector featureList)
    throws Exception
    {
      Hashtable maryPfeats = new Hashtable();
      ArrayList currentPfeats = new ArrayList();
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
      Vector v, n_v, nn_v;
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
            currentPfeats.add(new Vector()); 
            v = (Vector)currentPfeats.get(num_phoneme);
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
    v    = (Vector)currentPfeats.get(0);
    n_v  = (Vector)currentPfeats.get(1);
    nn_v = (Vector)currentPfeats.get(2);
      
    pp_phn  = (String)v.elementAt(0);
    p_phn   = (String)v.elementAt(0);
    cur_phn = (String)v.elementAt(0);
    n_phn   = (String)n_v.elementAt(0); 
    nn_phn  = (String)nn_v.elementAt(0);
   
    for(i=0; i<currentPfeats.size(); i++){
      lab += pp_phn + "^" + p_phn + "-" + cur_phn + "+" + n_phn + "=" + nn_phn + "||";  
      //System.out.print(pp_phn + "^" + p_phn + "-" + cur_phn + "+" + n_phn + "=" + nn_phn + "||");
      pp_phn = p_phn;
      p_phn = cur_phn;
      cur_phn = n_phn;
      n_phn = nn_phn;
      if( (i+3) < currentPfeats.size() ) {
        nn_v = (Vector)currentPfeats.get(i+3);
        nn_phn = (String)nn_v.elementAt(0);
      } else {
          nn_v = (Vector)currentPfeats.get(currentPfeats.size()-1);
          nn_phn = (String)nn_v.elementAt(0); 
      }
            
      v = (Vector)currentPfeats.get(i);
      
      for(j=0; j<featureList.size(); j++) {
        fea_out = (String)featureList.elementAt(j);
        /* check if the feature is in maryPfeats list */  
        if( maryPfeats.containsKey(fea_out) ) {
           index = (Integer) maryPfeats.get(fea_out);
          
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
    
    //System.out.println("\nLAB:\n" + lab);
       
    return lab;
    
    } /* method _process */

    
    /** Translation table for labels which are incompatible with HTK or shell filenames
     * See common_routines.pl in HTS training.
     * @param lab
     * @return String
     */
    private String replaceTrickyPhones(String lab){
      String s;
      /** CHECK!! these replacements for German, ??? */
      /** the replace is done for the labels: mary_phoneme, mary_prev_phoneme and mary_next_phoneme */
   
      /* DE (replacements in German phoneme set) 
      s = lab.replaceAll("6", "ER6");
      s = lab.replaceAll("=6", "ER6");  //  REM: =6 is mapped to 6 
      s = lab.replaceAll("2:", "EU2");
      s = lab.replaceAll( "9", "EU9");
      s = lab.replaceAll("9~", "UM9");
      s = lab.replaceAll("e~", "IMe");
      s = lab.replaceAll( "a~", "ANa");
      s = lab.replaceAll("o~", "ONo"); */
    //  s = lab.replaceAll("?", "gstop"); /* CHECK!! this replacement can not be done in this way ??? */

      /** EN (replacements in English phoneme set) */
      s = lab.replaceAll("r=", "rr"); 
      
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
      
      return s;     
    }
    

} /* class HTSContextTranslator*/
