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

package de.dfki.lt.mary.dbselection;


import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.dom.MaryDomUtils;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.XML2UttAcoustParams;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.modules.TargetFeatureLister;


/**
 * Takes text and converts to features
 * Needs a running Mary server
 * 
 * @author Anna Hunecke
 *
 */
public class FeatureMakerMaryServer{
	
	//the Mary Client connected to the server
	protected static MaryClient mary;
	//buffer used for collecting words of a sentence
	protected static StringBuffer sentence;
	//stores result of credibility check for current sentence
	protected static boolean usefulSentence;	
	//list of sentences of a chunk of text
	protected static Map index2sentences;    
	//feature definition
	protected static FeatureDefinition featDef;
	//print writer for writing list of processed files
	protected static PrintWriter doneOut;
	//print writer for writing list of unreliable files
	protected static PrintWriter unreliableLog;
	//the list of files containing the text to be processed
	protected static String textFiles;
	//directory containing the features
	protected static String featOutDirName;
	//directory containing the sentences for the features
	protected static String sentOutDirName;
	//file containing the list of already processed sentences
	protected static String doneFileName;
	//host of the Mary server
	protected static String maryHost;
	//port of the Mary server
	protected static String maryPort;
	//maximum time in ms to process a chunk of text
	protected static int timeOutAfter;
	//log file for unreliable sentences
	protected static String unreliableLogFile;
	//if true, credibility is strict, else crebibility is lax
	protected static boolean strictCredibility;
	//file for the final list of basenames
	protected static String basenamesOutFile;
	//index of the feature/sentence directory
	protected static int outDirIndex;
	
	
	public static void main(String[] args)throws Exception{
		
		System.out.println("FeatureMaker started...");
		/* check the arguments */
		if (!readArgs(args)){
			printUsage();
			return;
		}
				
		/* read in the basenames */
		BufferedReader basenameIn = 
			new BufferedReader(
					new FileReader(
							new File(textFiles)));
		String line;
		List basenames = new ArrayList();
		while ((line=basenameIn.readLine())!= null){
			if (line.equals("")) continue;
			basenames.add(line.trim());
		}
		
		/* Start the Mary client */
		System.setProperty("server.host", maryHost);
		System.setProperty("server.port", maryPort);
		mary = new MaryClient();
		
		/* start the Credibility Checker */
		unreliableLog = 
			new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(
									new File(unreliableLogFile),true)),true);   
		
		/* create output dirs */
		File featOutDir = new File(featOutDirName);
		if (!featOutDir.exists()) featOutDir.mkdir();
		
		File sentOutDir = new File(sentOutDirName);
		if (!sentOutDir.exists()) sentOutDir.mkdir();
		
		/* read in the list of already processed files */
		List alreadyDone = readInDoneFiles(doneFileName);
		
		/* open the file to write the basenames to */
		PrintWriter basenamesOut = 
			new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(
									new File(basenamesOutFile),true)),true);   
		
		/* loop over the files */
		System.out.println("Looping over files...");
		for (Iterator it = basenames.iterator();it.hasNext();){
			String filename = (String) it.next();
			//continue, if we already processed this sentence
			if (alreadyDone.remove(filename)) continue;
			
			System.out.println(filename);
			doneOut.println(filename);
			//open the article file
			BufferedReader fileIn =
				new BufferedReader(
						new InputStreamReader(
								new FileInputStream(filename),"UTF-8"));
			//store whole article in one buffer
			StringBuffer fileBuf = new StringBuffer();
			while((line= fileIn.readLine())!=null){   
				if (line != ""){
					fileBuf.append(line+"\n");
				}
			}
			fileIn.close();
            String text = fileBuf.toString();
			if (text.equals("")
                    || text.equals("\n")) continue;
			//process the article in a different thread
			MaryCallerThread mct = new MaryCallerThread(text,filename);
			mct.start();
			
			// allow the separate thread to process a limited time span
			mct.join(timeOutAfter);
			
			// check if there was a timeout
			if(!(mct.isFinished())){
				// resolution was stopped due to time out
				mct.interrupt();
				mct.join();
				System.out.println("Timeout when processing sentence "+filename);
				doneOut.println(filename);
				continue;
			}
			if (!mct.wasSuccessful()){
				System.out.println("Could not process sentence "+filename);
				doneOut.println(filename);
				continue;
			}
			mct = null;
			File newFeatDir;
			File newSentDir;
			String f = filename.substring(0, filename.lastIndexOf('.'));
			f = f.substring(f.lastIndexOf('/'),f.length());
			try{
				newFeatDir = new File(featOutDirName+"/"+f);
				newFeatDir.mkdir();
				newSentDir = new File(sentOutDirName+"/"+f);
				newSentDir.mkdir();
			}catch(Exception e){
				//the featOutDir is full
				//choose a different output dir
				outDirIndex++;
				featOutDirName = featOutDirName+outDirIndex;
				sentOutDirName = sentOutDirName+outDirIndex;
				//make new feature and sentence directories
				File newDir = new File(featOutDirName);
				if (!newDir.exists()) newDir.mkdir();
				newDir = new File(sentOutDirName);
				if (!newDir.exists()) newDir.mkdir();
				//make new directory for the current text
				newFeatDir = new File(featOutDirName+"/"+f);
				newFeatDir.mkdir();
				newSentDir = new File(sentOutDirName+"/"+f);
				newSentDir.mkdir();
			}
			int index=0;
			boolean wroteNothing = true;
			
			for (Iterator it2=index2sentences.keySet().iterator();it2.hasNext();){
				Integer nextKey = (Integer) it2.next();
				try{
					String sentence = (String) index2sentences.get(nextKey);
                    //System.out.println(sentence);
					index = nextKey.intValue();
					MaryData d = 
						processSentence(sentence,filename);
					if (d==null) continue;
					/* get and dump the features of the sentence */                     
					getFeatures(newFeatDir+"/"+f+"_"+index+".feats",d);     
					/* dump the sentence */
					dumpSentence(newSentDir+"/"+f+"_"+index+".txt",sentence);
					basenamesOut.println(newFeatDir+"/"+f+"_"+index+".feats");
					wroteNothing = false;
				}catch (Exception e){
					System.out.println("Error processing sentence "
							+newFeatDir+"_"+index+" :");
					e.printStackTrace();
				}
			}//end of loop over sentences
			if (wroteNothing){
				//no feature files have actually been created
				//remove directories
				newFeatDir.delete();
				newSentDir.delete();
			}             
		} //end of loop over articles             
		doneOut.close();
		basenamesOut.close();
		System.out.println("Done");
	}//end of main method
	
	/**
	 * Print usage of this program 
	 *
	 */
	protected static void printUsage(){
		System.out.println("Usage:\n"
				+"java -cp $CLASSPATH -ea -Dendorsed.dirs=$MARYBASE/lib/endorsed "
				+"-Dmary.base=$MARYBASE "
				+"de.dfki.lt.mary.dbselection.FeatureMakerMaryServer\n"
				+"Please see readme file for details of starting this program\n\n"
				+"Arguments:\n"
				+"-textFiles <file>: File containing the list of text files to be "
				+"processed. Default: textFiles.txt\n\n"
				+"-doneFile <file>: File containing the list of files that have already "
				+"been processed. This file is created automatically during the "
				+"run of the program. Default: done.txt\n\n"
				+"-featureDir <file>: Directory where the features are stored. "
				+"Default: features1. Per default, appropriate sentence files are stored "
				+"in sentences1. The index of feature/sentence dir is increased when the feature "
				+"dir is full.\n\n"
				+"-host <hostname>: Host of the Mary server. Default: localhost\n\n"
				+"-port <port>: Port of the Mary server. Default: 59125\n\n"
				+"-timeOut <time in ms>: The time in milliseconds the Mary server is allowed "
				+"to split the text of a file into sentence. After the limit is exceeded, "
				+"processing on this file is stopped, and the program continues to the "
				+"next file. Default 30000ms\n\n"
				+"-unreliableLog <file>: Logfile for the unreliable sentence. "
				+"Default: unreliableSents.log\n\n"
				+"-credibility <setting>: Setting that determnines what kind of sentences "
				+"are regarded as credible. There are two settings: strict and lax. With "
				+"setting strict, only those sentences that contain words in the lexicon "
				+"or words that were transcribed by the preprocessor are regarded as credible; "
				+"the other sentences as unreliable. With setting lax, also those words that "
				+"are transcribed with the Denglish and the compound module are regarded credible. "
				+"Default: strict\n\n"
				+"-basenames <file>: File containing the list of feature files that can be "
				+"used in the selection algorithm. Default: basenames.lst\n");
	}
	
	/**
	 * Read and parse the command line args
	 * 
	 * @param args the args
	 * @return true, if successful, false otherwise
	 */
	protected static boolean readArgs(String[] args){
		//initialise default values
		textFiles = "./textFiles.txt";
		doneFileName = "./done.txt";
		featOutDirName = "./features1";
		maryHost = "localhost";
		maryPort = "59125";
		timeOutAfter = 30000;
		unreliableLogFile = "./unreliableSents.log";
		strictCredibility = true;
		basenamesOutFile = "./basenames.lst";
		
		//now parse the args
		int i = 0;
		while (args.length>i){
			System.out.println(args[i]);
			if (args[i].equals("-basenames")){
				if (args.length<i+1){
					i++;
					basenamesOutFile = args[i];
					System.out.println("-basenames "+args[i]);
				} else {
					System.out.println("Please specify a file after -basenames");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-textFiles")){
				if (args.length<i+1){
					i++;
					textFiles = args[i];
					System.out.println("-textFiles "+args[i]);
				} else {
					System.out.println("Please specify a file after -textFiles");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-featureDir")){
				if (args.length<i+1){
					i++;
					featOutDirName = args[i];
					System.out.println("-featureDir "+args[i]);
				} else {
					System.out.println("Please specify a directory after -featureDir");
					return false;
				}
				i++;
				continue;
			}
			
			if (args[i].equals("-doneFile")){
				if (args.length<i+1){
					i++;
					doneFileName = args[i];
					System.out.println("-doneFile "+args[i]);
				} else {
					System.out.println("Please specify a file after -doneFile");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-host")){
				if (args.length<i+1){
					i++;
					maryHost = args[i];
					System.out.println("-host "+args[i]);
				} else {
					System.out.println("Please specify a server host after -host");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-port")){
				if (args.length<i+1){
					i++;
					maryPort = args[i];
					System.out.println("-port "+args[i]);
				} else {
					System.out.println("Please specify a server port after -port");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-timeOut")){
				if (args.length<i+1){
					i++;
					timeOutAfter = Integer.parseInt(args[i]);
					System.out.println("-timeOut "+args[i]);
				} else {
					System.out.println("Please specify the timeout (in ms) after -timeOut");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-unreliableLog")){
				if (args.length<i+1){
					i++;
					unreliableLogFile = args[i];
					System.out.println("unreliableLog "+unreliableLogFile);
				} else {
					System.out.println("Please specify a file after -unreliableLog");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-credibility")){
				if (args.length<i+1){
					i++;
					String credibilitySetting = args[i];
					if (credibilitySetting.equals("strict")
							|| credibilitySetting.equals("strict")){
						strictCredibility = true;
						System.out.println("credibility strict");
					} else {
						if (credibilitySetting.equals("lax")
								|| credibilitySetting.equals("lax")){
							strictCredibility = false;
							System.out.println("credibility lax");
						} else {
							System.out.println("Unknown argument for credibility "
									+credibilitySetting);
							return false;
						}
					}
				} else {
					System.out.println("Please specify setting \"strict\" or \"lax\" after -credibility");
					return false;
				}
				i++;
				continue;
			}
			//unknown argument
			System.out.println("Unknown argument "+args[i]);
			return false;
		}
		//construct the name of the sentence dir from the feature dir
		outDirIndex = 1;
		if (featOutDirName.equals("features1")){
			//default name
			sentOutDirName = "./sentences1";
		} else {
			if (featOutDirName.matches(".*\\d+")){
				//featOutDir ends with digit
				//add digit to end of sentOutDirName
				Pattern digitPattern = Pattern.compile("\\d+");
				Matcher m = digitPattern.matcher(featOutDirName);
				m.find();
				outDirIndex = Integer.parseInt(m.group());  
				sentOutDirName = "./sentences"+outDirIndex;
			} else {
				//no digit at end of feature dir name - add one
				featOutDirName = featOutDirName+"1";
				sentOutDirName = "./sentences1";
			}
		}
		return true;
	}
	
	
	/**
	 * Process one sentences from text to target features
	 * 
	 * @param nextSentence the sentence
	 * @param filename the file containing the sentence
	 * @return the result of the processing as MaryData object
	 */
	protected static MaryData processSentence(String nextSentence,
			String filename){
		//do a bit of normalization
		nextSentence = nextSentence.replaceAll("\\\\","").trim();
		nextSentence = nextSentence.replaceAll("\\s/\\s","").trim();
		nextSentence = nextSentence.replaceAll("^/\\s","").trim();
		MaryData d = null;
		try{                    
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			//process and dump
			mary.process(nextSentence, "TEXT_DE","TARGETFEATURES_DE" , null, null, os);
			//read into mary data object                
			d = new MaryData(MaryDataType.get("TARGETFEATURES_DE"));
			d.readFrom(new ByteArrayInputStream(os.toByteArray()));			
		} catch (Exception e){
			e.printStackTrace();
			if (d!=null){  
				if (d.getPlainText()!=null){
					System.out.println("Error processing sentence "
							+filename
							+": \""+nextSentence+"\":\n"+d.getPlainText()
							+"; skipping sentence");
				} else {
					if (d.getDocument() != null){
						StringBuffer docBuf = new StringBuffer();
						getXMLAsString(d.getDocument(),docBuf);
						System.out.println("Error processing sentence "
								+": \""+nextSentence+"\":\n"+docBuf.toString()
								+"; skipping sentence");
					} else {
						if (d.getUtterances() != null){
							List utterances = d.getUtterances();
							Iterator it = utterances.iterator();
							System.out.println("Error processing sentence "
									+": \""+nextSentence+"\":\n");
							while (it.hasNext()) {
								Utterance utterance = (Utterance) it.next();
								StringWriter sw = new StringWriter();
								PrintWriter pw = new PrintWriter(sw);
								utterance.dump(pw, 2, "", true); // padding, justRelations
								System.out.println(sw.toString());
							}
							System.out.println("; skipping sentence");
						}else {
							System.out.println("Error processing sentence "
									+filename
									+": \""+nextSentence+"\"; skipping sentence");                        
						}
					}
				}
			} else {
				System.out.println("Error processing sentence "
						+filename
						+": \""+nextSentence+"\"; skipping sentence");                        
			}
			return null;
		}
		catch (AssertionError ae){
			ae.printStackTrace();
			System.out.println("Error processing sentence "
					+filename
					+": \""+nextSentence+"\"; skipping sentence");
			return null;
		}
		return d;
		
		
		
	}
	
	
	
	/**
	 * Phonemise the given document with 
	 * the help of JPhonemiser
	 * 
	 * @param d
	 * @return
	 */
	protected static boolean checkCredibility(Element t){
		boolean usefulSentence = true;	
		if (t.hasAttribute("sampa")){
			//we have a transcription
			if (t.hasAttribute("g2p_method")) {
				//check method of transcription
				String method = t.getAttribute("g2p_method");
				if (!method.equals("lexicon") &&
						!method.equals("userdict")){
					if (strictCredibility){
						//method other than lexicon or userdict -> unreliable
						usefulSentence = false;
					} else {
						//lax credibility criterion
						if (!method.equals("phonemiseDenglish") &&
								!method.equals("compound")){
							//method other than lexicon, userdict, phonemiseDenglish 
							//or compound -> unreliable
							usefulSentence = false;
						} //else method is phonemiseDenglish or compound -> credible						
					}
				}// else method is lexicon or userdict -> credible				
			} //else no method -> preprocessed -> credible			
		} else {      
			//we dont have a transcription
			if (t.hasAttribute("pos") &&
					!t.getAttribute("pos").startsWith("$")){					
				//no transcription given -> unreliable	
				usefulSentence = false;
				
			} //else punctuation -> credible
		} 
		return usefulSentence;
	}
		
		
		
		/**
		 * Process the target features
		 * and print them to the given file 
		 * 
		 * @param filename the file to print the features to
		 * @param d the target features as Mary Data object
		 * @throws Exception
		 */
		protected static void getFeatures(String filename,MaryData d)throws Exception{
			BufferedReader featsDis = 
				new BufferedReader(
						new InputStreamReader(
								new ByteArrayInputStream(d.getPlainText().getBytes())));
			String line;
			if (featDef == null){
				featDef = new FeatureDefinition(featsDis,false);            
			} else {
				//read until an empty line occurs
				while ((line = featsDis.readLine()) != null){
					if (line.equals("")) break;
				}
			}
			
			/* get the indices of our features */
			int phoneIndex = featDef.getFeatureIndex("mary_phoneme");
			int nextPhoneIndex = featDef.getFeatureIndex("mary_next_phoneme");
			int nextPhoneClassIndex = featDef.getFeatureIndex("mary_selection_next_phone_class");
			int prosodyIndex = featDef.getFeatureIndex("mary_selection_prosody");
			/* loop over the feature vectors */
			List featureLines = new ArrayList();
			while ((line = featsDis.readLine()) != null){
				if (line.equals("")) break;
				featureLines.add(line);
			}
			int numLines = featureLines.size();
			//System.out.println("num vectors = "+numLines);
			byte[][] featVects = new byte[numLines][];
			for (int i=0;i<numLines;i++){
				line = (String) featureLines.get(i);
				String[] fv = line.split(" ");
				String phoneString = fv[phoneIndex];
				byte[] nextVector = new byte[4];
				nextVector[0] = 
					featDef.getFeatureValueAsByte(phoneIndex,phoneString);
				nextVector[1] = 
					featDef.getFeatureValueAsByte(nextPhoneIndex,fv[nextPhoneIndex]);
				nextVector[2] = 
					featDef.getFeatureValueAsByte(nextPhoneClassIndex,fv[nextPhoneClassIndex]);
				nextVector[3] = 
					featDef.getFeatureValueAsByte(prosodyIndex,fv[prosodyIndex]);
				featVects[i] = nextVector;
				
				
			} //end of while-loop over the feature vectors
			/* write the features to disk */
			DataOutputStream features = 
				new DataOutputStream(
						new BufferedOutputStream(
								new FileOutputStream(
										new File(filename))));           
			features.writeInt(numLines);
			for (int i=0;i<featVects.length;i++){
				byte[] nextFeatVects = featVects[i];
				if (nextFeatVects == null){
					System.out.println("nextFeatVects are null at index "+i);
				}
				features.writeByte(nextFeatVects[0]);
				features.writeByte(nextFeatVects[1]);
				features.writeByte(nextFeatVects[2]);
				features.writeByte(nextFeatVects[3]);
			}
			features.flush();
			features.close();
			
			
		}
		
		/**
		 * Print the given sentence to the given file
		 * 
		 * @param filename the file
		 * @param sentence the sentence
		 * @throws Exception
		 */
		protected static void dumpSentence(String filename, String sentence)
		throws Exception{
			
			PrintWriter out = 
				new PrintWriter(
						new OutputStreamWriter(
								new FileOutputStream(
										new File(filename)),"UTF-8"));
			out.println(sentence);
			out.close();        
		}
		
		
		/**
		 * Read the list of already processed files
		 * 
		 * @param doneDirsTextName the file to read from
		 * @return the list of already processed files
		 * @throws Exception
		 */
		protected static List readInDoneFiles(String doneFilesTextName) throws Exception{
			File doneDirsText = new File(doneFilesTextName);
			List doneList = new ArrayList();
			
			if (doneDirsText.exists()){
				
				BufferedReader doneIn =
					new BufferedReader(new FileReader(doneDirsText));
				String line;
				
				while((line=doneIn.readLine()) != null){
					doneList.add(line.trim());
				}
				doneIn.close();
			} 
			
			doneOut = new PrintWriter(new FileWriter(doneDirsText,true),true);
			return doneList;
		}
		
		/**
		 * Split the content of the file
		 * into separate sentences
		 * 
		 * @param file the file
		 * @return true, if successful
		 * @throws Exception
		 */
		protected static boolean splitIntoSentences(String text, String filename)throws Exception{
			index2sentences = new TreeMap();
			Document doc = phonemiseText(text);
			if (doc == null) return false;
			NodeList sentences = doc.getElementsByTagName("s");   
			
			int sentenceIndex = 1;
			for (int j=0;j<sentences.getLength();j++){
				Node nextSentence = sentences.item(j);
				//ignore all non-element children
				if (!(nextSentence instanceof Element)) continue; 
				sentence = null;
				//get the tokens
				NodeList tokens = nextSentence.getChildNodes();
				usefulSentence = true;
				for (int k=0;k<tokens.getLength();k++){
					Node nextToken = tokens.item(k);
					//ignore all non-element children
					if (!(nextToken instanceof Element)) continue; 
					collectTokens(nextToken);                            
				}
				if (sentence!=null){
					if (usefulSentence){	
						//store sentence in sentence map
						index2sentences.put(new Integer(sentenceIndex),sentence.toString());  
					} else {
						//just print useless sentence to log file
						unreliableLog.println(filename+"; "+sentenceIndex+": "+sentence
								+" : is unreliable");
					}
					sentenceIndex++;
				} else {
					//ignore
					//System.out.println("NULL SENTENCE!!!");
				}
			} 
			return true;
		}
		
		/**
		 * Process the given text with the MaryClient
		 * from Text to Chunked
		 * 
		 * @param textString the text to process
		 * @return the resulting XML-Document
		 * @throws Exception
		 */
		protected static Document phonemiseText(String textString) throws Exception{
			try{
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				//process and dump
				mary.process(textString, "TEXT_DE","PHONEMISED_DE" , null, null, os);
				//read into mary data object                
				MaryData maryData = new MaryData(MaryDataType.get("PHONEMISED_DE"));
				maryData.readFrom(new ByteArrayInputStream(os.toByteArray()));
				return maryData.getDocument();
			} catch (Exception e){
				e.printStackTrace();
                System.out.println(textString);
				return null;            
			}
			
		}
		
		/**
		 * Collect the tokens of a sentence
		 * 
		 * @param nextToken the Node to start from
		 */
		protected static void collectTokens(Node nextToken){
			String name = nextToken.getLocalName();
			if (name.equals("t")){
				if (!checkCredibility((Element) nextToken)){
					//memorize that we found unreliable sentence
					usefulSentence = false;
				}
				if (sentence == null){
					sentence = new StringBuffer();
					//first word of the sentence
					sentence.append(MaryDomUtils.tokenText((Element)nextToken));
				} else {
					String pos = ((Element)nextToken).getAttribute("pos");
					if (pos.startsWith("$")){
						//punctuation
						String tokenText = MaryDomUtils.tokenText((Element)nextToken);
						//just append without whitespace
						sentence.append(tokenText);
						
					} else {
						//normal word, append a whitespace before it
						sentence.append(" "+MaryDomUtils.tokenText((Element)nextToken));
					}
				}
			} else {
				if (name.equals("mtu")){
					//get the tokens
					NodeList mtuTokens = nextToken.getChildNodes();
					for (int l=0;l<mtuTokens.getLength();l++){
						Node nextMTUToken = mtuTokens.item(l);
						//ignore all non-element children
						if (!(nextMTUToken instanceof Element)) continue; 
						collectTokens(nextMTUToken);
					}
				}
				
			}
		}
		
		/**
		 * Convert the given xml-node and its subnodes to Strings
		 * and collect them in the given Stringbuffer
		 * 
		 * @param motherNode the xml-node
		 * @param ppText the Stringbuffer
		 */
		protected static void getXMLAsString(Node motherNode,StringBuffer ppText){
			NodeList children = motherNode.getChildNodes();
			for (int i=0;i<children.getLength();i++){
				Node nextChild = children.item(i);
				String name = nextChild.getLocalName();
				if (name == null){
					continue;
				}          
				
				ppText.append("<"+name);
				if (nextChild instanceof Element){
					if (nextChild.hasAttributes()){
						NamedNodeMap atts = nextChild.getAttributes();
						for (int j=0;j<atts.getLength();j++){
							String nextAtt = atts.item(j).getNodeName();
							ppText.append(" "+nextAtt+"=\""
									+((Element)nextChild).getAttribute(nextAtt)
									+"\"");
						}
					}
					
				}
				if (name.equals("boundary")){
					ppText.append("/>\n");
					continue;
				}
				ppText.append(">\n");
				if (name.equals("t")){
					ppText.append(MaryDomUtils.tokenText((Element)nextChild)
							+"\n</t>\n");                
				} else {
					if (nextChild.hasChildNodes()){
						getXMLAsString(nextChild,ppText);
					} 
					ppText.append("</"+name+">\n");
				}
			}
		}
		
		/**
		 * Class for processing one chunk of text
		 * 
		 * @author Anna
		 *
		 */
		static class MaryCallerThread extends Thread{
			
			protected String text;
			protected String filename;
			protected boolean finished;
			protected boolean successful;
			
			/**
			 * Build a new MaryCallerThread
			 * 
			 * @param file the file to process
			 */
			public MaryCallerThread(String text, String filename){
				this.text = text;
				this.filename = filename;
				finished = false;
				successful = false;
				setName("mary caller");
			}
			
			/**
			 * Process the file
			 */
			public void run(){
				try{
					successful = splitIntoSentences(text,filename);
				}catch(Exception e){
					e.printStackTrace();
					throw new Error("Error processing text");
				}
				finished = true;
			}
			
			public boolean isFinished(){
				return finished;
			} 
			
			public boolean wasSuccessful(){
				return successful;
			}
		}
		
		
	}