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

import java.io.IOException;
import java.util.Arrays;

import marytts.signalproc.adaptation.gmm.GMMMatch;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMMapper;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMMatch;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMSet;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMTransformerParams;
import marytts.signalproc.analysis.MelCepstralCoefficients;
import marytts.signalproc.analysis.Mfccs;
import marytts.util.io.FileUtils;

/**
 * @author oytun.turk
 *
 * This class transforms MFCCs to MFCCs
 * 
 */
public class MfccAdapter {
    
    protected Mfccs inputMfccs;
    public boolean bSilent;
    private BaselineTransformerParams baseParams;
    protected String outputFile;
    protected int numfrm;
    
    public MfccAdapter(BaselineAdaptationItem inputItem, 
                       String strOutputFile, 
                       JointGMMTransformerParams jgmmParamsIn)
    {
        baseParams = new JointGMMTransformerParams(jgmmParamsIn);

        init(inputItem, strOutputFile);
    }
    
    public void init(BaselineAdaptationItem inputItem, String strOutputFile)
    {
        outputFile = null; 

        boolean bContinue = true;

        if (!FileUtils.exists(inputItem.mfccFile))
        {
            System.out.println("Error! MFCC file " + inputItem.mfccFile + " not found.");
            bContinue = false;
        }
       
        if (strOutputFile==null || strOutputFile=="")
        {
            System.out.println("Invalid output file...");
            bContinue = false;
        }

        numfrm = 0;
        if (bContinue)
        {
            inputMfccs = new Mfccs(inputItem.mfccFile);
            numfrm = inputMfccs.mfccs.length;
            outputFile = strOutputFile;    
        }
    }
    
    public void transformOnline(VocalTractTransformationFunction vtMapper,
                                VocalTractTransformationData vtData)
    {   
        int i;

        for (i=0; i<numfrm; i++) 
            inputMfccs.mfccs[i] = processFrame(inputMfccs.mfccs[i], vtMapper, vtData);
        
        try {
            MelCepstralCoefficients.writeRawMfccFile(inputMfccs.mfccs, outputFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //Voice conversion version
    public double [] processFrame(double[] frameMfccs, 
                                  VocalTractTransformationFunction mapper,
                                  VocalTractTransformationData data)
    {   
        GMMMatch gmmMatch = null;

        //Find target estimate from codebook
        if (baseParams.isVocalTractTransformation)
        {
            if (mapper instanceof JointGMMMapper)
            {
                //Different weighting strategies can be tested here, i.e. doing a fuzzy phoneme classification
                double[] gmmWeights = new double[1];
                Arrays.fill(gmmWeights, 1.0);

                gmmMatch = ((JointGMMMapper)mapper).transform(frameMfccs, (JointGMMSet)data, gmmWeights, baseParams.isVocalTractMatchUsingTargetModel);
            }
        }

        if (!baseParams.isResynthesizeVocalTractFromSourceModel)
            return ((JointGMMMatch)gmmMatch).outputFeatures;
        else
            return ((JointGMMMatch)gmmMatch).mappedSourceFeatures;
    }
}
