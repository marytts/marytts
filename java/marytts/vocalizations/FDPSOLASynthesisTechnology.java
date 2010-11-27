/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.vocalizations;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.htsengine.HMMData;
import marytts.htsengine.HTSPStream;
import marytts.htsengine.HTSVocoder;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.server.MaryProperties;
import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.effects.EffectsApplier;
import marytts.signalproc.process.FDPSOLAProcessor;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.unitselection.concat.DatagramDoubleDataSource;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.VocalizationFFRTargetCostFunction;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;
import marytts.util.signal.SignalProcUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;


/**
 * FDPSOLA Synthesis technology to synthesize vocalizations
 * @author Sathish Pammi
 */

public class FDPSOLASynthesisTechnology extends VocalizationSynthesisTechnology {
    
    protected VocalizationIntonationReader vIntonationReader;
    protected TimelineReader audioTimeline;
    protected VocalizationUnitFileReader unitFileReader;
    protected boolean f0ContourImposeSupport;
    
    public FDPSOLASynthesisTechnology(String waveTimeLineFile, String unitFile, 
            String intonationFeatureFile, boolean imposeF0Support) throws MaryConfigurationException {

        try {
            this.audioTimeline  = new TimelineReader(waveTimeLineFile);
            this.unitFileReader = new VocalizationUnitFileReader(unitFile);
            this.f0ContourImposeSupport = imposeF0Support;
            
            if ( f0ContourImposeSupport ) {
                this.vIntonationReader  = new VocalizationIntonationReader(intonationFeatureFile);
            }
            else {
                this.vIntonationReader = null;
            }
        }
        catch (IOException e) {
            throw new MaryConfigurationException("Can not read data from files "+e);
        }
    }

    public FDPSOLASynthesisTechnology(TimelineReader audioTimeline, VocalizationUnitFileReader unitFileReader, 
            HNMFeatureFileReader vHNMFeaturesReader, VocalizationIntonationReader vIntonationReader, boolean imposeF0Support) {
        
        this.audioTimeline  = audioTimeline;
        this.unitFileReader = unitFileReader;
        this.vIntonationReader  = vIntonationReader;
        this.f0ContourImposeSupport = imposeF0Support;
                
    }
    
