package de.dfki.lt.signalproc.effects;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.util.StringUtils;
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
    public static String strLineBreak = "_LINEBREAK_";
    
    public static float NULL_FLOAT_PARAM = -100000.0f;
    public static double NULL_DOUBLE_PARAM = -100000;
    public static int NULL_INT_PARAM = -100000;
    public static String NULL_STRING_PARAM = "";
    
    public String strEffectName="";
    public String strHelpText="";
    public String strExampleParameters="";
    
    public String [] paramNames;
    public float [] paramVals;
    public String strParams; //To set the parameters using a String generated from outside
    
    
    public static char chParamEquals = ':';
    public static char chParamSeparator = ';';
    public static char chEffectParamStart = '(';
    public static char chEffectParamEnd = ')';
    
    private boolean isHMMEffect;
    
    public int fs;
    
    public BaseAudioEffect(BaseAudioEffect existing)
    {
        int i;
        fs = existing.fs;
        if (existing.paramNames!=null && existing.paramNames.length>0)
        {
            paramNames = new String[existing.paramNames.length];
           
            for (i=0; i<paramNames.length; i++)
                paramNames[i] = existing.paramNames[i];
        }
        else
            paramNames = null;  
        
        if (existing.paramVals!=null && existing.paramVals.length>0)
        {
            paramVals = new float[existing.paramVals.length];
            
            for (i=0; i<paramVals.length; i++)
                paramVals[i] = existing.paramVals[i];
        }
        else
            paramVals = null;
        
        strEffectName = existing.strEffectName;
        strParams = existing.strParams;
        strExampleParameters = existing.strExampleParameters;
        strHelpText = existing.strHelpText;
        
        setHMMEffect(false); //By default all effects are available to all voices
                             //Set to true if the effect is only available to HMM voices
    }
    
    public BaseAudioEffect(int samplingRate)
    {
        fs = samplingRate;
        parseParameters(getExampleParameters());
    }
    
    public BaseAudioEffect(int samplingRate, String strParams)
    {
        fs = samplingRate;
        parseParameters(strParams);
    }
   
    public DoubleDataSource apply(DoubleDataSource input) 
    {
        return apply(input, strParams);
    }
    
    public DoubleDataSource apply(DoubleDataSource input, String param) 
    {
        strParams = param;
        
        parseParameters(strParams);
  
        return process(input);
    }
    
    //This baseline version does nothing, implement functionality in derived classes
    public DoubleDataSource process(DoubleDataSource input)
    {
        return input;
    }
    

    public void setParams(String params)
    {
        String params2 = preprocessParams(params);
                
        parseParameters(params2);
        
        checkParameters();
    }
    
    //Preprocess to replace "=" with chParamEquals and ";" with chParamSeparator
    public String preprocessParams(String params)
    {
        String params2 = params;
        params2 = params2.replace('=', chParamEquals);
        params2 = params2.replace(',', chParamSeparator);
        
        return params2;
    }
    
    public void parseParameters(String param)
    {
        if (param!=null && param!="")
        {
            int count = 0;
            int ind = -1;
            
            while (true)
            {
                ind = param.indexOf(chParamEquals, ind+1);
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
                    ind1 = param.indexOf(chParamEquals, ind2+1);
                    ind2 = param.indexOf(chParamSeparator, ind1+1);
                    
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
                        strTmp = StringUtils.deblank(strTmp);
                        paramNames[i] = strTmp;
                        
                        strTmp = param.substring(stVal, enVal+1);
                        strTmp = StringUtils.deblank(strTmp);
                        
                        try
                        {
                            paramVals[i] = StringUtils.String2Float(strTmp);
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
                
                strParams = this.getParamsAsString(false);
            }
        }
    }
    
    //Perform parameter range controls in the derived classes
    public void checkParameters()
    {
        
    }
    
    //Should return a string containing exemplar parameters in the following form:
    // "param1=1.2; param2=0.5; param3=5;" 
    public String getExampleParameters() {
            return strExampleParameters;
    }

    //Should return a unique name for each derived effect class
    public String getName() {
        return strEffectName;
    }
    
    public void setName(String strName) {
        strEffectName = strName;
    }
    
    public void setExampleParameters(String strExampleParams) {
        
        strExampleParameters = strExampleParams;
        
        strExampleParameters = preprocessParams(strExampleParameters);
    }

    //Should return a help text explaining what the effect does and what parameters it has with information on the ranges of parameters
    public String getHelpText() {
        return strHelpText;
    }
    
    public String getParamsAsString()
    {
        return getParamsAsString(true);
    }
    
    public String getParamsAsString(boolean bWithParantheses)
    {
        String strRet = "";
        
        if (paramNames!=null && paramNames.length>0)
        {
            if (bWithParantheses)
                strRet += chEffectParamStart;
            
            for (int i=0; i<paramNames.length; i++)
            {
                strRet += paramNames[i] + chParamEquals + String.valueOf(paramVals[i]);
                
                if (i<paramNames.length-1)
                    strRet += chParamSeparator;
            }
            
            if (bWithParantheses)
                strRet += chEffectParamEnd;
        }
        
        return strRet;
    }
    
    public String getFullEffectAsString()
    {
        return getName() + getParamsAsString(true);
    }
    
    public String setParamsFromFullName(String fullEffectAsString)
    {
        int ind = fullEffectAsString.indexOf(strEffectName);
        
        if (ind>-1)
        {
            String strNewParams = fullEffectAsString.substring(ind+strEffectName.length(), fullEffectAsString.length());
            setParams(strNewParams);
        }
        
        return getParamsAsString();
    }
    
    public String getFullEffectWithExampleParametersAsString()
    {
        if (strExampleParameters!=null && strExampleParameters!="")
            return getName() + "(" + strExampleParameters + ")";
        else
            return getName();
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
    
    public boolean isHMMEffect()
    {
        return isHMMEffect;
    }
    
    public void setHMMEffect(boolean bHMMEffect)
    {
        isHMMEffect = bHMMEffect;
    }
    
    public static void main(String[] args) throws Exception
    {
        BaseAudioEffect b = new BaseAudioEffect(16000, "a1:1.1 ; a2 :2.02; a3: 3.003; a4  :  4.0004  ;a5:5.12345; a6: 6.6   ; a7:7.0a");
        
        for (int i=0; i<b.paramNames.length; i++)
            System.out.println(b.paramNames[i] + "=" + String.valueOf(b.paramVals[i]));
    }
}
