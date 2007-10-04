package de.dfki.lt.signalproc.effects;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.tools.ant.util.StringUtils;

import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.window.Window;

public class EffectsApplier {
    public BaseAudioEffect [] audioEffects;
    public String [] audioEffectsParams;
    public static char chEffectSeparator = ',';
    public static char chEffectParamStart = '(';
    public static char chEffectParamEnd = ')';
    
    public AudioInputStream apply(AudioInputStream input, String param)
    {
        AudioFormat audioformat = input.getFormat();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(input);
        DoubleDataSource tmpSignal = null;
        
        parseEffectsAndParams(param, (int)audioformat.getSampleRate());
        boolean bFirstEffect = true;
        
        if (audioEffects != null) //There are audio effects to apply
        {   
            for (int i=0; i<audioEffects.length; i++)
            {
                if (audioEffects[i]!=null)
                {
                    if (bFirstEffect)
                    {
                        if (audioEffects[i]!= null)
                        {
                            tmpSignal = audioEffects[i].apply(signal, audioEffectsParams[i]);
                            bFirstEffect = false;
                        }
                    }
                    else
                    {
                        if (audioEffects[i]!= null)
                            tmpSignal = audioEffects[i].apply(tmpSignal, audioEffectsParams[i]);
                    }
                }
            }
            
            return new DDSAudioInputStream(tmpSignal, audioformat);
        }
        else
            return input;
    }
    
    //Extract effects and parameters and create the corresponding effects
    // !!!TO DO!!!: Do the parsing in a more structured manner, i.e. by first determining the effect indices and then the corresponding parameters
    //              Currently it is not possible to parse FIRFilter,Robot(amount=50) for example, 
    //              as there is no way to understand whether the parameters belong to the first or the second effect
    //              This problem can be solved by finding the index where each effect name starts and ends and then search for the corresponding parameters
    //              of that effect up to the index where the successive effect begins
    //              P.S.: Use StringUtil.find function for this kind of parsing.
    //                    We have similar parsing in BaseAudioEffect.java also. It might need to be re-checked and made more robust
    public void parseEffectsAndParams(String param, int samplingRate)
    {
        param = StringUtil.deblank(param);
        int [] effectInds = StringUtil.find(param, chEffectSeparator);
        int numEffects = 0;
        
        if (effectInds!=null)
        {
            numEffects = effectInds.length;
            if (effectInds[effectInds.length-1] != param.length())
                numEffects++;
        }
        else
        {
            if (param.length()!=0)
                numEffects = 1;
        }

        if (numEffects>0)
        {
            audioEffects = new BaseAudioEffect[numEffects];
            audioEffectsParams = new String[numEffects];

            String strEffectName, strParams;
            int [] paramInds;
            for (int i=0; i<numEffects; i++)
            {
                if (i==0)
                {
                    if (numEffects==1 || effectInds==null)
                        strEffectName = param;
                    else
                        strEffectName = param.substring(0, effectInds[0]);
                }
                else
                {
                    if (effectInds==null)
                        strEffectName = param;
                    else if (i<effectInds.length)
                        strEffectName = param.substring(effectInds[i-1]+1, effectInds[i]);
                    else
                        strEffectName = param.substring(effectInds[i-1]+1, param.length());
                }   
                    
                strEffectName = StringUtil.deblank(strEffectName);

                if (strEffectName!=null && strEffectName!="")
                {
                    paramInds = StringUtil.find(strEffectName, chEffectParamStart);
                    if (paramInds!=null)
                    {
                        int stParam = MathUtils.max(paramInds);
                        paramInds = StringUtil.find(strEffectName, chEffectParamEnd);
                        int enParam = MathUtils.min(paramInds);

                        strParams = strEffectName.substring(stParam+1, enParam);
                        strParams = StringUtil.deblank(strParams);

                        strEffectName = strEffectName.substring(0, stParam);
                        strEffectName = StringUtil.deblank(strEffectName);
                    }
                    else
                        strParams = "";
                }
                else
                    strParams = "";

                audioEffects[i] = string2AudioEffect(strEffectName, samplingRate);
                audioEffectsParams[i] = strParams;
            }
        }
        else
        {
            audioEffects = null;
            audioEffectsParams = null;
        }
    }
    
    public BaseAudioEffect string2AudioEffect(String strEffectName, int samplingRate)
    {
        if (strEffectName.compareToIgnoreCase("Robot")==0)
            return new RobotiserEffect(samplingRate);
        else if (strEffectName.compareToIgnoreCase("Chorus")==0)
            return new ChorusEffectBase(samplingRate);
        else if (strEffectName.compareToIgnoreCase("Stadium")==0)
            return new StadiumEffect(samplingRate);
        else if (strEffectName.compareToIgnoreCase("FIRFilter")==0)
            return new FilterEffectsBase(samplingRate);
        else if (strEffectName.compareToIgnoreCase("JetPilot")==0)
            return new JetPilotEffect(samplingRate);
        else if (strEffectName.compareToIgnoreCase("Whisper")==0)
            return new LPCWhisperiserEffect(samplingRate);
        else
            return null;
    }
    
    public static void main(String[] args) throws Exception
    {   
        EffectsApplier e = new EffectsApplier();
        
        //String strEffectsAndParams = "Robot(amount=50)";
        //String strEffectsAndParams = "Robot(amount=100), Chorus(delay1=866; amp1=0.24; delay2=300; amp2=-0.40;)";
        //String strEffectsAndParams = "Robot(amount=80), Stadium(amount=50)";
        //String strEffectsAndParams = "FIRFilter(type=3;fc1=6000; fc2=10000), Robot";
        String strEffectsAndParams = "Stadium(amount=40), Robot(amount=87), Whisper(amount=65), FIRFilter(type=1;fc1=1540;),,,,; ";
        
        AudioInputStream input = AudioSystem.getAudioInputStream(new File(args[0]));

        AudioInputStream output = e.apply(input, strEffectsAndParams);

        AudioSystem.write(output, AudioFileFormat.Type.WAVE, new File(args[1]));
    }
}
