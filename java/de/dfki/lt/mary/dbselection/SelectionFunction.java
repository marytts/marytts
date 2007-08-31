
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

import java.util.*;
import java.io.*;

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
    private String selectedBasename;
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
    * @param selectedFilenames the list of selected filenames
    * @param coverageDefinition the coverage definition for the feature vectors
    * @param logFile the logFile to document the progress
    * @param basenameList the list of filenames of the sentences
    * @param holdVectorsInMemory if true, get vectors from coverage definition,
    *                            if false, read vectors from disk 
    * @param verbose print output also to command line
    * @return the list of selected filenames
    * @throws IOException
    */
    public void select(List selectedFilenames,
            CoverageDefinition coverageDefinition,
            PrintWriter logFile,
            String[] basenameList,
            boolean holdVectorsInMemory,
            boolean verbose)throws IOException{
        this.verbose = verbose;
        //get the array of vectors if they are loaded in memory
        byte[][] vectorArray = null;
        if (holdVectorsInMemory){
            vectorArray = coverageDefinition.getVectorArray();
        }
        byte[][] featVects = null;
        int sentIndex = selectedFilenames.size()+1;
        selectedVectors = null;
                
        // while the stop criterion is not reached      
        while(!stopCriterionIsReached(selectedFilenames,
                coverageDefinition)){ 
            
            //select the next sentence  
            selectNext(coverageDefinition,
                    logFile,
                    sentIndex,
                    basenameList,
                    vectorArray);

            //check if we selected something
            if (selectedBasename == null){
                //nothing more to select
                //System.out.println("Nothing more to select");
                logFile.println("Nothing more to select");
                break;
            }


            //add the selected sentence to the set
            selectedFilenames.add(selectedBasename);                
            //update coverageDefinition         
            coverageDefinition.updateCover(selectedVectors);
            sentIndex++;
        }  
        //print out total number of sentences
        sentIndex--;
        if (verbose)
            System.out.println("Total number of sentences : "+sentIndex);
        logFile.println("Total number of sentences : "+sentIndex);
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
    private void selectNext(CoverageDefinition coverageDefinition,
            PrintWriter logFile,
            int sentenceIndex,
            String[] basenameList,
            byte[][] vectorArray)throws IOException{
        
        selectedBasename = null;
        double highestUsefulness = -1;
        int selectedSentenceIndex = 0;
        int numSentsInBasenames = 0;
        //loop over the filenames
        for (int i=0;i<basenameList.length;i++){
            String nextBasename = basenameList[i];
            //if the next sentence was already selected, continue
            if (nextBasename == null) continue;
            numSentsInBasenames++;
            //get the next feature vector
            byte[] nextFeatVects;
            if (vectorArray != null){
                nextFeatVects = vectorArray[i];
            } else {
                nextFeatVects = 
                    getNextFeatureVectors(nextBasename);
            }
            //calculate how useful the feature vectors are
            double usefulness = 
                coverageDefinition.usefulnessOfFVs(nextFeatVects);
           
            if(usefulness > highestUsefulness){                         
                //the current sentence is (currently) 
                //the best sentence to add
                selectedBasename = nextBasename;
                selectedVectors = nextFeatVects;
                highestUsefulness = usefulness;     
                selectedSentenceIndex = i;
            }
            
            if (usefulness == -1.0)
                basenameList[i] = null;
        }
        //System.out.println(numSentsInBasenames+" sentences left");

        if (selectedBasename != null){
            //if we selected something,
            //remove selected filename from basename list
            basenameList[selectedSentenceIndex] = null;
            //print information
            if (verbose){
                System.out.println("sentence "+sentenceIndex
                        +" ("+selectedBasename+"), score: "
                        +highestUsefulness);
            }
            logFile.println("Sentence "+sentenceIndex
                    +" ("+selectedBasename+"), score: "
                    +highestUsefulness);
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
    private boolean stopCriterionIsReached(List sentences,
            CoverageDefinition covDef){

        if (stopNumSentences && sentences.size()>=maxNumSents)
            //if we have the maximum number of sentences 
            //stop selecting immediately
            return true;
        
        //other stop criteria can be combined
        boolean result = false;
        if (stopSimpleDiphones && covDef.reachedMaxSimpleDiphones())
            result = true;
        if (stopClusteredDiphones){
            if (covDef.reachedMaxClusteredDiphones()){
                if (!stopSimpleDiphones){
                    //set result to true only if we do not have to consider 
                    //the simpleDiphones stop criterion 
                    result = true;
                }
                //else result remains false/true, depending on what the 
                //test result for simpleDiphones was
            } else {
                //set the result to false, no matter what the result for
                //simpleDiphones was
                result = false;
            }
        }
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
        if (stopClusteredProsody){
            if (covDef.reachedMaxClusteredProsody()){
                if (!stopSimpleDiphones 
                        && !stopClusteredDiphones
                        && !stopSimpleProsody){
                    //set result to true only if we do not have to consider 
                    //the simpleDiphones, clusteredDiphones or simpleProsody 
                    //stop criterion 
                    result = true;
                }
                //else result remains false/true, depending on what the 
                //test result for 
                //simpleDiphones/clusteredDiphones/simpleProsody was
            } else {
                //set the result to false, no matter what the result for
                //simpleDiphones/clusteredDiphones/simpleProsody was
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
    private byte[] getNextFeatureVectors(String basename)
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