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

import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;

/**
 * @author oytun.turk
 *
 */
public class ContextualGMMParams {
    public String[][] phonemeClasses; //Each row corresponds to a String arrray of phonemes that are grouped in the same class
    public GMMTrainerParams[] classTrainerParams; //Training parameters for each context class
    
    public ContextualGMMParams()
    {
        this(null, null);
    }
    
    
    public ContextualGMMParams(PhonemeSet phonemeSet, GMMTrainerParams commonParams)
    {
        if (phonemeSet!=null)
        {
            Set<String> tmpPhonemes = phonemeSet.getPhonemeNames();
            allocate(tmpPhonemes.size());
            int count = 0;
            for (Iterator<String> it=tmpPhonemes.iterator(); it.hasNext();)
            {
                GMMTrainerParams tmpParams = new GMMTrainerParams(commonParams);
                setClassFromSinglePhoneme(count++, it.next(), tmpParams);
            }
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
