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

package de.dfki.lt.signalproc.process;

import java.io.File;
import java.io.FileReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.ESTTextfileDoubleDataSource;
import de.dfki.lt.signalproc.util.LabelfileDoubleDataSource;
import de.dfki.lt.signalproc.util.PraatTextfileDoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.window.Window;

public class PitchLSFInterpolator
{

    public static void main(String[] args) throws Exception
    {
        long startTime = System.currentTimeMillis();
        double r = Double.parseDouble(System.getProperty("r", "0.5"));
        String file1 = null;
        String pm1 = null;
        String file2 = null;
        String pm2 = null;
        DoubleDataSource label1 = null;
        DoubleDataSource label2 = null;
        if (args.length == 4) {
            file1 = args[0];
            pm1 = args[1];
            file2 = args[2];
            pm2 = args[3];
        } else if (args.length == 6) {
            file1 = args[0];
            pm1 = args[1];
            label1 = new LabelfileDoubleDataSource(new FileReader(args[2]));
            file2 = args[3];
            pm2 = args[4];
            label2 = new LabelfileDoubleDataSource(new FileReader(args[5]));
            // Safety check: verify that we have the same number of labels in both files
            double[] labelData1 = label1.getAllData();
            double[] labelData2 = label2.getAllData();
            if (labelData1.length != labelData2.length) {
                System.err.println("Warning: Number of labels is different!");
                System.err.println(args[2]+":");
                System.err.println(FileUtils.getFileAsString(new File(args[2]), "ASCII"));
                System.err.println(args[5]+":");
                System.err.println(FileUtils.getFileAsString(new File(args[5]), "ASCII"));
            } // but continue
            label1 = new BufferedDoubleDataSource(labelData1);
            label2 = new BufferedDoubleDataSource(labelData2);
        } else {
            System.out.println("Usage: java [-Dr=<mixing ratio> de.dfki.lt.signalproc.process.PitchLSFInterpolator signal.wav signal.pm [signal.lab] other.wav other.pm [other.lab]");
            System.out.println("where");
            System.out.println("    <mixing ratio> is a value between 0.0 and 1.0 indicating how much of \"other\" is supposed to be mixed into \"signal\"");
            System.exit(1);
        }

        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(file1));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pm1));
        AudioInputStream otherAudio = AudioSystem.getAudioInputStream(new File(file2));
        DoubleDataSource otherSource = new AudioDoubleDataSource(otherAudio);
        DoubleDataSource otherPitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pm2));
        int predictionOrder = Integer.getInteger("signalproc.lpcanalysisresynthesis.predictionorder", 20).intValue();
        FramewiseMerger foas = new FramewiseMerger(signal, pitchmarks, samplingRate, label1,
                otherSource, otherPitchmarks, samplingRate, label2, 
                new LSFInterpolator(predictionOrder, r));
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
        String outFileName = file1.substring(0, file1.length()-4) + "_" + file2.substring(file2.lastIndexOf("\\")+1, file2.length()-4)+"_"+r+"_ps.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        long endTime = System.currentTimeMillis();
        int audioDuration = (int) (AudioSystem.getAudioFileFormat(new File(file1)).getFrameLength() / (double)samplingRate * 1000);
        System.out.println("Pitch-synchronous LSF-based interpolatin took "+ (endTime-startTime) + " ms for "+ audioDuration + " ms of audio");
        
    }

}
