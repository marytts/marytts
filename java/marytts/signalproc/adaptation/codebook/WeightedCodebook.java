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

package marytts.signalproc.adaptation.codebook;

import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.VocalTractTransformationData;
import marytts.util.string.StringUtils;

/**
 * 
 * @author oytun.turk
 *
 * Wrapper class for weighted codebooks for voice conversion
 *
 */
public class WeightedCodebook extends VocalTractTransformationData {
    //These are for feature requests from the codebook
    public static final int SOURCE = 1;
    public static final int TARGET = 2;
    public static final int SOURCE_TARGET = 3; 
    public static final int TARGET_SOURCE = 4;
    //
    
    public WeightedCodebookEntry[] entries;
    public WeightedCodebookFileHeader header;
    
    public WeightedCodebook()
    {
        this(0, 0);
    }
    
    public WeightedCodebook(int totalLsfEntriesIn, int totalF0StatisticsIn)
    {
        if (header==null)
            header = new WeightedCodebookFileHeader(totalLsfEntriesIn);
        
        allocate(); 
    }
    
    public void allocate()
    {
        allocate(header.totalEntries);
    }
    
    public void allocate(int totalEntriesIn)
    {  
       if (totalEntriesIn>0)
       {
           entries = new WeightedCodebookEntry[totalEntriesIn];
           header.totalEntries = totalEntriesIn;
       }
       else
       {
           entries = null;
           header.totalEntries = 0;
       }
    }
    
    public double[][] getFeatures(int speakerType, int desiredFeatures)
    {
        double[][] features = null;
        
        if (entries!=null)
        {
            features = new double[header.totalEntries][];
            int dimension = 0;
            boolean isLsfDesired = false;
            boolean isF0Desired = false;
            boolean isEnergyDesired = false;
            boolean isDurationDesired = false;
            boolean isMfccDesired = false;
            
            if (StringUtils.isDesired(BaselineFeatureExtractor.LSF_FEATURES, desiredFeatures))
            {
                dimension += header.lsfParams.dimension;
                isLsfDesired = true;
            }
            if (StringUtils.isDesired(BaselineFeatureExtractor.F0_FEATURES, desiredFeatures))
            {
                dimension += 1;
                isF0Desired = true;
            }
            if (StringUtils.isDesired(BaselineFeatureExtractor.ENERGY_FEATURES, desiredFeatures))
            {
                dimension += 1;
                isEnergyDesired = true;
            }
            if (StringUtils.isDesired(BaselineFeatureExtractor.DURATION_FEATURES, desiredFeatures))
            {
                dimension += 1;
                isDurationDesired = true;
            }
            if (StringUtils.isDesired(BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES, desiredFeatures))
            {
                dimension += header.mfccParams.dimension;
                isMfccDesired = true;
            }
            
            int currentPos;
            for (int i=0; i<header.totalEntries; i++)
            {
                features[i] = new double[dimension];
                currentPos = 0;
                
                //Source
                if (speakerType==SOURCE || speakerType==SOURCE_TARGET)
                {
                    if (isLsfDesired && entries[i].sourceItem.lsfs!=null)
                    {
                        System.arraycopy(entries[i].sourceItem.lsfs, 0, features[i], currentPos, header.lsfParams.dimension);
                        currentPos += header.lsfParams.dimension;
                    }
                    if (isF0Desired)
                    {
                        features[i][currentPos] = entries[i].sourceItem.f0;
                        currentPos += 1;
                    }
                    if (isEnergyDesired)
                    {
                        features[i][currentPos] = entries[i].sourceItem.energy;
                        currentPos += 1;
                    }
                    if (isDurationDesired)
                    {
                        features[i][currentPos] = entries[i].sourceItem.duration;
                        currentPos += 1;
                    } 
                    if (isMfccDesired)
                    {
                        System.arraycopy(entries[i].sourceItem.mfccs, 0, features[i], currentPos, header.mfccParams.dimension);
                        currentPos += header.mfccParams.dimension;
                    } 
                }
                
                //Target
                if (speakerType==TARGET || speakerType==TARGET_SOURCE)
                {
                    if (isLsfDesired)
                    {
                        System.arraycopy(entries[i].targetItem.lsfs, 0, features[i], currentPos, header.lsfParams.dimension);
                        currentPos += header.lsfParams.dimension;
                    }
                    if (isF0Desired)
                    {
                        features[i][currentPos] = entries[i].targetItem.f0;
                        currentPos += 1;
                    }
                    if (isEnergyDesired)
                    {
                        features[i][currentPos] = entries[i].targetItem.energy;
                        currentPos += 1;
                    }
                    if (isDurationDesired)
                    {
                        features[i][currentPos] = entries[i].targetItem.duration;
                        currentPos += 1;
                    } 
                    if (isMfccDesired)
                    {
                        System.arraycopy(entries[i].targetItem.mfccs, 0, features[i], currentPos, header.mfccParams.dimension);
                        currentPos += header.mfccParams.dimension;
                    }
                } 
                
                //Repeat Source here (i.e. target is requested first)
                if (speakerType==TARGET_SOURCE)
                {
                    if (isLsfDesired)
                    {
                        System.arraycopy(entries[i].sourceItem.lsfs, 0, features[i], currentPos, header.lsfParams.dimension);
                        currentPos += header.lsfParams.dimension;
                    }
                    if (isF0Desired)
                    {
                        features[i][currentPos] = entries[i].sourceItem.f0;
                        currentPos += 1;
                    }
                    if (isEnergyDesired)
                    {
                        features[i][currentPos] = entries[i].sourceItem.energy;
                        currentPos += 1;
                    }
                    if (isDurationDesired)
                    {
                        features[i][currentPos] = entries[i].sourceItem.duration;
                        currentPos += 1;
                    }
                    if (isMfccDesired)
                    {
                        System.arraycopy(entries[i].sourceItem.mfccs, 0, features[i], currentPos, header.mfccParams.dimension);
                        currentPos += header.mfccParams.dimension;
                    }
                }
            }
        }
        
        return features;
    }
    
}
