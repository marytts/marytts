/**
 * Copyright 2000-2007 DFKI GmbH.
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
package marytts.util.data.audio;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.filter.LowPassFilter;
import marytts.util.data.BufferedDoubleDataSource;

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
    public static AudioInputStream convertStereoToMono(AudioInputStream ais){
        
        return convertStereoToMono(ais, AudioPlayer.STEREO);
        
    }
    /**
     * Stereo to Mono Converter, flexibility to choose Left or Right channel 
     * @param AudioInputStream 
     * @param Channel ( Ex: AudioPlayer.LEFT_ONLY, AudioPlayer.RIGHT_ONLY )
     * @return AudioInputStream
     */
    public static AudioInputStream convertStereoToMono(AudioInputStream ais, int mode){
        return new MonoAudioInputStream(ais, mode);
    }
 
    
    /**
     * 24-Bit Audio to  16-bit Audio converter 
     * @param AudioInputStream 
     * @return AudioInputStream
     * @throws Exception
     */
    
    public static AudioInputStream convertBit24ToBit16(AudioInputStream ais) throws Exception {
        
        int bitsPerSample = 24;
        int targetBitsPerSample = 16;
        
        int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
        if(noOfbitsPerSample != bitsPerSample){
            throw new Exception("24-Bit Audio Data Expected. But given Audio Data is "+noOfbitsPerSample+"-Bit data");
        }
        if(ais.getFormat().getChannels() != 1){
            throw new Exception("Expected Audio type is Mono. But given Audio Data has "+ais.getFormat().getChannels()+" channels");
        }
        
        float samplingRate = ais.getFormat().getSampleRate();
        int channels = ais.getFormat().getChannels();
        int nBytes = ais.available();
        boolean bigEndian = ais.getFormat().isBigEndian();
        byte [] byteBuf = new byte[nBytes];
        int nBytesRead = ais.read(byteBuf, 0, nBytes); // Reading all Bytes at a time
        int currentPos = 0;
        int noOfSamples =  nBytes/3;
        int[] sample =  new int[noOfSamples];
        
        for (int i=0; i<nBytesRead; i+=3, currentPos++) {
            byte lobyte;
            byte midbyte;
            byte hibyte;
            if (!bigEndian) {
                lobyte = byteBuf[i];
                midbyte = byteBuf[i+1];
                hibyte = byteBuf[i+2];
            } else {
                lobyte = byteBuf[i+2];
                midbyte = byteBuf[i+1];
                hibyte = byteBuf[i];
            }
            sample[currentPos] = hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
       }
        
        int maxBitPos = 0;
        int valueAfterShift;
        
        for(int i=0;i<sample.length;i++){
        for(int j=bitsPerSample;j>=1;j--){
            valueAfterShift = Math.abs(sample[i]) >> j;
            if(valueAfterShift != 0){  
                if(maxBitPos < j) maxBitPos = j;
                break;
             }
           }
        }
        
        int shiftBits = maxBitPos - targetBitsPerSample + 2; // need to change 24 to 16 
        int sign;
        for(int i=0; (shiftBits>0 && i<sample.length); i++ ){
            if(sample[i] < 0) sign = -1;
            else sign = 1;
            sample[i] = sign * (Math.abs(sample[i]) >> shiftBits);
        }
        
        currentPos = 0 ; // off
        int nRead = sample.length;
        byte[] b = new byte[2*sample.length];
        int MAX_AMPLITUDE = 32767;
       
        
        // Conversion to BYTE ARRAY 
        for (int i=0; i<nRead; i++, currentPos+=2) {
            
            int samp = sample[i];
            if (samp > MAX_AMPLITUDE || samp < -MAX_AMPLITUDE) {
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
        targetBitsPerSample,
        channels,
        signed,
        bigEndian);

        long lengthInSamples = b.length / (targetBitsPerSample/8);
        
        return new AudioInputStream(bais, af, lengthInSamples);
    }

    /**
     * 24-Bit Audio to  16-bit Audio converter 
     * @param AudioInputStream 
     * @param shiftBits
     * @return AudioInputStream
     * @throws Exception
     */
    
    public static AudioInputStream convertBit24ToBit16(AudioInputStream ais, int shiftBits) throws Exception {
        
        int bitsPerSample = 24;
        int targetBitsPerSample = 16;
        
        int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
        if(noOfbitsPerSample != bitsPerSample){
            throw new Exception("24-Bit Audio Data Expected. But given Audio Data is "+noOfbitsPerSample+"-Bit data");
        }
        if(ais.getFormat().getChannels() != 1){
            throw new Exception("Expected Audio type is Mono. But given Audio Data has "+ais.getFormat().getChannels()+" channels");
        }
        // System.out.println("Shift bits: "+shiftBits);
        float samplingRate = ais.getFormat().getSampleRate();
        int channels = ais.getFormat().getChannels();
        int nBytes = ais.available();
        boolean bigEndian = ais.getFormat().isBigEndian();
        byte [] byteBuf = new byte[nBytes];
        int nBytesRead = ais.read(byteBuf, 0, nBytes); // Reading all Bytes at a time
        int currentPos = 0;
        int noOfSamples =  nBytes/3;
        int[] sample =  new int[noOfSamples];
        
        for (int i=0; i<nBytesRead; i+=3, currentPos++) {
            byte lobyte;
            byte midbyte;
            byte hibyte;
            if (!bigEndian) {
                lobyte = byteBuf[i];
                midbyte = byteBuf[i+1];
                hibyte = byteBuf[i+2];
            } else {
                lobyte = byteBuf[i+2];
                midbyte = byteBuf[i+1];
                hibyte = byteBuf[i];
            }
            sample[currentPos] = hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
       }
        
        int sign;
        for(int i=0; (shiftBits>0 && i<sample.length); i++ ){
            if(sample[i] < 0) sign = -1;
            else sign = 1;
            sample[i] = sign * (Math.abs(sample[i]) >> shiftBits);
        }
        
        currentPos = 0 ; // off
        int nRead = sample.length;
        byte[] b = new byte[2*sample.length];
        int MAX_AMPLITUDE = 32767;
       
        
        // Conversion to BYTE ARRAY 
        for (int i=0; i<nRead; i++, currentPos+=2) {
            
            int samp = sample[i];
            if (samp > MAX_AMPLITUDE || samp < -MAX_AMPLITUDE) {
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
        targetBitsPerSample,
        channels,
        signed,
        bigEndian);

        long lengthInSamples = b.length / (targetBitsPerSample/8);
        
        return new AudioInputStream(bais, af, lengthInSamples);
    }
    
    
 /**
  * Get samples in Integer Format (un-normalized) from AudioInputStream
  * @param ais
  * @return
  * @throws Exception
  */   
 public static int[] getSamples(AudioInputStream ais) throws Exception{
        
        
        int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
        float samplingRate = ais.getFormat().getSampleRate();
        int channels = ais.getFormat().getChannels();
        int nBytes = ais.available();
        boolean bigEndian = ais.getFormat().isBigEndian();
        byte [] byteBuf = new byte[nBytes];
        int nBytesRead = ais.read(byteBuf, 0, nBytes); // Reading all Bytes at a time
        int noOfBytesPerSample = noOfbitsPerSample / 8;
        
        int[] samples = new int[nBytes / noOfBytesPerSample]; 
        int currentPos = 0 ; // off
        
        if (noOfBytesPerSample == 1) {
            for (int i=0; i<nBytesRead; i++, currentPos++) {
                samples[currentPos] = (byteBuf[i]<<8) ;
            }
        
        } else if (noOfBytesPerSample == 2){ // 16 bit
            for (int i=0; i<nBytesRead; i+=2, currentPos++) {
                int sample;
                byte lobyte;
                byte hibyte;
                if (!bigEndian) {
                    lobyte = byteBuf[i];
                    hibyte = byteBuf[i+1];
                } else {
                    lobyte = byteBuf[i+1];
                    hibyte = byteBuf[i];
                }
                samples[currentPos] = hibyte<<8 | lobyte&0xFF;
           }
            
        } else { // noOfBytesPerSample == 3, i.e. 24 bit
            for (int i=0; i<nBytesRead; i+=3, currentPos++) {
                int sample;
                byte lobyte;
                byte midbyte;
                byte hibyte;
                if (!bigEndian) {
                    lobyte = byteBuf[i];
                    midbyte = byteBuf[i+1];
                    hibyte = byteBuf[i+2];
                } else {
                    lobyte = byteBuf[i+2];
                    midbyte = byteBuf[i+1];
                    hibyte = byteBuf[i];
                }
                samples[currentPos] = hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
            }
        }
        
        return samples; 
    }

 /**
  * Remove Low Frequency Noise (It will Remove Signal Content which is less than 50Hz)
  * @param ais
  * @return
  * @throws Exception
  */
 public static AudioInputStream removeLowFrequencyNoise(AudioInputStream ais, double cutoffFrequency, double transitionBandwidth)
 throws Exception
 {
        
        double[] samples = new AudioDoubleDataSource(ais).getAllData();
        float samplingRate = ais.getFormat().getSampleRate();
        double cutOff = cutoffFrequency / samplingRate;
        double transition = transitionBandwidth / samplingRate;
        HighPassFilter hFilter = new HighPassFilter(cutOff, transition);
        double[] fsamples = hFilter.apply(samples);
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(fsamples), ais.getFormat()); 
        return outputAudio; 
    }
 
 
 /**
  * DownSampling given Audio Input Stream 
  * @param ais
  * @param targetSamplingRate
  * @return
  * @throws Exception
  */   
 public static AudioInputStream downSampling(AudioInputStream ais, int targetSamplingRate) throws Exception{
        
        float currentSamplingRate = ais.getFormat().getSampleRate();
        if(targetSamplingRate >= currentSamplingRate){
            throw new Exception("Requested sampling rate "+targetSamplingRate+" is greater than or equal to Audio sampling rate "+currentSamplingRate);
        }
        int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
        int channels = ais.getFormat().getChannels();
        int nBytes = ais.available();
        
        boolean bigEndian = ais.getFormat().isBigEndian();
        double[] samples = new AudioDoubleDataSource(ais).getAllData();
        
        // **** Filtering to Remove Aliasing ****** 
        double filterCutof = 0.5 * (double) targetSamplingRate/currentSamplingRate;
        //System.out.println("filterCutof: "+filterCutof);
        LowPassFilter filter = new LowPassFilter(filterCutof);
        samples = filter.apply(samples);
        double duration = (double) samples.length / currentSamplingRate;
        //System.out.println("duration: "+duration);
        int newSampleLen = (int) Math.floor(duration * targetSamplingRate) ;
        //System.out.println("New Sample Length: "+newSampleLen);
        double fraction = (double)currentSamplingRate / targetSamplingRate;
        //System.out.println("Fraction: "+fraction);
        
        double[] newSignal = new double[newSampleLen];
        for(int i=0;i<newSignal.length;i++){
            double posIdx =  fraction * i;
            int nVal = (int) Math.floor(posIdx);
            double diffVal = posIdx - nVal;
            
            // Linear Interpolation 
            newSignal[i] = (diffVal * samples[nVal+1]) + ((1 - diffVal) * samples[nVal]);
            
        }
        boolean signed = true; //true,false
        AudioFormat af = new AudioFormat(
                targetSamplingRate,
                noOfbitsPerSample,
                channels,
                signed,
                bigEndian);
        
        DDSAudioInputStream oais = new DDSAudioInputStream(new
                BufferedDoubleDataSource(newSignal), af);

        return oais;
    }
    
}    
   




