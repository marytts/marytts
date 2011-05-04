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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Selects sentences from a given set using the greedy algorithm.
 * At each step, the most useful sentence is added to the set of
 * selected sentences.
 * Selection is stopped when the stop criterion is reached.
 * Usefulness of sentences is determined by CoverageDefinition.
 * 
 * @author Anna Hunecke
 *
 */
public class SelectionFunction{

    //maximum number of sentences to select
    private int maxNumSents;
    //the vectors that are selected next 
    private byte[] selectedVectors;
    //the filename of the sentence that is selected next
    //private String selectedBasename;
    private int selectedIdSentence;
    
    //if true, algorithm stop after maxNumSents are selected
    private boolean stopNumSentences;
    //if true, algorithm stops when maximum coverage of 
    //simple diphones is reached
    private boolean stopSimpleDiphones;
    //if true, algorithm stops when maximum coverage of 
    //clustered diphones is reached
    private boolean stopClusteredDiphones;
    //if true, algorithm stops when maximum coverage of 
    //simple prosody is reached
    private boolean stopSimpleProsody;
    //if true, algorithm stops when maximum coverage of 
    //clustered prosody is reached
    private boolean stopClusteredProsody;
    //if true, print information to command line
    private boolean verbose;

    /**
     * Build a new Selection Function
     * 
     */
    public SelectionFunction(){
        }

    /**
     * Check, if given stop criterion is okay.
     * At the same time, initialise stop criterion
     * as this SelectionFunction's stop criterion
     * 
     * @param stopString the stop criterion
     * @return true if stopString can be parsed, false otherwise
     */
    public boolean stopIsOkay(String stopString){
        //set all stop criteria to false
        stopNumSentences = false;
        stopSimpleDiphones = false;
        stopClusteredDiphones = false;
        stopSimpleProsody = false;
        stopClusteredProsody = false;
        //split the stopString
        System.out.println("\nChecking stop criterion:");
        String[] split = stopString.split(" ");
        int i=0;
        while (split.length > i){
            if (split[i].startsWith("numSentences")){
                //criterion is numSentences
                stopNumSentences = true;
                if (split.length > i+1){
                    //read in the maximum number of sentences
                    maxNumSents = Integer.parseInt(split[++i]);
                } else {
                    //maximum number of sentences is missing - can not parse
                    return false;
                }
                System.out.println("Stop: num sentences "+maxNumSents);
            } else {
                if (split[i].equals("simpleDiphones")){
                    //stop criterion is simpleDiphones
                    stopSimpleDiphones = true;
                    System.out.println("Stop: simpleDiphones");
                } else {
                    if (split[i].equals("clusteredDiphones")){
                        //stop criterion is clusteredDiphones
                        stopClusteredDiphones = true;
                        System.out.println("Stop: clusteredDiphones");
                    } else {
                        if (split[i].equals("simpleProsody")){
                            //stop criterion is simpleProsody
                            stopSimpleProsody = true;
                            System.out.println("Stop: simpleProsody");
                        } else {
                            if (split[i].equals("clusteredProsody")){
                                //stop criterion is clusteredProsody
                                stopClusteredProsody = true;
                                System.out.println("Stop: clusteredProsody");
                            } else {
                                //unknown stop criterion - can not parse
                                return false;
                            }
                        }
                    }
                }
            }
            i++;
        }
        //everything allright
        return true;
    }

