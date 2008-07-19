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

package marytts.signalproc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import marytts.signalproc.window.RectWindow;
import marytts.util.MaryUtils;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.string.PrintfFormat;


/**
 * 
 * @author Marc Schr&ouml;der
 * 
 * A class that analyses the energy distribution, 
 * and computes a silence cutoff threshold,
 * in the linear energy domain.
 *
 */
public class EnergyAnalyser extends FrameBasedAnalyser {
    protected final int DEFAULT_MAXSIZE = Integer.MAX_VALUE/2;
    /** array of frame energies, for further analysis */
    protected double[] frameEnergies = new double[16384];
    /** Beginning of valid data in frameEnergies; will be >0 only after more than
     * maxSize frames have been read.
     */
    protected int offset = 0;
    /**
     * Length of valid data, counting from offset. This will count up to maxSize
     * and then stay equal to maxSize. 
     */
    protected int len = 0;
    /** maximum size of the double[] storing the frame energies */
    protected int maxSize;
    
    
    public EnergyAnalyser(DoubleDataSource signal, int framelength, int samplingRate)
    {
        super(signal, new RectWindow(framelength), framelength, samplingRate);
        maxSize = DEFAULT_MAXSIZE;
    }

    public EnergyAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate)
    {
        super(signal, new RectWindow(framelength), frameShift, samplingRate);
        maxSize = DEFAULT_MAXSIZE;
    }

    public EnergyAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate, int maxSize)
    {
        super(signal, new RectWindow(framelength), frameShift, samplingRate);
        this.maxSize = maxSize;
    }

    /**
     * Apply this FrameBasedAnalyser to the given data.
     * @param frame the data to analyse, which must be of the length prescribed by this
     * FrameBasedAnalyser, i.e. by @see{#getFrameLengthSamples()}.
     * @return a Double representing the total energy in the frame.
     * @throws IllegalArgumentException if frame does not have the prescribed length 
     */
    public Object analyse(double[] frame)
    {
        if (frame.length != getFrameLengthSamples())
            throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples()
                    + ", got " + frame.length);
        double totalEnergy = 0;
        for (int i=0; i<frame.length; i++) {
            totalEnergy += frame[i]*frame[i];
        }
        rememberFrameEnergy(totalEnergy);
        return new Double(totalEnergy);
    }
    
    protected void rememberFrameEnergy(double energy)
    {
        if (offset+len==frameEnergies.length) { // need to make space
            if (len < maxSize) { // need to increase the array size
                assert offset == 0;
                double[] dummy = new double[2*frameEnergies.length];
                System.arraycopy(frameEnergies, 0, dummy, 0, frameEnergies.length);
                frameEnergies = dummy;
            } else { // we have reached the maximum length
                if (frameEnergies.length < 2*maxSize) { // make sure we have a buffer twice maxSize
                    double[] dummy = new double[2*maxSize];
                    System.arraycopy(frameEnergies, offset, dummy, 0, len);
                    frameEnergies = dummy;
                    offset = 0;
                } else { // need to copy valid data to the beginning of the array
                    System.arraycopy(frameEnergies, offset, frameEnergies, 0, len);
                    offset = 0;
                }
            }
        }
        assert offset+len<frameEnergies.length;
        frameEnergies[offset+len] = energy;
        if (len < maxSize) len++;
        else offset++;
    }
    
    /**
     * Compute the overall mean energy in all frames.
     * @return a double representing the mean energy (non-normalised, i.e. in
     * units of square sample amplitudes).
     */
    public double getMeanFrameEnergy()
    {
        double mean = 0;
        for (int i=0; i<len; i++) {
            mean += frameEnergies[offset+i];
        }
        mean /= len;
        return mean;
    }

    /**
     * Compute the overall maximum energy in all frames.
     * @return a double representing the maximum energy (non-normalised, i.e. in
     * units of square sample amplitudes).
     */
    public double getMaxFrameEnergy()
    {
        if (len == 0) return Double.NaN;
        // otherwise, we have at least one valid value
        double max = frameEnergies[offset];
        for (int i=0; i<len; i++) {
            double val = frameEnergies[offset+i];
            if (val > max) max = val;
        }
        return max;
    }

    /**
     * Compute the overall minimum energy in all frames.
     * @return a double representing the minimum energy (non-normalised, i.e. in
     * units of square sample amplitudes).
     */
    public double getMinFrameEnergy()
    {
        if (len == 0) return Double.NaN;
        // otherwise, we have at least one valid value
        double min = frameEnergies[offset];
        for (int i=0; i<len; i++) {
            double val = frameEnergies[offset+i];
            if (val < min) min = val;
        }
        return min;
    }

    /**
     * Compute a histogram of energies found in the data. Bin sizes are
     * automatically determined based on the min and max frame energies,
     * such that the interval between min and max energy is split into 100
     * bins.
     * @return an array of doubles of length nbins, representing percentage distribution across
     * bins.
     */
    public double[] getEnergyHistogram()
    {
        return getEnergyHistogram(100);
    }

    /**
     * Compute a histogram of energies found in the data. Bin sizes are
     * automatically determined based on the min and max frame energies,
     * such that the interval between min and max energy is split into nbins
     * bins.
     * @param nbins the number of bins to compute, e.g. 100
     * @return an array of doubles of length nbins, representing percentage distribution across
     * bins.
     */
    public double[] getEnergyHistogram(int nbins)
    {
        double[] histogram = new double[nbins];
        double min = getMinFrameEnergy();
        double range = getMaxFrameEnergy() - min;
        double binWidth = range / nbins;
        double increment = 1./len;
        for (int i=0; i<len; i++) {
            int bin = (int)Math.floor((frameEnergies[offset+i]-min)/binWidth);
            // special case maximum energy: it still belongs to the top bin
            if (bin == nbins) bin = nbins-1;
            assert bin < nbins;
            histogram[bin] += increment;
        }
        return histogram;
    }

    /**
     * Determine the energy level below which to find silence. This is 
     * based on the energy histogram.
     * @return the energy below which is silence.
     */
    public double getSilenceCutoff()
    {
        double[] hist = getEnergyHistogram();
        double[] lowerHalf = new double[hist.length/2];
        // computation of the length of upperHalf accounts for the possibility that hist.length is odd 
        double[] upperHalf = new double[hist.length - lowerHalf.length];
        System.arraycopy(hist, 0, lowerHalf, 0, lowerHalf.length);
        System.arraycopy(hist, lowerHalf.length, upperHalf, 0, upperHalf.length);
        int silencePeak = MathUtils.findGlobalPeakLocation(lowerHalf);
        int speechPeak = lowerHalf.length + MathUtils.findGlobalPeakLocation(upperHalf);
        int iCutoff = silencePeak + (speechPeak-silencePeak)/2;
        // Compute dB correlate of cutoff level
        double minEnergy = getMinFrameEnergy();
        double maxEnergy = getMaxFrameEnergy();
        double cutoffEnergy = minEnergy + (maxEnergy-minEnergy)*iCutoff/hist.length;
        
        return cutoffEnergy;
    }
    
    public double getSilenceCutoffFromSortedEnergies(FrameAnalysisResult[] far, double silenceThreshold)
    {
        double[] energies = new double[far.length];
        double cutoffEnergy;
        
        for (int i=0; i<far.length; i++)
            energies[i] = ((Double)far[i].get()).doubleValue();
        
        MathUtils.quickSort(energies);
        int cutoffIndex = (int)Math.floor(silenceThreshold*energies.length);
            
        while (energies[cutoffIndex]==0.0)
        {
            cutoffIndex++;
            if (cutoffIndex>energies.length-1)
            {
                cutoffIndex = energies.length-1;
                break;
            }
        }
        
        cutoffEnergy = energies[cutoffIndex];
        
        return cutoffEnergy;
    }

    /**
     * For the current audio data and the automatically calculated silence
     * cutoff, compute a list of start and end times representing speech
     * stretches within the file.
     * This method will take the following System properties into account:
     * <ul>
     * <li><code>signalproc.minsilenceduration</code> (default: 0.1 (seconds))
     * <li><code>signalproc.minspeechduration</code> (default: 0.1 (seconds))
     * </ul>
     * Silence or speech stretches shorter than these values will be ignored.
     * @return an array of double pairs, representing start and end times (in seconds)
     * for each speech stretch.
     */
    public double[][] getSpeechStretches()
    {
        double minSilenceDur = Double.parseDouble(System.getProperty("signalproc.minsilenceduration", "0.1"));
        double minSpeechDur = Double.parseDouble(System.getProperty("signalproc.minspeechduration", "0.1"));
        FrameAnalysisResult[] far = analyseAllFrames();
        double silenceCutoff = getSilenceCutoff();
        LinkedList stretches = new LinkedList();
        boolean withinSpeech = false;
        for (int i=0; i<far.length; i++) {
            double energy = ((Double)far[i].get()).doubleValue();
            if (energy>silenceCutoff) { // it's a speech frame
                if (!withinSpeech) { // previous was silence
                    boolean addStretch = false;
                    // Check that the preceding silence was long enough:
                    if (stretches.size() == 0) {
                        addStretch = true;
                    } else { // there is a preceding stretch
                        double silenceStart = ((double[])stretches.getLast())[1];
                        double silenceEnd = i*getFrameLengthTime(); // current time 
                        if (silenceEnd - silenceStart >= minSilenceDur) {
                            addStretch = true;
                        }
                    }
                    if (addStretch) {
                        double[] newStretch = new double[2];
                        // Start of current frame is start of new stretch
                        newStretch[0] = i*getFrameLengthTime();
                        stretches.add(newStretch);
                    } // else, overwrite position [1] of existing stretch
                    withinSpeech = true;
                    assert stretches.size() > 0;
                }
            } else { // it's a silence frame
                if (withinSpeech) { // previous was speech
                    assert stretches.size() > 0;
                    double[] latestStretch = (double[])stretches.getLast(); 
                    double speechStart = latestStretch[0];
                    double speechEnd = (double)(i+1)*getFrameLengthTime(); // end of current frame
                    if (speechEnd - speechStart >= minSpeechDur) { // long enough 
                        // complete the segment:
                        latestStretch[1] = speechEnd;
                    } else { // not long enough
                        // delete the stretch
                        stretches.removeLast();
                    }
                    withinSpeech = false;
                }
            }
        }
        return (double[][])stretches.toArray(new double[0][0]);
    }
    
    /**
     * 
     */
    public double[][] getSpeechStretchesUsingEnergyHistory()
    {
        int i, j;
        double minSilenceDur = Double.parseDouble(System.getProperty("signalproc.minsilenceduration", "0.3"));
        double minSpeechDur = Double.parseDouble(System.getProperty("signalproc.minspeechduration", "0.3"));
        double endPointWindowDuration = Double.parseDouble(System.getProperty("signalproc.endpointwindowduration", "0.3"));
        FrameAnalysisResult[] far = analyseAllFrames();
        
        double[] energies = new double[far.length];
        for (i=0; i<far.length; i++)
            energies[i] = ((Double)far[i].get()).doubleValue();
        
        double[] deltaEnergies = new double[far.length-1];
        for (i=1; i<far.length; i++)
            deltaEnergies[i-1] = energies[i]-energies[i-1];
        
        MaryUtils.plot(energies);
        MaryUtils.plot(deltaEnergies);
        
        int energyBufferLength = (int)Math.floor(endPointWindowDuration/getFrameShiftTime()+0.5);
        double silenceThreshold = 0.10; //Increasing this will make silence more likely
        double speechStartEnergyThreshold = 0.9; //Increasing this will make speech segments shorter and less likely
        double silenceStartEnergyThreshold = 1.0; //Increasing this will make silence segments longer and more likely
        
        double silenceCutoff = getSilenceCutoffFromSortedEnergies(far, silenceThreshold);
        //double silenceCutoff = getSilenceCutoff();
        LinkedList stretches = new LinkedList();
        
        if (energyBufferLength>far.length)
            energyBufferLength=far.length;
        
        double[] energyBuffer = new double[energyBufferLength];
        boolean[] isSpeechs = new boolean[energyBufferLength];
        
        int speechCount = 0;
        for (i=0; i<energyBufferLength; i++)
        {
            energyBuffer[i] = ((Double)far[i].get()).doubleValue();
            if (energyBuffer[i]>silenceCutoff)
            {
                isSpeechs[i] = true;
                speechCount++;
            }
            else
                isSpeechs[i] = false;
        }
        
        boolean withinSpeech;
        double speechStart = -1.0;
        double speechEnd = -1.0;
        if (speechCount>speechStartEnergyThreshold*energyBufferLength)
        {
            withinSpeech = true;
            speechStart = Math.max(0.0, i*getFrameShiftTime()-0.5*getFrameLengthTime());
        }
        else
            withinSpeech = false;
        
        int bufferIndex = energyBufferLength-1;
        
        for (i=energyBufferLength; i<far.length; i++) 
        {
            bufferIndex++;
            if (bufferIndex>energyBufferLength-1)
                bufferIndex = 0;
            
            energyBuffer[bufferIndex] = ((Double)far[i].get()).doubleValue();
            
            if (energyBuffer[bufferIndex]>silenceCutoff)
                isSpeechs[bufferIndex] = true;
            else
                isSpeechs[bufferIndex] = false;
            
            speechCount = 0;
            for (j=0; j<energyBufferLength; j++)
            {
                if (isSpeechs[j])
                    speechCount++;
            }
            
            if (!withinSpeech)
            {
                if (speechCount>speechStartEnergyThreshold*energyBufferLength) //Start of new speech segment
                {
                    withinSpeech = true;
                    speechStart = Math.max(0.0, i*getFrameShiftTime()-0.5*getFrameLengthTime());
                }
            }
            else
            {
                if (speechCount<=silenceStartEnergyThreshold*energyBufferLength) //End of speech segment
                {
                    speechEnd = Math.max(0.0, i*getFrameShiftTime()-0.5*getFrameLengthTime());
                    withinSpeech = false;
                }
            }
            
            if (speechStart>=0.0 && speechEnd>=0.0) //A new speech segment detected
            {
                double[] newStretch = new double[2];
                newStretch[0] = speechStart;
                newStretch[1] = speechEnd;
                stretches.add(newStretch);
                
                speechStart = -1.0;
                speechEnd = -1.0;
                withinSpeech = false;
            }
        }
        
        if (withinSpeech)
        {
            speechEnd = Math.max(0, (int)Math.floor((far.length-1)*getFrameShiftTime()-0.5*getFrameLengthTime()+0.5));

            double[] newStretch = new double[2];
            newStretch[0] = speechStart;
            newStretch[1] = speechEnd;
            stretches.add(newStretch);
            
            speechStart = -1;
            speechEnd = -1;
            withinSpeech = false;
        }
        
        double[][] speechStretches = (double[][])stretches.toArray(new double[0][0]);
        boolean[] bRemoveds = new boolean[speechStretches.length];
        Arrays.fill(bRemoveds, false);
        
        //Check overlapping segments and short silence segments
        double[] stretch1 = new double[2];
        double[] stretch2 = new double[2];
        for (i=speechStretches.length-1; i>0; i--)
        {
            if (speechStretches[i][0]-speechStretches[i-1][1]<minSilenceDur)
            {
                speechStretches[i-1][1] = speechStretches[i][1];
                bRemoveds[i] = true;
            }
        }
        //
        
        //Check and remove short speech segments
        for (i=0; i<speechStretches.length; i++)
        {
            if (!bRemoveds[i] && speechStretches[i][1]-speechStretches[i][0]<minSpeechDur)
                bRemoveds[i] = true;
        }
        //
        
        stretches.clear();
        for (i=0; i<bRemoveds.length; i++)
        {
            if (!bRemoveds[i])
            {
                double[] newStretch = new double[2];
                newStretch[0] = speechStretches[i][0];
                newStretch[1] = speechStretches[i][1];
                stretches.add(newStretch);
            }
        }
                
        return (double[][])stretches.toArray(new double[0][0]);
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length > 0) {
            for (int file=0; file<args.length; file++) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[file]));
                if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
                }
                if (ais.getFormat().getChannels() > 1) {
                    throw new IllegalArgumentException("Can only deal with mono audio signals");
                }
                int samplingRate = (int) ais.getFormat().getSampleRate();
                DoubleDataSource signal = new AudioDoubleDataSource(ais);
                int framelength = (int)(0.01 /*seconds*/ * samplingRate);
                EnergyAnalyser ea = new EnergyAnalyser(signal, framelength, framelength, samplingRate);
                double[][] speechStretches1 = ea.getSpeechStretches();
                double[][] speechStretches2 = ea.getSpeechStretchesUsingEnergyHistory();
                
                System.out.println("Speech stretches1 in "+args[file]+":");
                PrintfFormat format = new PrintfFormat("%.4f");
                for (int i=0; i<speechStretches1.length; i++) {
                    System.out.println(format.sprintf(speechStretches1[i][0])
                            +" "+format.sprintf(speechStretches1[i][1]));
                }
                
                System.out.println("Speech stretches2 in "+args[file]+":");
                for (int i=0; i<speechStretches2.length; i++) {
                    System.out.println(format.sprintf(speechStretches2[i][0])
                            +" "+format.sprintf(speechStretches2[i][1]));
                }
            }
            
        } else {
            AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 1, 2, 44100.0F, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            AudioInputStream input = null;
            try {
                TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                mic.open(audioFormat);
                mic.start();
                input = new AudioInputStream(mic);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
            DoubleDataSource signal = new AudioDoubleDataSource(input);
            int framelength = (int)(0.01 /*seconds*/ * audioFormat.getSampleRate());
            EnergyAnalyser ea = new EnergyAnalyser(signal, framelength, framelength, (int)audioFormat.getSampleRate());
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {}
                System.out.println(ea.getSilenceCutoff());
            }

        }
            
    }
}
