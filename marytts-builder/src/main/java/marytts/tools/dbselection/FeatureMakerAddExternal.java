package marytts.tools.dbselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import marytts.features.FeatureRegistry;
import marytts.server.Mary;
import marytts.util.MaryUtils;

/**
 * FeatureMakerAddExternal add sentences from a text file, classify them as reliable, or non-reliable (sentences with unknownWords or strangeSymbols) and extracts context features from the reliable sentences. All this extracted data will be kept in the DB.
 * 
 * @author fabio
 *
 */
public class FeatureMakerAddExternal extends FeatureMaker {
     //additional arguments
	 private static String sentencefilename; //file contains the sentence to add
	
	 public static void main(String[] args)throws Exception{
	        boolean test=false;	
	        String dateStringIni="";
	        String dateStringEnd="";
	        DateFormat fullDate = new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss");
	        Date dateIni = new Date();
	        dateStringIni = fullDate.format(dateIni);

	        /* check the arguments */
	        if (!readArgs(args)){
	            printUsage();
	            return;
	        }

	        System.out.println("\nFeatureMaker started...");

	        /* Here the DB connection is open */
	        wikiToDB = new DBHandler(locale);
	        wikiToDB.createDBConnection(mysqlHost,mysqlDB,mysqlUser,mysqlPasswd);

	        // check if table exists, if exists already ask user if delete or re-use
	        char c;
	        boolean result=false, processCleanTextRecords=true;
	        InputStreamReader isr = new InputStreamReader(System.in);
	        BufferedReader br = new BufferedReader(isr);

	        String table = wikiToDB.getDBselectionTableName();
	        if(wikiToDB.tableExist(table)) {
	            System.out.print("    TABLE = \"" + table + "\" already exists, should it be deleted (y/n)?"); 
	            try{
	                String s = br.readLine();  
	                if( s.contentEquals("y")){
	                    wikiToDB.createDataBaseSelectionTable();
	                } else {                
	                    System.out.print("    ADDING sentences TO EXISTING dbselection TABLE \"" + table + "\" (y/n)?");
	                    s = br.readLine();
	                    if( s.contentEquals("y"))
	                        processCleanTextRecords=true;
	                    else{
	                        processCleanTextRecords=false;
	                        System.out.print("    please check the \"locale\" prefix of the dbselection TABLE you want to create or add to.");
	                    }
	                }        
	            } catch(Exception e){
	                System.out.println(e); 
	            }
	        } else {
	            System.out.print("    TABLE = \"" + table + "\" does not exist, it will be created.");
	            wikiToDB.createDataBaseSelectionTable();
	        }
	        
	        System.out.print("Starting builtin MARY TTS...");
	        Mary.startup();
	        System.out.println(" MARY TTS started.");

	        
	        FileWriter added_dbselection_id_filewriter = new FileWriter(sentencefilename+"_dbselection_added_id.txt");  
	        
	        if(processCleanTextRecords) {     
	            // Get the set of id for unprocessed records in clean_text
	            // this will be useful when the process is stopped and then resumed
	            System.out.println("\nGetting list of unprocessed clean_text records from " + sentencefilename);
	            
	            String[] listOfSentences =  readLines(sentencefilename);
	            
	            //int textId[];
	            //textId = sentencefilename.getUnprocessedTextIds();
	            System.out.println("Number of unprocessed clean_text records to process --> [" + listOfSentences.length + "]");
	            String text;

	            Vector<String> sentenceList;  // this will be the list of sentences in each clean_text
				sentenceList=null;
	           
	            String targetFeatures = "";
	            int i, j;

	            // get a list separated by spaces of the target features to extract
	            for(i=0; i<selectionFeature.size(); i++)
	                targetFeatures += selectionFeature.elementAt(i) + " ";
	            /* loop over the text records in clean_text table of wiki */
	            // once processed the clean_text records are marked as processed=true, so here retrieve
	            // the next clean_text record until all are processed.
	            System.out.println("Looping over unprocessed clean_text records from " + sentencefilename);
	            System.out.println("TARGETFEATURES to extract: " + targetFeatures);
	            System.out.println("Starting time:" + dateStringIni + "\n");

	            featureComputer = FeatureRegistry.getTargetFeatureComputer(MaryUtils.string2locale(locale), targetFeatures);
	            fdef = featureComputer.getFeatureDefinition();
	            PrintWriter pw = new PrintWriter(new FileWriter(new File(locale + "_featureDefinition.txt")));
	            fdef.writeTo(pw, false);
	            pw.close();
	            System.out.println("\nCreated featureDefinition file:" + locale + "_featureDefinition.txt");


	            for(i=0; i<listOfSentences.length; i++) {
	                // get next unprocessed text  
	                text = listOfSentences[i];
	                System.out.println("Processing(" + i + ") text id=" + i + " text length=" + text.length());
	                // the following method is used also in the adding sentence from external text  
	                split_check_reliability_and_insert(text,sentenceList,i,test, added_dbselection_id_filewriter);
	            } //end of loop over articles  
	            
	            wikiToDB.closeDBConnection();

	            Date dateEnd = new Date();
	            dateStringEnd = fullDate.format(dateEnd);
	            System.out.println("numSentencesInText added = " + numSentences);
	            System.out.println("Start time:" + dateStringIni + "  End time:" + dateStringEnd);   
	            System.out.println("Done");

	        } else {
	            wikiToDB.closeDBConnection(); 
	            System.out.println("FeatureMakerMaryServer terminated.");   
	        }

	    }//end of main method
		
	 
		/**
		 * Print usage of this program 
		 *
		 */
	protected static void printUsage(){
	        System.out.println("\nUsage: " +
	                "java FeatureMakerAddExternal -locale language -mysqlHost host -mysqlUser user\n" +
	                "                 -mysqlPasswd passwd -mysqlDB wikiDB -sentFile sentencefilename\n" +
	                "                 [-reliability strict]\n" +
	                "                 [-featuresForSelection phone,next_phone,selection_prosody]\n\n" +
	                "  default/optional: [-featuresForSelection phone,next_phone,selection_prosody] (features separated by ,) \n" +
	                "  optional: [-reliability [strict|lax]]\n\n" +
	                "  -reliability: setting that determines what kind of sentences \n" +
	                "  are regarded as credible. There are two settings: strict and lax. With \n" +
	                "  setting strict, only those sentences that contain words in the lexicon \n" +
	                "  or words that were transcribed by the preprocessor can be selected for the synthesis script; \n" +
	                "  the other sentences as unreliable. With setting lax (default), also those words that \n" +
	                "  are transcribed with the letter to sound component can be selected. \n\n"
	        		);
	}
		
	    
	  

