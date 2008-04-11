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

package de.dfki.lt.mary.unitselection.adaptation;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebook;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookMapper;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookTransformerParams;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchTransformationData;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.StringUtil;

/**
 * @author oytun.turk
 *
 */
public class BaselineTransformer {
    public BaselinePreprocessor preprocessor;
    public BaselineFeatureExtractor featureExtractor; 
    public BaselinePostprocessor postprocessor;
    BaselineTransformerParams params;
    
    public BaselineTransformer(BaselinePreprocessor pp,
                               BaselineFeatureExtractor fe, 
                               BaselinePostprocessor po,
                               BaselineTransformerParams pa)
    {
        preprocessor = new BaselinePreprocessor(pp);
        featureExtractor = new BaselineFeatureExtractor(fe);
        postprocessor = new BaselinePostprocessor(po);
        params = new BaselineTransformerParams(pa);
    }
    
    //Baseline version does nothing, override in derived classes
    public boolean checkParams() throws IOException
    {
        return true;
    }
    
    //Baseline version does nothing, override in derived classes
    public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet) throws UnsupportedAudioFileException
    {
        
    }
    
    public static void transformOneItem(BaselineAdaptationItem inputItem, 
            BaselineAdaptationItem outputItem,
            BaselineTransformerParams tfmParams,
            VocalTractTransformationFunction vttFunction,
            VocalTractTransformationData vtData,
            PitchTransformationData pMap
            ) throws UnsupportedAudioFileException, IOException
            {
        
            }
    
    public static boolean isScalingsRequired(double[] pscales, double[] tscales, double[] escales, double[] vscales)
    {
        int i;
        for (i=0; i<pscales.length; i++)
        {
            if (pscales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<tscales.length; i++)
        {
            if (tscales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<escales.length; i++)
        {
            if (escales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<vscales.length; i++)
        {
            if (vscales[i]!=1.0)
                return true;
        }
        
        return false;
    }
}