    /**
     * Synthesize given vocalization (i.e. unit-selection)  
     * @param unitIndex unit index
     * @param aft audio file format
     * @return AudioInputStream of synthesized vocalization
     * @throws SynthesisException if failed to synthesize vocalization
     */   
    @Override
    public AudioInputStream synthesize(int backchannelNumber, AudioFileFormat aft) throws SynthesisException {
        
        int numberOfBackChannels = unitFileReader.getNumberOfUnits();
        if(backchannelNumber >= numberOfBackChannels){
            throw new IllegalArgumentException("This voice has "+numberOfBackChannels+ " backchannels only. so it doesn't support unit number "+backchannelNumber);
        }
        
        VocalizationUnit bUnit = unitFileReader.getUnit(backchannelNumber);
        long start = bUnit.startTime;
        int duration  = bUnit.duration;
        Datagram[] frames = null;
        try {
            frames = audioTimeline.getDatagrams(start, duration);
        } catch (IOException e) {
            throw new SynthesisException("Can not read data from timeline file "+e);
        } 
        // Generate audio from frames
        LinkedList<Datagram> datagrams = new LinkedList<Datagram>();
        datagrams.addAll(Arrays.asList(frames));
        DoubleDataSource audioSource = new DatagramDoubleDataSource(datagrams);
        //audioSource.getAllData();
        return (new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), aft.getFormat()));
    }
    
    /**
     * Re-synthesize given vocalization using FDPSOLA technology   
     * @param unitIndex unit index
     * @param aft audio file format
     * @return AudioInputStream of synthesized vocalization
     * @throws SynthesisException if failed to synthesize vocalization
     */
    public AudioInputStream reSynthesize( int backchannelNumber, AudioFileFormat aft ) throws SynthesisException{
        double[] pScalesArray = {1.0f};
        double[] tScalesArray = {1.0f};
        return synthesizeUsingF0Modification(backchannelNumber, pScalesArray, tScalesArray, aft);
    }
    
    /**
     * Impose target intonation contour on given vocalization using HNM technology 
     * @param sourceIndex unit index of vocalization 
     * @param targetIndex unit index of target intonation
     * @param aft audio file format
     * @return AudioInputStream of synthesized vocalization
     * @throws SynthesisException if failed to synthesize vocalization
     */
    @Override
    public AudioInputStream synthesizeUsingImposedF0(int sourceIndex, int targetIndex, AudioFileFormat aft) throws SynthesisException{
        
        if ( !f0ContourImposeSupport ) {
            throw new SynthesisException("Mary configuration of this voice doesn't support intonation contour imposition");
        }
        
        int numberOfUnits = unitFileReader.getNumberOfUnits();
        if ( sourceIndex >= numberOfUnits || targetIndex >=  numberOfUnits) {
            throw new IllegalArgumentException("sourceIndex("+sourceIndex+") and targetIndex("+targetIndex+") are should be less than number of available units ("+numberOfUnits+")");
        }
        
        double[] sourceF0 = this.vIntonationReader.getContour(sourceIndex);
        double[] targetF0coeffs = this.vIntonationReader.getIntonationCoeffs(targetIndex);
        double[] sourceF0coeffs = this.vIntonationReader.getIntonationCoeffs(sourceIndex);
        
        if ( targetF0coeffs == null || sourceF0coeffs == null ) {
            return reSynthesize(sourceIndex, aft);
        }
        
        if ( targetF0coeffs.length == 0 || sourceF0coeffs.length == 0 ) {
            return reSynthesize(sourceIndex, aft);
        }
        
        double[] targetF0 = Polynomial.generatePolynomialValues(targetF0coeffs, sourceF0.length, 0, 1);
        sourceF0 = Polynomial.generatePolynomialValues(sourceF0coeffs, sourceF0.length, 0, 1);
        
        assert  targetF0.length == sourceF0.length;
        double[] tScalesArray = new double[sourceF0.length];
        double[] pScalesArray = new double[sourceF0.length];
        
        for ( int i=0; i < targetF0.length; i++ ) {
            pScalesArray[i] = (float) (targetF0[i] / sourceF0[i]);
            tScalesArray[i] = (float) (1.0);
        }
        
        return synthesizeUsingF0Modification(sourceIndex, pScalesArray, tScalesArray, aft);
    }
    
    /**
     * modify intonation contour using HNM technology 
     * @param backchannelNumber unit index of vocalization 
     * @param pScalesArray pitch scales array
     * @param tScalesArray time scales array
     * @param aft audio file format
     * @return AudioInputStream of synthesized vocalization
     * @throws SynthesisException if failed to synthesize vocalization
     */
    private AudioInputStream synthesizeUsingF0Modification(int backchannelNumber, double[] pScalesArray, 
            double[] tScalesArray, AudioFileFormat aft) throws SynthesisException{
        
        if ( backchannelNumber > unitFileReader.getNumberOfUnits() ) {
            throw new IllegalArgumentException("requesting unit should not be more than number of units");
        }
        
        if ( !f0ContourImposeSupport ) {
            throw new SynthesisException("Mary configuration of this voice doesn't support intonation contour imposition");
        }
        
        VocalizationUnit bUnit = unitFileReader.getUnit(backchannelNumber);
        long start = bUnit.startTime;
        int duration  = bUnit.duration;
        Datagram[] frames = null;
        try {
            frames = audioTimeline.getDatagrams(start, duration);
        } catch (IOException e) {
            throw new SynthesisException("cannot get audio frames from timeline file " + e);
        } 
        assert frames != null : "Cannot generate audio from null frames";
        
        boolean[] voicings = vIntonationReader.getVoicings(backchannelNumber);
        assert voicings.length == pScalesArray.length;
        assert voicings.length == tScalesArray.length;
        
        
        
        AudioFormat af;
        if ( aft == null ) { // default audio format
            float sampleRate = 16000.0F;  //8000,11025,16000,22050,44100
            int sampleSizeInBits = 16;  //8,16
            int channels = 1;     //1,2
            boolean signed = true;    //true,false
            boolean bigEndian = false;  //true,false
            af = new AudioFormat( sampleRate, sampleSizeInBits, channels, signed, bigEndian);        
        } else {
            af = aft.getFormat();
        }
        
        double[] audio_double = (new FDPSOLAProcessor()).processDatagram(frames, null, aft.getFormat(), voicings, pScalesArray, tScalesArray, false);
        /* Normalise the signal before return, this will normalise between 1 and -1 */
        double MaxSample = MathUtils.getAbsMax(audio_double);
        for (int i=0; i<audio_double.length; i++) {
            audio_double[i] = 0.3 * ( audio_double[i] / MaxSample );
        }
        return (new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af));
    }

}

