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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.http.HttpResponse;

import marytts.client.AudioEffectsBoxData;
import marytts.client.MaryClient;
import marytts.client.http.MaryHttpClient;
import marytts.client.http.MaryBaseClient;
import marytts.client.http.MaryHttpClientUtils;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

/**
 * This class implements the server-side code for handling a web browser client.
 * Te handling is different as compared to conventional Mary clients since 
 * MaryHttpServer has to use this class to send an appropriate html form 
 * to the web browser client.
 * The form is updated dynamically by user requests and server responses in this class.
 * 
 * @author Oytun T&uumlrk
 */
public class MaryWebHttpClientHandler
{    
    public MaryWebHttpClientHandler()
    {
        
    }
    
    //Convert for to an html page and put it in response
    public void toHttpResponse(MaryBaseClient htmlForm, HttpResponse response) throws IOException, InterruptedException
    {        
        toHttpResponse(htmlForm, response, "text/html; charset=UTF-8");
    }
    
    public void toHttpResponse(MaryBaseClient htmlForm, HttpResponse response, String contentType) throws IOException, InterruptedException
    {        
        String htmlPage = toHtmlPage(htmlForm);
            
        MaryHttpServerUtils.toHttpResponse(htmlPage, response, contentType);
    }
    
    public static String formatStringForJavaScript(String strIn)
    {
        String strOut = StringUtils.replace(strIn.trim(), "'", "\\'");
        
        strOut = StringUtils.replace(strOut, "\r\n", "\\n");
        strOut = StringUtils.replace(strOut, "\r\r", "\\n");
        strOut = StringUtils.replace(strOut, "\r\t", "\\n");
        strOut = StringUtils.replace(strOut, "\r\0", "\\n");
        strOut = StringUtils.replace(strOut, "\t\n", "\\n");
        strOut = StringUtils.replace(strOut, "\t\r", "\\n");
        strOut = StringUtils.replace(strOut, "\t\t", "\\n");
        strOut = StringUtils.replace(strOut, "\t\0", "\\n");
        strOut = StringUtils.replace(strOut, "\0\n", "\\n");
        strOut = StringUtils.replace(strOut, "\0\r", "\\n");
        strOut = StringUtils.replace(strOut, "\0\t", "\\n");
        strOut = StringUtils.replace(strOut, "\0\0", "\\n");
        strOut = StringUtils.replace(strOut, System.getProperty("line.separator") + "\n", "\\n");
        strOut = StringUtils.replace(strOut, System.getProperty("line.separator") + "\r", "\\n");
        strOut = StringUtils.replace(strOut, System.getProperty("line.separator") + "\t", "\\n");
        strOut = StringUtils.replace(strOut, System.getProperty("line.separator") + "\0", "\\n");
        
        strOut = StringUtils.replace(strOut, "\\r", "\\n");
        strOut = StringUtils.replace(strOut, "\\t", "\\n");
        strOut = StringUtils.replace(strOut, "\\0", "\\n");
        
        strOut = StringUtils.replace(strOut, "\n", "\\n");
        strOut = StringUtils.replace(strOut, "\r", "\\n");
        strOut = StringUtils.replace(strOut, "\t", "\\n");
        strOut = StringUtils.replace(strOut, "\0", "\\n");
        
        if (System.getProperty("line.separator").compareTo("\\n")!=0)
            strOut = StringUtils.replace(strOut, System.getProperty("line.separator"), "\\n");
        
        return strOut;
    }
    
    //Avoid server requests here! 
    //It will cause an infinite loop as the server calls this function while processing web browser client requests
    public String toHtmlPage(MaryBaseClient htmlForm) throws IOException, InterruptedException
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
        
        //Hide/show Java scripts here inside <head> tag
        htmlPage += indenter(numIndents, strIndent) + "<script language=\"javascript\">" + nextline;
        
