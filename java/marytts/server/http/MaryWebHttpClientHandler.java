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

package marytts.server.http;

import java.io.IOException;
import java.util.Vector;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.http.HttpResponse;

import marytts.client.AudioEffectsBoxData;
import marytts.client.MaryClient;
import marytts.client.http.MaryHttpClient;
import marytts.client.http.MaryHtmlForm;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

/**
 * @author oytun.turk
 *
 * This class implements a web browser client
 * It is different as compared to other Mary clients in the sense that 
 * it is dynamically created by MaryHttpServer to send an appropriate html form 
 * to a web browser that connect to a Mary server.
 * This form is updated dynamically by user requests and server responses.
 * 
 */
public class MaryWebHttpClientHandler
{    
    public MaryWebHttpClientHandler()
    {
        
    }
    
    //Convert for to an html page and put it in response
    public void toHttpResponse(MaryHtmlForm htmlForm, HttpResponse response) throws IOException, InterruptedException
    {
        String htmlPage = toHtmlPage(htmlForm);
            
        MaryHttpServerUtils.toHttpResponse(htmlPage, response);
    }
    
    public static String formatStringForJavaScript(String strIn)
    {
        String strOut = StringUtils.replace(strIn.trim(), "'", "\\'");
        strOut = StringUtils.replace(strOut, System.getProperty("line.separator"), "\\n");
        
        return strOut;
    }
    
