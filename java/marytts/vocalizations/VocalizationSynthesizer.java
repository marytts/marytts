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
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.server.MaryProperties;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.effects.EffectsApplier;
import marytts.signalproc.process.FDPSOLAProcessor;
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
 * The vocalization synthesis module.
 *
 * @author Sathish Pammi
 */

public class VocalizationSynthesizer {
    
    protected TimelineReader audioTimeline;
    protected VocalizationUnitFileReader unitFileReader;
    protected int samplingRate;
    protected boolean f0ContourImposeSupport;
    protected VocalizationFeatureFileReader featureFileReader;
    protected FeatureDefinition featureDefinition;
    protected FeatureDefinition f0FeatureDefinition;
    protected VocalizationIntonationReader vIntonationReader;
    protected int noOfSuitableUnits = 1;
    protected VocalizationFFRTargetCostFunction vffrtCostFunction = null;
    protected VocalizationFFRTargetCostFunction vffrtIntonationCostFunction = null;
    
    protected float maxCandidateCost;
    protected float maxF0Cost;
    
    protected Logger logger = MaryUtils.getLogger("Vocalization");
            
    final double INFINITE = 100000;
    
    public VocalizationSynthesizer(Voice voice) throws MaryConfigurationException {
        try {
            String unitFileName = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.unitfile");
            String timelineFile = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.timeline");
            String featureFile  = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.featurefile");
            String featureDefinitionFile  = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.featureDefinitionFile");
            f0ContourImposeSupport = MaryProperties.getBoolean("voice."+voice.getName()+".f0ContourImposeSupport", false);
            
            this.unitFileReader = new VocalizationUnitFileReader(unitFileName);
            BufferedReader fDBufferedReader = new BufferedReader( new FileReader( new File(featureDefinitionFile)));
            this.featureDefinition = new FeatureDefinition(fDBufferedReader, true);
            this.featureFileReader = new VocalizationFeatureFileReader(featureFile);
            this.samplingRate   = unitFileReader.getSampleRate();
            this.audioTimeline  = new TimelineReader(timelineFile);
            vffrtCostFunction = new VocalizationFFRTargetCostFunction();
            vffrtCostFunction.load(this.featureFileReader, this.featureDefinition);
                    
            if ( f0ContourImposeSupport ) {
                String intonationFDFile = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.intonation.featureDefinitionFile");
                String intonationFile = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.intonationfile");
                BufferedReader f0FDBufferedReader = new BufferedReader( new FileReader( new File(intonationFDFile)));
                f0FeatureDefinition = new FeatureDefinition(f0FDBufferedReader, true);
                vIntonationReader = new VocalizationIntonationReader(intonationFile);
                noOfSuitableUnits = MaryProperties.getInteger("voice."+voice.getName()+".vocalization.intonation.numberOfSuitableUnits");
                vffrtIntonationCostFunction = new VocalizationFFRTargetCostFunction();
                vffrtIntonationCostFunction.load(this.featureFileReader, this.f0FeatureDefinition);
            }

        } catch (IOException ioe) {
            throw new MaryConfigurationException("Problem loading vocalization files for voice "+voice.getName(), ioe);
        }
    }
    
    /**
     * Handle a request for synthesis of vocalization
     * @param voice the selected voice 
     * @param aft AudioFileFormat of the output AudioInputStream
     * @param domElement target xml element ('vocalization' element)
     * @return AudioInputStream of requested vocalization
     *         it returns null if the voice doesn't support synthesis of vocalizations  
     * @throws IllegalArgumentException if domElement contains 'variant' attribute value 
     *         is greater than available number of vocalizations  
     */
    public AudioInputStream synthesize(Voice voice, AudioFileFormat aft, Element domElement) throws Exception{
        
        if(!voice.hasVocalizationSupport()) return null;
        
        if (domElement.hasAttribute("variant")) {
            return synthesizeVariant(voice, aft, domElement);
        }
        
        if ( f0ContourImposeSupport ) {
            return synthesizeImposedIntonation(voice, aft, domElement);
        }
        
        return synthesizeVocalization(voice, aft, domElement);
    }
    
