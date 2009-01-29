/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.dbselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import marytts.features.FeatureDefinition;



/**
 * Main class to be run over a database for selection
 * 
 * @author Anna Hunecke
 *
 */
public class DatabaseSelector{
    
    private static String locale;
    //the feature definition for the feature vectors
    public static FeatureDefinition featDef;
    //the file containing the feature definition
    private static String featDefFileName;
    //the file containing the coverage data needed to initialise the algorithm
    private static String initFileName;
    //the directory to print the selection results to
    private static String selectionDirName;
    //the config file for the coverage definition
    private static String covDefConfigFileName;
    //the stop criterion (as string)
    private static String stopCriterion;
    //the list of sentences from which to select
    private static int[] idSentenceList;
    //the log file to log the result to
    private static String overallLogFile;
    //if true, feature vectors are kept in memory
    private static boolean holdVectorsInMemory;
    //if true, print more information to command line
    private static boolean verbose;
    //if true, print a table containing the coverage 
    //development over time
    private static boolean logCovDevelopment;
    //private static List of selected sentences ids;
    private static List<Integer> selectedIdSents;
    private static String selectedSentencesTableName;
    private static String tableDescription;
    // mySql database
    protected static DBHandler wikiToDB;     
    private static String mysqlHost;
    private static String mysqlDB;
    private static String mysqlUser;
    private static String mysqlPasswd;
    
