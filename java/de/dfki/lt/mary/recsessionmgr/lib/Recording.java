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
package de.dfki.lt.mary.recsessionmgr.lib;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat.Encoding;

import org.jsresources.AudioRecorder;

import de.dfki.lt.mary.recsessionmgr.debug.Test;
import de.dfki.lt.signalproc.analysis.EnergyAnalyser;
import de.dfki.lt.signalproc.analysis.EnergyAnalyser_dB;
import de.dfki.lt.signalproc.analysis.FrameBasedAnalyser.FrameAnalysisResult;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
 */
public class Recording extends Speech {
   
    // ______________________________________________________________________
    // Instance fields

    // ______________________________________________________________________
    // Class fields
    public static final AudioFormat audioFormat = new AudioFormat(Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);
    public AudioRecorder.BufferingRecorder recorder = null;
    public boolean isAmpClipped = false;  // Boolean flag to indicate if recording is saturated (amplitude clipping occurred)
    public boolean isTempClipped = false; // Boolean flag to indicate if temporal clipping occurred (no silence at either end)
    public boolean isAmpWarning = false;  // Boolean flag to indicate recording is close to being saturated
    public static final double ampYellowThreshold = -1.5;
    public static final double ampRedThreshold = -0.5;
    
    // ______________________________________________________________________
    // Instance methods
        
    
    // ______________________________________________________________________
    // Class methods
        
    /** Record for a given number of milliseconds and save as a wav file */
    public void timedRecord(TargetDataLine line, int millis) {
        AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
        recorder = new AudioRecorder.BufferingRecorder(line, targetType, getFile(), millis);

    	recorder.start();
    	try {
    		recorder.join();
            recorder = null;
    	} catch (InterruptedException ie) {}
    }
    
    /**
     * Stop an ongoing recording before the time is up. This may be useful when the user
     * presses the stop button.
     * @return true when a recording was stopped, false if no recording was going on.
     */
    public boolean stopRecording()
    {
        if (recorder != null) {
            recorder.stopRecording();
            return true;
        }
        return false;
    }
    
    /**
     * Rename the file (basename).wav by appending a suffix.
     *   Example: If spike0003 has three recordings already, then the assumption
     *             is that spike0003.wav, spike0003a.wav and spike0003b.wav are the
     *             names of the existing files. The new name for the most recent
     *             recording (spike0003.wav) will be spike0003c.wav. The newly 
     *             recorded file will then take the spike0003.wav name.
     *
     */
    public void archiveLatestRecording()
    {
        if (fileCount == 0) return;
        File latest = new File(filePath, basename+".wav");
        if (!latest.exists()) return;
        
        int suffixCodeBase = 96;  // Code point for the character before 'a'        
        // Need an upper boundary
        int suffixCode = suffixCodeBase + fileCount;
        if (fileCount > 26) suffixCode = suffixCodeBase + 26;
        File newName = new File(filePath, basename+((char)suffixCode)+".wav");
        latest.renameTo(newName);
        
        // TESTCODE
        Test.output("|Recording.getRename| Renamed " + basename + " to " + newName.getPath());       

    }

    public void checkForAmpClipping() {
        File f = getFile();
        if (!f.exists()) return;
        
        double amplitude = getPeakAmplitude();
        //System.err.println("Peak amplitude: "+amplitude+" dB");
        if (amplitude >= ampRedThreshold) {            
            this.isAmpClipped = true;
            this.isAmpWarning = false;
        }
        else if (amplitude >= ampYellowThreshold) {
            this.isAmpWarning = true;
            this.isAmpClipped = false;
        }
        else {
            this.isAmpClipped = false;
            this.isAmpWarning = false;
        }
    }

    public void checkForTempClipping() {
        File f = getFile();
        if (!f.exists()) return;
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            double[] audio = new AudioDoubleDataSource(ais).getAllData();
            int samplingRate = (int) ais.getFormat().getSampleRate();
            int frameLength = (int) (0.005 * samplingRate); // each frame is 5 ms long
            EnergyAnalyser silenceFinder = new EnergyAnalyser_dB(new BufferedDoubleDataSource(audio), frameLength, samplingRate);
            FrameAnalysisResult[] energies = silenceFinder.analyseAllFrames();
            double silenceCutoff = silenceFinder.getSilenceCutoff();
            // Need at least 100 ms of silence at the beginning and at the end:
            for (int i=0; i<20; i++) {
                double energy = ((Double)energies[i].get()).doubleValue();
                if (energy >= silenceCutoff) {
                    this.isTempClipped = true;
                }
            }

            for (int i=1, len=energies.length; i<=20; i++) {
                double energy = ((Double)energies[len-i].get()).doubleValue();
                if (energy >= silenceCutoff) {
                    this.isTempClipped = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        this.isTempClipped = false;
        
        return;
    }

    public double getPeakAmplitude() {
        File f = getFile();
        assert f.exists();
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            double[] audio = new AudioDoubleDataSource(ais).getAllData();
            double max = MathUtils.absMax(audio);
            int bits = ais.getFormat().getSampleSizeInBits();
            double possibleMax = 1.; // normalised scale
            return MathUtils.db((max*max)/(possibleMax*possibleMax));
            
        } catch (Exception e) {
            e.printStackTrace();
            return -30;
        }
        
    }

    // ______________________________________________________________________
    // Constructors
    
    /** Creates a new instance of Recording
     *  @param filePath The file path for the wav recordings
     *  @param basename The basename for the currently selected prompt
     */
    public Recording(File filePath, String basename) {
        super(filePath, basename);
    }
    
}
