/**
 * Copyright 2011 DFKI GmbH.
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



package marytts.language.fr;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.language.tib.datatypes.TibetanDataTypes;
import marytts.modules.InternalModule;
import marytts.modules.MaryXMLToText;
import marytts.server.Mary;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;

import org.apache.log4j.BasicConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;



/**
 * French phonemizer and POStagger. 
 * 
 * This class feeds the MaryXML with 
 * - words
 * - phonemes
 * - parts of speech
 * 
 * We call lia_phon and then apply preprocessing, and conversion to
 * SAMPA format.
 * 
 * LIA_phon developped by Frédéric Bechet, LIA.
 * 
 * @author Florent Xavier
 */



public class PhonemiserFR extends InternalModule
{
    
    
    public PhonemiserFR()
	{
	    super("FrenchPhonemiser",
	            MaryDataType.RAWMARYXML,
                MaryDataType.PHONEMES,
                Locale.FRENCH);
	    
	}
	
    
        
    /**
     * Very simple method that convert a string into array by splitting the \n
     * an get rid of the headers in LIA_phon output.
     * 
     * @param str
     * @return
     */
    public static String[] string2Array(String str)
    {
        String[] tab;
        tab = str.split("\n");  
        int k= tab.length-2;
        String[] tab2= new String[k];
        for(int i=0;i<k;i++)
        {
            tab2[i] = tab[i+1];
        }            
        return tab2;
    }
    
    
    		
    /**
     * A simple method that check the OS and then defines which command to execute.
     * 
     * 																		
     * @return
     */	
    public static String[] whichOS(String path)
	{
        //The command to launch
		String command1 = new String("");
		String command2 = new String("");
		
		//Get the name of the OS
		String os = System.getProperty("os.name").toLowerCase();
		
		//Test the OS and launch the appropriated command.
        if (os.indexOf("win")>-1)
        {
        	/*Windows Command
        	to do if we adapt LIA_phon to Windows*/
        }
        else if (os.indexOf("mac")>-1)
        {
        	//Mac command, an UNIX system
        }
        else if ((os.indexOf("nix")>-1) || (os.indexOf("nux"))>-1)
        {        
        	//Unix command            
        	command1 = path+"/script/lia_text2phon";
        	
        }
		String[] command = {command1};
		return command;
	}
        
	
        
    /**
     * Method that reads from the OuptutTEST.txt in ISO 8859-1
     * in order to convert it to MarXML and wite it in MaryXMLfr.txt
     * 
     * @param textPhon
     * @param textClean
     * @throws Exception 
     */
    public Document convert2MaryXML(String textPhon, String textClean, String path) throws Exception
    {
        //Word
        String WOR[];
        
        //Phoneme
        String PHON[];
        
        //POS
        String POS[];
        
    	//Instantiate the class getPOSorPhonemFR in order to get the phonemes and POSs.
    	getPOSorPhonemFR gP = new getPOSorPhonemFR();
    	gP.textInput = textPhon;
    	
    	//Get phoneme transcription 
    	String PHONE;
    	gP.whichPart = 1;
    	PHONE = (String) gP.phonemSeek();
    	PHON = string2Array(PHONE);

    	//Get POS
    	String POStagg;
    	gP.whichPart = 2;
    	POStagg = (String) gP.phonemSeek();
    	POStagg = gP.charReplace(POStagg);
    	POS = string2Array(POStagg);
    	
    	//Get word
        String WORD;
        String commandWord = new String(path+"/script/lia_nett");
        WORD = callLIA (commandWord, textClean, path); 
        WOR = string2Array(WORD);
    	
    	//Creating the MaryXML
    	Document marydoc = MaryXML.newDocument();
    	Element root = marydoc.getDocumentElement(); // <maryxml>
    	Element paragraph = MaryXML.appendChildElement(root, MaryXML.PARAGRAPH); 
    	// <p>
    	Element sentence = MaryXML.appendChildElement(paragraph, 
    	MaryXML.SENTENCE); // <s>
    	// assuming you have three arrays of same length, words, sampa, and pos:
    	for (int i=0; i<WOR.length; i++) 
    	{
    	     Element t = MaryXML.appendChildElement(sentence, MaryXML.TOKEN);
    	     t.setTextContent(WOR[i]);
    	     t.setAttribute("ph", PHON[i]);
             t.setAttribute("pos", POS[i]);
    	}
    	       
    	return marydoc;    	
    }
    
    
    