    /**
     * Main method to be run from the directory where the data is.
     * Expects already computed unit features in directory unitfeatures
     * 
     * @param args the command line args (see printUsage for details)
     */
    public static void main(String[] args)throws Exception{      
          main2(args,null);
          // main1(args);
    }

    
    public static void main1(String[] args)throws Exception{
      byte[][] vecArray = main2(args,null);     
      main2(args,vecArray);
    }
    
    
    /**
     * Main method to be run from the directory where the data is.
     * Expects already computed unit features in directory unitfeatures.
     * Can be given an array of feature vectors - this is useful if the 
     * program is run several times with the same feature vectors.
     * 
     * @param args the command line args (see printUsage for details)
     * @param vectorArray the array of feature vectors
     * 
     * @return the array of feature vectors used in the current pass
     */
    public static byte[][] main2(String[] args,byte[][] vectorArray)throws Exception{
        /* Sort out the filenames and dirs for the logfiles */
        System.out.println("Starting DatabSystem.out.println(e.getMessage());ase Selection...");
        
        long time = System.currentTimeMillis();
        PrintWriter logOut;
        
        String dateString = "", dateDir = "";
     /*   DateFormat fullDate = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        DateFormat day = new SimpleDateFormat("dd_MM_yyyy");
        Date date = new Date();
        dateString = fullDate.format(date);
        dateDir = day.format(date);
      */          
        System.out.println("Reading arguments ...");
        StringBuffer logBuf = new StringBuffer();
        if (!readArgs(args,logBuf)){
            throw new Exception("Something wrong with the arguments.");
        }

        //make sure the stop criterion is allright
        SelectionFunction selFunc = new SelectionFunction();
        if (!selFunc.stopIsOkay(stopCriterion)){
            System.out.println("Stop criterion format is wrong: " + stopCriterion);
            printUsage();
            throw new Exception("Stop criterion format is wrong: " + stopCriterion);
        }

        //make various dirs
        File selectionDir = new File(selectionDirName);
        if (!selectionDir.exists())
            selectionDir.mkdir();
        File dateDirFile = new File(selectionDirName+dateDir);
        if (!dateDirFile.exists())
            dateDirFile.mkdir();

        //open log file
        String filename = selectionDirName + dateDir + "/selectionLog_" + dateString + ".txt";
        try{
            logOut = new PrintWriter(new BufferedWriter(new FileWriter(new File(filename))),true);
        } catch (Exception e){
            e.printStackTrace();
            throw new Exception("Error opening logfile");
        }
        //print date and arguments to log file
        logOut.println("Date: "+dateString);
        logOut.println(logBuf.toString());
        
        wikiToDB = new DBHandler(locale);
        
        // Check if name of selectedSentencesTable has to be changed
        if(selectedSentencesTableName != null)
          wikiToDB.setSelectedSentencesTableName(selectedSentencesTableName);
        else
          System.out.println("Current selected sentences table name = " + selectedSentencesTableName);
        
        // If connection succeed
        if( wikiToDB.createDBConnection(mysqlHost,mysqlDB,mysqlUser,mysqlPasswd) ) {
            
        /* Read in the feature definition */
        System.out.println("\nLoading feature definition...");
        try {
          BufferedReader uttFeats = new BufferedReader(new InputStreamReader(
                                  new FileInputStream(new File( featDefFileName )), "UTF-8"));
          featDef = new FeatureDefinition(uttFeats, false);  
          uttFeats.close();
          System.out.println("TARGETFEATURES:" + featDef.getNumberOfFeatures() + " =  " + featDef.getFeatureNames());
        } catch (Exception e){
          e.printStackTrace();
          throw new Exception("Error opening featureDefinition file");
        }
        
        /* Initialise the coverage definition */
        System.out.println("\nInitiating coverage...");
        CoverageDefinition covDef = new CoverageDefinition(featDef,covDefConfigFileName,holdVectorsInMemory,vectorArray);
        
        // If the selectedSentencesTable is new, (does not exist) then a new table
        // will be created, the selected field in the dbselection table will be initialised to selected=false. 
        // The sentences already marke in this db as unwanted=true will be kept. 
        wikiToDB.createSelectedSentencesTable(stopCriterion, featDefFileName, covDefConfigFileName);
        // With the information provided by the user
        wikiToDB.setTableDescription(wikiToDB.getSelectedSentencesTableName(), tableDescription, 
                                     stopCriterion,featDefFileName, covDefConfigFileName);

        long startTime = System.currentTimeMillis();
        File covSetFile = new File(initFileName);
        boolean readCovFromFile = true;
        boolean vectorArrayNull = (vectorArray == null);
        if (!covSetFile.exists()){
            //coverage has to be initialised
            readCovFromFile = false;
            idSentenceList = covDef.initialiseCoverage(wikiToDB, verbose); 
            System.out.println("\nWriting coverage to file "+initFileName);
            covDef.writeCoverageBin(initFileName);
        } else {                
            idSentenceList = wikiToDB.getIdListOfType("dbselection", "reliable=true");
            covDef.readCoverageBin(wikiToDB, initFileName,featDef,idSentenceList);
        }
        
        if (vectorArrayNull) vectorArray = covDef.getVectorArray();
        
        /* add already selected sentences to cover */
        System.out.println("\nAdd to cover already selected sentences marked as unwanted=false.");
        selectedIdSents = new ArrayList<Integer>();
        addSelectedSents(selectedSentencesTableName, covDef);
       
        /* remove unwanted sentences from basename list */
        System.out.println("\nRemoving selected sentences marked as unwanted=true.");
        removeUnwantedSentences(selectedSentencesTableName);    
       
        long startDuration = System.currentTimeMillis() -startTime;
        if (verbose)
            System.out.println("Startup took "+startDuration+" milliseconds");
        logOut.println("Startup took "+startDuration+" milliseconds");
        
        /* print text corpus statistics */
        if (!readCovFromFile){
            //only print if we did not read from file
            filename = selectionDirName+"textcorpus_distribution.txt";
            System.out.println("Printing text corpus statistics to "+filename+"...");       
            try{
                covDef.printTextCorpusStatistics(filename);
            } catch (Exception e){
                e.printStackTrace();
                throw new Exception("Error printing statistics");
            }
        }

        //print settings of the coverage definition to log file 
        covDef.printSettings(logOut);

        /* Start the algorithm */
        System.out.println("\nSelecting sentences...");
       
        //selFunc.select(selectedSents,covDef,logOut,basenameList,holdVectorsInMemory,verbose);
        selFunc.select(selectedIdSents,covDef,logOut,idSentenceList,holdVectorsInMemory,verbose,wikiToDB);

        /* Store list of selected files */
        filename = selectionDirName+dateDir + "/selectionResult_" + dateString + ".txt";
        //storeResult(filename,selectedSents);
        storeResult(filename,selectedIdSents);

        /* print statistics */
        System.out.println("Printing selection distribution and table...");
        String disFile = selectionDirName+dateDir + "/selectionDistribution_" + dateString + ".txt";
        String devFile = selectionDirName+dateDir + "/selectionDevelopment_" + dateString + ".txt";
        try{
            covDef.printSelectionDistribution(disFile,devFile,logCovDevelopment);
        } catch (Exception e){
            e.printStackTrace();
            throw new Exception("Error printing statistics");
        }

        if (overallLogFile != null){
            //append results to end of overall log file
            PrintWriter overallLogOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                                        new File(overallLogFile),true),"UTF-8"),true);
            overallLogOut.println("*******************************\n" + "Results for "+dateString+":");
            
