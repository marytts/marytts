package de.dfki.lt.signalproc.effects;

import java.io.File;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.window.Window;

//Baseline audio effect class
//It serves as a null effect
//It´s main functionality is providing base functions for parsing parameter strings
public class BaseAudioEffect implements AudioEffect{
    
    public static float NULL_FLOAT_PARAM = -100000.0f;
    public static double NULL_DOUBLE_PARAM = -100000;
    public static int NULL_INT_PARAM = -100000;
    public static String NULL_STRING_PARAM = "";
    
    String [] paramNames;
    float [] paramVals;
    static char chEquals = '=';
    static char chSeparator = ';';
    int fs;
    
    public BaseAudioEffect(int samplingRate)
    {
        fs = samplingRate;
    }
   
    public DoubleDataSource apply(DoubleDataSource input, String param) 
    {
        parseParameters(param);
  
        return process(input);
    }
    
    //This baseline version does nothing, implement functionality in derived classes
    public DoubleDataSource process(DoubleDataSource input)
    {
        return input;
    }
    
    public void parseParameters(String param)
    {
        if (param!=null && param!="")
        {
            int count = 0;
            int ind = -1;
            
            while (true)
            {
                ind = param.indexOf(chEquals, ind+1);
                if (ind>-1)
                    count++;
                else
                    break;
            }
            
            if (count>0)
            {
                int stName = 0;
                int enName = -1;
                int stVal = 0;
                int enVal = -1;
                int ind1 = -1;
                int ind2 = -1;
                
                paramNames = new String [count];
                paramVals = new float[count];
                String strTmp;
                
                for (int i=0; i<count; i++)
                {
                    ind1 = param.indexOf(chEquals, ind2+1);
                    ind2 = param.indexOf(chSeparator, ind1+1);
                    
                    if (ind1>-1)
                    { 
                        enName = ind1-1;
                        stVal = ind1+1;
                        
                        if (ind2>-1)
                            enVal = ind2-1;
                        else
                            enVal = param.length()-1;
                        
                        //Extract param and val
                        strTmp = param.substring(stName, enName+1);
                        strTmp = StringUtil.deblank(strTmp);
                        paramNames[i] = strTmp;
                        
                        strTmp = param.substring(stVal, enVal+1);
                        strTmp = StringUtil.deblank(strTmp);
                        
                        try
                        {
                            paramVals[i] = StringUtil.String2Float(strTmp);
                        }
                        catch(NumberFormatException e)
                        {
                            System.out.println("Error! The parameter should be numeric...");
                        }
                        //
                        
                        stName = ind2+1;  
                        
                        if (stName>param.length()-1)
                            break;
                    }
                }
            }
        }
    }
    
    //Should return a string containing exemplar parameters in the following form:
    // "param1=1.2; param2=0.5; param3=5;" 
    public String getExampleParameters() {
        return "";
    }

    //Should return a unique name for each derived effect class
    public String getName() {
        return "";
    }

    //Should return a help text explaining what the effect does and what parameters it has with information on the ranges of parameters
    public String getHelpText() {
        return "";
    }
    
    public float expectFloatParameter(String strParamName)
    {
        float ret = NULL_FLOAT_PARAM;
        
        if (paramNames!=null)
        {
            for (int i=0; i<paramNames.length; i++)
            {
                if (strParamName.compareToIgnoreCase(paramNames[i])==0)
                {
                    ret = paramVals[i];
                    break;
                }
            }
        }
           
        return ret;
    }
    
    public double expectDoubleParameter(String strParamName)
    {
        double ret = NULL_DOUBLE_PARAM;
        
        if (paramNames!=null)
        {
            for (int i=0; i<paramNames.length; i++)
            {
                if (strParamName.compareToIgnoreCase(paramNames[i]) == 0)
                {
                    ret = (double)paramVals[i];
                    break;
                }
            }
        }
           
        return ret;
    }
    
    public int expectIntParameter(String strParamName)
    {
        int ret = NULL_INT_PARAM;
        
        if (paramNames!=null)
        {
            for (int i=0; i<paramNames.length; i++)
            {
                if (strParamName.compareToIgnoreCase(paramNames[i]) == 0)
                {
                    ret = (int)paramVals[i];
                    break;
                }
            }
        }
           
        return ret;
    }
    
    public static void main(String[] args) throws Exception
    {
        BaseAudioEffect b = new BaseAudioEffect(16000);
        b.parseParameters("a1=1.1 ; a2 =2.02; a3= 3.003; a4  =  4.0004  ;a5=5.12345; a6= 6.6   ; a7=7.0a");
        
        for (int i=0; i<b.paramNames.length; i++)
            System.out.println(b.paramNames[i] + "=" + String.valueOf(b.paramVals[i]));
    }
}
