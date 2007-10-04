/**
 * Copyright 2000-2007 DFKI GmbH.
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

package de.dfki.lt.signalproc.util;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import de.dfki.lt.mary.util.MaryAudioUtils;

/**
 * 
 * Audio Convertion Utilities
 * @author Sathish Chandra Pammi
 *
 */

public class AudioConverterUtils {

  
    /**
     * Default Stereo to Mono Converter
     * @param AudioInputStream 
     * @return AudioInputStream
     */
    public static AudioInputStream ConvertStereoToMono(AudioInputStream ais){
        
        return ConvertStereoToMono(ais, AudioPlayer.STEREO);
        
    }
    /**
     * Stereo to Mono Converter, flexibility to choose Left or Right channel 
     * @param AudioInputStream 
     * @param Channel ( Ex: AudioPlayer.LEFT_ONLY, AudioPlayer.RIGHT_ONLY )
     * @return AudioInputStream
     */
    public static AudioInputStream ConvertStereoToMono(AudioInputStream ais, int mode){

        return new MonoAudioInputStream(ais, mode);
        
    }
    
    /**
     * 24-Bit Audio to  16-bit Audio converter 
     * @param AudioInputStream 
     * @return AudioInputStream
     * @throws Exception
     */
    
    public static AudioInputStream ConvertBit24ToBit16(AudioInputStream ais) throws Exception {
        
        int BitsPerSample = 24;
        int TargetBitsPerSample = 16;
        
        float samplingRate = ais.getFormat().getSampleRate();
        int noOfBitsPerSample = ais.getFormat().getSampleSizeInBits();
        int channels = ais.getFormat().getChannels();
        
        if(noOfBitsPerSample != BitsPerSample){
            throw new Exception("24-Bit Audio Data Expected. But given Audio Data is "+noOfBitsPerSample+"-Bit data");
        }
        
        double[] signal = new AudioDoubleDataSource(ais).getAllData();
        int[] sample =  new int[signal.length];
        
        for(int i=0; i<signal.length; i++ ){
            sample[i] = (int) Math.round((signal[i] * 8388606.0)); // De-Normalisation (24-bit) 
        }
       
        int maxBitPos = 0;
        int LSB;
        
        for(int i=0;i<sample.length;i++){
        for(int j=BitsPerSample;j>=1;j--){
            LSB = sample[i] >> j;
            if(LSB != 0 && LSB != -1){  // Condition for Positive and Negative Numbers (Since 2's Complement)
                if(maxBitPos < j) maxBitPos = j;
                break;
             }
           }
        }
        
        int shiftBits = maxBitPos - TargetBitsPerSample + 2; // need to change 24 to 16 

        for(int i=0; (shiftBits>0 && i<sample.length); i++ ){
            sample[i] = sample[i] >> shiftBits;
        }
        
        int currentPos = 0 ; // off
        int nRead = sample.length;
        byte[] b = new byte[2*sample.length];
        int MAX_AMPLITUD = 32767;
        boolean bigEndian = true;
        
        // Conversion to BYTE ARRAY 
        for (int i=0; i<nRead; i++, currentPos+=2) {
            
            int samp = sample[i];
            if (samp > MAX_AMPLITUD || samp < -MAX_AMPLITUD) {
                System.err.println("Warning: signal amplitude out of range: "+samp);
            }
            byte hibyte = (byte) (samp >> 8);
            byte lobyte = (byte) (samp & 0xFF);
            if (!bigEndian) {
                b[currentPos] = lobyte;
                b[currentPos+1] = hibyte;
            } else {
                b[currentPos] = hibyte;
                b[currentPos+1] = lobyte;
            }
       }
        
        
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        boolean signed = true; //true,false
       
        AudioFormat af = new AudioFormat(
        samplingRate,
        TargetBitsPerSample,
        channels,
        signed,
        bigEndian);

        long lengthInSamples = b.length / (TargetBitsPerSample/8);
       
        return new AudioInputStream(bais, af, lengthInSamples);
    }

    
}



