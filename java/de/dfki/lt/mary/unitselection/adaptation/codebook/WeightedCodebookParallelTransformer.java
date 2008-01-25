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
import de.dfki.lt.signalproc.analysis.LsfFileHeader;

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
            if (inputSet==null)
                System.out.println("No input files found in " + params.inputFolder);
            else
            {
                BaselineAdaptationSet outputSet = getOutputSet(inputSet, params.outputFolder);

                transform(inputSet, outputSet);
            }
        }
    }
    
    public boolean checkParams() throws IOException
    {
        params.inputFolder = StringUtil.checkLastSlash(params.inputFolder);
        params.outputFolder = StringUtil.checkLastSlash(params.outputFolder);
        
        if (!FileUtils.exists(params.codebookFile))
        {
            System.out.println("Error: Codebook file " + params.codebookFile + " not found!");
            return false;     
        }
        else //Read lsfParams from the codebook header
        {
            params.lsfParams = new LsfFileHeader(WeightedCodebookFile.readCodebookHeaderAndClose(params.codebookFile).lsfParams);
            params.mapperParams.lpOrder = params.lsfParams.lpOrder;
        }
            
        if (!FileUtils.exists(params.inputFolder) || !FileUtils.isDirectory(params.inputFolder))
        {
            System.out.println("Error: Input folder " + params.inputFolder + " not found!");
            return false; 
        }
        
        if (!FileUtils.isDirectory(params.outputFolder))
        {
            System.out.println("Creating output folder " + params.outputFolder + "...");
            FileUtils.createDirectory(params.outputFolder);
        }
        
        return true;
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
            WeightedCodebookFile f = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_READ);
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
        //Desired values should be specified in the follwing four parameters
        double [] pscales = {1.0};
        double [] tscales = {1.0};
        double [] escales = {1.0};
        double [] vscales = {1.0};
        //
        
        //These are for fixed rate vocal tract transformation: Do not change these!!!
        double [] pscalesTemp = {1.0};
        double [] tscalesTemp = {1.0};
        double [] escalesTemp = {1.0};
        double [] vscalesTemp = {1.0};
        //
        
        WeightedCodebookFdpsolaAdapter adapter = null;

        if (bSeparateProsody)
        {
            adapter = new WeightedCodebookFdpsolaAdapter(
                                  inputItem.audioFile, inputItem.f0File, inputItem.lsfFile, 
                                  outputItem.audioFile, 
                                  bFixedRateVocalTractConversion, 
                                  pscalesTemp, tscalesTemp, escalesTemp, vscalesTemp);
        }
        else
        {
            adapter = new WeightedCodebookFdpsolaAdapter(
                                  inputItem.audioFile, inputItem.f0File, inputItem.lsfFile, 
                                  outputItem.audioFile, 
                                  bFixedRateVocalTractConversion, 
                                  pscales, tscales, escales, vscales);
        }

        if (adapter!=null)
        {

            adapter.bSilent = true;
            adapter.fdpsolaOnline(wctParams, wcMapper, wCodebook); //Call voice conversion version

            if (bSeparateProsody)
            {
                if (isScalingsRequired(pscales, tscales, escales, vscales))
                {
                    adapter = new WeightedCodebookFdpsolaAdapter(
                            inputItem.audioFile, inputItem.f0File, null, 
                            outputItem.audioFile, 
                            false,
                            pscales, tscales, escales, vscales);

                    adapter.bSilent = true;
                    adapter.fdpsolaOnline(); //Call parent class version (i.e. no voice conversion)
                }
            }
        }
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

    public static void main(String[] args) throws IOException {
        WeightedCodebookPreprocessor pp = new WeightedCodebookPreprocessor();
        WeightedCodebookFeatureExtractor fe = new WeightedCodebookFeatureExtractor();
        WeightedCodebookPostprocessor po = new WeightedCodebookPostprocessor();
        WeightedCodebookTransformerParams pa = new WeightedCodebookTransformerParams();
        
        pa.inputFolder = "d:\\1\\neutral\\test";
        pa.outputFolder = "d:\\1\\neutral_X_angry\\neutral2angryOut";
        //pa.codebookFile = "d:\\1\\neutralF_X_angryF.wcf";
        //pa.codebookFile = "d:\\1\\neutralFG_X_angryFG.wcf";
        pa.codebookFile = "d:\\1\\neutral_X_angry\\neutralL_X_angryL.wcf";
        //pa.codebookFile = "d:\\1\\neutralLG_X_angryLG.wcf";
        //pa.codebookFile = "d:\\1\\neutralS_X_angryS.wcf";
        
        //Set codebook mapper parameters
        pa.mapperParams.numBestMatches = 4; // Number of best matches in codebook
        
        // Distance measure for comparing source training and transformation features
        pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE;
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC; pa.mapperParams.alphaForSymmetric = 0.5; //Weighting factor for using weights of two lsf vectors in distance computation relatively. The range is [0.0,1.0]
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE;
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_MAHALANOBIS_DISTANCE;
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_ABSOLUTE_VALUE_DISTANCE;
        
        // Method for weighting best codebook matches
        //pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.GAUSSIAN_HALF_WINDOW;
        pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.EXPONENTIAL_HALF_WINDOW;
        //pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.TRIANGLE_HALF_WINDOW;
        
        pa.mapperParams.weightingSteepness = 0.9; // Steepness of weighting function in range [MIN_STEEPNESS, MAX_STEEPNESS]=[0.0,1.0]
        
        ////Mean and variance of a specific distance measure can be optionally kept in the following
        // two parameters for z-normalization
        pa.mapperParams.distanceMean = 0.0; 
        pa.mapperParams.distanceVariance = 1.0;
        //
        
        
        WeightedCodebookParallelTransformer t = new WeightedCodebookParallelTransformer(pp, fe, po, pa);
        t.run();
    }
}
