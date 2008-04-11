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

package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.BaselineTransformer;
import de.dfki.lt.mary.unitselection.adaptation.BaselineFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.BaselinePostprocessor;
import de.dfki.lt.mary.unitselection.adaptation.BaselinePreprocessor;
import de.dfki.lt.mary.unitselection.adaptation.BaselineTransformerParams;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.StringUtil;

/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookTransformer extends BaselineTransformer {

    public WeightedCodebookTransformerParams params;
    
    public WeightedCodebookTransformer(BaselinePreprocessor pp,
                                       BaselineFeatureExtractor fe, 
                                       BaselinePostprocessor po,
                                       WeightedCodebookTransformerParams pa)
    {
        super(pp, fe, po, (BaselineTransformerParams)pa);
        
        params = new WeightedCodebookTransformerParams(pa);
    }
    
    public boolean checkParams() throws IOException
    {
        return super.checkParams();
    }
    
    public void run() throws IOException, UnsupportedAudioFileException
    {
        if (checkParams())
        {
            BaselineAdaptationSet inputSet = getInputSet(params.inputFolder);
            if (inputSet==null)
                System.out.println("No input files found in " + params.inputFolder);
            else
            {
                BaselineAdaptationSet outputSet = getOutputSet(inputSet, params.outputFolder);

                transform(inputSet, outputSet);
            }
        }
    }
    
  //Create list of input files
    public BaselineAdaptationSet getInputSet(String inputFolder)
    {   
        BasenameList b = new BasenameList(inputFolder, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        
        BaselineAdaptationSet inputSet = new BaselineAdaptationSet(b.getListAsVector().size());
        
        for (int i=0; i<inputSet.items.length; i++)
            inputSet.items[i].setFromWavFilename(inputFolder + b.getName(i) + BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        
        return inputSet;
    }
    //
    
    //Create list of output files using input set
    public BaselineAdaptationSet getOutputSet(BaselineAdaptationSet inputSet, String outputFolder)
    {   
        BaselineAdaptationSet outputSet  = null;

        outputFolder = StringUtil.checkLastSlash(outputFolder);
        
        if (inputSet!=null && inputSet.items!=null)
        {
            outputSet = new BaselineAdaptationSet(inputSet.items.length);

            for (int i=0; i<inputSet.items.length; i++)
                outputSet.items[i].audioFile = outputFolder + StringUtil.getFileName(inputSet.items[i].audioFile) + "_output" + BaselineAdaptationSet.DEFAULT_WAV_EXTENSION;
        }

        return outputSet;
    }
    //
}
