/**
 * Copyright 2007 DFKI GmbH.
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

package de.dfki.lt.machinelearning;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class ContextualGMMParams {
    public static final int FRICATIVE =   Integer.parseInt("000000000001", 2);
    public static final int GLIDE     =   Integer.parseInt("000000000010", 2);
    public static final int LIQUID    =   Integer.parseInt("000000000100", 2);
    public static final int NASAL     =   Integer.parseInt("000000001000", 2);
    public static final int PAUSE     =   Integer.parseInt("000000010000", 2);
    public static final int PLOSIVE   =   Integer.parseInt("000000100000", 2);
    public static final int SONORANT  =   Integer.parseInt("000001000000", 2);
    public static final int SYLLABIC  =   Integer.parseInt("000010000000", 2);
    public static final int VOICED    =   Integer.parseInt("000100000000", 2);
    public static final int VOWEL     =   Integer.parseInt("001000000000", 2);
    
    public int contextClassificationType;
    public static final int NO_PHONEME_CLASS = -1;
    public static final int PHONEME_IDENTITY = 0;
    public static final int PHONOLOGY_CLASS = 1;
    public static final int FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER = 2;
    public static final int VOWEL_CONSONANT_SILENCE = 3;
    
    public String[][] phonemeClasses; //Each row corresponds to a String arrray of phonemes that are grouped in the same class
    public GMMTrainerParams[] classTrainerParams; //Training parameters for each context class
    
    public ContextualGMMParams()
    {
        this(null, null, NO_PHONEME_CLASS);
    }
    
    public ContextualGMMParams(PhonemeSet phonemeSet, GMMTrainerParams commonParams, int contextClassificationTypeIn)
    {
        //To do: Use contextClassificationType to actually create classes here
        contextClassificationType = contextClassificationTypeIn;
        
        if (phonemeSet!=null)
        {
            Set<String> tmpPhonemes = phonemeSet.getPhonemeNames();
            
            allocate(tmpPhonemes.size());
            int count = 0;
            Phoneme[] phns = new Phoneme[tmpPhonemes.size()];
            for (Iterator<String> it=tmpPhonemes.iterator(); it.hasNext();)
            {
                phns[count] = phonemeSet.getPhoneme(it.next());
                count++;
                
                if (count>=tmpPhonemes.size())
                    break;
            }
            
            setClasses(phns, commonParams);
        }
        else
        {
            allocate(0);
        }
    }
    
    public ContextualGMMParams(int numPhonemeClasses)
    {
        allocate(numPhonemeClasses);
    }
    
    public ContextualGMMParams(ContextualGMMParams existing)
    {
       if (existing!=null)
       {
           if (existing.phonemeClasses!=null && existing.classTrainerParams!=null && 
                   existing.phonemeClasses.length==existing.classTrainerParams.length)
           {
               allocate(existing.phonemeClasses.length);
               setClasses(existing.phonemeClasses, existing.classTrainerParams);
           }
           else
               allocate(0);
       }
       else
       {
           allocate(0);
       }
    }
    
    public void allocate(int numPhonemeClasses)
    {
        if (numPhonemeClasses>0)
        {
            phonemeClasses = new String[numPhonemeClasses][];
            classTrainerParams = new GMMTrainerParams[numPhonemeClasses];
            for (int i=0; i<numPhonemeClasses; i++)
                classTrainerParams[i] = new GMMTrainerParams();
        }
        else
        {
            phonemeClasses = null;
            classTrainerParams = null;
        }
    }
    
    public void setClassFromSinglePhoneme(int classIndex, String phoneme)
    {
        setClassFromSinglePhoneme(classIndex, phoneme, null);
    }
    
    public void setClassFromSinglePhoneme(int classIndex, String phoneme, GMMTrainerParams currentClassTrainerParams)
    {
        String[] phonemes = new String[1];
        phonemes[0] = phoneme;
        
        setClass(classIndex, phonemes, currentClassTrainerParams);
    }
    
    public void setClasses(String[][] phonemeClassesIn)
    {
        if (phonemeClassesIn!=null)
        {
            for (int i=0; i<phonemeClassesIn.length; i++)
                setClass(i, phonemeClassesIn[i], null);
        }
    }
    
    public void setClasses(String[][] phonemeClassesIn, GMMTrainerParams[] classTrainerParamsIn)
    {
        if (phonemeClassesIn!=null && classTrainerParamsIn!=null)
        {
            for (int i=0; i<Math.min(phonemeClassesIn.length, classTrainerParamsIn.length); i++)
                setClass(i, phonemeClassesIn[i], classTrainerParamsIn[i]);
        }
    }
    
    public void setClasses(Phoneme[] phns, GMMTrainerParams commonParams)
    {
        if (phns!=null)
        {
            int i;
            if (contextClassificationType==NO_PHONEME_CLASS) //All phonemes go to the same class, this is identical to non-contextual GMM training
            {
                phonemeClasses = new String[1][phns.length];
                classTrainerParams = new GMMTrainerParams[1];
                classTrainerParams[0] = new GMMTrainerParams(commonParams);
                
                for (i=0; i<phns.length; i++)
                    phonemeClasses[0][i] = phns[i].name();
            }
            else if (contextClassificationType==PHONEME_IDENTITY) //Each phoneme goes into a separate class, phoneme replications are taken care of
            {
                String[] allPhonemes = new String[phns.length];
                for (i=0; i<phns.length; i++)
                    allPhonemes[i] = phns[i].name();
                
                String[] differentPhonemes = StringUtil.getDifferentItemsList(allPhonemes);
                
                phonemeClasses = new String[differentPhonemes.length][1];
                classTrainerParams = new GMMTrainerParams[differentPhonemes.length];
                
                for (i=0; i<differentPhonemes.length; i++)
                {
                    phonemeClasses[i][0] = differentPhonemes[i];
                    classTrainerParams[i] = new GMMTrainerParams(commonParams);
                }
            }
            else if (contextClassificationType==PHONOLOGY_CLASS) //Each phonology class goes into a separate class, however this cannot handle phoneme replications since labels do not have phonology information that could be used in transformation phase
            {
                int[] phonologyClasses = getPhonologyClasses(phns);
                int[] differentPhonologyClasses = StringUtil.getDifferentItemsList(phonologyClasses);

                phonemeClasses = new String[differentPhonologyClasses.length][];
                classTrainerParams = new GMMTrainerParams[differentPhonologyClasses.length];

                int j;
                for (i=0; i<differentPhonologyClasses.length; i++)
                {
                    int[] indices = MathUtils.find(phonologyClasses, MathUtils.EQUALS, differentPhonologyClasses[i]);
                    phonemeClasses[i] = new String[indices.length];
                    classTrainerParams[i] = new GMMTrainerParams(commonParams);
                    for (j=0; j<indices.length; j++)
                        phonemeClasses[i][j] = phns[indices[j]].name();
                }
            }
            else if(contextClassificationType==FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER)
            {
                int[] phonologyClasses = getPhonologyClasses(phns);
                int[] differentPhonologyClasses = StringUtil.getDifferentItemsList(phonologyClasses);
                int[][] inds = new int[6][];
                
                //Fricatives
                inds[0] = MathUtils.find(phonologyClasses, MathUtils.LESS_THAN_OR_EQUALS, FRICATIVE);
                //Glide or liquids
                inds[1] = MathUtils.findOr(phonologyClasses, MathUtils.GREATER_THAN_OR_EQUALS, GLIDE, MathUtils.LESS_THAN_OR_EQUALS, LIQUID);
                //Nasals
                inds[2] = MathUtils.findOr(phonologyClasses, MathUtils.GREATER_THAN, NASAL-1, MathUtils.LESS_THAN_OR_EQUALS, NASAL);
                //Plosives
                inds[3] = MathUtils.findOr(phonologyClasses, MathUtils.GREATER_THAN, PLOSIVE-1, MathUtils.LESS_THAN_OR_EQUALS, PLOSIVE);
                //Vowels
                inds[4] = MathUtils.findOr(phonologyClasses, MathUtils.GREATER_THAN, VOWEL-1, MathUtils.LESS_THAN_OR_EQUALS, VOWEL);
                //Remaining will be other, i.e. pauses in inds[5]
                
                int j;
                int totalOther = 0;
                for (i=0; i<phonologyClasses.length; i++)
                {
                    boolean bFound = false;
                    for (j=0; j<inds.length-1; j++)
                    {
                        if (MathUtils.find(inds[j], MathUtils.EQUALS, j)!=null)
                        {
                            bFound=true;
                            break;
                        }
                    }
                    
                    if (!bFound)
                        totalOther++;      
                }
                
                int count = 0;
                if (totalOther>0)
                {
                    inds[5] = new int[totalOther];
                    
                    for (i=0; i<phonologyClasses.length; i++)
                    {
                        boolean bFound = false;
                        for (j=0; j<inds.length-1; j++)
                        {
                            if (MathUtils.find(inds[j], MathUtils.EQUALS, j)!=null)
                            {
                                bFound=true;
                                break;
                            }
                        }
                        
                        if (!bFound)
                        {
                            inds[5][count] = i;
                            count++;      
                        }
                        
                        if (count>=totalOther)
                            break;
                    }
                }
                
                int total = 0;
                for (i=0; i<inds.length; i++)
                {
                    if (inds[i]!=null)
                        total++;
                }
                
                phonemeClasses = new String[total][];
                classTrainerParams = new GMMTrainerParams[total];
                
                count = 0;
                for (i=0; i<total; i++)
                {
                    if (inds[i]!=null)
                    {
                        phonemeClasses[count] = new String[inds[i].length];
                        for (j=0; j<inds[i].length; j++)
                            phonemeClasses[count][j] = phns[inds[i][j]].name();
                        
                        classTrainerParams[count] = new GMMTrainerParams(commonParams);
                        
                        count++;
                    }
                }
            }
            else if (contextClassificationType==VOWEL_CONSONANT_SILENCE)
            {
                int[] phonologyClasses = getPhonologyClasses(phns);
                int[] differentPhonologyClasses = StringUtil.getDifferentItemsList(phonologyClasses);
                int[][] inds = new int[3][];
                
                //Vowels
                inds[0] = MathUtils.findOr(phonologyClasses, MathUtils.GREATER_THAN_OR_EQUALS, VOWEL-1, MathUtils.LESS_THAN_OR_EQUALS, VOWEL);
                //Silences
                inds[1] = MathUtils.findOr(phonologyClasses, MathUtils.GREATER_THAN, PAUSE-1, MathUtils.LESS_THAN_OR_EQUALS, PAUSE);
                //Remaining will be inds[2], i.e. consonants
                
                int j;
                int totalOther = 0;
                for (i=0; i<phonologyClasses.length; i++)
                {
                    boolean bFound = false;
                    for (j=0; j<inds.length-1; j++)
                    {
                        if (MathUtils.find(inds[j], MathUtils.EQUALS, j)!=null)
                        {
                            bFound=true;
                            break;
                        }
                    }
                    
                    if (!bFound)
                        totalOther++;      
                }
                
                int count = 0;
                if (totalOther>0)
                {
                    inds[2] = new int[totalOther];
                    
                    for (i=0; i<phonologyClasses.length; i++)
                    {
                        boolean bFound = false;
                        for (j=0; j<inds.length-1; j++)
                        {
                            if (MathUtils.find(inds[j], MathUtils.EQUALS, j)!=null)
                            {
                                bFound=true;
                                break;
                            }
                        }
                        
                        if (!bFound)
                        {
                            inds[2][count] = i;
                            count++;      
                        }
                        
                        if (count>=totalOther)
                            break;
                    }
                }
                
                int total = 0;
                for (i=0; i<inds.length; i++)
                {
                    if (inds[i]!=null)
                        total++;
                }
                
                phonemeClasses = new String[total][];
                classTrainerParams = new GMMTrainerParams[total];
                
                count = 0;
                for (i=0; i<total; i++)
                {
                    if (inds[i]!=null)
                    {
                        phonemeClasses[count] = new String[inds[i].length];
                        for (j=0; j<inds[i].length; j++)
                            phonemeClasses[count][j] = phns[inds[i][j]].name();
                        
                        classTrainerParams[count] = new GMMTrainerParams(commonParams);
                        
                        count++;
                    }
                }
                
            }
            else
            {
                phonemeClasses = null;
                classTrainerParams = null;
            }
        }
    }
    
    public int[] getPhonologyClasses(Phoneme[] phns)
    {
        int[] phonologyClasses = null;
        
        if (phns!=null)
        {
            phonologyClasses = new int[phns.length];

            for (int i=0; i<phns.length; i++)
            {
                phonologyClasses[i] = 0;
                if (phns[i].isFricative())
                    phonologyClasses[i] += FRICATIVE;

                if (phns[i].isGlide())
                    phonologyClasses[i] += GLIDE;

                if (phns[i].isLiquid())
                    phonologyClasses[i] += LIQUID;

                if (phns[i].isNasal())
                    phonologyClasses[i] += NASAL;

                if (phns[i].isPause())
                    phonologyClasses[i] += PAUSE;

                if (phns[i].isPlosive())
                    phonologyClasses[i] += PLOSIVE;

                if (phns[i].isSonorant())
                    phonologyClasses[i] += SONORANT;

                if (phns[i].isSyllabic())
                    phonologyClasses[i] += SYLLABIC;

                if (phns[i].isVoiced())
                    phonologyClasses[i] += VOICED;

                if (phns[i].isVowel())
                    phonologyClasses[i] += VOWEL;  
            }
        }

        return phonologyClasses;
    }
    
    public void setClass(int classIndex, String[] phonemes)
    {
        setClass(classIndex, phonemes, null);
    }

    public void setClass(int classIndex, String[] phonemes, GMMTrainerParams currentClassTrainerParams)
    {
        if (phonemeClasses!=null && classTrainerParams!=null && 
                classIndex>=0 && classIndex<phonemeClasses.length &&
                phonemeClasses.length==classTrainerParams.length)
        {
            phonemeClasses[classIndex] = null;
            
            if (phonemes!=null)
            {
                phonemeClasses[classIndex] = new String[phonemes.length];
                for (int i=0; i<phonemes.length; i++)
                    phonemeClasses[classIndex][i] = phonemes[i];
            }
            
            classTrainerParams[classIndex] = new GMMTrainerParams(currentClassTrainerParams);
        }
    }
    
    //Returns the zero based index of the class the phoneme belongs to
    //If it is not an element of any of the existing classes, -1 is returned
    public int getClassIndex(String phoneme)
    {
        int classInd = -1;
        
        if (phonemeClasses!=null)
        {
            int i, j;

            for (i=0; i<phonemeClasses.length; i++)
            {
                if (phonemeClasses[i]!=null)
                {
                    for (j=0; j<phonemeClasses[i].length; j++)
                    {
                        if (phoneme.compareTo(phonemeClasses[i][j])==0)
                            return i;
                    }
                }
            }
        }
        
        return classInd;
    }
    
    public void write(MaryRandomAccessFile stream)
    {
        if (stream!=null)
        {
            if (phonemeClasses!=null)
            {
                int i, j;
                try {
                    stream.writeInt(phonemeClasses.length);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                for (i=0; i<phonemeClasses.length; i++)
                {   
                    if (phonemeClasses[i].length>0)
                    {
                        try {
                            stream.writeInt(phonemeClasses[i].length);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        for (j=0; j<phonemeClasses[i].length; j++)
                        {
                            if (phonemeClasses[i][j].length()>0)
                            {
                                try {
                                    stream.writeInt(phonemeClasses[i][j].length());
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                
                                try {
                                    stream.writeChar(phonemeClasses[i][j].toCharArray());
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            } 
                            else
                            {
                                try {
                                    stream.writeInt(0);
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        } 
                    }
                    else
                    {
                        try {
                            stream.writeInt(0);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                
                if (classTrainerParams!=null)
                {
                    if (classTrainerParams.length>0)
                    {
                        try {
                            stream.writeInt(classTrainerParams.length);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        for (i=0; i<classTrainerParams.length; i++)
                            classTrainerParams[i].write(stream);
                    }
                    else
                    {
                        try {
                            stream.writeInt(0);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else
                {
                    try {
                        stream.writeInt(0);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } 
                }
            }
            else
            {
                try {
                    stream.writeInt(0);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void read(MaryRandomAccessFile stream)
    {
        if (stream!=null)
        {
            int tmpLen = 0;
            try {
                tmpLen = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (tmpLen>0)
                phonemeClasses = new String[tmpLen][];

            if (phonemeClasses!=null)
            {
                int i, j;

                for (i=0; i<phonemeClasses.length; i++)
                {   
                    tmpLen = 0;
                    try {
                        tmpLen = stream.readInt();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (tmpLen>0)
                        phonemeClasses[i] = new String[tmpLen];

                    if (phonemeClasses[i].length>0)
                    {
                        for (j=0; j<phonemeClasses[i].length; j++)
                        {
                            try {
                                tmpLen = stream.readInt();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            if (tmpLen>0)
                            { 
                                try {
                                    phonemeClasses[i][j] = String.copyValueOf(stream.readChar(tmpLen));
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        } 
                    }
                }

                tmpLen = 0;
                try {
                    tmpLen = stream.readInt();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                classTrainerParams = null;
                if (tmpLen>0)
                    classTrainerParams = new GMMTrainerParams[tmpLen];

                if (classTrainerParams.length>0)
                {
                    for (i=0; i<classTrainerParams.length; i++)
                        classTrainerParams[i] = new GMMTrainerParams(stream);
                }
            }
        }
    }
}
