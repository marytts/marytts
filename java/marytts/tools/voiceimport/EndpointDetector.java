/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.EnergyAnalyser;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;



/**
 * Identify and Remove End-ponints (intitial and final silences) from
 * given set of wave files.
 * @author Sathish and Oytun
 *  
 */
public class EndpointDetector extends VoiceImportComponent
{
    protected File textDir;
    protected File inputWavDir;
    protected File outputWavDir;
    protected String waveExt = ".wav";
    private BasenameList bnlist;

    protected DatabaseLayout db = null;
    protected int percent = 0;

    public String INPUTWAVDIR = "EndpointDetector.inputWaveDirectory";
    public String OUTPUTWAVDIR = "EndpointDetector.outputWaveDirectory";
    public String ENERGYBUFFERLENGTH = "EndpointDetector.energyBufferLength";
    public String SPEECHSTARTLIKELIHOOD = "EndpointDetector.speechStartLikelihood";
    public String SPEECHENDLIKELIHOOD = "EndpointDetector.speechEndLikelihood";
    public String SHIFTFROMMINIMUMENERGYCENTER = "EndpointDetector.shiftFromMinimumEnergyCenter";
    public String NUMENERGYCLUSTERS = "EndpointDetector.numEnergyClusters";
    public String MINIMUMSTARTSILENCEINSECONDS = "EndpointDetector.minimumStartSilenceInSeconds";
    public String MINIMUMENDSILENCEINSECONDS = "EndpointDetector.minimumEndSilenceInSeconds";

    public String getName(){
        return "EndpointDetector";
    }

    public void initialiseComp()
    {     

    }

    public SortedMap<String,String> getDefaultProps(DatabaseLayout theDb){
        this.db = theDb;
        if (props == null){
            props = new TreeMap<String, String>();
            props.put(INPUTWAVDIR, db.getProp(db.ROOTDIR)
                    +"inputwav"
                    +System.getProperty("file.separator"));

            props.put(OUTPUTWAVDIR, db.getProp(db.ROOTDIR)
                    +"outputwav"
                    +System.getProperty("file.separator"));

            props.put(ENERGYBUFFERLENGTH, "20");
            props.put(SPEECHSTARTLIKELIHOOD, "0.1");
            props.put(SPEECHENDLIKELIHOOD, "0.1");
            props.put(SHIFTFROMMINIMUMENERGYCENTER, "0.0");
            props.put(NUMENERGYCLUSTERS, "4");
            props.put(MINIMUMSTARTSILENCEINSECONDS, "1.0");
            props.put(MINIMUMENDSILENCEINSECONDS, "1.0");
        } 
        return props;
    }

    protected void setupHelp()
    {
        props2Help = new TreeMap<String, String>();
        
        props2Help.put(INPUTWAVDIR, "input wave files directory."); 

        props2Help.put(OUTPUTWAVDIR, "output directory to store initial-end silences removed wave files." 
                + "Will be created if it does not exist");

        props2Help.put(ENERGYBUFFERLENGTH, "number of consecutive speech frames when searching for speech/silence start events" 
                + "Range [1, 1000], decrease to detect more events");
        
        props2Help.put(SPEECHSTARTLIKELIHOOD, "likelihood of speech starting event"
                + "Range [0.0,1.0], decrease to get more silence before speech segments");
        
        props2Help.put(SPEECHENDLIKELIHOOD, "likelihood of speech ending event"
                + "Range [0.0,1.0], decrease to get more silence after speech segments");
        
        props2Help.put(SHIFTFROMMINIMUMENERGYCENTER, "multiplied by lowest energy cluster mean to generate speech/silence energy threshold"
                + "Range [0.0,5.0], decrease to get more silence in speech segments");
        
        props2Help.put(NUMENERGYCLUSTERS, "number of energy clusters"
                + "Range [1,20], decrease to get more silence in speech segments");
        
        props2Help.put(MINIMUMSTARTSILENCEINSECONDS, "minimum silence in the beginning of the output files in seconds"
                + "Range [0.0,30.0], increase to get more silence in the beginning");
        
        props2Help.put(MINIMUMENDSILENCEINSECONDS, "minimum silence at the end of the output files in seconds"
                + "Range [0.0,30.0], increase to get more silence at the end");
    }