    /**
     * To get number of available vocalizations for this voice
     * @return integer available number of vocalizations
     */
    public int getNumberOfVocalizations() {
       assert unitFileReader != null;
       return unitFileReader.getNumberOfUnits();
    }
    
    /**
     * Synthesize a "variant" vocalization 
     * @param voice the selected voice 
     * @param aft AudioFileFormat of the output AudioInputStream
     * @param domElement target 'vocalization' xml element
     * @return AudioInputStream of requested vocalization
     * @throws IllegalArgumentException if domElement contains 'variant' attribute value 
     *         is greater than available number of vocalizations 
     */
    private AudioInputStream synthesizeVariant(Voice voice, AudioFileFormat aft, Element domElement) throws Exception{
        
        int numberOfBackChannels = unitFileReader.getNumberOfUnits();
        int backchannelNumber  = 0;
        
        if(domElement.hasAttribute("variant")){
            backchannelNumber  = Integer.parseInt(domElement.getAttribute("variant"));
        }

        if(backchannelNumber >= numberOfBackChannels){
            throw new IllegalArgumentException("This voice has "+numberOfBackChannels+ " backchannels only. so it doesn't support unit number "+backchannelNumber);
        }
        
        return synthesizeSelectedVocalization(backchannelNumber, aft, domElement);
    }

    /**
     * Synthesize a vocalization which fits better for given target 
     * @param voice
     * @param aft AudioFileFormat of the output AudioInputStream
     * @param domElement target 'vocalization' xml element
     * @return
     * @throws Exception
     */
    private AudioInputStream synthesizeVocalization(Voice voice, AudioFileFormat aft, Element domElement) throws Exception{
        
        int numberOfBackChannels = unitFileReader.getNumberOfUnits();
        
        // create target 
        Target targetUnit = createTarget(domElement);
        int backchannelNumber = getBestMatchingCandidate(targetUnit);
        // here it is a bug, if getBestMatchingCandidate select a backchannelNumber greater than numberOfBackChannels
        assert backchannelNumber < numberOfBackChannels : "This voice has "+numberOfBackChannels+ " backchannels only. so it doesn't support unit number "+backchannelNumber;
        
        return synthesizeSelectedVocalization(backchannelNumber, aft, domElement);
    }

    /**
     * Synthesize a vocalization which fits better for given target, 
     * in addition, impose intonation from closest best vocalization according to given feature definition for intonation selection  
     * @param voice
     * @param aft AudioFileFormat of the output AudioInputStream
     * @param domElement target 'vocalization' xml element
     * @return
     * @throws Exception
     */
    private AudioInputStream synthesizeImposedIntonation(Voice voice, AudioFileFormat aft, Element domElement) throws Exception{
        
        // create targets 
        Target targetUnit = createTarget(domElement);
        Target targetF0Unit = createIntonationTarget(domElement);
        
        //backchannelNumber = getBestMatchingCandidate(targetUnit);
        VocalizationCost[] vCosts = getBestMatchingCandidates(targetUnit);
        // get VocalizationCost[] for only new feature definition
        VocalizationCost[] vIntonationCosts = getBestIntonationCandidates(targetF0Unit);
        
        VocalizationCost[] suitableCandidates = new VocalizationCost[noOfSuitableUnits];
        System.arraycopy(vCosts, 0, suitableCandidates, 0, noOfSuitableUnits);
        VocalizationCost[] suitableF0Candidates = new VocalizationCost[noOfSuitableUnits];
        System.arraycopy(vIntonationCosts, 0, suitableF0Candidates, 0, noOfSuitableUnits);
        
        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            debugLogCandidates(targetUnit, suitableCandidates, suitableF0Candidates);
        }
        
        ImposeIntonationData[] sortedImposeF0Data = vocalizationF0DistanceComputer(suitableCandidates, suitableF0Candidates);
        