            //overallLogOut.println("number of basenames "+basenameList.length);
            overallLogOut.println("number of basenames "+idSentenceList.length);
            
            overallLogOut.println("Stop criterion "+stopCriterion);
            covDef.printResultToLog(overallLogOut);
            overallLogOut.close();
        }

        //print timing information
        long elapsedTime = System.currentTimeMillis() - time;
        double minutes = (double)elapsedTime/(double)1000/(double)60;
        System.out.println("Selection took "+minutes+" minutes("+elapsedTime+" milliseconds)");
        logOut.println("Selection took "+minutes+" minutes ("+elapsedTime+" milliseconds)");
        logOut.flush();
        logOut.close();
              
        wikiToDB.closeDBConnection(); 
        System.out.println("All done!");  
        
        } else { // connection did not succeed
            System.out.println("\nERROR: Problems with connection to the DB, please check the mysql parameters.");
            throw new Exception("ERROR: Problems with connection to the DB, please check the mysql parameters.");
        }
        
        return vectorArray;
    }

    /**
     * Read and check the command line arguments
     * 
     * @param args the arguments
     * @param log a StringBufffer for logging
     * @return true if args can be parsed and all essential args are there,
     *         false otherwise 
     */
    private static boolean readArgs(String[] args,StringBuffer log){
        //initialise default values
        locale = null;
        selectionDirName = null;
        initFileName = null;
        covDefConfigFileName = null;
        overallLogFile = null;
        holdVectorsInMemory = true;
        verbose = false;
        logCovDevelopment = false;
        mysqlHost = null;
        mysqlDB = null;
        mysqlUser = null;
        mysqlPasswd = null;
        selectedSentencesTableName = null;
        tableDescription = "";
        
        int i=0;
        int numEssentialArgs = 0;
        
        //loop over args
        while (args.length > i){ 
            if (args[i].equals("-locale")){
                if (args.length > i+1){
                    i++;
                    locale = args[i];
                    log.append("locale : "+args[i]+"\n");
                    System.out.println("  locale : "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No locale.");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-mysqlHost")){
                if (args.length > i+1){
                    i++;
                    mysqlHost = args[i];
                    log.append("mysqlHost : "+args[i]+"\n");
                    System.out.println("  mysqlHost : "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No mysqlHost.");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-mysqlDB")){
                if (args.length > i+1){
                    i++;
                    mysqlDB = args[i];
                    log.append("mysqlDB : "+args[i]+"\n");
                    System.out.println("  mysqlDB : "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No mysqlDB.");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-mysqlUser")){
                if (args.length > i+1){
                    i++;
                    mysqlUser = args[i];
                    log.append("mysqlUser : "+args[i]+"\n");
                    System.out.println("  mysqlUser : "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No mysqlUser.");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-mysqlPasswd")){
                if (args.length > i+1){
                    i++;
                    mysqlPasswd = args[i];
                    log.append("mysqlPasswd : "+args[i]+"\n");
                    System.out.println("  mysqlPasswd : "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No mysqlPasswd.");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-featDef")){
                if (args.length > i+1){
                    i++;
                    featDefFileName = args[i];
                    log.append("FeatDefFileName : "+args[i]+"\n");
                    System.out.println("  FeatDefFileName : "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No featDef file");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-initFile")){
                if (args.length > i+1){
                    i++;
                    initFileName = args[i];
                    log.append("initFile : "+args[i]+"\n");
                    System.out.println("  initFile : "+args[i]);
                } else {
                    System.out.println("No initFile");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-tableName")){
                if (args.length > i+1){
                    i++;
                    selectedSentencesTableName = args[i];
                    log.append("selectedSentencesTable name : "+args[i]+"\n");
                    System.out.println("  selectedSentencesTable name: "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No selectedSentencesTable name");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-tableDescription")){
                if (args.length > i+1){
                    i++;
                    tableDescription = args[i];
                    log.append("tableDescription : "+args[i]+"\n");
                    System.out.println("  tableDescription: "+args[i]);
                } else {
                    System.out.println("No tableDescription");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-vectorsOnDisk")){
                holdVectorsInMemory = false;
                log.append("vectorsOnDisk");
                System.out.println("  vectorsOnDisk");
                i++;
                continue;
            }
            if (args[i].equals("-verbose")){
                verbose = true;
                log.append("verbose");
                System.out.println("  verbose");
                i++;
                continue;
            }
            if (args[i].equals("-logCoverageDevelopment")){
                logCovDevelopment = true;
                log.append("logCoverageDevelopment");
                System.out.println("  logCoverageDevelopment");
                i++;
                continue;
            }
            if (args[i].equals("-selectionDir")){
                if (args.length > i+1){
                    i++;
                    selectionDirName = args[i];
                    //make sure we have a slash at the end
                    char lastChar = 
                        selectionDirName.charAt(selectionDirName.length()-1);
                    if (Character.isLetterOrDigit(lastChar)){
                        selectionDirName = selectionDirName+"/"; 
                    }
                    log.append("selectionDir : "+args[i]+"\n");
                    System.out.println("  selectionDir : "+args[i]);
                } else {
                    System.out.println("No selectionDir");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-coverageConfig")){
                if (args.length > i+1){
                    i++;
                    covDefConfigFileName = args[i];
                    log.append("coverageConfig : "+args[i]+"\n");
                    System.out.println("  coverageConfig : "+args[i]);
                } else {
                    System.out.println("No coverageConfig");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            if (args[i].equals("-stop")){
                StringBuffer tmp = new StringBuffer();
                i++;
                while (args.length > i){                    
                    if (args[i].startsWith("-")) break;
                    tmp.append(args[i]+" ");  
                    i++;
                } 
                stopCriterion = tmp.toString();
                log.append("stop criterion : "+stopCriterion+"\n");
                System.out.println("  stop criterion : "+stopCriterion);
                numEssentialArgs++;
                continue;
            }
            if (args[i].equals("-overallLog")){
                if (args.length > i+1){
                    i++;
                    overallLogFile = args[i];
                    log.append("overallLogFile : "+args[i]+"\n");
                    System.out.println("  overallLogFile : "+args[i]);
                    numEssentialArgs++;
                } else {
                    System.out.println("No overall log file");
                    printUsage();
                    return false;
                }
                i++;
                continue;
            }
            i++;
        }
        System.out.println();
        if (numEssentialArgs<9){
            //not all essential arguments were given
            System.out.println("You must at least specify locale, mysql (host,user,paswd,DB), selectedSentencesTableName \n" +
                    "featureDefinition file, stop criterion and coverage config file.");
            printUsage();
            return false;
        } 
        if(selectedSentencesTableName==null){
            System.out.println("Please provide a name for the selectedSentencesTable.");
            printUsage();
            return false;  
        }
        if (selectionDirName == null){
            selectionDirName = "./selection/";
        }
        if (initFileName == null){
            initFileName = selectionDirName+"init.bin";
        }
        if (covDefConfigFileName == null){
            covDefConfigFileName = selectionDirName+"covDef.config";   
        }
        
        return true;
    }



    /**
     * Print usage of main method
     * to standard out
     */
    private static void printUsage(){
        System.out.println("\nUsage: "
                +"java DatabaseSelector -locale language -mysqlHost host -mysqlUser user -mysqlPasswd passwd -mysqlDB wikiDB \n"
                +"        -tableName selectedSentencesTableName -featDef file -stop stopCriterion \n"
                +"        [-coverageConfig file -initFile file -selectedSentences file -unwantedSentences file ]\n" 
                +"        [-tableDescription a brief description of the table ]\n"
                +"        [-vectorsOnDisk -overallLog file -selectionDir dir -logCoverageDevelopment -verbose]\n\n"     
                +"Arguments:\n"
                +"-tableName selectedSentencesTableName : The name of a new selection set, change this name when\n"
                +"    generating several selection sets. FINAL name will be: \"locale_name_selectedSenteces\". \n"
                +"    where name is the name provided for the selected sentences table.\n"
                +"-tableDescription : short description of the selected sentences table. (default: empty)\n"
                +"-featDef file : The feature definition for the features\n"
                +"-stop stopCriterion : which stop criterion to use. There are five stop criteria. \n"
                +" They can be used individually or can be combined:\n"
                +"  - numSentences n : selection stops after n sentences\n"
                +"  - simpleDiphones : selection stops when simple diphone coverage has reached maximum\n"
                +"  - simpleProsody : selection stops when simple prosody coverage has reached maximum\n"
                +"-coverageConfig file : The config file for the coverage definition. \n"
                +"   Default config file is ./covDef.config.\n"                
                +"-vectorsOnDisk: if this option is given, the feature vectors are not loaded into memory during \n"
                +" the run of the program. This notably slows down the run of the program!\n"
                +"-initFile file : The file containing the coverage data needed to initialise the algorithm.\n"
                +"   Default init file is ./init.bin\n"
                +"-overallLog file : Log file for all runs of the program: date, settings and results of the current\n"
                +" run are appended to the end of the file. This file is needed if you want to analyse your results \n"
                +" with the ResultAnalyser later.\n"
                +"-selectionDir dir : the directory where all selection data is stored.\n"
                +"   Standard directory is ./selection\n" 
                +"-logCoverageDevelopment : If this option is given, the coverage development over time \n"
                +" is stored.\n"
                +"-verbose : If this option is given, there will be more output on the command line\n"
                +" during the run of the program.\n");       
    }

    /***
     * Manual selection of wanted/unwanted selected sentences
     *
     */
    private static void checkSelectedSentences(){
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        
        try{
        System.out.println("\nChecking selected sentences whether they are wanted or not.");   
        System.out.println(" selected sentences will be saved in ./selected.log");
        PrintWriter selectedLog = new PrintWriter(new FileWriter(new File("./selected.log")));
        
        System.out.println(" unwanted sentences will be saved in ./unwanted.log");
        PrintWriter unwantedLog = new PrintWriter(new FileWriter(new File("./unwanted.log")));
        
        int sel[] = wikiToDB.getIdListOfType("dbselection", "selected=true and unwanted=false");
               
        if( sel != null){
          // checking selected sentences
          System.out.println(" Select \"y\" for marking sentence as \"wanted\" otherwise \"n\" . Press any other key to finish: \n");     
          String str;
          for(int i=0; i<sel.length; i++){
            str = wikiToDB.getSelectedSentence(wikiToDB.getSelectedSentencesTableName(), sel[i]);  
            System.out.print("id=" + sel[i] + ":  "+ str + "\n  Wanted?(y/n):");             
            
                String s = br.readLine();  
                if( s.contentEquals("n")){
                  wikiToDB.setSentenceRecord(sel[i], "unwanted", true);
                  unwantedLog.println(sel[i] + " " + str);
                } else if( s.contentEquals("y")){
                  selectedLog.println(sel[i] + " " + str);
                } else{
                  unwantedLog.close();
                  selectedLog.close();
                  break;
                }          
          }
        } else
            System.out.println("There is no selected sentences in the DB.");  
        
        } catch(Exception e){
            System.out.println(e); 
        }
        
    }
    
    /**
     * Add a list of sentences to the cover
     * Here the already selected sentences are added to the cover and the indexes removed
     * (or set to -1) in the idSentenceList
     * @param covDef the cover
     * @throws Exception
     */
    private static void addSelectedSents(String tableName, CoverageDefinition covDef)throws Exception{
         
        if (verbose)
            System.out.println("\nAdding previously selected sentences ...");
        int idSentenceListSelected[] = wikiToDB.getIdListOfSelectedSentences(
                                       wikiToDB.getSelectedSentencesTableName(), "unwanted=false");
        int id;
        byte[] vectorBuf;
        if( idSentenceListSelected != null ){
         for(int i=0; i<idSentenceListSelected.length; i++){
            id = idSentenceListSelected[i];
            vectorBuf = wikiToDB.getFeatures(id); 
            
            //fill the cover set with the sentence
            covDef.updateCover(vectorBuf);
            
            //add the filename to the sentence list
            selectedIdSents.add((Integer)id);
            
        }
        int numSelectedSents = selectedIdSents.size();
        int numRemovedSents = 0;
             
        //loop over basename array
        for (int i=0;i<idSentenceList.length;i++){
            if (selectedIdSents.contains(idSentenceList[i])){
                //remove the sentence also from the idSentenceList
                if (verbose)
                  System.out.println("  Removing from idSentenceList id=" + idSentenceList[i]);
                idSentenceList[i] = -1;
                numRemovedSents++;
            }            
            if (numSelectedSents == numRemovedSents) break;
        } 
        System.out.println("Added to cover " + idSentenceListSelected.length + " selected sentences");        
        } else
          System.out.println("There is no already selected sentences to add to the list.");
         
    }

    /**
     * Remove unwanted sentences from the basename list
     * 
     * @throws Exception
     */   
    private static void removeUnwantedSentences(String tableName) throws Exception{
        if (verbose)
          System.out.println("\nRemoving unwanted sentences ...");
        int idSentenceListUnwanted[] = wikiToDB.getIdListOfSelectedSentences(
                                       wikiToDB.getSelectedSentencesTableName(), "unwanted=true");
        
        ArrayList<Integer> unwantedIdSents = new ArrayList<Integer>();
        int id;
        if( idSentenceListUnwanted != null ){
            for(int i=0; i<idSentenceListUnwanted.length; i++){
            id = idSentenceListUnwanted[i];          
            // mark sentence as unwanted in the locale_dbselection table
            // this is already done when selecting unwanted with the SynthesisScriptGUI
            //wikiToDB.setSentenceRecord(id, "unwanted", true);
            unwantedIdSents.add((Integer)id);
        }
        // remove sentences from basename list 
        int numSelectedSents = unwantedIdSents.size();
        int numRemovedSents = 0;
        // loop over basename array
       for (int i=0;i<idSentenceList.length;i++){
            if (unwantedIdSents.contains(idSentenceList[i])){
                //remove the sentence also from the idSentenceList
                if (verbose)
                  System.out.println("  Removing (unwanted)from idSentenceList id=" + idSentenceList[i]);
                idSentenceList[i] = -1;
                numRemovedSents++;
            }
            if (numSelectedSents == numRemovedSents) break;
        }
           System.out.println("Removed " + idSentenceListUnwanted.length + " unwanted sentences.");        
        } else 
           System.out.println("There is no unwanted sentences to remove.");
        
    }
  
 
    
    /**
     * Print the list of selected files
     * 
     * @param filename the file to print to
     * @param selected the list of files
     */
    private static void storeResult(String filename, List selected){

        PrintWriter out;
        try{
            out = new PrintWriter(new FileWriter(new File(filename)));
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error storing result");
        }
        StringBuffer resultBuf = new StringBuffer();
        for (int i=0;i<selected.size();i++){
            resultBuf.append(selected.get(i)+"\n");
        }
        out.print(resultBuf.toString());
        out.flush();
        out.close();
    }

}