        //Form initialization
        //htmlPage += indenter(numIndents, strIndent) + "var bFirstTime = new Boolean(true);" + nextline; 
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function initForm()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.VOICE_SELECTIONS.selectedIndex = " + String.valueOf(htmlForm.voiceSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.INPUT_TYPE.selectedIndex = " + String.valueOf(htmlForm.inputTypeSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.OUTPUT_TYPE.selectedIndex = " + String.valueOf(htmlForm.outputTypeSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.AUDIO_OUT.selectedIndex = " + String.valueOf(htmlForm.audioOutSelected) + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.AUDIO.value = maryWebClient.AUDIO_OUT.options[maryWebClient.AUDIO_OUT.selectedIndex].text;" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.INPUT_TEXT.value = '" + formatStringForJavaScript(htmlForm.inputText) + "';" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.OUTPUT_TEXT.value = '" + formatStringForJavaScript(htmlForm.outputText) + "';" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.LOCALE.value = '" + htmlForm.allVoices.get(htmlForm.voiceSelected).getLocale() + "';" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.VOICE.value = '" + htmlForm.allVoices.get(htmlForm.voiceSelected).name() + "';" + nextline;
        
        if (htmlForm.allVoices.elementAt(htmlForm.voiceSelected).isLimitedDomain())
            htmlPage += indenter(numIndents, strIndent) + "maryWebClient.exampletext.selectedIndex = " + String.valueOf(htmlForm.limitedDomainExampleTextSelected) + ";" + nextline;
        
        //htmlPage += indenter(numIndents, strIndent) + "bFirstTime = false;" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "handleVisibility();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Handle visibility of items depending on output type
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function handleVisibility()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        
        String visibilityOfOutputTextItems = "'none'"; //not visible
        String visibilityOfOutputAudioItems = "'inline'"; //visible
        if (htmlForm.isOutputText)
        {
            visibilityOfOutputTextItems = "'inline'"; //visible
            visibilityOfOutputAudioItems = "'none'"; //not visible
        }
        
        ////If output is text: hide audio effects, audio format types, SPEAK button; 
        ////                   show output text area, PROCESS button
        ////If output is audio: vice versa
        
        //Handle visibility of audio format types
        htmlPage += indenter(++numIndents, strIndent) + "document.getElementById('hideAudioOut').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "document.getElementById('AUDIO_OUT').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
        
        //Handle visibility of audio effects
        if (htmlForm.effectsBoxData!=null && htmlForm.effectsBoxData.hasEffects())
        {
            htmlPage += indenter(numIndents, strIndent) + "document.getElementById('hideEffects').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
            htmlPage += indenter(numIndents, strIndent) + "document.getElementById('hideParameters').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;

            for (i=0; i<htmlForm.effectsBoxData.getTotalEffects(); i++)
            {
                String effectName = htmlForm.effectsBoxData.getControlData(i).getEffectName();
                htmlPage += indenter(numIndents, strIndent) + "document.getElementById('" + effectName + "').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
                htmlPage += indenter(numIndents, strIndent) + "document.getElementById('effect_" + effectName + "_selected').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
                htmlPage += indenter(numIndents, strIndent) + "document.getElementById('effect_" + effectName + "_parameters').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
                htmlPage += indenter(numIndents, strIndent) + "document.getElementById('effect_" + effectName + "_help').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
                htmlPage += indenter(numIndents, strIndent) + "document.getElementById('effect_" + effectName + "_default').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
            }
        }
        
        //Handle visibility of help text title and area
        htmlPage += indenter(numIndents, strIndent) + "document.getElementById('hideHelpTextTitle').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "document.getElementById('HELP_TEXT').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
        
        
        //Handle visibility of SPEAK button
        htmlPage += indenter(numIndents, strIndent) + "document.getElementById('SPEAK').style.display = " + visibilityOfOutputAudioItems + ";" + nextline;
        
        //Handle visibility of output text area
        htmlPage += indenter(numIndents, strIndent) + "document.getElementById('hideOutputText').style.display = " + visibilityOfOutputTextItems + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "document.getElementById('OUTPUT_TEXT').style.display = " + visibilityOfOutputTextItems + ";" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "document.getElementById('PROCESS').style.display = " + visibilityOfOutputTextItems + ";" + nextline;
        //
        
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        ////
        
        //Submit form
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function doSubmit()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.submit();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Send synthesis request
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function requestSynthesis()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.AUDIO.value = maryWebClient.AUDIO_OUT.options[maryWebClient.AUDIO_OUT.selectedIndex].text;" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.SYNTHESIS_OUTPUT.value = '?';" + nextline;  
        htmlPage += indenter(numIndents, strIndent) + "doSubmit();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Default clicked to request default page from server
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function defaultClicked()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.DEFAULT_PAGE.value = '?';" + nextline;  
        htmlPage += indenter(numIndents, strIndent) + "doSubmit();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Input Type changed
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function inputTypeChanged()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.EXAMPLE_TEXT.value = '? ' + maryWebClient.INPUT_TYPE.options[maryWebClient.INPUT_TYPE.selectedIndex].text + ' ' + maryWebClient.LOCALE.value;" + nextline;  
        htmlPage += indenter(numIndents, strIndent) + "doSubmit();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //

        //Output type changed
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function outputTypeChanged()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "requestSynthesis();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Voice changed
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function voiceChanged()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.LOCALE.value = '" + htmlForm.allVoices.get(htmlForm.voiceSelected).getLocale() + "';" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "maryWebClient.VOICE.value = '" + htmlForm.allVoices.get(htmlForm.voiceSelected).name() + "';" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "doSubmit();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Audio format changed
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "function audioOutChanged()" + nextline; 
        htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "requestSynthesis();" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
        //
        
        //Default parameter loaders for audio effects
        if (htmlForm.effectsBoxData!=null && htmlForm.effectsBoxData.hasEffects())
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
        
        //Help displayers for audio effects
        if (htmlForm.effectsBoxData!=null && htmlForm.effectsBoxData.hasEffects())
        {
            for (i=0; i<htmlForm.effectsBoxData.getTotalEffects(); i++)
            {
                String effectName = htmlForm.effectsBoxData.getControlData(i).getEffectName();
                String helpText = htmlForm.effectsBoxData.getControlData(i).getHelpText();
                helpText = StringUtils.replace(helpText, htmlForm.audioEffectsHelpTextLineBreak, "\\n");
                helpText = formatStringForJavaScript(helpText);
                
                htmlPage += nextline;
                htmlPage += indenter(numIndents, strIndent) + "function " + effectName + "HelpClicked()" + nextline; 
                htmlPage += indenter(numIndents, strIndent) + "{" + nextline;
                htmlPage += indenter(++numIndents, strIndent) + "maryWebClient.HELP_TEXT.value = '" + helpText + "';" + nextline;
                htmlPage += indenter(--numIndents, strIndent) + "};" + nextline;
            }
        }
        //
        
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent) + "</script>" + nextline;
        //
        
