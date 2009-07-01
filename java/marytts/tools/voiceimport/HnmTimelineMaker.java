/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package marytts.tools.voiceimport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.unitselection.data.HnmDatagram;
import marytts.unitselection.data.MCepDatagram;
import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;


/**
 * The mcepTimelineMaker class takes a database root directory and a list of basenames,
 * and converts the related wav files into a hnm timeline in Mary format.
 * 
 * @author Oytun T&uumlrk
 */
public class HnmTimelineMaker extends VoiceImportComponent
{ 
    protected DatabaseLayout db = null;
    protected int percent = 0;
    
    public final String HNMTIMELINE = "HnmTimelineMaker.hnmTimeline";
    
    public String getName(){
        return "HnmTimelineMaker";
    }
    
    public SortedMap<String,String> getDefaultProps(DatabaseLayout theDb){
        this.db = theDb;
        if (props == null)
        {
            HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
            HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();
            
            props = new TreeMap<String, String>();

            props.put(HNMTIMELINE, db.getProp(db.FILEDIR)
                    +"timeline_hnm"+db.getProp(db.MARYEXT));
    
            props.put("HnmTimelineMaker.noiseModel", String.valueOf(analysisParams.noiseModel));
            props.put("HnmTimelineMaker.numFiltStages", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.numFilteringStages));
            props.put("HnmTimelineMaker.medianFiltLen", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.medianFilterLength));
            props.put("HnmTimelineMaker.maFiltLen", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.movingAverageFilterLength));
            props.put("HnmTimelineMaker.cumAmpTh", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.cumulativeAmpThreshold));
            props.put("HnmTimelineMaker.maxAmpTh", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumAmpThresholdInDB));
            props.put("HnmTimelineMaker.harmDevPercent", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.harmonicDeviationPercent));
            props.put("HnmTimelineMaker.sharpPeakAmpDiff", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.sharpPeakAmpDiffInDB));
            props.put("HnmTimelineMaker.minHarmonics", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.minimumTotalHarmonics));
            props.put("HnmTimelineMaker.maxHarmonics", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics));
            props.put("HnmTimelineMaker.minVoicedFreq", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.minimumVoicedFrequencyOfVoicing));
            props.put("HnmTimelineMaker.maxVoicedFreq", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumVoicedFrequencyOfVoicing));
            props.put("HnmTimelineMaker.maxFreqVoicingFinalShift", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumFrequencyOfVoicingFinalShift));
            props.put("HnmTimelineMaker.neighsPercent", String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.neighsPercent));
            props.put("HnmTimelineMaker.harmCepsOrder", String.valueOf(analysisParams.harmonicPartCepstrumOrder));
            props.put("HnmTimelineMaker.regCepWarpMethod", String.valueOf(analysisParams.regularizedCepstrumWarpingMethod));
            props.put("HnmTimelineMaker.regCepsLambda", String.valueOf(analysisParams.regularizedCepstrumLambdaHarmonic));
            props.put("HnmTimelineMaker.noiseLpOrder", String.valueOf(analysisParams.noisePartLpOrder));
            props.put("HnmTimelineMaker.preCoefNoise", String.valueOf(analysisParams.preemphasisCoefNoise));
            props.put("HnmTimelineMaker.hpfBeforeNoiseAnalysis", String.valueOf(analysisParams.hpfBeforeNoiseAnalysis));
            props.put("HnmTimelineMaker.harmNumPer", String.valueOf(analysisParams.numPeriodsHarmonicsExtraction));
            props.put("HnmTimelineMaker.hpfAfterNoiseSynth", String.valueOf(synthesisParams.hpfAfterNoiseSynthesis));
        }
        
        return props;
    }
    
    protected void setupHelp(){         
        props2Help = new TreeMap<String, String>();
        
        props2Help.put(HNMTIMELINE,"file containing all hnm noise waveform files. Will be created by this module");  
        
        props2Help.put("HnmTimelineMaker.noiseModel", "Noise model: 1=WAVEFORM, 2=LPC, Default=1");
        props2Help.put("HnmTimelineMaker.numFiltStages", "Number of filtering stages to smooth out maximum frequency of voicing curve: Range={0, 1, 2, 3, 4, 5}. 0 means no smoothing. Default=2");
        props2Help.put("HnmTimelineMaker.medianFiltLen", "Length of median filter for smoothing the maximum frequency of voicing  curve: Range={4, 8, 12, 16, 20}, Default=12");
        props2Help.put("HnmTimelineMaker.maFiltLen", "Length of moving average filter for smoothing the maximum frequency of voicing  curve: Range={4, 8, 12, 16, 20}, Default=12");
        props2Help.put("HnmTimelineMaker.cumAmpTh", "Cumulative amplitude threshold [linear scale] for harmonic band voicing detection; decrease to increase max. freq. of voicing values. Range=[0.1, 10.0], Default=2.0");
        props2Help.put("HnmTimelineMaker.maxAmpTh", "Maximum amplitude threshold [in DB] for harmonic band voicing detection. Decrease to increase max. freq. of voicing values. Range=[0.0, 20.0], Default=13.0");
        props2Help.put("HnmTimelineMaker.harmDevPercent", "Percent deviation allowed for harmonic peak. Increase to increase max. freq. of voicing values. Range=[0.0, 100.0], Default=20.0");
        props2Help.put("HnmTimelineMaker.sharpPeakAmpDiff", "Minimum amplitude difference [in DB] to declare an isolated peak as harmonic. Decrease to increase max. freq. of voicing values. range=[0.0, 20.0], Default=12.0");
        props2Help.put("HnmTimelineMaker.minHarmonics", "Minimum total harmonics allowed in voiced regions. Range=[0, 100], Default=0");
        props2Help.put("HnmTimelineMaker.maxHarmonics", "Maximum total harmonics allowed in voiced regions. Range=[minHarmonics, 100], Default=100");
        props2Help.put("HnmTimelineMaker.minVoicedFreq", "Minimum voiced frequency for voiced regions [in Hz]. Range=[0.0, 0.5*samplingRate], Default=0");
        props2Help.put("HnmTimelineMaker.maxVoicedFreq", "Maximum voiced frequency for voiced regions [in Hz]. Range=Default=[minVoicedFreq, 0.5*samplingRate], Default=5000");
        props2Help.put("HnmTimelineMaker.maxFreqVoicingFinalShift", "Final amount of shift to be applied to the MWF curve [in Hz]. Range=[0.0, 0.5*samplingRate-maxVoicedFreq], Default=0");
        props2Help.put("HnmTimelineMaker.neighsPercent", "Percentage of samples that the harmonic peak needs to be larger than within a band. Decrease to increase max. freq. of voicing values. Range=[0.0, 100.0], Default=50");
        props2Help.put("HnmTimelineMaker.harmCepsOrder", "Cepstrum order to represent harmonic amplitudes. Increase to obtain better match with actual harmonic values. Range=[8, 40], Default=24");
        props2Help.put("HnmTimelineMaker.regCepWarpMethod", "Warping method for regularized cepstrum estimation. 1=POST_MEL, 2=PRE_BARK, Default=1");
        props2Help.put("HnmTimelineMaker.regCepsLambda", "Regularization term for cepstrum estimation. Increase to obtain smoother spectral match for harmonic amplitudes. However, this reduces the match with the actual amplitudes. Range=[0.0, 0.1], Default=1.0e-5");
        props2Help.put("HnmTimelineMaker.noiseLpOrder", "Linear prediction order for LPC noise part. Range=[8, 50], Default=12");
        props2Help.put("HnmTimelineMaker.preCoefNoise", "Pre-emphasis coefficient for linear prediction analysis of noise part. Range=[0.0, 0.99], Default=0.97");
        props2Help.put("HnmTimelineMaker.hpfBeforeNoiseAnalysis", "Remove lowpass frequency residual after harmonic subtraction? 0=NO, 1=YES, Default=1");
        props2Help.put("HnmTimelineMaker.harmNumPer", "Total periods for harmonic analysis. Range=[2.0, 4.0], Default=2");
        props2Help.put("HnmTimelineMaker.hpfAfterNoiseSynth", "Remove lowpass noise components after synthesis. 0=NO, 1=YES, Default=1");
    }
    
    /**
     *  Performs HNM analysis and writes the results to a single timeline file
     *
     */
    public boolean compute()
    {
        System.out.println("---- Importing Harmonics plus noise parameters\n\n");
        System.out.println("Base directory: " + db.getProp(db.ROOTDIR) + "\n");
        
        /* Export the basename list into an array of strings */
        String[] baseNameArray = bnl.getListAsArray();
        
        /* Prepare the output directory for the timelines if it does not exist */
        File timelineDir = new File(db.getProp(db.FILEDIR));
        
        
        try{
            /* 1) Determine the reference sampling rate as being the sample rate of the first encountered
             *    wav file */
            WavReader wav = new WavReader(db.getProp(db.ROOTDIR) + db.getProp(db.WAVDIR) 
                    + baseNameArray[0] + db.getProp(db.WAVEXT));
            int globSampleRate = wav.getSampleRate();
            System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz." );

            System.out.println("---- Performing HNM analysis..." );
            
            /* Make the file name */
            System.out.println( "Will create the hnm timeline in file [" 
                    + getProp(HNMTIMELINE) + "]." );
            
            /* An example of processing header: */
            Properties headerProps = new Properties();

            headerProps.setProperty("HnmTimelineMaker.noiseModel", props.get("HnmTimelineMaker.noiseModel"));
            headerProps.setProperty("HnmTimelineMaker.numFiltStages", props.get("HnmTimelineMaker.numFiltStages"));
            headerProps.setProperty("HnmTimelineMaker.medianFiltLen", props.get("HnmTimelineMaker.medianFiltLen"));
            headerProps.setProperty("HnmTimelineMaker.maFiltLen", props.get("HnmTimelineMaker.maFiltLen"));
            headerProps.setProperty("HnmTimelineMaker.cumAmpTh", props.get("HnmTimelineMaker.cumAmpTh"));
            headerProps.setProperty("HnmTimelineMaker.maxAmpTh", props.get("HnmTimelineMaker.maxAmpTh"));
            headerProps.setProperty("HnmTimelineMaker.harmDevPercent", props.get("HnmTimelineMaker.harmDevPercent"));
            headerProps.setProperty("HnmTimelineMaker.sharpPeakAmpDiff", props.get("HnmTimelineMaker.sharpPeakAmpDiff"));
            headerProps.setProperty("HnmTimelineMaker.minHarmonics", props.get("HnmTimelineMaker.minHarmonics"));
            headerProps.setProperty("HnmTimelineMaker.maxHarmonics", props.get("HnmTimelineMaker.maxHarmonics"));
            headerProps.setProperty("HnmTimelineMaker.minVoicedFreq", props.get("HnmTimelineMaker.minVoicedFreq"));
            headerProps.setProperty("HnmTimelineMaker.maxVoicedFreq", props.get("HnmTimelineMaker.maxVoicedFreq"));
            headerProps.setProperty("HnmTimelineMaker.maxFreqVoicingFinalShift", props.get("HnmTimelineMaker.maxFreqVoicingFinalShift"));
            headerProps.setProperty("HnmTimelineMaker.neighsPercent", props.get("HnmTimelineMaker.neighsPercent"));
            headerProps.setProperty("HnmTimelineMaker.harmCepsOrder", props.get("HnmTimelineMaker.harmCepsOrder"));
            headerProps.setProperty("HnmTimelineMaker.regCepWarpMethod", props.get("HnmTimelineMaker.regCepWarpMethod"));
            headerProps.setProperty("HnmTimelineMaker.regCepsLambda", props.get("HnmTimelineMaker.regCepsLambda"));
            headerProps.setProperty("HnmTimelineMaker.noiseLpOrder", props.get("HnmTimelineMaker.noiseLpOrder"));
            headerProps.setProperty("HnmTimelineMaker.preCoefNoise", props.get("HnmTimelineMaker.preCoefNoise"));
            headerProps.setProperty("HnmTimelineMaker.hpfBeforeNoiseAnalysis", props.get("HnmTimelineMaker.hpfBeforeNoiseAnalysis"));
            headerProps.setProperty("HnmTimelineMaker.harmNumPer", props.get("HnmTimelineMaker.harmNumPer"));
            headerProps.setProperty("HnmTimelineMaker.hpfAfterNoiseSynth", props.get("HnmTimelineMaker.hpfAfterNoiseSynth"));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            headerProps.store(baos, null);
            String processingHeader = baos.toString("latin1");
            
            /* Instantiate the TimelineWriter: */
            TimelineWriter hnmTimeline = new TimelineWriter( getProp(HNMTIMELINE), processingHeader, globSampleRate, 0.01 );
            
            //TO DO: Update these paratemers according to props
            HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
            HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis = new HntmSynthesizerParams();
            
            analysisParams.noiseModel = Integer.valueOf(props.get("HnmTimelineMaker.noiseModel"));
            analysisParams.hnmPitchVoicingAnalyzerParams.numFilteringStages = Integer.valueOf(props.get("HnmTimelineMaker.numFiltStages"));
            analysisParams.hnmPitchVoicingAnalyzerParams.medianFilterLength = Integer.valueOf(props.get("HnmTimelineMaker.medianFiltLen"));
            analysisParams.hnmPitchVoicingAnalyzerParams.movingAverageFilterLength = Integer.valueOf(props.get("HnmTimelineMaker.maFiltLen"));
            analysisParams.hnmPitchVoicingAnalyzerParams.cumulativeAmpThreshold = Double.valueOf(props.get("HnmTimelineMaker.cumAmpTh"));
            analysisParams.hnmPitchVoicingAnalyzerParams.maximumAmpThresholdInDB = Double.valueOf(props.get("HnmTimelineMaker.maxAmpTh"));
            analysisParams.hnmPitchVoicingAnalyzerParams.harmonicDeviationPercent = Double.valueOf(props.get("HnmTimelineMaker.harmDevPercent"));
            analysisParams.hnmPitchVoicingAnalyzerParams.sharpPeakAmpDiffInDB = Double.valueOf(props.get("HnmTimelineMaker.sharpPeakAmpDiff"));
            analysisParams.hnmPitchVoicingAnalyzerParams.minimumTotalHarmonics = Integer.valueOf(props.get("HnmTimelineMaker.minHarmonics"));
            analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics = Integer.valueOf(props.get("HnmTimelineMaker.maxHarmonics"));
            analysisParams.hnmPitchVoicingAnalyzerParams.minimumVoicedFrequencyOfVoicing = Float.valueOf(props.get("HnmTimelineMaker.minVoicedFreq"));
            analysisParams.hnmPitchVoicingAnalyzerParams.maximumVoicedFrequencyOfVoicing = Float.valueOf(props.get("HnmTimelineMaker.maxVoicedFreq"));
            analysisParams.hnmPitchVoicingAnalyzerParams.maximumFrequencyOfVoicingFinalShift = Float.valueOf(props.get("HnmTimelineMaker.maxFreqVoicingFinalShift"));
            analysisParams.hnmPitchVoicingAnalyzerParams.neighsPercent = Double.valueOf(props.get("HnmTimelineMaker.neighsPercent"));
            analysisParams.harmonicPartCepstrumOrder = Integer.valueOf(props.get("HnmTimelineMaker.harmCepsOrder"));
            analysisParams.regularizedCepstrumWarpingMethod = Integer.valueOf(props.get("HnmTimelineMaker.regCepWarpMethod"));
            analysisParams.regularizedCepstrumLambdaHarmonic = Double.valueOf(props.get("HnmTimelineMaker.regCepsLambda"));
            analysisParams.noisePartLpOrder = Integer.valueOf(props.get("HnmTimelineMaker.noiseLpOrder"));
            analysisParams.preemphasisCoefNoise = Float.valueOf(props.get("HnmTimelineMaker.preCoefNoise"));
            analysisParams.hpfBeforeNoiseAnalysis = Boolean.valueOf(props.get("HnmTimelineMaker.hpfBeforeNoiseAnalysis"));
            analysisParams.numPeriodsHarmonicsExtraction = Float.valueOf(props.get("HnmTimelineMaker.harmNumPer"));
            synthesisParamsBeforeNoiseAnalysis.hpfAfterNoiseSynthesis = Boolean.valueOf(props.get("HnmTimelineMaker.hpfAfterNoiseSynth"));
            //
            
            /* 2) Write the datagrams and feed the index */
            
            long totalTime = 0l;
            long numDatagrams = 0l; // Total number of hnm datagrams in the timeline file
            
            /* For each wav file: */
            for ( int n = 0; n < baseNameArray.length; n++ ) 
            {
                percent = 100*n/baseNameArray.length;
                /* - open+load */
                System.out.println( baseNameArray[n] );
                wav = new WavReader(db.getProp(db.ROOTDIR) + db.getProp(db.WAVDIR) + baseNameArray[n] + db.getProp(db.WAVEXT));
                short[] wave = wav.getSamples();
                
                PitchReaderWriter f0 = new PitchReaderWriter(db.getProp(db.ROOTDIR) + db.getProp(db.PTCDIR) + baseNameArray[n] + db.getProp(db.PTCEXT));

                HntmAnalyzer ha = new HntmAnalyzer();
                HntmSpeechSignal hnmSignal = ha.analyze(wave, wav.getSampleRate(), f0, null, analysisParams, synthesisParamsBeforeNoiseAnalysis); 
                
                /* - For each frame in the hnm modeled speech signal: */
                int frameStart = 0;
                int frameEnd = 0;
                int duration = 0;
                long localTime = 0l;
                //float tAnalysisInSeconds = 0.0f;
                for ( int i = 0; i < hnmSignal.frames.length; i++ ) 
                {
                    //tAnalysisInSeconds += hnmSignal.frames[i].deltaAnalysisTimeInSeconds;
                    
                    /* Get the datagram duration */
                    frameStart = frameEnd;
                    if (i<hnmSignal.frames.length-1)
                    {
                        //frameEnd = (int)( (double)(tAnalysisInSeconds+hnmSignal.frames[i+1].deltaAnalysisTimeInSeconds) * (double)(globSampleRate) );
                        frameEnd = (int)( (double)(hnmSignal.frames[i+1].tAnalysisInSeconds) * (double)(globSampleRate) );
                    }
                    else
                        frameEnd = (int)( (double)hnmSignal.originalDurationInSeconds * (double)(globSampleRate) );
                    
                    duration = frameEnd - frameStart;
                    
                    /* Feed the datagram to the timeline */
                    hnmTimeline.feed( new HnmDatagram(duration, hnmSignal.frames[i]) , globSampleRate );
                    totalTime += duration;
                    localTime += duration;
                }
                
                numDatagrams += hnmSignal.frames.length;
            }
            
            System.out.println("---- Done." );
            
            /* 7) Print some stats and close the file */
            System.out.println( "---- hnm timeline result:");
            System.out.println( "Number of files scanned: " + baseNameArray.length );
            System.out.println( "Total duration: [" + totalTime + "] samples / [" + ((double)(totalTime) / (double)(globSampleRate)) + "] seconds." );
            System.out.println( "Number of frames: [" + numDatagrams + "]." );
            System.out.println( "Size of the index: [" + hnmTimeline.idx.getNumIdx() + "] ("
                    + (hnmTimeline.idx.getNumIdx() * 16) + " bytes, i.e. "
                    + ( (double)(hnmTimeline.idx.getNumIdx()) * 16.0 / 1048576.0) + " megs)." );
            System.out.println( "---- hnm timeline done.");
            
            hnmTimeline.close();
        }
        catch ( SecurityException e ) {
            System.err.println( "Error: you don't have write access to the target database directory." );
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
        
        return( true );
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
    
    public static void main(String[] args) throws Exception
    {
        System.setProperty("user.dir", "D:\\hnmTimelineTest");
        VoiceImportComponent vic  =  new HnmTimelineMaker();
        DatabaseLayout db = new DatabaseLayout(vic);
        vic.compute();
    }
}