   /**
    * Select a set of vectors according to their usefulness
    * which is defined by the coverageDefinition. Stop, when 
    * the stop criterion is reached
    * 
    * @param selectedIdSents the list of selected id sentences
    * @param unwantedIdSents the list of unwanted id sentences
    * @param coverageDefinition the coverage definition for the feature vectors
    * @param logFile the logFile to document the progress
    * @param basenameList the list of filenames of the sentences
    * @param holdVectorsInMemory if true, get vectors from coverage definition,
    *                            if false, read vectors from disk 
    * @param verbose print output also to command line
    * @return the list of selected filenames
    * @throws IOException
    */ 
    public void select(Set<Integer>selectedIdSents,
                Set<Integer>unwantedIdSents,
                CoverageDefinition coverageDefinition,
                PrintWriter logFile,
                int[] idSentenceList,
                boolean holdVectorsInMemory,
                boolean verboseSelect,
                DBHandler wikiToDB)throws IOException{
              
        this.verbose = verboseSelect;
        //get the array of vectors if they are loaded in memory
        byte[][] vectorArray = null;
        if (holdVectorsInMemory){
            vectorArray = coverageDefinition.getVectorArray();
        }
        byte[][] featVects = null;
        int sentIndex = selectedIdSents.size()+1;
        selectedVectors = null;  
        DateFormat fullDate = new SimpleDateFormat("HH_mm_ss");
       
        // create the selectedSentences table 
        // while the stop criterion is not reached      
        while(!stopCriterionIsReached(selectedIdSents, coverageDefinition)){ 
            
            //select the next sentence  
            //selectNext(coverageDefinition, logFile, sentIndex, basenameList, vectorArray);
            selectNext(selectedIdSents, unwantedIdSents, coverageDefinition, logFile, sentIndex, idSentenceList, vectorArray, wikiToDB);

            //check if we selected something
            //if (selectedBasename == null){
            if (selectedIdSentence < 0){
                //nothing more to select
                //System.out.println("Nothing more to select");
                logFile.println("Nothing more to select");
                break;
            }
            // the selected sentences will be marked as selected=true in the DB
            Date date = new Date();
            System.out.println("  " + sentIndex + " selectedId=" + selectedIdSentence + "  " 
                    + fullDate.format(date)); 
            // Mark the sentence as selected in dbselection
            wikiToDB.setSentenceRecord(selectedIdSentence, "selected", true);
            // Insert selected sentence in table
            wikiToDB.insertSelectedSentence(selectedIdSentence, false);

            //add the selected sentence to the set
            //selectedFilenames.add(selectedBasename);
            // selectedIdSents.add(selectedIdSentence); already done in selectNext
            //update coverageDefinition         
            coverageDefinition.updateCover(selectedVectors);
            sentIndex++;
        }  
        //print out total number of sentences
        sentIndex--;
        System.out.println("Total number of selected sentences in TABLE: " + wikiToDB.getSelectedSentencesTableName()
                + " = " + sentIndex);
        
        
        int sel[] = wikiToDB.getIdListOfType("dbselection", "selected=true and unwanted=false");
        
        if( sel != null){
          // saving sentences in a file
          System.out.println("Saving selected sentences in ./selected.log");
          PrintWriter selectedLog = new PrintWriter(new FileWriter(new File("./selected.log")));
            
          String str;
          for(int i=0; i<sel.length; i++){
            // not sure if we need to make another table???
            // str = wikiToDB.getSentence("selectedSentences", sel[i]);
            str = wikiToDB.getDBSelectionSentence(sel[i]);  
            //System.out.println("id=" + sel[i] + str);  
            selectedLog.println(sel[i] + " " + str);
          }
          selectedLog.close();
          logFile.println("Total number of sentences : "+sentIndex);
        } else
            System.out.println("No selected sentences to save.");  
        
    }


 
    /**
     * Select the next sentence
     * 
     * @param coverageDefinition the coverage definition
     * @param logFile the logFile 
     * @param sentenceIndex the index of the next sentence
     * @param basenameList the list of filenames
     * @param vectorArray the array of vectors or null
     *                    if the vectors are on disk
     * @throws IOException
     */
    private void selectNext(Set<Integer>selectedIdSents,
                Set<Integer>unwantedIdSents,
                CoverageDefinition coverageDefinition,
                PrintWriter logFile,
                int sentenceIndex,
                int[] idSentenceList,
                byte[][] vectorArray,
                DBHandler wikiToDB)throws IOException{
             
        selectedIdSentence = -1;
        double highestUsefulness = -1;
        double usefulness;
        int selectedSentenceIndex = 0;
        int numSentsInBasenames = 0;
        HashMap<Integer,byte[]> feas;
        
        int i, j, k, n, maxNum=100000;
        int id;
        // loop over the ids
        if( vectorArray != null) {  // so the vectors are in memory
          for (i=0;i<idSentenceList.length;i++){
            id = idSentenceList[i];
        
            //if the next sentence was already selected, continue
            // the ids = -1 correspond to sentences already selected
            //if (id < 0) continue;
            if( selectedIdSents.contains(id) || unwantedIdSents.contains(id) ) continue;
            
            numSentsInBasenames++;
            //get the next feature vector
            byte[] nextFeatVects = vectorArray[i];
           
            //calculate how useful the feature vectors are
            usefulness = coverageDefinition.usefulnessOfFVs(nextFeatVects);
           
            if(usefulness > highestUsefulness){                         
                //the current sentence is (currently) the best sentence to add
                selectedIdSentence = id;
                selectedVectors = nextFeatVects;
                highestUsefulness = usefulness;     
                selectedSentenceIndex = i;
            }           
            if (usefulness == -1.0){
              unwantedIdSents.add(id);
              // idSentenceList[i] = -1;     // Here the sentence should be marked as unwanted?
              //System.out.println("unwanted id=" + id);
            }
             
                
          }  // end loop over all idsentence list
          
        } else { //The vectors are not in memory but will be loaded in groups
            //System.out.println("idSentenceList.length=" + idSentenceList.length);
            int numIdSent = idSentenceList.length;
            for(j=0; j<numIdSent; j+=maxNum){
              
              // load in memory maxNum features from the DB   
              if( (j+maxNum) >= numIdSent ){
                //System.out.println("Processing from j=" + j + " (" + idSentenceList[j] + ")  --> j=" + (j+maxNum) + " (" + idSentenceList[(numIdSent-1)] + ")");                  
                feas = wikiToDB.getFeaturesSet(j, (numIdSent-1), idSentenceList);
              }
              else {
                //System.out.println("Processing from j=" + j + " (" + idSentenceList[j] + ")  --> j=" + (j+maxNum) + " (" + idSentenceList[(j+maxNum)] + ")");                   
                feas = wikiToDB.getFeaturesSet(j, (j+maxNum), idSentenceList);
              }
              
             for (k=j; k<(j+maxNum) && k<numIdSent; k++){
                id = idSentenceList[k];
                //if the next sentence was already selected, continue
                // the ids = -1 correspond to sentences already selected
                //if (id < 0) continue;
                if( selectedIdSents.contains(id) || unwantedIdSents.contains(id) ) continue;
                
                numSentsInBasenames++;
                //get the next feature vector
                byte[] nextFeatVects = feas.get(id);
                if(nextFeatVects == null){
                  System.out.println("Warning id not found k=" + k + " id=" + idSentenceList[k]);
                  usefulness = -1.0;
                } else {                
                   //calculate how useful the feature vectors are
                  usefulness = coverageDefinition.usefulnessOfFVs(nextFeatVects);
                }               
                if(usefulness > highestUsefulness){                         
                    //the current sentence is (currently) the best sentence to add
                    selectedIdSentence = id;
                    selectedVectors = nextFeatVects;
                    highestUsefulness = usefulness;     
                    selectedSentenceIndex = k;
                }           
                if (usefulness == -1.0){
                  unwantedIdSents.add(id);
                  //System.out.println("unwanted id=" + id);                
                 //idSentenceList[k] = -1;     // Here the sentence should be marked as unwanted?
                }
              }  // end loop over one group 
              feas = null;
              
            }  // end loop of processing groups of maxNum  
            
        }  // end else the vectors are not in mm
        // end loop over all idsentence list 
        
   
        // end loop over all idsentence list 
        // System.out.println(numSentsInBasenames+" sentences left");
       
        
        //if (selectedBasename != null){
        if (selectedIdSentence > 0){
            //if we selected something,
            //remove selected filename from basename list
            //basenameList[selectedSentenceIndex] = null;
            //idSentenceList[selectedSentenceIndex] = -1;  
            selectedIdSents.add(selectedIdSentence);
            
            //print information
            if (verbose){
              //System.out.println("sentence "+sentenceIndex+" ("+selectedBasename+"), score: "+highestUsefulness);
              System.out.println("sentence "+sentenceIndex+" ("+selectedIdSentence+"), score: "+highestUsefulness);  
            }
            //logFile.println("Sentence "+sentenceIndex+" ("+selectedBasename+"), score: "+highestUsefulness);
            logFile.println("Sentence "+sentenceIndex+" ("+selectedIdSentence+"), score: "+highestUsefulness);
            logFile.flush();
        }   
        
    }
    
    
    /**
     * Determine if the stop criterion is reached
     * 
     * @param sentences the list of selected sentences
     * @param covDef the coverageDefinition
     * @return true, if stop criterion is reached, false otherwise
     */
    private boolean stopCriterionIsReached(Set<Integer> sentences,
            CoverageDefinition covDef){

        if (stopNumSentences && sentences.size()>=maxNumSents)
            //if we have the maximum number of sentences 
            //stop selecting immediately
            return true;
        
        //other stop criteria can be combined
        boolean result = false;
        if (stopSimpleDiphones && covDef.reachedMaxSimpleDiphones())
            result = true;
        if (stopSimpleProsody){
            if (covDef.reachedMaxSimpleProsody()){
                if (!stopSimpleDiphones && !stopClusteredDiphones){
                    //set result to true only if we do not have to consider 
                    //the simpleDiphones or clusteredDiphones stop criterion 
                    result = true; 
                }
                //else result remains false/true, depending on what the 
                //test result for simpleDiphones/clusteredDiphones was
            } else {
                //set the result to false, no matter what the result for
                //simpleDiphones/clusteredDiphones was
                result = false;
            }
        }
        return result;
    }



    /**
     * Read the feature vectors from disk 
     * for a given filename
     * 
     * @param basename the file from which to read from
     * @return the feature vectors from the file
     * 
     * @throws IOException
     */
    private byte[] getNextFeatureVectors1(String basename)
    throws IOException{
        //open the file
        FileInputStream fis =
            new FileInputStream(new File(basename));
        //read the first 4 bytes and combine them to get the number 
        //of feature vectors
        byte[] vlength = new byte[4];
        fis.read(vlength);
        int numFeatVects = (((vlength[0] & 0xff) << 24) 
                | ((vlength[1] & 0xff) << 16) 
                | ((vlength[2] & 0xff) << 8)
                | (vlength[3] & 0xff));
        //read the content of the file into a byte array
        byte[] vectorBuf = new byte[4*numFeatVects];
        fis.read(vectorBuf);
        fis.close();        
        //return the feature vectors
        return vectorBuf;
    }


}