        htmlPage += indenter(--numIndents, strIndent) + "</head>" + nextline;
        htmlPage += nextline;
        
        htmlPage += indenter(numIndents, strIndent) + "<body onload=\"initForm();\">" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<font STYLE=\"font-family: Verdana\">" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<table>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td><img src=\"favicon.ico\"></td>" + nextline;
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
        htmlPage += indenter(++numIndents, strIndent) + "<tr>" + nextline;
        htmlPage += indenter(++numIndents, strIndent) + "<td><input type=\"button\" value=\"Default\" STYLE=\"font-size:11pt;\" onClick=\"return defaultClicked();\"></td>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<td>Click to load default client settings from server</td>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        htmlPage += indenter(--numIndents, strIndent) + "</table>" + nextline;
        htmlPage += nextline;  
        htmlPage += indenter(numIndents, strIndent) + "<table>" + nextline;
        htmlPage += indenter(numIndents, strIndent) + "<form id=\"maryWebClient\" action=\"" + htmlForm.hostAddress.getHttpAddress() + "\" method=\"post\">" + nextline;
        
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
        
        htmlPage += indenter(--numIndents, strIndent) + "</select>" + nextline;
        
        htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;
        
        htmlPage += indenter(numIndents, strIndent);
        if (htmlForm.isOutputText)
            htmlPage += "<td></td>" + nextline;
        htmlPage += nextline;
        
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
        
