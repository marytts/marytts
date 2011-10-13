/**
 * Copyright 2011 DFKI GmbH.
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


package marytts.language.fr;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;




/**
 * Get the part of speech from the lia_phon output
 * or the phoneme transcription
 * @author Florent Xavier
 *
 */


public class getPOSorPhonemFR 
{
    public static String textInput = new String();
    static String sPOS;
    public static int whichPart;
    
    //HashMap representing the LIA_phon phonemes and their equivalent in the SAMPA format 
    private final static Map<String,String> LIA_PHONE_TO_SAMPA = new HashMap<String,String>();
    static {
        LIA_PHONE_TO_SAMPA.put("ii", "i");
        LIA_PHONE_TO_SAMPA.put("ei", "e");
        LIA_PHONE_TO_SAMPA.put("ee","@"); //e muet
        LIA_PHONE_TO_SAMPA.put("ai", "E");
        LIA_PHONE_TO_SAMPA.put("aa", "a");
        LIA_PHONE_TO_SAMPA.put("oo", "0");
        LIA_PHONE_TO_SAMPA.put("au", "o");
        LIA_PHONE_TO_SAMPA.put("ou", "u");
        LIA_PHONE_TO_SAMPA.put("uu", "y");
        LIA_PHONE_TO_SAMPA.put("EU", "2");
        LIA_PHONE_TO_SAMPA.put("oe", "9");
        LIA_PHONE_TO_SAMPA.put("eu", "@");
        LIA_PHONE_TO_SAMPA.put("in", "e~");
        LIA_PHONE_TO_SAMPA.put("an", "a~");
        LIA_PHONE_TO_SAMPA.put("on", "o~");
        LIA_PHONE_TO_SAMPA.put("un", "9~");
        LIA_PHONE_TO_SAMPA.put("yy", "j");
        LIA_PHONE_TO_SAMPA.put("ww", "w");
        LIA_PHONE_TO_SAMPA.put("pp", "p");
        LIA_PHONE_TO_SAMPA.put("tt", "t");
        LIA_PHONE_TO_SAMPA.put("kk", "k");
        LIA_PHONE_TO_SAMPA.put("bb", "b");
        LIA_PHONE_TO_SAMPA.put("dd", "d");
        LIA_PHONE_TO_SAMPA.put("gg", "g");
        LIA_PHONE_TO_SAMPA.put("ff", "f");
        LIA_PHONE_TO_SAMPA.put("ss", "s");
        LIA_PHONE_TO_SAMPA.put("ch", "S");
        LIA_PHONE_TO_SAMPA.put("vv", "v");
        LIA_PHONE_TO_SAMPA.put("zz", "z");
        LIA_PHONE_TO_SAMPA.put("jj", "Z");
        LIA_PHONE_TO_SAMPA.put("ll", "l");
        LIA_PHONE_TO_SAMPA.put("rr", "R");
        LIA_PHONE_TO_SAMPA.put("mm", "m");
        LIA_PHONE_TO_SAMPA.put("nn", "n");
        LIA_PHONE_TO_SAMPA.put("gn", "J");
        LIA_PHONE_TO_SAMPA.put("uy", "H");
        LIA_PHONE_TO_SAMPA.put("##", "_");
        LIA_PHONE_TO_SAMPA.put("??", " ");        
    }
    
    
    
    //HashMap representing the LIA_phon special character including POS for punctuation
    private final static Map<String,String> LIA_PHON_SPE_CHAR = new HashMap<String,String>();
    static {
        LIA_PHON_SPE_CHAR.put("<s>", "");   
        LIA_PHON_SPE_CHAR.put("</s>", "");
        LIA_PHON_SPE_CHAR.put("[YPFOR]", ".");   
        LIA_PHON_SPE_CHAR.put("[YPFAI]", ",");
    }
    
    
     
    
    
    /**
     * Method that split the lia_phon output into phonemes (in the SAMPA format)
     * or part of speech, depending on the value of the int "whichPart".
     * if whichPart == 1, then it is phonemes
     * else if whichPart == 2, then it is part of speech
     * @return
     */
    public String phonemSeek()
    {
        String str = new String();
        StringBuilder sB = new StringBuilder();
        String[] tab = null;
        Scanner scanner = new Scanner(textInput);
        while (scanner.hasNextLine()) 
        {
          //We read the string line per line.
          String line = scanner.nextLine(); 
          //and we split each line into 3 part, according to the space.
          tab = line.split(" ");
          try
          {
              //if we want the phonemes, then let us convert it to SAMPA format.
              if (whichPart == 1)
              {
                  str = tab[whichPart];               
                  str = convert2SAMPA(str);
              }
              else
              {
                  str = tab[whichPart]; 
              }
              sB.append(str).append("\n");
          }
          catch (ArrayIndexOutOfBoundsException ar)
          {
              sB.append("[SILENT]").append("\n");
          }       
        }
        sPOS = sB.toString();
        return sPOS;
    }
    

    
    /**
     * Method that convert the liaPhon format to the SAMPA one.
     * @param liaPhon
     * @return
     */
    public static String convert2SAMPA (String liaPhon)
    {       
        int length = liaPhon.length();
        if (length % 2 != 0) 
        {
            //throw new IllegalArgumentException("LIA_phon must contain an even number of characters!");
            
        }
        
        StringBuilder sampa = new StringBuilder();
        //lia_phon phonemes always are two characters long. So we test each 2 characters.
        for (int i=0; i<length; i+=2) 
        {
            //Split the word into lia_phone phonemes.
            String liaPhonPhoneme = liaPhon.substring(i, i+2);
            //Check the equivalent SAMPA phoneme from the HashMap.
            String sampaPhoneme = LIA_PHONE_TO_SAMPA.get(liaPhonPhoneme);
            if (sampaPhoneme == null) 
            {
                throw new IllegalArgumentException("Unrecognized LIA_phon phoneme: " + liaPhonPhoneme);
            }
            sampa.append(sampaPhoneme);
        }
        return sampa.toString();
    }
    
    
    
    /**
     * Method that replaces the special lia_phon char, including POS for the 
     * punctuation into their equivalent.
     * 
     * @return
     */
    public static String charReplace (String txt)
    {       
        StringBuilder newTxt = new StringBuilder();        
        StringTokenizer st = new StringTokenizer(txt);
        String cleanTxt = new String();
        String str = new String();
        while (st.hasMoreTokens()) 
        {
            str = st.nextToken();
            cleanTxt = LIA_PHON_SPE_CHAR.get(str);
            //if we don't have to replace anything.
            if (cleanTxt == null)
            {
                cleanTxt = str;
            }
            newTxt.append(cleanTxt).append("\n");
        }
        return newTxt.toString();
    }
}
