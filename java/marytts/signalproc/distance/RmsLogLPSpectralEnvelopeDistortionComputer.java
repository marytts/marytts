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

package marytts.signalproc.distance;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.Lsfs;
import marytts.signalproc.util.ESTLabels;
import marytts.signalproc.util.SignalProcUtils;
import marytts.util.FileUtils;
import marytts.util.MathUtils;
import marytts.util.audio.AudioDoubleDataSource;


/**
 * @author oytun.turk
 *
 */
public class RmsLogLPSpectralEnvelopeDistortionComputer extends BaselineLPSpectralEnvelopeDistortionComputer {
    
    public RmsLogLPSpectralEnvelopeDistortionComputer()
    {
        super();
    }
    
    public double frameDistance(double[] frm1, double[] frm2, int fftSize, int lpOrder)
    {
        super.frameDistance(frm1, frm2, fftSize, lpOrder);
        
        return SpectralDistanceMeasures.rmsLogSpectralDist(frm1, frm2, fftSize, lpOrder);
    }
    
    //Put source and target wav and lab files into two folders and call this function
    public static void main(String[] args)
    {
        RmsLogLPSpectralEnvelopeDistortionComputer sdc = new RmsLogLPSpectralEnvelopeDistortionComputer();
        
        sdc.mainBase("rmsLogLPSpectralEnvelope.txt");
    }
}
