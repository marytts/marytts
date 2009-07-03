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
import java.util.Properties;

import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;


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
}