        int targetIndex = sortedImposeF0Data[0].targetUnitIndex;
        int sourceIndex = sortedImposeF0Data[0].sourceUnitIndex;
        
        logger.debug("Synthesizing candidate "+sourceIndex+" with intonation contour "+targetIndex);
        
        if ( targetIndex == sourceIndex ) {
            return synthesizeSelectedVocalization(sourceIndex, aft, domElement);
        }
        
        return imposeF0ContourOnVocalization(sourceIndex, targetIndex, aft, domElement);
    }

    /**
     * @param targetUnit
     * @param suitableCandidates
     * @param suitableF0Candidates
     */
    private void debugLogCandidates(Target targetUnit, VocalizationCost[] suitableCandidates,
            VocalizationCost[] suitableF0Candidates) {
        FeatureVector targetFeatures = targetUnit.getFeatureVector();
        FeatureDefinition fd = featureFileReader.getFeatureDefinition();
        int fiName = fd.getFeatureIndex("name");
        int fiIntonation = fd.getFeatureIndex("intonation");
        int fiVQ = fd.getFeatureIndex("voicequality");
        for (int i=0; i<noOfSuitableUnits; i++) {
            int unitIndex = suitableCandidates[i].unitIndex;
            FeatureVector fv = featureFileReader.getFeatureVector(unitIndex);
            StringBuilder sb = new StringBuilder();
            sb.append("Candidate ").append(i).append(": ").append(unitIndex).append(" -- ");
            byte bName = fv.getByteFeature(fiName);
            if (fv.getByteFeature(fiName) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiName, fd));
            }
            if (fv.getByteFeature(fiVQ) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiVQ, fd));
            }
            if (fv.getByteFeature(fiIntonation) != 0 && targetFeatures.getByteFeature(fiIntonation) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiIntonation, fd));
            }
            for (int j=0; j<targetFeatures.getLength(); j++) {
                if (targetFeatures.isContinuousFeature(j) && !Float.isNaN((Float)targetFeatures.getFeature(j))
                        && !Float.isNaN((Float)fv.getFeature(j))) {
                    String featureName = fd.getFeatureName(j);
                    sb.append(" ").append(featureName).append("=").append(fv.getFeature(j));
                }
            }
            logger.debug(sb.toString());
        }
        for (int i=0; i<noOfSuitableUnits; i++) {
            int unitIndex = suitableF0Candidates[i].unitIndex;
            FeatureVector fv = featureFileReader.getFeatureVector(unitIndex);
            StringBuilder sb = new StringBuilder();
            sb.append("F0 Candidate ").append(i).append(": ").append(unitIndex).append(" -- ");
            byte bName = fv.getByteFeature(fiName);
            if (fv.getByteFeature(fiName) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiName, fd));
            }
            if (fv.getByteFeature(fiVQ) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiVQ, fd));
            }
            if (fv.getByteFeature(fiIntonation) != 0 && targetFeatures.getByteFeature(fiIntonation) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiIntonation, fd));
            }
            for (int j=0; j<targetFeatures.getLength(); j++) {
                if (targetFeatures.isContinuousFeature(j) && !Float.isNaN((Float)targetFeatures.getFeature(j))
                        && !Float.isNaN((Float)fv.getFeature(j))) {
                    String featureName = fd.getFeatureName(j);
                    sb.append(" ").append(featureName).append("=").append(fv.getFeature(j));
                }
            }
            logger.debug(sb.toString());
        }
    }
    
    /**
     * Impose a target f0 contour onto a (source) unit
     * @param targetIndex unit index of unit providing f0 contour
     * @param sourceIndex unit index of unit to be generated with the given contour
     * @param aft AudioFileFormat of the output AudioInputStream
     * @param domElement target 'vocalization' xml element
     * @return AudioInputStream of requested vocalization
     * @throws IOException if no data can be read at the given target time
     * @throws UnsupportedAudioFileException if audio processing fails
     */
    private AudioInputStream imposeF0ContourOnVocalization(int targetIndex, int sourceIndex, AudioFileFormat aft,
            Element domElement) throws IOException, UnsupportedAudioFileException {
        
        int numberOfBackChannels = unitFileReader.getNumberOfUnits();
        assert sourceIndex < numberOfBackChannels : "This voice has "+numberOfBackChannels+ " backchannels only. so it doesn't support unit number "+sourceIndex;
        assert targetIndex < numberOfBackChannels : "This voice has "+numberOfBackChannels+ " backchannels only. so it doesn't support unit number "+targetIndex;
                
        VocalizationUnit bUnit = unitFileReader.getUnit(sourceIndex);
        long start = bUnit.startTime;
        int duration  = bUnit.duration;
        Datagram[] frames = audioTimeline.getDatagrams(start, duration); 
        assert frames != null : "Cannot generate audio from null frames";
        
        Unit[] units = bUnit.getUnits();
        String[] unitNames = bUnit.getUnitNames();
        long endTime = 0l;
        for(int i=0;i<units.length;i++){
            int unitDuration = units[i].duration * 1000 / samplingRate;
            endTime += unitDuration;
            Element element = MaryXML.createElement(domElement.getOwnerDocument(), MaryXML.PHONE);
            element.setAttribute("d", Integer.toString(unitDuration));
            element.setAttribute("end", Long.toString(endTime));
            element.setAttribute("p", unitNames[i]);
            domElement.appendChild(element);
        }
        
        //double[] sourceF0Contour = this.vIntonationReader.getContour(sourceIndex);
        //double[] targetF0Contour = lF0Resize(vIntonationReader.getContour(targetIndex), sourceF0Contour.length);
        double[] sourceF0Contour = MathUtils.arrayResize(this.vIntonationReader.getContour(sourceIndex), frames.length);
        double[] targetF0Contour = MathUtils.arrayResize(vIntonationReader.getContour(targetIndex), frames.length);
        targetF0Contour = MathUtils.interpolateNonZeroValues(targetF0Contour);
        double windowSize = this.vIntonationReader.getWindowSizeInSeconds();
        double skipSize = this.vIntonationReader.getSkipSizeInSeconds();
        
        boolean[] voicings = new boolean[sourceF0Contour.length];
        //double[] tScales= {1.0};
        double[] tScales = new double[sourceF0Contour.length];
        double[] eScales= {1.0};
        double[] vScales= {1.0};
        double[] pScales = new double[sourceF0Contour.length];
        for( int i=0; i<sourceF0Contour.length; i++ ){
            tScales[i] = 1.0;
            pScales[i] = targetF0Contour[i] / sourceF0Contour[i];
            if (sourceF0Contour[i] == 0) {
                voicings[i] = false;
                pScales[i] = 1.0;
            }
            else {
                voicings[i] = true;
            }
        }
        
        // One possibility
        /*boolean[][] newVoicings = new boolean[1][];
        Datagram[][] allDatagrams = new Datagram[1][];
        double[][] pitchScales = new double[1][];
        double[][] timeScales = new double[1][];
        allDatagrams[0] = frames;
        newVoicings[0] = voicings;
        pitchScales[0] = pScales;
        timeScales[0] = tScales;
        System.out.println("frames length: "+frames.length);
        System.out.println("Source length: "+sourceF0Contour.length);
        return (new FDPSOLAProcessor()).processDecrufted(allDatagrams, null, aft.getFormat(), newVoicings, pitchScales, timeScales);*/
        
        // Second possibility
        /*// Generate audio from frames
        LinkedList<Datagram> datagrams = new LinkedList<Datagram>();
        datagrams.addAll(Arrays.asList(frames));
        DoubleDataSource audioSource = new DatagramDoubleDataSource(datagrams);
        DDSAudioInputStream ddsais = new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), aft.getFormat());
        return FDPSOLAProcessor.applyFDPSOLA(ddsais, pScales, tScales, eScales, vScales);*/
        
        // Third possibility 
        double[] modifiedSignal = (new FDPSOLAProcessor()).processDatagram(frames, null, aft.getFormat(), voicings, pScales, tScales, false);
        return (new DDSAudioInputStream(new BufferedDoubleDataSource(modifiedSignal), aft.getFormat()));
    }

    /**
     * Synthesize a selected vocalization
     * @param backchannelNumber
     * @param aft AudioFileFormat of the output AudioInputStream
     * @param domElement target 'vocalization' xml element
     * @return
     * @throws IOException
     */
    private AudioInputStream synthesizeSelectedVocalization(int backchannelNumber, AudioFileFormat aft, Element domElement) throws IOException {
        
        int numberOfBackChannels = unitFileReader.getNumberOfUnits();
        if(backchannelNumber >= numberOfBackChannels){
            throw new IllegalArgumentException("This voice has "+numberOfBackChannels+ " backchannels only. so it doesn't support unit number "+backchannelNumber);
        }
        
        VocalizationUnit bUnit = unitFileReader.getUnit(backchannelNumber);
        long start = bUnit.startTime;
        int duration  = bUnit.duration;
        Datagram[] frames = audioTimeline.getDatagrams(start, duration); 
        assert frames != null : "Cannot generate audio from null frames";
        
        Unit[] units = bUnit.getUnits();
        String[] unitNames = bUnit.getUnitNames();
        long endTime = 0l;
        for(int i=0;i<units.length;i++){
            int unitDuration = units[i].duration * 1000 / samplingRate;
            endTime += unitDuration;
            Element element = MaryXML.createElement(domElement.getOwnerDocument(), MaryXML.PHONE);
            element.setAttribute("d", Integer.toString(unitDuration));
            element.setAttribute("end", Long.toString(endTime));
            element.setAttribute("p", unitNames[i]);
            domElement.appendChild(element);
        }
        
        // Generate audio from frames
        LinkedList<Datagram> datagrams = new LinkedList<Datagram>();
        datagrams.addAll(Arrays.asList(frames));
        DoubleDataSource audioSource = new DatagramDoubleDataSource(datagrams);
        //audioSource.getAllData();
        return (new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), aft.getFormat()));
    }

    /**
     * polynomial distance computer between two units
     * @param suitableCandidates
     * @param suitableF0Candidates
     * @return
     */
    private ImposeIntonationData[] vocalizationF0DistanceComputer(VocalizationCost[] suitableCandidates,
            VocalizationCost[] suitableF0Candidates) {
        
        int noPossibleImpositions = suitableCandidates.length * suitableF0Candidates.length;
        ImposeIntonationData[] imposeF0Data = new ImposeIntonationData[noPossibleImpositions];
        int count = 0;
        
        for ( int i=0; i < suitableCandidates.length; i++ ) {
            for ( int j=0; j < suitableF0Candidates.length; j++ ) {
                int targetIndex = suitableCandidates[i].unitIndex;
                int sourceIndex = suitableF0Candidates[j].unitIndex;
                double distance;
                if ( targetIndex == sourceIndex ) {
                    distance = 0;
                }
                else {
                    double[] targetCoeffs = vIntonationReader.getIntonationCoeffs(targetIndex);
                    double[] sourceCoeffs = vIntonationReader.getIntonationCoeffs(sourceIndex);
                    if (targetCoeffs != null && sourceCoeffs != null && targetCoeffs.length == sourceCoeffs.length) {
                        distance = Polynomial.polynomialDistance(sourceCoeffs, targetCoeffs);
                    } else {
                        distance = Double.MAX_VALUE;
                    }
                }
                imposeF0Data[count++] = new ImposeIntonationData(sourceIndex, targetIndex, distance);
            }
        }
        
        Arrays.sort(imposeF0Data);
        
        return imposeF0Data;
    }

    /**
     * get a best matching candidate for a given target
     * @param targetUnit
     * @return
     * @throws IOException
     */
    private int getBestMatchingCandidate(Target targetUnit) throws IOException {
        //FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
       
        if(this.featureFileReader.getNumberOfUnits() != this.unitFileReader.getNumberOfUnits()) {
            throw new IllegalArgumentException("Feature file reader and unit file reader is not aligned properly");
        }
        
        int numberUnits = this.unitFileReader.getNumberOfUnits();
        double minCost = INFINITE;
        int index = 0;
        for( int i=0; i<numberUnits; i++ ) {
            Unit singleUnit = this.unitFileReader.getUnit(i);
            double cost = vffrtCostFunction.cost(targetUnit, singleUnit);
            if( cost < minCost ) {
                minCost = cost;
                index = i;
            }
        }
        
        return index;
    }
    
    /**
     * get a array of best candidates sorted according to cost
     * @param targetUnit
     * @return
     * @throws IOException
     */
    private VocalizationCost[] getBestMatchingCandidates(Target targetUnit) throws IOException {
        //FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
        
        if(this.featureFileReader.getNumberOfUnits() != this.unitFileReader.getNumberOfUnits()) {
            throw new IllegalArgumentException("Feature file reader and unit file reader is not aligned properly");
        }
        
        int numberUnits = this.unitFileReader.getNumberOfUnits();
        VocalizationCost[] vocalizationCost = new VocalizationCost[numberUnits];
        for( int i=0; i<numberUnits; i++ ) {
            Unit singleUnit = this.unitFileReader.getUnit(i);
            double cost = vffrtCostFunction.cost(targetUnit, singleUnit);
            vocalizationCost[i] = new VocalizationCost(i,cost);
        }
        Arrays.sort(vocalizationCost);
        return vocalizationCost;
    }


    
    /**
     * get a array of best candidates sorted according to cost (cost computed on f0_feature_definition features only)
     * @param targetUnit
     * @return
     * @throws IOException
     */
    private VocalizationCost[] getBestIntonationCandidates(Target targetUnit) throws IOException {
        
        if(this.featureFileReader.getNumberOfUnits() != this.unitFileReader.getNumberOfUnits()) {
            throw new IllegalArgumentException("Feature file reader and unit file reader is not aligned properly");
        }
        
        int numberUnits = this.unitFileReader.getNumberOfUnits();
        VocalizationCost[] vocalizationCost = new VocalizationCost[numberUnits];
        for( int i=0; i<numberUnits; i++ ) {
            Unit singleUnit = this.unitFileReader.getUnit(i);
            double cost = vffrtIntonationCostFunction.cost(targetUnit, singleUnit);
            vocalizationCost[i] = new VocalizationCost(i,cost);
        }
        Arrays.sort(vocalizationCost);
        return vocalizationCost;
    }
    
    
    /**
     * create target from XML request
     * @param domElement
     * @return
     */
    private Target createTarget(Element domElement) {
        
        //FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
        FeatureDefinition featDef = this.featureDefinition;
        int numFeatures = featDef.getNumberOfFeatures();
        int numByteFeatures = featDef.getNumberOfByteFeatures();
        int numShortFeatures = featDef.getNumberOfShortFeatures();
        int numContiniousFeatures = featDef.getNumberOfContinuousFeatures();
        byte[]  byteFeatures  = new byte[numByteFeatures];
        short[] shortFeatures = new short[numShortFeatures];
        float[] floatFeatures = new float[numContiniousFeatures];
        int byteCount = 0;
        int shortCount = 0;
        int floatCount = 0;
        
        for( int i=0; i<numFeatures; i++ ) {
            
            String featName  = featDef.getFeatureName(i);
            String featValue = "0";
            
            if ( featDef.isByteFeature(featName) || featDef.isShortFeature(featName) ) {
                if( domElement.hasAttribute( featName ) ) {
                    featValue = domElement.getAttribute(featName);
                }
                
                boolean hasFeature = featDef.hasFeatureValue(featName, featValue);
                if( !hasFeature ) featValue = "0";
                
                if ( featDef.isByteFeature(i) ) {
                    byteFeatures[byteCount++]   = featDef.getFeatureValueAsByte(i, featValue);
                }
                else if ( featDef.isShortFeature(i) ) {
                    shortFeatures[shortCount++] = featDef.getFeatureValueAsShort(i, featValue);
                }
            }
            else {
                if( domElement.hasAttribute( "meaning" ) ) {
                    featValue = domElement.getAttribute("meaning");
                }
                //float contFeature = getMeaningScaleValue ( featName, featValue );
                floatFeatures[floatCount++] = getMeaningScaleValue ( featName, featValue );
            }
        }
        
        FeatureVector newFV = featDef.toFeatureVector(0, byteFeatures, shortFeatures, floatFeatures);
        
        String name = "0";
        if( domElement.hasAttribute( "name" ) ) {
            name = domElement.getAttribute("name");
        }
        
        Target newTarget = new Target(name, domElement);
        newTarget.setFeatureVector(newFV);
                 
        return newTarget;
    }
    
    
    /**
     * create F0 target from XML request
     * @param domElement
     * @return
     */
    private Target createIntonationTarget(Element domElement) {
        
        //FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
        FeatureDefinition featDef = this.f0FeatureDefinition;
        int numFeatures = featDef.getNumberOfFeatures();
        int numByteFeatures = featDef.getNumberOfByteFeatures();
        int numShortFeatures = featDef.getNumberOfShortFeatures();
        int numContiniousFeatures = featDef.getNumberOfContinuousFeatures();
        byte[]  byteFeatures  = new byte[numByteFeatures];
        short[] shortFeatures = new short[numShortFeatures];
        float[] floatFeatures = new float[numContiniousFeatures];
        int byteCount = 0;
        int shortCount = 0;
        int floatCount = 0;
        
        for( int i=0; i<numFeatures; i++ ) {
            
            String featName  = featDef.getFeatureName(i);
            String featValue = "0";
            
            if ( featDef.isByteFeature(featName) || featDef.isShortFeature(featName) ) {
                if( domElement.hasAttribute( featName ) ) {
                    featValue = domElement.getAttribute(featName);
                }
                
                boolean hasFeature = featDef.hasFeatureValue(featName, featValue);
                if( !hasFeature ) featValue = "0";
                
                if ( featDef.isByteFeature(i) ) {
                    byteFeatures[byteCount++]   = featDef.getFeatureValueAsByte(i, featValue);
                }
                else if ( featDef.isShortFeature(i) ) {
                    shortFeatures[shortCount++] = featDef.getFeatureValueAsShort(i, featValue);
                }
            }
            else {
                if( domElement.hasAttribute( "meaning" ) ) {
                    featValue = domElement.getAttribute("meaning");
                }
                //float contFeature = getMeaningScaleValue ( featName, featValue );
                floatFeatures[floatCount++] = getMeaningScaleValue ( featName, featValue );
            }
        }
        
        FeatureVector newFV = featDef.toFeatureVector(0, byteFeatures, shortFeatures, floatFeatures);
        
        String name = "0";
        if( domElement.hasAttribute( "name" ) ) {
            name = domElement.getAttribute("name");
        }
        
        Target newTarget = new Target(name, domElement);
        newTarget.setFeatureVector(newFV);
                 
        return newTarget;
    }
    
    /**
     * get value on meaning scale as a float value
     * @param featureName
     * @param meaningAttribute
     * @return
     */
    private float getMeaningScaleValue(String featureName, String meaningAttribute) {
        
        String[] categories = meaningAttribute.split("\\s+");
        List<String> categoriesList = Arrays.asList(categories);
        
        if( "anger".equals(featureName) && categoriesList.contains("anger") ) {
            return 5;
        }
        else if( "sadness".equals(featureName) && categoriesList.contains("sadness") ) {
            return 5;
        }
        else if( "amusement".equals(featureName) && categoriesList.contains("amusement") ) {
            return 5;
        }
        else if( "happiness".equals(featureName) && categoriesList.contains("happiness") ) {
            return 5;
        }
        else if( "contempt".equals(featureName) && categoriesList.contains("contempt") ) {
            return 5;
        }
        else if( "certain".equals(featureName) && categoriesList.contains("uncertain") ) {
            return -2;
        }
        else if( "certain".equals(featureName) && categoriesList.contains("certain") ) {
            return 2;
        }
        else if( "agreeing".equals(featureName) && categoriesList.contains("disagreeing") ) {
            return -2;
        }
        else if( "agreeing".equals(featureName) && categoriesList.contains("agreeing") ) {
            return 2;
        }
        else if( "interested".equals(featureName) && categoriesList.contains("uninterested") ) {
            return -2;
        }
        else if( "interested".equals(featureName) && categoriesList.contains("interested") ) {
            return 2;
        }
        else if( "anticipation".equals(featureName) && categoriesList.contains("low-anticipation") ) {
            return -2;
        }
        else if( "anticipation".equals(featureName) && categoriesList.contains("anticipation") ) {
            return 2;
        }
        else if( "anticipation".equals(featureName) && categoriesList.contains("high-anticipation") ) {
            return 2;
        }
        else if( "solidarity".equals(featureName) && categoriesList.contains("solidarity") ) {
            return 5;
        }
        else if( "solidarity".equals(featureName) && categoriesList.contains("low-solidarity") ) {
            return 1;
        }
        else if( "solidarity".equals(featureName) && categoriesList.contains("high-solidarity") ) {
            return 5;
        }
        else if( "antagonism".equals(featureName) && categoriesList.contains("antagonism") ) {
            return 5;
        }
        else if( "antagonism".equals(featureName) && categoriesList.contains("high-antagonism") ) {
            return 5;
        }
        else if( "antagonism".equals(featureName) && categoriesList.contains("low-antagonism") ) {
            return 1;
        }
        
        return Float.NaN;
    }
    
 
    /**
     * List the possible vocalization names that are available for the given voice.
     * These values can be used in the "name" attribute of the vocalization tag.
     * @return an array of Strings, each string containing one unique vocalization name.
     */
    public String[] listAvailableVocalizations() {
        assert featureDefinition.hasFeature("name");
        int nameIndex = featureDefinition.getFeatureIndex("name");
        return featureDefinition.getPossibleValues(nameIndex);
    }
    
    /**
     * 
     * @author sathish
     *
     */
    class ImposeIntonationData implements Comparable<ImposeIntonationData>
    {
        int targetUnitIndex;
        int sourceUnitIndex;
        double distance;
        
        ImposeIntonationData(int sourceUnitIndex, int targetUnitIndex, double distance) {
            this.sourceUnitIndex = sourceUnitIndex;
            this.targetUnitIndex = targetUnitIndex;
            this.distance = distance;
        }
        
        public int compareTo(ImposeIntonationData other) {
            if (distance == other.distance) return 0;
            if (distance < other.distance) return -1;
            return 1;
        }
        
        public boolean equals(Object dc)
        {
            if (!(dc instanceof ImposeIntonationData)) return false;
            ImposeIntonationData other = (ImposeIntonationData) dc;
            if (distance == other.distance) return true;
            return false;
        }
    }
    
    /**
     * 
     * @author sathish
     *
     */
    class VocalizationCost implements Comparable<VocalizationCost>
    {
        int unitIndex;
        double cost;
        
        VocalizationCost(int unitIndex, double cost) {
            this.unitIndex = unitIndex;
            this.cost = cost;
        }
        
        public int compareTo(VocalizationCost other) {
            if (cost == other.cost) return 0;
            if (cost < other.cost) return -1;
            return 1;
        }
        
        public boolean equals(Object dc)
        {
            if (!(dc instanceof VocalizationCost)) return false;
            VocalizationCost other = (VocalizationCost) dc;
            if (cost == other.cost) return true;
            return false;
        }
        
        @Override
        public String toString() {
            return unitIndex+" "+cost;
        }
    }

}