        //Output text
        htmlPage += indenter(numIndents, strIndent);
        if (htmlForm.isOutputText) htmlPage += "<td>";     
        htmlPage += "<div id=\"hideOutputText\">Output Text:</div>";
        if (htmlForm.isOutputText) htmlPage += "</td>";
        htmlPage += nextline;
            
        htmlPage += indenter(numIndents, strIndent);
        if (htmlForm.isOutputText) htmlPage += "<td>";
        htmlPage += "<textarea name=\"OUTPUT_TEXT\" id=\"OUTPUT_TEXT\" rows=\"30\" cols=\"45\"></textarea>" + nextline;
        htmlPage += indenter(numIndents, strIndent);
        if (htmlForm.isOutputText) htmlPage += "</td>" + nextline;
        htmlPage += nextline;
        //

        //Audio effects
        if (htmlForm.effectsBoxData!=null && htmlForm.effectsBoxData.hasEffects())
        {
            //Effects and Parameters text
            htmlPage += indenter(numIndents, strIndent);
            if (!htmlForm.isOutputText) htmlPage += "<td>"; 
            htmlPage += nextline; 
            htmlPage += indenter(++numIndents, strIndent);
            if (!htmlForm.isOutputText) htmlPage += "<table>";
            htmlPage += nextline;
            htmlPage += indenter(++numIndents, strIndent);
            if (!htmlForm.isOutputText) htmlPage += "<tr> <td> </td> <td>";

            htmlPage += "<div id=\"hideEffects\">Effects</div>";
            if (!htmlForm.isOutputText) htmlPage += "</td> <td>";
            htmlPage += "<div id=\"hideParameters\">Parameters</div>";
            if (!htmlForm.isOutputText) htmlPage += "</td></tr>";
            htmlPage += nextline;
            //

            for (i=0; i<htmlForm.effectsBoxData.getTotalEffects(); i++)
            {
                String effectName = htmlForm.effectsBoxData.getControlData(i).getEffectName();
                String defaultParams = htmlForm.effectsBoxData.getControlData(i).getParams();
                boolean isSelected = htmlForm.effectsBoxData.getControlData(i).getSelected();
                htmlPage += indenter(numIndents, strIndent);
                if (!htmlForm.isOutputText) htmlPage += "<tr> <td>";
                htmlPage += "<input type=\"checkbox\" name=\"effect_" + effectName + "_selected\" id=\"effect_" + effectName + "_selected\" " + (isSelected?("checked"):("")) + ">"; 
                if (!htmlForm.isOutputText) htmlPage += "</td> ";
                if (!htmlForm.isOutputText) htmlPage += "<td>";
                htmlPage += "<div id=\"" + effectName + "\">" + effectName + "</div>";
                if (!htmlForm.isOutputText) htmlPage += "</td> ";
                if (!htmlForm.isOutputText) htmlPage += "<td>";
                htmlPage += "<textarea name=\"effect_" + effectName + "_parameters\" id=\"effect_" + effectName + "_parameters\" rows=\"1\" cols=\"20\">" + defaultParams + "</textarea>";
                if (!htmlForm.isOutputText) htmlPage += "</td> "; 
                if (!htmlForm.isOutputText) htmlPage += "<td>";
                htmlPage += "<input type=\"button\" name=\"effect_" + effectName + "_default\" id=\"effect_" + effectName + "_default\" value=\"Default\" onClick=\"return " + effectName + "DefaultClicked();\">";
                if (!htmlForm.isOutputText) htmlPage += "</td> ";
                if (!htmlForm.isOutputText) htmlPage += "<td>";
                htmlPage += "<input type=\"button\" name=\"effect_" + effectName + "_help\" id=\"effect_" + effectName + "_help\" value=\"Help\" onClick=\"return " + effectName + "HelpClicked();\">";
                if (!htmlForm.isOutputText) htmlPage += "</td> ";
                if (!htmlForm.isOutputText) htmlPage += "</tr>";
                htmlPage += nextline;
            }

            htmlPage += indenter(--numIndents, strIndent);
            if (!htmlForm.isOutputText)  htmlPage += "</table>";
            htmlPage += nextline;
            htmlPage += indenter(--numIndents, strIndent);
            if (!htmlForm.isOutputText) htmlPage += "</td>";
            htmlPage += nextline;  
            
            //Help text area for audio effects
            htmlPage += indenter(numIndents, strIndent) + "<td>" + nextline;
            if (!htmlForm.isOutputText) htmlPage += indenter(++numIndents, strIndent) + "<table>" + nextline;
            htmlPage += indenter(++numIndents, strIndent) + "<tr>";
            if (!htmlForm.isOutputText) htmlPage += "<td><div id=\"hideHelpTextTitle\">Help Text:</div></td>";
            htmlPage += "</tr>";
            htmlPage += nextline;
            htmlPage += indenter(numIndents, strIndent) + "<tr>";
            if (!htmlForm.isOutputText) htmlPage += "<td><textarea name=\"HELP_TEXT\" id=\"HELP_TEXT\" rows=\"15\" cols=\"50\"></textarea>";
            htmlPage += "</td></tr>" + nextline;
            if (!htmlForm.isOutputText) htmlPage += indenter(--numIndents, strIndent);
            if (!htmlForm.isOutputText) htmlPage += "</table>" + nextline;
            htmlPage += indenter(--numIndents, strIndent) + "</td>" + nextline;
            //
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
        htmlPage += indenter(numIndents, strIndent) + "<td><select name=\"VOICE_SELECTIONS\" size=\"1\" onChange=\"return voiceChanged();\">" + nextline;

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
        
        htmlPage += indenter(++numIndents, strIndent);
        if (htmlForm.isOutputText) htmlPage += "<td>";
        htmlPage += "<input type=\"button\" value=\"PROCESS\" id=\"PROCESS\" STYLE=\"font-size:11pt; font-weight=bold;\" onClick=\"return requestSynthesis();\">";
        if (htmlForm.isOutputText) htmlPage += "</td>";
        htmlPage += nextline;

        htmlPage += indenter(++numIndents, strIndent);
        if (!htmlForm.isOutputText) htmlPage += "<td>";
        htmlPage += "<div id=\"hideAudioOut\">Audio-Out:</div>";
        if (!htmlForm.isOutputText) htmlPage += "</td>";
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent);
        if (!htmlForm.isOutputText) htmlPage += "<td>";
        htmlPage += "<select name=\"AUDIO_OUT\" id=\"AUDIO_OUT\" size=\"1\" onChange=\"return audioOutChanged();\">" + nextline;