	 private static void printParameters(){
	        System.out.println("FeatureMaker parameters:" +
	              
	        "\n  -locale " + locale +  
	        "\n  -mysqlHost " + mysqlHost +
	        "\n  -mysqlUser " + mysqlUser +
	        "\n  -mysqlPasswd " + mysqlPasswd +
	        "\n  -mysqlDB " + mysqlDB + 
	        "\n  -sentFile " + sentencefilename);
	        
	        if( strictReliability )
	          System.out.println("  -reliability strict");
	        else
	          System.out.println("  -reliability lax");  
	       
	        System.out.print("  -featuresForselection ");
	        int i=0;
	        for(i=0; i<selectionFeature.size()-1; i++)
	          System.out.print(selectionFeature.elementAt(i) + ",");
	        System.out.println(selectionFeature.elementAt(i));
	    }
	        
	    
		/**
		 * Read and parse the command line args
		 * 
		 * @param args the args
		 * @return true, if successful, false otherwise
		 */
		protected static boolean readArgs(String[] args){
			//initialise default values	
	        locale = null;
			strictReliability = false; // per default, allow system to select sentences with unknown words
	        featDef = null;
	        selectionFeature = new Vector<String>();
	        selectionFeature.add("phone");
	        selectionFeature.add("next_phone");
	        selectionFeature.add("selection_prosody");
	        
			//now parse the args
	        if (args.length >= 11){
	          for(int i=0; i<args.length; i++) { 
				
	            if (args[i].equals("-locale") && args.length>=i+1 )
	              locale = args[++i];  
			    
	            else if (args[i].equals("-reliability") && args.length>=i+1){
				  String credibilitySetting = args[++i];
				  if (credibilitySetting.equals("strict"))
					strictReliability = true;
				  else {
					if (credibilitySetting.equals("lax"))
						strictReliability = false;
				    else 
					  System.out.println("Unknown argument for reliability " +credibilitySetting);
				  }
	            }
	            
	            else if(args[i].contentEquals("-featuresForSelection") && args.length >= (i+1) ){
	              selectionFeature.clear();  
	              String selection = args[++i];
	              String feas[] = selection.split(",");
	              for(int k=0; k<feas.length; k++)
	                  selectionFeature.add(feas[k]);             
	            }
	            
	            // mysql database parameters
	            else if(args[i].contentEquals("-mysqlHost") && args.length >= (i+1) )
	              mysqlHost = args[++i];
	                
	            else if(args[i].contentEquals("-mysqlUser") && args.length >= (i+1) )
	              mysqlUser = args[++i];
	                   
	            else if(args[i].contentEquals("-mysqlPasswd") && args.length >= (i+1) )
	              mysqlPasswd = args[++i];
	                 
	            else if(args[i].contentEquals("-mysqlDB") && args.length >= (i+1) )
	              mysqlDB = args[++i];
	            
	            else if(args[i].contentEquals("-sentFile") && args.length >= (i+1) )
		              sentencefilename = args[++i];
	          
	            else { //unknown argument
	              System.out.println("\nOption not known: " + args[i]);
	              return false;
	            }
	                
	            
	          }	
			} else  // arguments less than 12
				return false;

	        if(mysqlHost==null || mysqlUser==null || mysqlPasswd==null || mysqlDB==null){
	           System.out.println("\nMissing mysql parameters.\n");
	           printParameters();
	           return false;
	        }
	        if (sentencefilename == null){
	            System.out.println("\nPlease specify sentFile.\n");
	            printParameters();
	            return false;
	         }
	        if(locale==null){
	            System.out.println("\nPlease specify locale = wikipedia language.\n");
	            printParameters();
	            return false;
	         }
	 
	        printParameters();
			return true;
		}
	
	
		public static String[] readLines(String filename) throws IOException   
	    {  
	        FileReader fileReader = new FileReader(filename);  
	          
	        BufferedReader bufferedReader = new BufferedReader(fileReader);  
	        ArrayList<String> lines = new ArrayList<String>();  
	        String line = null;  
	          
	        while ((line = bufferedReader.readLine()) != null)   
	        {  
	            lines.add(line);  
	        }  
	          
	        bufferedReader.close();  
	          
	        return lines.toArray(new String[lines.size()]);  
	    }     
		
		
}
