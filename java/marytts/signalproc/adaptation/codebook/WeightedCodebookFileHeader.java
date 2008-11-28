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

import java.io.IOException;

import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.analysis.EnergyFileHeader;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.util.io.MaryRandomAccessFile;


/**
 * 
 * A class for handling file I/O of weighted codebook file headers
 * 
 * @author Oytun T&uumlrk
 */
public class WeightedCodebookFileHeader {
    public int totalEntries;
    
    //Codebook type
    public int codebookType;
    public static int FRAMES = 1; //Frame-by-frame mapping of features
    public static int FRAME_GROUPS = 2; //Mapping of frame average features (no label information but fixed amount of neighbouring frames is used)
    public static int LABELS = 3; //Mapping of label average features
    public static int LABEL_GROUPS = 4; //Mapping of average features collected across label groups (i.e. vowels, consonants, etc)
    public static int SPEECH = 5; //Mapping of average features collected across all speech parts (i.e. like spectral equalization)
    //
    
    public String sourceTag; //Source name tag (i.e. style or speaker identity)
    public String targetTag; //Target name tag (i.e. style or speaker identity)
    
    public LsfFileHeader lsfParams;
    public PitchFileHeader ptcParams;
    public EnergyFileHeader energyParams;
    public MfccFileHeader mfccParams;
    
    public int numNeighboursInFrameGroups; //Functional only when codebookType == FRAME_GROUPS
    public int numNeighboursInLabelGroups; //Functional only when codebookType == LABEL_GROUPS
    
    public int vocalTractFeature; //Feature to be used for representing vocal tract 
    
    public WeightedCodebookFileHeader()
    {
        this(0);
    } 
    
    public WeightedCodebookFileHeader(int totalEntriesIn)
    {
        totalEntries = totalEntriesIn;
        
        codebookType = FRAMES;
        
        sourceTag = "source"; //Source name tag (i.e. style or speaker identity)
        targetTag = "target"; //Target name tag (i.e. style or speaker identity)
        
        lsfParams = new LsfFileHeader();
        ptcParams = new PitchFileHeader();
        energyParams = new EnergyFileHeader();
        mfccParams = new MfccFileHeader();
        
        vocalTractFeature = BaselineFeatureExtractor.LSF_FEATURES;
    } 
    
    public WeightedCodebookFileHeader(WeightedCodebookFileHeader h)
    {
        totalEntries = h.totalEntries;
        
        codebookType = h.codebookType;
        
        sourceTag = h.sourceTag;
        targetTag = h.targetTag;
        
        lsfParams = new LsfFileHeader(h.lsfParams);
        ptcParams = new PitchFileHeader(h.ptcParams);
        energyParams = new EnergyFileHeader(h.energyParams);
        mfccParams = new MfccFileHeader(h.mfccParams);
        
        numNeighboursInFrameGroups = h.numNeighboursInFrameGroups;
        numNeighboursInLabelGroups = h.numNeighboursInLabelGroups;
        
        vocalTractFeature = h.vocalTractFeature;
    } 
    
    public void resetTotalEntries()
    {
        totalEntries = 0;
    }

    public void read(MaryRandomAccessFile ler) throws IOException
    {   
        totalEntries = ler.readInt();
        
        lsfParams = new LsfFileHeader();
        lsfParams.readHeader(ler);
        
        ptcParams = new PitchFileHeader();
        ptcParams.readPitchHeader(ler);
        
        energyParams = new EnergyFileHeader();
        energyParams.read(ler, true);
        
        mfccParams.readHeader(ler);
        
        codebookType = ler.readInt();
        numNeighboursInFrameGroups = ler.readInt();
        numNeighboursInLabelGroups = ler.readInt();
        
        int tagLen = ler.readInt();
        sourceTag = String.copyValueOf(ler.readChar(tagLen));
        tagLen = ler.readInt();
        targetTag = String.copyValueOf(ler.readChar(tagLen));
        
        vocalTractFeature = ler.readInt();
    }
    
    public void write(MaryRandomAccessFile ler) throws IOException
    {
        ler.writeInt(totalEntries);
        
        lsfParams.writeHeader(ler);
        ptcParams.writePitchHeader(ler);
        energyParams.write(ler);
        mfccParams.writeHeader(ler);
        
        ler.writeInt(codebookType);
        ler.writeInt(numNeighboursInFrameGroups);
        ler.writeInt(numNeighboursInLabelGroups);
        
        int tagLen = sourceTag.length();
        ler.writeInt(tagLen);
        ler.writeChar(sourceTag.toCharArray());

        tagLen = targetTag.length();
        ler.writeInt(tagLen);
        ler.writeChar(targetTag.toCharArray());
        
        ler.writeInt(vocalTractFeature);
    }
}
