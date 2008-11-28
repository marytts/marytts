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

package marytts.client;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Data for an audio effect control.
 * 
 * @author Oytun T&uumlrk
 *
 */
public class AudioEffectControlData {
    private String effectName;
    private String helpText;
    private String exampleParams;
    private String lineBreak;
    private String params;
    private boolean isSelected;
    
    public AudioEffectControlData(AudioEffectControlData dataIn)
    {
        this(dataIn.effectName, dataIn.exampleParams, dataIn.helpText, dataIn.lineBreak);
        
        this.params = dataIn.params;
        this.isSelected = dataIn.isSelected;
    }
    
    public AudioEffectControlData(String strEffectNameIn, String strExampleParams, String strHelpTextIn, String lineBreakIn)
    { 
        init(strEffectNameIn, strExampleParams, strHelpTextIn, lineBreakIn);
    }
    
    public void init(String strEffectNameIn, String strExampleParamsIn, String strHelpTextIn, String lineBreakIn)
    {
        lineBreak = lineBreakIn;
        setEffectName(strEffectNameIn);
        setExampleParams(strExampleParamsIn);
        setHelpText(strHelpTextIn); 
        setEffectParamsToExample();
    }
    
    public void setEffectName(String strEffectName) { effectName = strEffectName; }
    public String getEffectName() { return effectName; }
    public void setHelpText(String strHelpText) { helpText = strHelpText; }
    public String getHelpText() { return helpText; }
    public void setParams(String strParams) { params = strParams; }
    public String getParams() { return params; }
    public void setExampleParams(String strExampleParams) { exampleParams = strExampleParams; }
    public String getExampleParams() { return exampleParams; }
    public void setEffectParamsToExample() { setParams(exampleParams); }
    public void setSelected(boolean bSelected) { isSelected = bSelected; }
    public boolean getSelected() { return isSelected; }
    
    //Parse string according to line break string
    public String parseLineBreaks(String strInput)
    {
        String strOut = "";
        String strTmp;
        
        for (int i=0; i<strInput.length()-lineBreak.length(); i++)
        {
            strTmp = strInput.substring(i, i+lineBreak.length());
            
            if (strTmp.equals(lineBreak)==false)
                strOut += strInput.substring(i, i+1);
            else
            {
                strOut += System.getProperty("line.separator");
                i += lineBreak.length()-1;
            }
        }
        
        strTmp = strInput.substring(strInput.length()-lineBreak.length(), strInput.length());
        if (strTmp.compareTo(lineBreak)!=0)
            strOut += strTmp;
        else
            strOut += System.getProperty("line.separator");
        
        return strOut;
    }
}
