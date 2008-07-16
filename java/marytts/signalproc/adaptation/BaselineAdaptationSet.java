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

package marytts.signalproc.adaptation;

import marytts.tools.voiceimport.BasenameList;
import marytts.util.string.StringUtils;

/**
 * @author oytun.turk
 * 
 * Baseline class for speaker specific voice conversion training set
 *
 */
public class BaselineAdaptationSet {
    public BaselineAdaptationItem[] items;
    public static final String WAV_EXTENSION_DEFAULT = ".wav";
    public static final String SINUSOID_EXTENSION_DEFAULT = ".sin";
    public static final String NOISE_EXTENSION_DEFAULT = ".noi";
    public static final String TRANSIENT_EXTENSION_DEFAULT = ".tra";
    public static final String RESIDUAL_EXTENSION_DEFAULT = ".res";
    public static final String LABEL_EXTENSION_DEFAULT = ".lab";
    public static final String PITCH_EXTENSION_DEFAULT = ".ptc";
    public static final String PITCHMARK_EXTENSION_DEFAULT = ".pm";
    public static final String ENERGY_EXTENSION_DEFAULT = ".ene";
    public static final String TEXT_EXTENSION_DEFAULT= ".txt";
    public static final String RAWMFCC_EXTENSION_DEFAULT = ".mgc";
    public static final String MFCC_EXTENSION_DEFAULT = ".mfc";
    public static final String LSF_EXTENSION_DEFAULT = ".lsf";
    public static final String LPC_EXTENSION_DEFAULT = ".lpc";
    public static final String LPRESIDUAL_EXTENSION_DEFAULT = ".lpr";
    public static final String CEPSTRUM_EXTENSION_DEFAULT = ".cep";
    public static final String EGG_EXTENSION_DEFAULT = ".egg";
    public static final String TARGETFESTIVALUTT_EXTENSION_DEFAULT = ".tutt";
    public static final String TARGETLABEL_EXTENSION_DEFAULT = ".tlab";
    public static final String TARGETENERGY_EXTENSION_DEFAULT = ".tene";
    public static final String TARGETWAV_EXTENSION_DEFAULT = ".twav";

    public BaselineAdaptationSet()
    {
        items = null;   
    }

    public BaselineAdaptationSet(int numItems)
    {
        allocate(numItems);
    }
    
    public BaselineAdaptationSet(String folder)
    {
        this(folder, WAV_EXTENSION_DEFAULT);
    }
    
    public BaselineAdaptationSet(String folder, String referenceFileExt)
    {
        BasenameList b = new BasenameList(folder, referenceFileExt);
        
        allocate(b.getListAsVector().size());
        
        for (int i=0; i<items.length; i++)
            items[i].setFromWavFilename(folder + b.getName(i) + referenceFileExt);
    }
    
    public void allocate(int numItems)
    {
        if (numItems>0)
        {
            items = new BaselineAdaptationItem[numItems];
            for (int i=0; i<numItems; i++)
                items[i] = new BaselineAdaptationItem();
        }
        else
            items = null;
    }
    
    public String[] getLabelFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].labelFile;
        }
        
        return fileList;
    }
    
    public String [] getLsfFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].lsfFile;
        }
        
        return fileList;    
    }

    public String[] getAudioFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].audioFile;
        }
        
        return fileList;
    }
    
    public String [] getCepsFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].cepsFile;
        }
        
        return fileList;    
    }
    
    public String[] getEggFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].eggFile;
        }
        
        return fileList;
    }
    
    public String [] getF0Files()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].f0File;
        }
        
        return fileList;    
    }
    
    public String[] getLpcFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].lpcFile;
        }
        
        return fileList;
    }
    
    public String [] getLpResidualFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].lpResidualFile;
        }
        
        return fileList;    
    }
    
    public String[] getRawMfccFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].rawMfccFile;
        }
        
        return fileList;
    }
    
    public String[] getMfccFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].mfccFile;
        }
        
        return fileList;
    }
    
    public String [] getNoiseFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].noiseFile;
        }
        
        return fileList;    
    }
    
    public String[] getPitchMarkFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].pitchMarkFile;
        }
        
        return fileList;
    }
    
    public String [] getResidualFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].residualFile;
        }
        
        return fileList;    
    }
    
    public String[] getSinesFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].sinesFile;
        }
        
        return fileList;
    }
    
    public String [] getTextFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].textFile;
        }
        
        return fileList;    
    }
    
    public String [] getTransientsFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].transientsFile;
        }
        
        return fileList;    
    }
    
    public String [] getEnergyFiles()
    {
        String [] fileList = null;
        if (items!=null && items.length>0)
        {
            fileList = new String[items.length];
            for (int i=0; i<items.length; i++)
                fileList[i] = items[i].energyFile;
        }
        
        return fileList;    
    }
}