        for (i=0; i<htmlForm.audioOutTypes.size(); i++)
        {
            if (i==0)
                numIndents++;

            htmlPage += indenter(numIndents, strIndent) + "<option>" + htmlForm.audioOutTypes.get(i) + "</option>" + nextline;
        }
        //

        htmlPage += indenter(--numIndents, strIndent) + "</select>" + nextline;
        htmlPage += indenter(--numIndents, strIndent);
        if (!htmlForm.isOutputText) htmlPage += "</td>";
        htmlPage += nextline;
        htmlPage += indenter(--numIndents, strIndent);
        if (!htmlForm.isOutputText) htmlPage += "</tr>";
        //
        
        //Put SPEAK button and text
        htmlPage += nextline;
        htmlPage += indenter(numIndents, strIndent);
        if (!htmlForm.isOutputText) htmlPage += "<tr>";
        htmlPage += nextline;
        htmlPage += indenter(++numIndents, strIndent); 
        if (!htmlForm.isOutputText) htmlPage += "<td>";
        htmlPage += "<input type=\"button\" value=\"SPEAK\" id=\"SPEAK\" STYLE=\"font-size:11pt; font-weight=bold;\" onClick=\"return requestSynthesis();\">";
        if (!htmlForm.isOutputText) htmlPage += "</td>";
        htmlPage += nextline;