    public boolean compute() throws IOException
    {
        //      Check existance of input directory  
        inputWavDir = new File(getProp(INPUTWAVDIR));
        if (!inputWavDir.exists()){
            throw new Error("Could not find input Directory: "+ getProp(INPUTWAVDIR));
        }   

        // Check existance of output directory
        // if not exists, create a new directory
        outputWavDir = new File(getProp(OUTPUTWAVDIR));
        if (!outputWavDir.exists()){
            System.out.print(OUTPUTWAVDIR+" "+getProp(OUTPUTWAVDIR)
                    +" does not exist; ");
            if (!outputWavDir.mkdir()){
                throw new Error("Could not create OUTPUTWAVDIR");
            }
            System.out.print("Created successfully.\n");
        }

        // Automatically collect all ".wav" files from given directory
        bnlist = new BasenameList(inputWavDir+File.separator,waveExt);


        int energyBufferLength = Integer.valueOf(getProp(ENERGYBUFFERLENGTH));
        energyBufferLength = MathUtils.CheckLimits(energyBufferLength, 1, 1000);
        
        double speechStartLikelihood = Double.valueOf(getProp(SPEECHSTARTLIKELIHOOD));
        speechStartLikelihood = MathUtils.CheckLimits(speechStartLikelihood, 0.0, 1.0);
        
        double speechEndLikelihood = Double.valueOf(getProp(SPEECHENDLIKELIHOOD));
        speechEndLikelihood = MathUtils.CheckLimits(speechEndLikelihood, 0.0, 1.0);
        
        double shiftFromMinimumEnergyCenter = Double.valueOf(getProp(SHIFTFROMMINIMUMENERGYCENTER));
        shiftFromMinimumEnergyCenter = MathUtils.CheckLimits(shiftFromMinimumEnergyCenter, 0.0, 5.0);
        
        int numClusters = Integer.valueOf(getProp(NUMENERGYCLUSTERS));
        numClusters = MathUtils.CheckLimits(numClusters, 1, 20);
        
        double minimumStartSilenceInSeconds = Double.valueOf(getProp(MINIMUMSTARTSILENCEINSECONDS));
        minimumStartSilenceInSeconds = MathUtils.CheckLimits(minimumStartSilenceInSeconds, 0.0, 30.0);
        
        double minimumEndSilenceInSeconds = Double.valueOf(getProp(MINIMUMENDSILENCEINSECONDS));
        minimumEndSilenceInSeconds = MathUtils.CheckLimits(minimumEndSilenceInSeconds, 0.0, 30.0);
        //

        System.out.println( "Removing endpoints for "+ bnlist.getLength() + " wave files" );

        for (int i=0; i<bnlist.getLength(); i++) 
        {
            percent = 100*i/bnlist.getLength();
            String inputFile = inputWavDir + File.separator + bnlist.getName(i) + waveExt;
            String outputFile = outputWavDir + File.separator + bnlist.getName(i) + waveExt;

            removeEndpoints(inputFile, outputFile,
                    energyBufferLength, speechStartLikelihood, speechEndLikelihood, shiftFromMinimumEnergyCenter, numClusters,
                    minimumStartSilenceInSeconds, minimumEndSilenceInSeconds);

            System.out.println( "    " + bnlist.getName(i) );
        }

        System.out.println("...Done.");

        return true;
    }


    /**
     * Removes endpoints from given file
     * @param inputFile 
     * @param outputFile
     * @throws IOException
     * @throws  
     */
    public void removeEndpoints(String inputFile, String outputFile,
            int energyBufferLength,
            double speechStartLikelihood,
            double speechEndLikelihood,
            double shiftFromMinimumEnergyCenter,
            int numClusters,
            double minimumStartSilenceInSeconds,
            double minimumEndSilenceInSeconds) throws IOException
            {
        /*
         * Add corresponding module to remove endpoints
         * 1. identify and remove end points
         * 2. make sure at least some desired amount of silence in the beginning and at the end
         * 3. store as output wavefile 
         */

        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(new File(inputFile));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (ais!=null)
        {
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
            //double[][] speechStretches = ea.getSpeechStretches();

            double[][] speechStretches = ea.getSpeechStretchesUsingEnergyHistory(energyBufferLength, speechStartLikelihood, speechEndLikelihood, 
                    shiftFromMinimumEnergyCenter, numClusters);

            ais.close();

            try {
                ais = AudioSystem.getAudioInputStream(new File(inputFile));
            } catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            signal = new AudioDoubleDataSource(ais);
            double[] x = signal.getAllData();

            ais.close();

            if (speechStretches.length==0)
            {
                System.out.println("No segments detected in " + inputFile + " copying whole file...");

                DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), ais.getFormat());
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
            }
            else
            {
                int numStretches = speechStretches.length;
                int speechStartIndex = (int)(samplingRate*speechStretches[0][0]);
                int speechEndIndex = (int)(samplingRate*speechStretches[numStretches-1][1]);

                //Check if sufficient silence exists in the input waveform, if not generate as required
                int silStartRequired = Math.max(0, (int)(samplingRate*minimumStartSilenceInSeconds));
                int silStartLen = 0;
                if (speechStartIndex<silStartRequired)
                {
                    silStartLen = silStartRequired-speechStartIndex;
                    speechStartIndex = 0;
                }
                else
                    speechStartIndex -= silStartRequired;

                double[] silStart = null;
                if (silStartLen>0)
                    silStart = SignalProcUtils.getWhiteNoise(silStartLen, 1e-20);

                int silEndRequired = Math.max(0, (int)(samplingRate*minimumEndSilenceInSeconds));
                int silEndLen = 0;
                if (x.length-speechEndIndex<silEndRequired)
                {
                    silEndLen = silEndRequired-(x.length-speechEndIndex);
                    speechEndIndex = x.length-1;
                }
                else
                    speechEndIndex += silEndRequired;

                double[] silEnd = null;
                if (silEndLen>0)
                    silEnd = SignalProcUtils.getWhiteNoise(silEndLen, 1e-20);
                //

                double[] y = null;
                if (speechEndIndex-speechStartIndex+silStartLen+silEndLen>0)
                    y = new double[speechEndIndex-speechStartIndex+silStartLen+silEndLen];
                else
                    throw new Error("No output samples to write for " + inputFile);

                int start = 0;
                if (silStartLen>0)
                {
                    System.arraycopy(silStart, 0, y, start, silStartLen);
                    start += silStartLen;
                }

                if (speechEndIndex-speechStartIndex>0)
                {
                    System.arraycopy(x, speechStartIndex, y, start, speechEndIndex-speechStartIndex);
                    start += (speechEndIndex-speechStartIndex);
                }

                if (silEndLen>0)
                {
                    System.arraycopy(silEnd, 0, y, start, silEndLen);
                    start += silEndLen;
                }

                DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), ais.getFormat());
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
            }
        }
        else
            throw new Error("Cannot open input file " + inputFile);
            }

    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }

}