    //Avoid server requests here! 
    //It will cause an infinite loop as the server calls this function while processing web browser client requests
    public String toHtmlPage(MaryHtmlForm htmlForm) throws IOException, InterruptedException
    {   
        int i, spaceInd;
        String htmlPage = "";
        String nextline = System.getProperty("line.separator");
        int numIndents = 0;
        String strIndent = "  ";
        
        htmlPage += indenter(numIndents, strIndent) + "<html>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<head>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<title>MARY Web Client</title>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</head>" + nextline;
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "<body onload=\"initForm();\">" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<font STYLE=\"font-family: Verdana\">" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<table>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td><img src=\"http://mary.dfki.de/favicon.ico\"></td>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td>" + nextline;        
        htmlPage += indenter(++numIndents, strIndent) + "<table>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td></td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += nextline;
        htmlPage += indenter(--numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td><h1><font STYLE=\"color:#587D9D\">MARY Web Client</font></h1></td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td></td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</table>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</table>" + nextline;
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "<table>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<form id=\"defaultSettingsLoader\" action=\"http://localhost:59125\" method=\"post\">" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td><input type=\"button\" value=\"Default\" STYLE=\"font-size:11pt;\" onClick=\"return doSubmit();\"></td>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td>Click to load default client settings from server</td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</form>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</table>" + nextline;
        htmlPage += nextline;  
        htmlPage += indenter(numIndents, strIndent) + "<table>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<form id=\"maryWebClient\" action=\"http://localhost:59125\" method=\"post\">" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td></td>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td>Input Type:" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<select name=\"INPUT_TYPE\" size=\"1\" onChange=\"return inputTypeChanged();\">" + nextline;
        
        //Fill input types
        for (i=0; i<htmlForm.inputDataTypes.size(); i++)
        {
            if (i==0)
                numIndents++;
            
            htmlPage += indenter(numIndents, strIndent) + "<option>" + htmlForm.inputDataTypes.elementAt(i).name() + "</option>" + nextline;
        }
        //
        
        htmlPage += indenter(numIndents, strIndent) + "</select>" + nextline;
        
        htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;
        
        if (htmlForm.isOutputText)
            htmlPage += indenter(numIndents, strIndent) + "<td></td>" + nextline;
        
        htmlPage += indenter(numIndents, strIndent) + "<td>Output Type:" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<select name=\"OUTPUT_TYPE\" size=\"1\"  onChange=\"return outputTypeChanged();\">" + nextline;
        
        //Fill output types
        for (i=0; i<htmlForm.outputDataTypes.size(); i++)
        {
            if (i==0) 
                numIndents++;
            
            htmlPage += indenter(numIndents, strIndent) + "<option>" + htmlForm.outputDataTypes.elementAt(i).name() + "</option>" + nextline;
        }
        //
        
        htmlPage += indenter(numIndents, strIndent) + "</select>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;    
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += nextline;
        
        //Input text
        htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td>Input Text:</td>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td><textarea name=\"INPUT_TEXT\" rows=\"30\" cols=\"45\"></textarea>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "</td>" + nextline; 
        htmlPage += nextline; 
        //
        
        if (htmlForm.isOutputText)
        {
            //Output text
            htmlPage += indenter(numIndents, strIndent) + "<td>Output Text:</td>" + nextline;
            htmlPage += indenter(numIndents, strIndent) + "<td><textarea name=\"OUTPUT_TEXT\" rows=\"30\" cols=\"45\"></textarea>" + nextline;
            htmlPage += indenter(numIndents, strIndent) + "</td>" + nextline;
            htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
            htmlPage += nextline;
            //
        }
        else
        {
            //Audio effects
            if (htmlForm.effectsBoxData!=null && htmlForm.effectsBoxData.hasEffects())
            {
                htmlPage += indenter(numIndents, strIndent) + "<td>" + nextline; 
                htmlPage += indenter(numIndents, strIndent) + "<table>" + nextline;
                htmlPage += indenter(++numIndents, strIndent) + "<tr> <td> </td> <td>Effects</td> <td>Parameters</td></tr>" + nextline;
                
                for (i=0; i<htmlForm.effectsBoxData.getTotalEffects(); i++)
                {
                    String effectName = htmlForm.effectsBoxData.getControlData(i).getEffectName();
                    String defaultParams = htmlForm.effectsBoxData.getControlData(i).getParams();
                    boolean isSelected = htmlForm.effectsBoxData.getControlData(i).getSelected();
                    htmlPage += indenter(numIndents, strIndent) + "<tr> <td><input type=\"checkbox\" name=\"effect_" + effectName + "_selected\"" + (isSelected?("checked"):("")) + "></td> " + 
                                                                       "<td>" + effectName + "</td> " + 
                                                                       "<td><textarea name=\"effect_" + effectName + "_parameters\" rows=\"1\" cols=\"20\">" + defaultParams + "</textarea></td> " + 
                                                                       "<td><input type=\"button\" name=\"effect_" + effectName + "_help\" value=\"Help\"></td> " + 
                                                                       "<td><input type=\"button\" name=\"effect_" + effectName + "_default\" value=\"Default\" onClick=\"return " + effectName + "DefaultClicked();\"></td> " + 
                                                                       "</tr>" + nextline;
                }
                  
                htmlPage += indenter(numIndents, strIndent) + "</font>" + nextline;
                htmlPage += indenter(--numIndents, strIndent) + "</table>" + nextline;
                htmlPage += indenter(numIndents, strIndent) + "</td>" + nextline;
            }
        }
        
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += nextline;
        //
        
        //Show example texts if limited domain voice is selected
        String exampleText = "";
        if (htmlForm.allVoices.elementAt(htmlForm.voiceSelected).isLimitedDomain())
        {
            htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
            htmlPage += indenter(++numIndents, strIndent) + "<td>Example:</td>" + nextline;
            htmlPage += indenter(numIndents, strIndent) + "<td><select name=\"exampletext\" size=\"1\">" + nextline;

            //Fill example texts
            for (i=0; i<htmlForm.limitedDomainExampleTexts.size(); i++)
            {
                if (i==0)
                    numIndents++;

                htmlPage += indenter(numIndents, strIndent) + "<option>" + htmlForm.limitedDomainExampleTexts.get(i) + "</option>" + nextline;
            }
            //

            htmlPage += indenter(numIndents, strIndent) + "</select>" + nextline;
            htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;
            htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
            htmlPage += nextline;
            
            htmlForm.limitedDomainExampleTextSelected = MathUtils.CheckLimits(htmlForm.limitedDomainExampleTextSelected, 0, htmlForm.limitedDomainExampleTexts.size()-1);
            exampleText = htmlForm.limitedDomainExampleTexts.get(htmlForm.limitedDomainExampleTextSelected);
        }   
        else
            exampleText = htmlForm.currentExampleText;
        
        htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td>Voice:</td>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td><select name=\"VOICE\" size=\"1\" onChange=\"return doSubmit();\">" + nextline;

        //Fill voices
        for (i=0; i<htmlForm.allVoices.size(); i++)
        {
            if (i==0)
                numIndents++;
            
            htmlPage += indenter(numIndents, strIndent) + "<option>" + htmlForm.allVoices.elementAt(i).name() + " (" + htmlForm.allVoices.elementAt(i).getLocale().getDisplayLanguage() + ", " + htmlForm.allVoices.elementAt(i).gender() + ")" + "</option>" + nextline;
        }
        //

        htmlPage += indenter(numIndents, strIndent) + "</select>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += nextline;     
        htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
        
        if (htmlForm.isOutputText)
            htmlPage += indenter(++numIndents, strIndent) + "<td><input type=\"button\" value=\"PROCESS\" STYLE=\"font-size:11pt; font-weight=bold;\" onClick=\"return doSubmit();\"></td>" + nextline;
        else
        {
            htmlPage += indenter(++numIndents, strIndent) + "<td>Format:</td>" + nextline;
            htmlPage += indenter(numIndents, strIndent) + "<td><select name=\"audioformat\" size=\"1\" onChange=\"return doSubmit();\">" + nextline;
            
            //Fill audio file format types
            for (i=0; i<htmlForm.audioFileFormatTypes.length; i++)
            {
                if (i==0)
                    numIndents++;
                
                spaceInd = htmlForm.audioFileFormatTypes[i].indexOf(' ');
                String typeName = htmlForm.audioFileFormatTypes[i].substring(spaceInd+1);
                
                htmlPage += indenter(numIndents, strIndent) + "<option>" + typeName + "</option>" + nextline;
            }
            //

            htmlPage += indenter(--numIndents, strIndent) + "</select>" + nextline;
            htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;
            htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
            htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
            htmlPage += indenter(++numIndents, strIndent) + "<td><input type=\"button\" value=\"SPEAK\" STYLE=\"font-size:11pt; font-weight=bold;\" onClick=\"return doSubmit();\"></td>" + nextline;
        }
        
        htmlPage += indenter(numIndents, strIndent) + "<td>Click to send synthesis request to MARY server</td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        
        //Invisible form fields for communication with server
        //Tell server that this is a web browser client
        htmlPage += nextline;     
        htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td><input type=\"hidden\" name=\"iswebbrowserclient\" value=\"true\"></td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        //
        
        //For requesting example texts depending on input/output type and voice
        htmlPage += nextline;     
        htmlPage += indenter(numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td><input type=\"hidden\" name=\"EXAMPLE_TEXT\" value=\"\"></td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        
        //
        
        //Java scripts here
        htmlPage += indenter(numIndents, strIndent) + "<script language=\"javascript\">" + nextline;
        
        //Form initialization
        //htmlPage += indenter(numIndents, strIndent) + "var bFirstTime = new Boolean(true);" + nextline; 
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function initForm()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.VOICE.selectedIndex = " + String.valueOf(htmlForm.voiceSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.INPUT_TYPE.selectedIndex = " + String.valueOf(htmlForm.inputTypeSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.OUTPUT_TYPE.selectedIndex = " + String.valueOf(htmlForm.outputTypeSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.audioformat.selectedIndex = " + String.valueOf(htmlForm.audioFormatSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.INPUT_TEXT.value = '" + formatStringForJavaScript(htmlForm.inputText) + "';" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.OUTPUT_TEXT.value = '" + formatStringForJavaScript(htmlForm.outputText) + "';" + nextline;

        if (htmlForm.allVoices.elementAt(htmlForm.voiceSelected).isLimitedDomain())
            htmlPage += indenter(numIndents, strIndent) + "maryWebClient.exampletext.selectedIndex = " + String.valueOf(htmlForm.limitedDomainExampleTextSelected) + ";" + nextline;
        
        //htmlPage += indenter(numIndents, strIndent) + "bFirstTime = false;" + nextline;
        
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Submit form
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function doSubmit()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        //htmlPage += indenter(++numIndents, strIndent) + "if (!bFirstTime) {" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.submit();" + nextline;
        //htmlPage += indenter(++numIndents, strIndent) + "alert('submitting');" + nextline;
        //htmlPage += indenter(--numIndents, strIndent) + "}" + nextline;
        //htmlPage += indenter(++numIndents, strIndent) + "alert('not submitting');" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Input Type changed
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function inputTypeChanged()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.EXAMPLE_TEXT.value = '? ' + maryWebClient.INPUT_TYPE.value + ' ' + '" + htmlForm.allVoices.get(htmlForm.voiceSelected).getLocale() + "';" + nextline;  
        htmlPage += indenter(numIndents, strIndent) + "doSubmit();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //

        //Output type changed
        if (htmlForm.isOutputText)
        {
            htmlPage += nextline;
            htmlPage += indenter(numIndents, strIndent) + "function outputTypeChanged()" + nextline; 
            htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
            htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.EXAMPLE_TEXT.value = '? ' + maryWebClient.OUTPUT_TYPE.value + ' ' + '" + htmlForm.allVoices.get(htmlForm.voiceSelected).getLocale() + "';" + nextline; 
            htmlPage += indenter(numIndents, strIndent) + "doSubmit();" + nextline;
            htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        }
        //
        
        //Default parameter loaders for audio effects
        if (!htmlForm.isOutputText && htmlForm.effectsBoxData!=null && htmlForm.effectsBoxData.hasEffects())
        {
            for (i=0; i<htmlForm.effectsBoxData.getTotalEffects(); i++)
            {
                String effectName = htmlForm.effectsBoxData.getControlData(i).getEffectName();
                String defaultParams = htmlForm.effectsBoxData.getControlData(i).getExampleParams();
                
                htmlPage += nextline;
                htmlPage += indenter(numIndents, strIndent) + "function " + effectName + "DefaultClicked()" + nextline; 
                htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
                htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.effect_" + effectName + "_parameters.value = '" + defaultParams + "';" + nextline;
                htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
            }
        }
        //
        
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "</script>" + nextline;
        //
        
        htmlPage += indenter(--numIndents, strIndent) + "</form>" + nextline;
        htmlPage += nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</table>" + nextline;
        htmlPage += nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</font>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</body>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</html>" + nextline;
        
        return htmlPage;
    }
    
    public static String indenter(int numIndents, String strSingleIndent)
    {
        String strIndent = "";
        while (numIndents>0)
        {
            strIndent += strSingleIndent;
            numIndents--;
        }
        
        return strIndent;
    }
}