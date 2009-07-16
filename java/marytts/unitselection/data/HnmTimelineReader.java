/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.unitselection.data;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartWaveform;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.util.io.FileUtils;
import marytts.util.math.ComplexNumber;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;


public class HnmTimelineReader extends TimelineReader
{
    public HntmAnalyzerParams analysisParams;
    
    public HnmTimelineReader()
    {
    }

    public HnmTimelineReader(String fileName) throws IOException
    {
        super(fileName);
    }

    public void load(String fileName) throws IOException
    {
        super.load(fileName);
        // Now make sense of the processing header
        Properties props = new Properties();
        ByteArrayInputStream bais = new ByteArrayInputStream(procHdr.getString().getBytes("latin1"));
        props.load(bais);
        ensurePresent(props, "hnm.noiseModel");

        analysisParams = new HntmAnalyzerParams();
        
        analysisParams.noiseModel = Integer.parseInt(props.getProperty("hnm.noiseModel"));
        analysisParams.hnmPitchVoicingAnalyzerParams.numFilteringStages = Integer.parseInt(props.getProperty("hnm.numFiltStages"));
        analysisParams.hnmPitchVoicingAnalyzerParams.medianFilterLength = Integer.parseInt(props.getProperty("hnm.medianFiltLen"));
        analysisParams.hnmPitchVoicingAnalyzerParams.movingAverageFilterLength = Integer.parseInt(props.getProperty("hnm.maFiltLen"));
        analysisParams.hnmPitchVoicingAnalyzerParams.cumulativeAmpThreshold = Float.parseFloat(props.getProperty("hnm.cumAmpTh"));
        analysisParams.hnmPitchVoicingAnalyzerParams.maximumAmpThresholdInDB = Float.parseFloat(props.getProperty("hnm.maxAmpTh"));
        analysisParams.hnmPitchVoicingAnalyzerParams.harmonicDeviationPercent = Float.parseFloat(props.getProperty("hnm.harmDevPercent"));
        analysisParams.hnmPitchVoicingAnalyzerParams.sharpPeakAmpDiffInDB = Float.parseFloat(props.getProperty("hnm.sharpPeakAmpDiff"));
        analysisParams.hnmPitchVoicingAnalyzerParams.minimumTotalHarmonics = Integer.parseInt(props.getProperty("hnm.minHarmonics"));
        analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics = Integer.parseInt(props.getProperty("hnm.maxHarmonics"));
        analysisParams.hnmPitchVoicingAnalyzerParams.minimumVoicedFrequencyOfVoicing = Float.parseFloat(props.getProperty("hnm.minVoicedFreq"));
        analysisParams.hnmPitchVoicingAnalyzerParams.maximumVoicedFrequencyOfVoicing = Float.parseFloat(props.getProperty("hnm.maxVoicedFreq"));
        analysisParams.hnmPitchVoicingAnalyzerParams.maximumFrequencyOfVoicingFinalShift = Float.parseFloat(props.getProperty("hnm.maxFreqVoicingFinalShift"));
        analysisParams.hnmPitchVoicingAnalyzerParams.neighsPercent = Float.parseFloat(props.getProperty("hnm.neighsPercent"));
        analysisParams.harmonicPartCepstrumOrder = Integer.parseInt(props.getProperty("hnm.harmCepsOrder"));
        analysisParams.regularizedCepstrumWarpingMethod = Integer.parseInt(props.getProperty("hnm.regCepWarpMethod"));
        analysisParams.regularizedCepstrumLambdaHarmonic = Float.parseFloat(props.getProperty("hnm.regCepsLambda"));
        analysisParams.noisePartLpOrder = Integer.parseInt(props.getProperty("hnm.noiseLpOrder"));
        analysisParams.preemphasisCoefNoise = Float.parseFloat(props.getProperty("hnm.preCoefNoise"));
        analysisParams.hpfBeforeNoiseAnalysis = Boolean.parseBoolean(props.getProperty("hnm.hpfBeforeNoiseAnalysis"));
        analysisParams.numPeriodsHarmonicsExtraction = Float.parseFloat(props.getProperty("hnm.harmNumPer"));
    }
    
    private void ensurePresent(Properties props, String key) throws IOException
    {
        if (!props.containsKey(key))
            throw new IOException("Processing header does not contain required field '"+key+"'");

    }

