/**
 * Copyright 2004-2006 DFKI GmbH.
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

package de.dfki.lt.signalproc.tests;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.process.FrameProvider;
import de.dfki.lt.signalproc.process.PitchFrameProvider;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.ESTTextfileDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import junit.framework.TestCase;

public class PitchFrameProviderTest extends TestCase
{
    public void testIdentity1() throws Exception
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(PitchFrameProviderTest.class.getResourceAsStream("arctic_a0123.wav"));
        int samplingRate = (int) ais.getFormat().getSampleRate();
        DoubleDataSource signal = new AudioDoubleDataSource(ais);
        double[] origSignal = signal.getAllData();
        signal = new BufferedDoubleDataSource(origSignal);
        DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new InputStreamReader(PitchFrameProviderTest.class.getResourceAsStream("arctic_a0123.pm")));
        double[] origPitchmarks = pitchmarks.getAllData();
        double audioDuration = origSignal.length/(double)samplingRate;
        if (origPitchmarks[origPitchmarks.length-1] < audioDuration) {
            System.out.println("correcting last pitchmark to total audio duration: "+audioDuration);
            origPitchmarks[origPitchmarks.length-1] = audioDuration;
        }
        pitchmarks = new BufferedDoubleDataSource(origPitchmarks);
        PitchFrameProvider pfp = new PitchFrameProvider(signal, pitchmarks, null, samplingRate);
        double[] result = new double[origSignal.length];
        double[] frame = null;
        int resultPos = 0;
        while ((frame = pfp.getNextFrame()) != null) {
            int periodLength = pfp.validSamplesInFrame();
            System.arraycopy(frame, 0, result, resultPos, periodLength);
            resultPos += periodLength;
        }
        assertTrue("Got back "+resultPos+", expected "+origSignal.length, resultPos==origSignal.length);
        double err = MathUtils.sumSquaredError(origSignal, result);
        assertTrue("Error: " + err, err < 1.E-20);
    }
    
    public void testIdentity2() throws Exception
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(PitchFrameProviderTest.class.getResourceAsStream("arctic_a0123.wav"));
        int samplingRate = (int) ais.getFormat().getSampleRate();
        DoubleDataSource signal = new AudioDoubleDataSource(ais);
        double[] origSignal = signal.getAllData();
        signal = new BufferedDoubleDataSource(origSignal);
        DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new InputStreamReader(PitchFrameProviderTest.class.getResourceAsStream("arctic_a0123.pm")));
        double[] origPitchmarks = pitchmarks.getAllData();
        double audioDuration = origSignal.length/(double)samplingRate;
        if (origPitchmarks[origPitchmarks.length-1] < audioDuration) {
            System.out.println("correcting last pitchmark to total audio duration: "+audioDuration);
            origPitchmarks[origPitchmarks.length-1] = audioDuration;
        }
        pitchmarks = new BufferedDoubleDataSource(origPitchmarks);
        PitchFrameProvider pfp = new PitchFrameProvider(new SequenceDoubleDataSource(new DoubleDataSource[] {signal, new BufferedDoubleDataSource(new double[1000])}), pitchmarks, null, samplingRate,
                2, 1);
        double[] result = new double[origSignal.length];
        double[] frame = null;
        int resultPos = 0;
        while ((frame = pfp.getNextFrame()) != null) {
            int toCopy = Math.min(pfp.getFrameShiftSamples(), pfp.validSamplesInFrame());
            System.arraycopy(frame, 0, result, resultPos, toCopy);
            resultPos += toCopy;
        }
        assertTrue("Got back "+resultPos+", expected "+origSignal.length, resultPos==origSignal.length);
        double err = MathUtils.sumSquaredError(origSignal, result);
        assertTrue("Error: " + err, err < 1.E-20);
    }
    


}