    /**
     * Method that call LIA_phon. The UNIX command is launched, and we give as input for
     * the process the text to phonetize directly. We don't need to give a text file
     * as argument.
     * 
     * @param Command
     * @param text
     * @throws Exception 
     */
    public static String callLIA (String Command, String text, String path) throws Exception
    {
        //env variable to send
        String[] env = new String[] {"LIA_PHON_REP="+path};
        
    	//Text processed
    	String textLIA = new String();
    	
    	//Create the runtime to execute lia_phon for Windows or for UNIX 
        Runtime runtime = Runtime.getRuntime();
        
    	//Create the process to execute lia_phon
        Process p1 = runtime.exec(Command, env); 
        
        //Input of the process p1, ie the text to phonetize
        PrintWriter toP1 = new PrintWriter(new 
        OutputStreamWriter(p1.getOutputStream(), "ISO-8859-1"), true);

        //Output of the process p1, ie the text phonetized
        BufferedReader fromP1 = new BufferedReader(new 
                InputStreamReader(p1.getInputStream(), "ISO-8859-1"));
        
        //Pass the text to phonetize
        toP1.println(text); 
        toP1.close();
        
        //Get back the phonetized text
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = fromP1.readLine()) != null) 
        {
           sb.append(line).append("\n");
        }
        
        //Convert it to strings and display it on the console
        textLIA = sb.toString();
        fromP1.close();
        
        return textLIA;
    	
    }
    
    
    
    /**
     * Method that calls the preprocessFR class in order to clean the text.
     * 
     * @param str
     * @return
     */
    public static String preprocess (String str)
    {
    	String txtOutput = new String();
    	preprocessFR pr = new preprocessFR();
    	pr.textUnclean = str;
    	txtOutput = pr.CleanText();
    	return txtOutput;
    }
    
    
    
    /**
     * Method that read from the XML. Basically, we try to get the plain text
     * as our whole code works with strings as input.
     * 
     * @param d
     * @return
     */
    public static String readFromXML(MaryData d)
    {
        Document doc = d.getDocument();
        
        NodeIterator ni = ((DocumentTraversal)doc).createNodeIterator(
            doc, NodeFilter.SHOW_TEXT, null, false);
        Text textNode;
        StringBuilder inputText = new StringBuilder();
        // Keep this loop in sync with the second loop, below:
        while ((textNode = (Text)ni.nextNode()) != null) 
        {
            String text = textNode.getData().trim();
            
            if (text.length() == 0) continue;
            // Insert a space character between non-punctuation characters:
            if (inputText.length() > 0 &&
                !Character.isWhitespace(inputText.charAt(inputText.length() - 1)) &&
                Character.isLetterOrDigit(text.charAt(0))) 
            {
                inputText.append(" ");
            }
            inputText.append(text);
        }
        
        String txt = new String();
        txt = inputText.toString();
        return txt;
    }
	
    
	
	/**
	 * The main. It should launch the proper version of lia_phon depending on which OS you have.
	 * Should be "thread safe", as we don't use any static or global variables (except for the 
	 * preprocessing classes).
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public MaryData process(MaryData d) throws Exception 
	{
	    logger.info("\n\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!! locale info="+MaryProperties.localePrefix(getLocale())+"!!!!!!!!!!!!!!!!!!\n\n");
	    
	    //LIA_Phon path
	    String LIA_PHON_REP = new String(MaryProperties.maryBase() + "/lib/modules/fr/lia_phon");
	    
	    //Text to phonetize in ISO 8859-1 format
	    String textInput = new String();
	    
	    //Text clean
	    String textClean = new String();
	    
	    //Text phonetized (the lia_phon output)
	    String textPhone = new String();
	    
	    //Read from the XML
	    textInput = readFromXML(d);
	    
		//clean the text
		textClean = preprocess(textInput);
		
        //Which OS?
        String wOS = new String("");
        Object Command[] = whichOS(LIA_PHON_REP);
                
        //os command to launch
        String osCommand1 = (String) Command[0];
                
        //Call LIA_phon and phonetize the text
        textPhone = callLIA(osCommand1, textClean, LIA_PHON_REP);
        
        //Convert to MaryXML
        Document marydoc;
        marydoc = convert2MaryXML(textPhone, textClean, LIA_PHON_REP);
        MaryData phonemesData = new MaryData(MaryDataType.PHONEMES, 
                Locale.FRENCH, false);
                phonemesData.setDocument(marydoc);
        return phonemesData;
	}    

	
	  
}