        htmlPage += indenter(numIndents, strIndent) + "<td>Click to send synthesis request to MARY server</td>" + nextline;
        
        //Embed audio file (if synthesis is not failed)
        String tmpVal = htmlForm.keyValuePairs.get("SYNTHESIS_OUTPUT");
        boolean isSynthesisFailed = false;
        if (tmpVal!=null && tmpVal.compareTo("FAILED")==0)
            isSynthesisFailed = true;
        
        if (!htmlForm.isOutputText && 
               htmlForm.outputAudioResponseID!=null && 
               htmlForm.outputAudioResponseID.length()>0 &&
               !isSynthesisFailed)
        {
            //Using embed
            htmlPage += indenter(numIndents, strIndent) + "<td><embed src=\"" + htmlForm.outputAudioResponseID + "\" autostart=\"true\" repeat=\"false\" width=200 height=40></td>" + nextline;

            //Using object+embed: Does not work in Firefox
            //htmlPage += indenter(numIndents, strIndent) + "<td><object type=\"" + htmlForm.mimeType + "\" data=\"" + htmlForm.outputAudioResponseID + "\" width=\"200\" height=\"40\">" + nextline;
            //htmlPage += indenter(++numIndents, strIndent) + "<param name=\"src\" value=\"" + htmlForm.outputAudioResponseID + "\"/>" + nextline;
            //htmlPage += indenter(numIndents, strIndent) + "<param name=\"autoplay\" value=\"false\" />" + nextline;
            //htmlPage += indenter(numIndents, strIndent) + "<param name=\"autoStart\" value=\"1\"/>" + nextline;
            //htmlPage += indenter(--numIndents, strIndent) + "</object></td>" + nextline;
        }
        //
        htmlPage += indenter(--numIndents, strIndent) + "</tr>" + nextline;
        //

        //Invisible fields for communication with server
        htmlPage += nextline;     
        //Tells server that this is a web browser client
        htmlPage += indenter(numIndents, strIndent) + "<input type=\"hidden\" name=\"WEB_BROWSER_CLIENT\" value=\"true\">" + nextline;
        //Requests example texts depending on input/output type and voice
        htmlPage += indenter(numIndents, strIndent) + "<input type=\"hidden\" name=\"EXAMPLE_TEXT\" value=\"\">" + nextline;    
        //Requests default page
        htmlPage += indenter(numIndents, strIndent) + "<input type=\"hidden\" name=\"DEFAULT_PAGE\" value=\"\">" + nextline;
        //Sends synthesis request
        htmlPage += indenter(numIndents, strIndent) + "<input type=\"hidden\" name=\"SYNTHESIS_OUTPUT\" value=\"\">" + nextline; 
        //Keeps locale info
        htmlPage += indenter(numIndents, strIndent) + "<input type=\"hidden\" name=\"LOCALE\" value=\"\">" + nextline;
        //Keeps voice info
        htmlPage += indenter(numIndents, strIndent) + "<input type=\"hidden\" name=\"VOICE\" value=\"\">" + nextline;
        //Keeps audio info
        htmlPage += indenter(numIndents, strIndent) + "<input type=\"hidden\" name=\"AUDIO\" value=\"\">" + nextline;
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