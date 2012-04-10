/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.language.sv;

import java.io.*;
import org.apache.commons.io.FileUtils;
import java.util.regex.*;
import marytts.server.MaryProperties;

/**
 * Class that uses an open source language detection software by Nakatani Shuyo to 
 * determine language on words not in either Swedish nor English dictionary. A log
 * file is kept over all words run through language detection. The log is emptied
 * every time the method 'process' in svPhonemizer is called.
 * 
 * 
 * @author Erik Sterneberg
 *
 */
public class LI {
	protected PrintWriter logfile;

	public LI() throws Exception{
		try{
			FileUtils.deleteQuietly(new File(MaryProperties.maryBase() + "/log/sv_LI.txt"));
			this.logfile = new PrintWriter(new FileWriter(new File(MaryProperties.maryBase() + "/log/sv_LI.txt"), true));	        
		} 
		catch (Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * Logs the words run through language detection along with the results.
	 * 
	 * @param s
	 */
	protected void log(String s){
		try{
			this.logfile.println(s);
			this.logfile.flush();			
		} 
		catch (Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * Detects the language of an input string. Only English and Swedish language models are bundled.
	 * 
	 * @param text
	 * @return String detected language
	 * @throws Exception
	 */
	public static String detectLang(String text) throws Exception{    	    	    	
		try{
			// Create file 
			FileWriter fstream = new FileWriter("/tmp/langdetect_temp.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(text);
			//Close the output stream
			out.close();
		}
		catch (Exception e){//Catch exception if any
			System.err.println("Error when detecting language: " + e.getMessage());
		}

		OutputStream stdin = null;
		InputStream stderr = null;
		InputStream stdout = null;

		// Run external language detector
		try {    		
			String path1 = MaryProperties.maryBase() + "/java/marytts/language/sv/langdetect/";
			String temp_outfile = "/tmp/langdetect_temp.txt"; 
			String command = "java -jar " + path1 + "lib/langdetect.jar --detectlang -d " + path1 + "profiles " + temp_outfile;

			Process pr = Runtime.getRuntime().exec(command);

			stdin = pr.getOutputStream();
			stderr = pr.getErrorStream();
			stdout = pr.getInputStream();
		}
		catch (Exception err){
			err.printStackTrace();
		}

		String detectedLanguage = null;
		BufferedReader detectedLanguageStream = null;
		try{
			detectedLanguageStream = new BufferedReader(new InputStreamReader(stdout)); // language detector process stdout   
			detectedLanguage = detectedLanguageStream.readLine();
		}
		catch(Exception err){
			err.printStackTrace();
		}

		// Process the sentence
		detectedLanguage = detectedLanguage.trim(); // removing trailing \n if any
		//System.err.println("LI output: " + detectedLanguage);
		Pattern pattern = Pattern.compile("^.*\\[([a-z]{2}):0\\.([0-9]{2})[0-9]*.*$");			
		Matcher matcher = pattern.matcher(detectedLanguage);

		// Close the streams related to the postagger process.
		detectedLanguageStream.close();
		stdin.close();
		stderr.close();
		stdout.close();

		if (matcher.find()){
			//System.out.println("The probability for the language of the highest probability: " + matcher.group(2));
			try{
				if (matcher.group(1).equals("en")){
					if (Integer.parseInt(matcher.group(2)) == 99){
						//System.out.println("The string was detected as English with a probability exceeding the threshold of 99 %");
						return matcher.group(1);
					}
					else
						return new String("sv");
				}
				else{        	
					return matcher.group(1); // Detected language was either Swedish, or it was English but not with high enough probability
				}
			}
			catch(Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}    
		else
			return null;

		return null;
	}    
}