    /**
     * Read and return the upcoming datagram.
     * 
     * @return the current datagram, or null if EOF was encountered; internally updates the time pointer.
     * 
     * @throws IOException
     */
    protected Datagram getNextDatagram() throws IOException {
        
        Datagram d = null;
        
        /* If the end of the datagram zone is reached, gracefully refuse to read */
        if ( getBytePointer() == timeIdxBytePos ) return( null );
        /* Else, pop the datagram out of the file */
        try {
            d = new HnmDatagram(raf, analysisParams.noiseModel);
        }
        /* Detect a possible EOF encounter */
        catch ( EOFException e ) {
            throw new IOException( "While reading a datagram, EOF was met before the time index position: "
                    + "you may be dealing with a corrupted timeline file." );
        }
        
        /* If the read was successful, update the time pointer */
        timePtr += d.getDuration();
        
        return( d );
    }
    
    private static void testSynthesizeFromDatagrams(LinkedList<HnmDatagram> datagrams, int startIndex, int endIndex, String outputFile)
    {
        HntmSynthesizer s = new HntmSynthesizer();
        //TO DO: These should come from timeline and user choices...
        HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
        HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();
        BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams();
        int samplingRateInHz = 16000;
        
        int totalFrm = 0;
        int i;
        float originalDurationInSeconds = 0.0f;
        float deltaTimeInSeconds;
        
        for (i=startIndex; i<=endIndex; i++)
        {
            if (datagrams.get(i)!=null)
            {
                if (datagrams.get(i) instanceof HnmDatagram)
                {
                    totalFrm++;
                    deltaTimeInSeconds = SignalProcUtils.sample2time(((HnmDatagram)datagrams.get(i)).getDuration(), samplingRateInHz);
                }
                else
                    deltaTimeInSeconds = SignalProcUtils.sample2time(datagrams.get(i).getDuration(), samplingRateInHz);

                originalDurationInSeconds += deltaTimeInSeconds;
            } 
        }
        
        HntmSpeechSignal hnmSignal = null;
        hnmSignal = new HntmSpeechSignal(totalFrm, samplingRateInHz, originalDurationInSeconds);
        //
        
        int frameCount = 0;
        float tAnalysisInSeconds = 0.0f;
        for (i=startIndex; i<=endIndex; i++)
        {
            if (datagrams.get(i)!=null)
            {
                if (datagrams.get(i) instanceof HnmDatagram)
                {
                    tAnalysisInSeconds += SignalProcUtils.sample2time(((HnmDatagram)datagrams.get(i)).getDuration(), samplingRateInHz);

                    if  (frameCount<totalFrm)
                    {
                        hnmSignal.frames[frameCount] = new HntmSpeechFrame(((HnmDatagram)datagrams.get(i)).getFrame());
                        hnmSignal.frames[frameCount].tAnalysisInSeconds = tAnalysisInSeconds;
                        frameCount++;
                    }
                }
                else
                    tAnalysisInSeconds += SignalProcUtils.sample2time(datagrams.get(i).getDuration(), samplingRateInHz);
            }    
        }

        HntmSynthesizedSignal ss = null;
        if (totalFrm>0)
        {    
            ss = s.synthesize(hnmSignal, pmodParams, null, analysisParams, synthesisParams);
            //FileUtils.writeTextFile(hnmSignal.getAnalysisTimes(), "d:\\hnmAnalysisTimes1.txt");
            FileUtils.writeTextFile(ss.output, outputFile);
            if (ss.output!=null)
                ss.output = MathUtils.multiply(ss.output, 1.0/32768.0);
        }
    }
    
    public static void main(String[] args)
    {
        int i;
        HnmTimelineReader h = new HnmTimelineReader();
        try {
            h.load("timeline_hnm.mry");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LinkedList<HnmDatagram> datagrams = new LinkedList<HnmDatagram>();
        int count = 0;
        for (i=0; i<h.numDatagrams; i++)
        {
            HnmDatagram d = null;
            try {
                d = (HnmDatagram)h.getNextDatagram();
                datagrams.add(d);
                
                count++;
                System.out.println("Datagram " + String.valueOf(count) + "Noise waveform size=" + ((FrameNoisePartWaveform)(((HnmDatagram)d).frame.n)).waveform().length);

                if (count>=h.numDatagrams)
                    break;
            } 
            catch (IOException e) {
            } 
        }
        
        int clusterSize = 1000;
        int numClusters = (int)Math.floor(h.numDatagrams/((double)clusterSize)+0.5);
        int startIndex, endIndex;
        String outputFile;
        for (i=0; i<numClusters; i++)
        {
            startIndex = i*clusterSize;
            endIndex = (int)Math.min((i+1)*clusterSize-1, h.numDatagrams-1);
            outputFile = "d:\\output" + String.valueOf(i+1) + ".txt";
            testSynthesizeFromDatagrams(datagrams, startIndex, endIndex, outputFile);
            System.out.println("Timeline cluster " + String.valueOf(i+1) + " of " + String.valueOf(numClusters) + " synthesized...");
        }
    }
}

