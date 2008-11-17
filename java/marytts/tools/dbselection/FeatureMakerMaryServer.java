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

package marytts.tools.dbselection;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import marytts.client.MaryClient;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.features.FeatureDefinition;
import marytts.htsengine.HTSModel;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.speech.freetts.Utterance;



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
//	protected static StringBuffer sentence;
	//stores result of credibility check for current sentence
	protected static boolean usefulSentence;
    protected static boolean unknownWords;
    protected static boolean strangeSymbols;
    
	//list of sentences of a chunk of text
//	protected static Map <Integer,String>index2sentences;    
	//feature definition
	protected static FeatureDefinition featDef;
	//print writer for writing list of processed files
//	protected static PrintWriter doneOut;
	//print writer for writing list of unreliable files

//    protected static PrintWriter unreliableLog;
	//the list of files containing the text to be processed

    protected static String textFiles;
	//file containing the list of already processed sentences
	protected static String logFileName;
    //host of the Mary server
	protected static String maryHost;
	//port of the Mary server
	protected static String maryPort;
	//maximum time in ms to process a chunk of text
	protected static int timeOutAfter;
    //if true, credibility is strict, else crebibility is lax
	protected static boolean strictCredibility;

    protected static int numSentences = 0;
    protected static int numUnreliableSentences = 0;
    
    protected static DBHandler wikiToDB = new DBHandler();
	
	public static void main(String[] args)throws Exception{
		
		System.out.println("FeatureMaker started...");
       /* 
        DateFormat fullDate = new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss");
        Date dateIni = new Date();
        String dateStringIni = fullDate.format(dateIni);
        */
		/* check the arguments */
		if (!readArgs(args)){
			printUsage();
			return;
		}
		
		/* Start the Mary client */
		System.setProperty("server.host", maryHost);
		System.setProperty("server.port", maryPort);
		mary = new MaryClient();
		
		/* start the Credibility Checker */
		       
        /* Here the DB connection for reliable sentences is open */
         wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");
         //wikiToDB.createDBConnection("penguin.dfki.uni-sb.de","MaryDBSelector","MaryDBSel_admin","p4rpt3jr");
         // check if tables exist
         wikiToDB.createDataBaseSelectionTable();
        
         // Get the set of id for unprocessed records in clean_text
         // this will be useful when the process is stoped and then resumed
         String textId[];
         textId = wikiToDB.getUnprocessedTextIds();
         String text;
         
		
		/* loop over the text records in clean_text table of wiki */
        // once procesed the clean_text records are marked as processed=true, so here retrieve
        // the next clean_text record untill all are processed.
		System.out.println("Looping over clean_text records from wikipedia...");       
        PrintWriter pw = new PrintWriter(new FileWriter(new File(logFileName)));
        
        Vector<String> sentenceList;  // this will be the list of sentences in each clean_text
        int i, j;
        for(i=0; i<textId.length; i++){
          // get next unprocessed text  
          
          text = wikiToDB.getCleanText(textId[i]); 
          //System.out.println("Processing text id=" + textId[i] + " text length=" + text.length());
          
	      if (text.equals("") || text.equals("\n")) continue;
          
          sentenceList = splitIntoSentences(text, textId[i], pw);
          
          
    /*     
		  //process the article in a different thread
		  MaryCallerThread mct = new MaryCallerThread(text, textId[i]);
		  mct.start();			
	      // allow the separate thread to process a limited time span
	      mct.join(timeOutAfter);			
		  // check if there was a timeout
		  if(!(mct.isFinished())){
				// resolution was stopped due to time out
				mct.interrupt();
				mct.join();
				pw.println("Timeout when processing text id="+textId[i]);				
				continue;
		  }
		  if (!mct.wasSuccessful()){
				pw.println("Could not process text id="+textId[i]);				
				continue;
		  }
		  mct = null;
	*/
          if( sentenceList != null ) {
              
		  int index=0;			
          // loop over the sentences
          int numSentencesInText=0;
          String newSentence;
          byte feas[];  // for directly saving a vector of bytes as BLOB in mysql DB
          for(j=0; j<sentenceList.size(); j++) {
				newSentence = sentenceList.elementAt(j);
                MaryData d = processSentence(newSentence,textId[i]);
				if (d!=null){
				  // get the features of the sentence  
				  feas = getFeatures(d);     
                  // Insert in the database the new sentence and its features.
                  numSentencesInText++;
                  wikiToDB.insertSentence(newSentence,feas, true, false, false, Integer.parseInt(textId[i]));
                }
                     		
	      }//end of loop over list of sentences
          sentenceList.clear();
          

          numSentences += numSentencesInText;
          pw.println("Inserted " + numSentencesInText + " sentences from text id=" + textId[i] 
                                 + " (Total reliable = "+ numSentences+")\n");
          System.out.println("Inserted " + numSentencesInText + " sentences from text id=" 
                             + textId[i] + " (Total reliable = "+ numSentences+")\n");
          
          }
                         
		} //end of loop over articles    
        
        wikiToDB.closeDBConnection();
     /*   
        Date dateEnd = new Date();
        String dateStringEnd = fullDate.format(dateEnd);
        pw.println("numSentencesInText;=" + numSentences);
        pw.println("Start time:" + dateStringIni + "  End time:" + dateStringEnd);
        
          
          */
        pw.close(); 
        
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
				+"marytts.tools.dbselection.FeatureMakerMaryServer\n"
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
		logFileName = "./featureMaker.log";
		maryHost = "localhost";
		maryPort = "59125";
		timeOutAfter = 30000;
		strictCredibility = true;
		
		//now parse the args
		int i = 0;
		while (args.length>i){
			System.out.println(args[i]);
			if (args[i].equals("-textFiles")){
				if (args.length>i+1){
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
			if (args[i].equals("-logFile")){
				if (args.length>i+1){
					i++;
					logFileName = args[i];
					System.out.println("-logFile "+args[i]);
				} else {
					System.out.println("Please specify a file after -logFile");
					return false;
				}
				i++;
				continue;
			}
			if (args[i].equals("-host")){
				if (args.length>i+1){
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
				if (args.length>i+1){
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
				if (args.length>i+1){
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
			if (args[i].equals("-credibility")){
				if (args.length>i+1){
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

		return true;
	}
	
	
	/**
	 * Process one sentences from text to target features
	 * 
	 * @param nextSentence the sentence
	 * @param filename the file containing the sentence
	 * @return the result of the processing as MaryData object
	 */
	protected static MaryData processSentence(String nextSentence, String textId){
		//do a bit of normalization
		nextSentence = nextSentence.replaceAll("\\\\","").trim();
		nextSentence = nextSentence.replaceAll("\\s/\\s","").trim();
		nextSentence = nextSentence.replaceAll("^/\\s","").trim();
		MaryData d = null;
		try{                    
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			//process and dump
			mary.process(nextSentence, "TEXT","TARGETFEATURES", "en_US", null, "hsmm-slt", os);
			//read into mary data object                
			//d = new MaryData(MaryDataType.get("TARGETFEATURES"), null);
            d = new MaryData(MaryDataType.TARGETFEATURES, Locale.US);
            
			d.readFrom(new ByteArrayInputStream(os.toByteArray()));			
		} catch (Exception e){
			e.printStackTrace();
			if (d!=null){  
				if (d.getPlainText()!=null){
					System.out.println("Error processing sentence "
							+textId
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
									+textId
									+": \""+nextSentence+"\"; skipping sentence");                        
						}
					}
				}
			} else {
				System.out.println("Error processing sentence "
						+textId
						+": \""+nextSentence+"\"; skipping sentence");                        
			}
			return null;
		}
		catch (AssertionError ae){
			ae.printStackTrace();
			System.out.println("Error processing sentence "
					+textId
					+": \""+nextSentence+"\"; skipping sentence");
			return null;
		}
		return d;
		
		
		
	}
	
	
	
	/**
	 * Phonemise the given document with 
	 * the help of JPhonemiser
     * 
     * g2p_method
     * "contains-unknown-words" or "contains-strange-symbols",
	 * 
	 * @param d
	 * @return 0 if the sentence is useful
     *         1 if the sentence contains unknownWords
     *         2 if the sentence contains strangeSymbols
	 */
	protected static int checkCredibility(Element t){
        
		//boolean newUsefulSentence = true;
        int newUsefulSentence = 0;
        
		if (t.hasAttribute("ph")){
			//we have a transcription
			if (t.hasAttribute("g2p_method")) {
				//check method of transcription
				String method = t.getAttribute("g2p_method");
				if (!method.equals("lexicon") && !method.equals("userdict")){
					if (strictCredibility){
						//method other than lexicon or userdict -> unreliable
						//newUsefulSentence = false;
                        newUsefulSentence = 1;
                       
					} else {
						//lax credibility criterion
						if (!method.equals("phonemiseDenglish") && !method.equals("compound")){
							//method other than lexicon, userdict, phonemiseDenglish 
							//or compound -> unreliable
							//newUsefulSentence = false;
                            newUsefulSentence = 1;
                            
						} //else method is phonemiseDenglish or compound -> credible						
					}
				}// else method is lexicon or userdict -> credible				
			} //else no method -> preprocessed -> credible			
		} else {      
			//we dont have a transcription
			if (t.hasAttribute("pos") && !t.getAttribute("pos").startsWith("$")){					
				//no transcription given -> unreliable	
				//newUsefulSentence = false;
                //System.out.println("t.getTextContent = " + t.getTextContent() + "  t.getAttribute=" + t.getAttribute("pos"));
                newUsefulSentence = 2;
                
			} //else punctuation -> credible
		} 
		return newUsefulSentence;
	}
		
		
		
		/**
		 * Process the target features
		 * and print them to the given file 
		 * 
		 * @param filename the file to print the features to
		 * @param d the target features as Mary Data object
		 * @throws Exception
		 */
		protected static byte[] getFeatures(MaryData d)throws Exception{
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
            
            // DE example
            /*
			int phoneIndex = featDef.getFeatureIndex("phoneme");
			int nextPhoneIndex = featDef.getFeatureIndex("next_phoneme");
			int phoneIndex = featDef.getFeatureIndex("phoneme");
			int nextPhoneIndex = featDef.getFeatureIndex("next_phoneme");
			int nextPhoneClassIndex = featDef.getFeatureIndex("selection_next_phone_class");
			int prosodyIndex = featDef.getFeatureIndex("selection_prosody");
            */
            // EN example (not sure if these features are adequate! just for testing)
            int phoneIndex = featDef.getFeatureIndex("phoneme");
            int nextPhoneIndex = featDef.getFeatureIndex("next_phoneme");
            int nextPhoneClassIndex = featDef.getFeatureIndex("gpos");
            int prosodyIndex = featDef.getFeatureIndex("position_type");
            // these two are not available in EN
            //int nextPhoneClassIndex = featDef.getFeatureIndex("selection_next_phone_class");
			//int prosodyIndex = featDef.getFeatureIndex("selection_prosody");
            
			/* loop over the feature vectors */
			List<String> featureLines = new ArrayList<String>();
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
              
            // create a byte vector with the reults
            //System.out.println("number of lines=" + numLines);
            byte feasVector[] = new byte[(numLines*4)];
            
			for (int n=0,i=0;i<featVects.length;i++){
				byte[] nextFeatVects = featVects[i];
				if (nextFeatVects == null){
					System.out.println("nextFeatVects are null at index "+i);
				}
			    feasVector[n++] = nextFeatVects[0];
                feasVector[n++] = nextFeatVects[1];
                feasVector[n++] = nextFeatVects[2];
                feasVector[n++] = nextFeatVects[3];
			}
			
            //System.out.println("feas=" + feas);
            return feasVector;
			
		}
		
		
		/**
		 * Read the list of already processed files
		 * 
		 * @param doneDirsTextName the file to read from
		 * @return the list of already processed files
		 * @throws Exception
		 *//*
		protected static List readInDoneFiles(String doneFilesTextName) throws Exception{
			File doneDirsText = new File(doneFilesTextName);
			List<String> doneList = new ArrayList<String>();
			
			if (doneDirsText.exists()){
				
				BufferedReader doneIn =
					new BufferedReader(new FileReader(doneDirsText));
				String line;
				
				while((line=doneIn.readLine()) != null){
					doneList.add(line.trim());
				}
				doneIn.close();
			} 
			
			//doneOut = new PrintWriter(new FileWriter(doneDirsText,true),true);
			return doneList;
		}
		*/
        
        
		/**
		 * Split the text
		 * into separate sentences
		 * 
		 * @param file the file
		 * @return true, if successful
		 * @throws Exception
		 */
		protected static Vector<String> splitIntoSentences(String text, String id, PrintWriter pw)throws Exception{
            
            Vector<String> sentenceList = null;
            StringBuffer sentence;
            //index2sentences = new TreeMap<Integer,String>();
			
            Document doc = phonemiseText(text);
			//if (doc == null) return false;
            
            if (doc != null) {
            sentenceList = new Vector<String>();    
                
			NodeList sentences = doc.getElementsByTagName("s");   
			
            int sentenceIndex = 1;
            int unrelSentences=0;
			for (int j=0;j<sentences.getLength();j++){
				Node nextSentence = sentences.item(j);
				//ignore all non-element children
				if (!(nextSentence instanceof Element)) continue; 
				sentence = null;
				//get the tokens
				NodeList tokens = nextSentence.getChildNodes();
				usefulSentence = true;
                unknownWords = false;
                strangeSymbols = false;
				for (int k=0;k<tokens.getLength();k++){
					Node nextToken = tokens.item(k);
					//ignore all non-element children
					if (!(nextToken instanceof Element)) continue; 
					sentence = collectTokens(nextToken, sentence);                            
				}
				if (sentence!=null){
					if (usefulSentence){	
						//store sentence in sentence map
						//index2sentences.put(new Integer(sentenceIndex),sentence.toString());
                        sentenceList.add(sentence.toString());
					} else {
						//just print useless sentence to log file
						//System.out.println(filename+"; "+sentenceIndex+": "+sentence
						//		+" : is unreliable");
                        
                        unrelSentences++;
                        
                        //System.out.println("Inserting unreliable sentence:");
                         
                        // Here the reason why is unreliable can be added to the DB.
                        // for the moment there is just one field reliable=false in this case.
                        wikiToDB.insertSentence(sentence.toString(), null, usefulSentence, unknownWords, strangeSymbols, Integer.parseInt(id));
					}
					sentenceIndex++;
				} else {
					//ignore
					//System.out.println("NULL SENTENCE!!!");
				}
			} 
            numUnreliableSentences += unrelSentences;
            pw.println("Inserted " + unrelSentences + " sentences from text id=" + id + " (Total unreliable = " + numUnreliableSentences + ")");
            System.out.println("Inserted " + unrelSentences + " sentences from text id=" + id + " (Total unreliable = " + numUnreliableSentences + ")");
            
            } 
            
			return sentenceList;
		}
		
        /*
        private static String getShortFileName(int index, String longName){
          // find last  /   this will not work in windows!
          String shortName = longName.substring(longName.lastIndexOf("/")+1);
          shortName = shortName + "_" + ((Integer) index).toString();
            
          return shortName;  
        }
        */
        
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
				mary.process(textString, "TEXT","PHONEMES", "en_US", null, "hsmm-slt", os);
                
                //read into mary data object                
				//MaryData maryData = new MaryData(MaryDataType.PHONEMES, Locale.GERMAN);
                MaryData maryData = new MaryData(MaryDataType.PHONEMES, Locale.US);
                
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
         * checkCredibility returns
         *  0 if the sentence is useful
         *  1 if the sentence contains unknownWords (so the sentence is not useful)
         *  2 if the sentence contains strangeSymbols (so the sentence is not useful)
		 */
		protected static StringBuffer collectTokens(Node nextToken, StringBuffer sentence){
            int credibility = 0;  
			String name = nextToken.getLocalName();
			if (name.equals("t")){
				if ( ( credibility = checkCredibility((Element) nextToken) ) > 0 ){
					//memorize that we found unreliable sentence
					usefulSentence = false;
                    if(credibility == 1)
                      unknownWords = true;
                    else if(credibility == 2)
                      strangeSymbols = true;  
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
						collectTokens(nextMTUToken, sentence);
					}
				}
				
			}
            
            return sentence;
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
			protected String textId;
			protected boolean finished;
			protected boolean successful;
			
			/**
			 * Build a new MaryCallerThread
			 * 
			 * @param file the file to process
			 */
			public MaryCallerThread(String text, String id){
				this.text = text;
                this.textId = id;
				finished = false;
				successful = false;
				setName("mary caller");
			}
			
			/**
			 * Process the file
			 */
			public void run(){
				try{
					//successful = splitIntoSentences(text, textId);
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