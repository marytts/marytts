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

import marytts.machinelearning.KMeansClusteringTrainer;
import marytts.machinelearning.KMeansClusteringTrainerParams;
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
     * The latest version uses K-Means clustering to cluster energy values into 3 separate clusters.
     * Then, the energy threshold is selected using the lowest and highest energy cluster centers
     * 
     */
    public double[][] getSpeechStretchesUsingEnergyHistory(int energyBufferLength, 
                                                           double speechStartLikelihood, 
                                                           double speechEndLikelihood, 
                                                           double shiftFromMinimumEnergyCenter,
                                                           int numClusters)
    {
        int i, j;
        double minSilenceDur = Double.parseDouble(System.getProperty("signalproc.minsilenceduration", "0.3"));
        double minSpeechDur = Double.parseDouble(System.getProperty("signalproc.minspeechduration", "0.3"));

        FrameAnalysisResult[] far = analyseAllFrames();
        
        double[][] energies = new double[far.length][1];
        for (i=0; i<far.length; i++)
            energies[i][0] = ((Double)far[i].get()).doubleValue();
        
        double[] isSpeechsAll = new double[far.length];
        Arrays.fill(isSpeechsAll, 0.0);
        
        KMeansClusteringTrainerParams p = new KMeansClusteringTrainerParams();
        p.numClusters = numClusters;
        p.maxIterations = 40;
        KMeansClusteringTrainer t = new KMeansClusteringTrainer();
        t.train(energies, p);
        
        double[] meanEns = new double[p.numClusters];
        for (i=0; i<p.numClusters; i++)
        {
            meanEns[i] = 10*Math.log10(t.clusters[i].meanVector[0]);
            System.out.println(String.valueOf(meanEns[i])); 
        }
        
        double minEnCenter = MathUtils.getMin(meanEns);
        double maxEnCenter = MathUtils.getMax(meanEns);
        
        double energyTh = minEnCenter + shiftFromMinimumEnergyCenter*(maxEnCenter-minEnCenter);
        System.out.println(String.valueOf(energyTh)); 
        
        LinkedList stretches = new LinkedList();
        
        if (energyBufferLength>far.length)
            energyBufferLength=far.length;
        
        double[] energyBuffer = new double[energyBufferLength];
        
        int[] isSpeechs = new int[energyBufferLength];
        
        Arrays.fill(isSpeechs, 0);
        
        double ratio;
        int speechCount;
        
        int bufferInd = 0;
        for (i=0; i<energyBufferLength-1; i++)
            energyBuffer[bufferInd++] = 10*Math.log10(energies[i][0]);
        
        boolean isSpeechStarted = false;
        int tmpSpeechStartIndex = -1;
        int tmpSpeechEndIndex = -1;
        int prevStartIndex = -1;
        
        double speechStart = -1.0;
        double speechEnd = -1.0;
        
        for (i=energyBufferLength-1; i<energies.length; i++)
        {
            if (bufferInd>energyBufferLength-1)
                bufferInd=0;
            
            energyBuffer[bufferInd] = 10*Math.log10(energies[i][0]);
        
            if (energyBuffer[bufferInd]>energyTh)
            {
                isSpeechs[bufferInd] = 1;
                isSpeechsAll[i] = 1;
            }
            else
                isSpeechs[bufferInd] = 0;
            
            speechCount = 0;
            for (j=0; j<energyBufferLength; j++)
            {
                if (isSpeechs[j]==1)
                    speechCount++;
            }
            
            ratio = ((double)speechCount)/energyBufferLength;
            if (!isSpeechStarted && ratio>speechStartLikelihood)
            {
                isSpeechStarted = true;
                
                tmpSpeechStartIndex = i-energyBufferLength;
                speechStart = Math.max(0.0, tmpSpeechStartIndex*getFrameShiftTime()-0.5*getFrameLengthTime());
                
                tmpSpeechEndIndex = -1;
            }
            else if (isSpeechStarted && ratio<=speechEndLikelihood)
            {
                isSpeechStarted = false;
                tmpSpeechEndIndex = i;
                
                System.out.println(String.valueOf(tmpSpeechStartIndex*0.01) + " " + String.valueOf(tmpSpeechEndIndex*0.01));
                
                speechEnd = Math.max(0.0, i*getFrameShiftTime()+0.5*getFrameLengthTime());
                
                double[] newStretch = new double[2];
                newStretch[0] = speechStart;
                newStretch[1] = speechEnd;
                stretches.add(newStretch);
                
                tmpSpeechStartIndex = -1;
            }
            
            bufferInd++;
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
                int energyBufferLength = 30;
                double speechStartLikelihood = 0.6;
                double speechEndLikelihood = 0.2;
                double shiftFromMinimumEnergyCenter = 0.1;
                int numClusters = 3;
                double[][] speechStretches2 = ea.getSpeechStretchesUsingEnergyHistory(energyBufferLength, speechStartLikelihood, speechEndLikelihood, 
                                                                                      shiftFromMinimumEnergyCenter, numClusters);
                
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
