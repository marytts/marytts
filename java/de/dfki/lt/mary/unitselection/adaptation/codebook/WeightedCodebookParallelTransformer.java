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

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationItem;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;

/**
 * @author oytun.turk
 *
 * This class implements transformation for weighted codebook mapping based voice conversion
 * using parallel training data (i.e. source and target data in pairs of audio recordings which have identical content)
 */
public class WeightedCodebookParallelTransformer extends
        WeightedCodebookTransformer {
    
    public WeightedCodebookMapper mapper;
    public WeightedCodebook codebook;

    public WeightedCodebookParallelTransformer(WeightedCodebookPreprocessor pp,
            WeightedCodebookFeatureExtractor fe,
            WeightedCodebookPostprocessor po,
            WeightedCodebookTransformerParams pa) {
        super(pp, fe, po, pa);
        
        codebook = null;
        mapper = null;
    }
    
    public void run() throws IOException
    {
        boolean bContinue = checkParams();
        
        if (bContinue)
        {
            BaselineAdaptationSet inputSet = getInputSet(params.inputFolder);
            BaselineAdaptationSet outputSet = getOutputSet(inputSet, params.outputFolder);

            transform(inputSet, outputSet);
        }
        else
            System.out.println("Error: Codebook file not found!");
    }
    
    public boolean checkParams()
    {
        params.inputFolder = StringUtil.checkLastSlash(params.inputFolder);
        params.outputFolder = StringUtil.checkLastSlash(params.outputFolder);
        
        if (FileUtils.exists(params.codebookFile))
            return true;
        else
            return false;      
    }
    
    //Create list of input files
    public BaselineAdaptationSet getInputSet(String inputFolder)
    {   
        BasenameList b = new BasenameList(inputFolder, wavExt);
        
        BaselineAdaptationSet inputSet = new BaselineAdaptationSet(b.getListAsVector().size());
        
        for (int i=0; i<inputSet.items.length; i++)
            inputSet.items[i].setFromWavFilename(inputFolder + b.getName(i) + wavExt);
        
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
                outputSet.items[i].audioFile = outputFolder + StringUtil.getFileName(inputSet.items[i].audioFile) + "_output" + wavExt;
        }

        return outputSet;
    }
    //
    
    
    public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet)
    {
        System.out.println("Transformation started...");
        
        if (inputSet.items!=null && outputSet.items!=null)
        {
            int numItems = Math.min(inputSet.items.length, outputSet.items.length);
            
            if (numItems>0)
            {
                preprocessor.run(inputSet);
                
                int desiredFeatures = WeightedCodebookFeatureExtractor.LSF_ANALYSIS +
                                      WeightedCodebookFeatureExtractor.F0_ANALYSIS;
                
                try {
                    featureExtractor.run(inputSet, params, desiredFeatures);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            //Load the codebook
            WeightedCodebookFile f = new WeightedCodebookFile(params.codebookFile);
            try {
                codebook = f.readCodebookFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            //Create a mapper object
            mapper = new WeightedCodebookMapper(params.mapperParams);
            
            //Do the transformations now
            for (int i=0; i<numItems; i++)
            {
                try {
                    transformOneItem(inputSet.items[i], outputSet.items[i], params, mapper, codebook);
                } catch (UnsupportedAudioFileException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                System.out.println("Transformed file " + String.valueOf(i+1) + " of "+ String.valueOf(numItems));
            }
        } 
        
        System.out.println("Transformation completed...");
    }
    
    //This function performs the actual voice conversion
    public static void transformOneItem(BaselineAdaptationItem inputItem, 
                                        BaselineAdaptationItem outputItem,
                                        WeightedCodebookTransformerParams wctParams,
                                        WeightedCodebookMapper wcMapper,
                                        WeightedCodebook wCodebook
                                        ) throws UnsupportedAudioFileException, IOException
    {
        boolean bSeparateProsody = true;
        boolean bFixedRateVocalTractConversion = true;
        double [] pscales = {1.0};
        double [] tscales = {1.0};
        double [] escales = {1.0};
        double [] vscales = {1.0};
        
        WeightedCodebookFdpsolaAdapter adapter = null;

        if (bSeparateProsody)
        {
            adapter = new WeightedCodebookFdpsolaAdapter(
                                  inputItem.audioFile, inputItem.f0File, inputItem.lsfFile, 
                                  outputItem.audioFile, bFixedRateVocalTractConversion);
        }
        else
        {
            adapter = new WeightedCodebookFdpsolaAdapter(
                                  inputItem.audioFile, inputItem.f0File, inputItem.lsfFile, 
                                  outputItem.audioFile, 
                                  pscales, tscales, escales, vscales);
        }

        if (adapter!=null)
        {

            adapter.fdpsolaOnline(wctParams, wcMapper, wCodebook); //Call voice conversion version

            if (bSeparateProsody)
            {

                    adapter = new WeightedCodebookFdpsolaAdapter(
                                          inputItem.audioFile, inputItem.f0File, null, 
                                          outputItem.audioFile, 
                                          pscales, tscales, escales, vscales);
                    
                    adapter.fdpsolaOnline(); //Call parent class version (i.e. no voice conversion)
            }
        }
    }

    public static void main(String[] args) {
        WeightedCodebookPreprocessor pp = new WeightedCodebookPreprocessor();
        WeightedCodebookFeatureExtractor fe = new WeightedCodebookFeatureExtractor();
        WeightedCodebookPostprocessor po = new WeightedCodebookPostprocessor();
        WeightedCodebookTransformerParams pa = new WeightedCodebookTransformerParams();
        
        pa.inputFolder = "d:\\1\\src_test";
        pa.outputFolder = "d:\\1\\src2tgt_output";

    }
}
