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
import java.text.DecimalFormat;
import java.util.*;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;


/**
 * Builds and manages the cover sets 
 * 
 * @author Anna Hunecke
 *
 */
public class CoverageDefinition{

    private int trueNumSentences;
    /* cover sets for simple and clustered diphones */
    private CoverNode simpleCover;
    private CoverNode clusteredCover;
    /* map from simple/clustered diphones to their frequency in the corpus */
    private SortedMap simpleDiphones2Frequency;
    private SortedMap clusteredDiphones2Frequency;
    /* weights of the different levels of the cover set */
    private double phoneLevelWeight;
    private double diphoneLevelWeight;
    private double prosodyLevelWeight;    
    /* use simple or clustered cover set for usefulness computation */
    private boolean simpleDiphones;
    /* consider frequency when computing usefulness */
    private boolean considerFrequency;
    /* the actual setting of the frequency 
     * (setting only considered when considerFrequency is true)*/
    private String frequencySetting;
    /* consider the length of a sentence when computing usefulness */
    private boolean considerSentenceLength;
    /* max/min sentence length a selected sentence is allowed to have 
     * (settings only considered when considerSentenceLength is true)*/
    private int maxSentLengthAllowed;
    private int minSentLengthAllowed;
    /* number by which the wanted weight of a node is divided 
     * each time a new feature vector is added to the node*/
    private double wantedWeightDecrease;
    /* the index of the four features in the feature vector */
    private int phoneFeatIndex;
    private int diphoneFeatIndex;
    private int phoneClassesIndex;
    private int prosodyIndex;
    /* the number of possible phone classes */
    private int numPhoneClasses;
    /* the number of possible phones */
    private int numPhoneValues;
    /* the number of possible phones minus the phones to ignore */
    private int numPhoneValuesMinusIgnored;
    /* the number of possible simple diphones */
    private int numPossibleSimpleDiphones;
    /* the number of possible clustered diphones */
    private int numPossibleClusteredDiphones;
    /* the number of feature vectors in the cover set */
    private int numSelectedFeatVects;
    /* the number of tokens in the corpus */
    private int numTokens;
    /* the number of simple/clustered diphones types in the corpus*/
    private int numSimpleDiphoneTypes;
    private int numClusteredDiphoneTypes;
    /* the number of simple/clustered feature vector types in the corpus */
    private int numSimpleFeatVectTypes;
    private int numClusteredFeatVectTypes;
    /* average/max/min sentence length in the corpus */
    private double averageSentLength;
    private int maxSentLength;
    private int minSentLength;
    /* the number of sentences in the cover set*/
    private int numSentencesInCover;
    /* max/min sentence length in the cover set */
    private int maxSentLengthInCover;
    private int minSentLengthInCover;
    /* maximum sizes of simple/clustered cover 
     * (=number of Leaves) */
    private int numSimpleLeaves;
    private int numClusteredLeaves;
    /* the phone coverage of the corpus */
    private double possiblePhoneCoverage;
    /* the simple diphone coverage of the corpus */
    private double possibleSimpleDiphoneCoverage;
    /* the clustered diphone coverage of the corpus */
    private double possibleClusteredDiphoneCoverage;
    /* the overall (=phone+simpleDiphone+prosody) coverage of the corpus */
    private double possibleOverallSimpleCoverage;
    /* the overall (=phone+clusteredDiphone+prosody) coverage of the corpus */
    private double possibleOverallClusteredCoverage;
    /* the phone types in the corpus */
    private Set possiblePhoneTypes;    
    /* keep track of the coverage development over time 
     * by adding a the current coverage value each time 
     * the cover is updated */
    private List phoneCoverageInTime;
    private List diphoneCoverageInTime;
    private List overallCoverageInTime;
    /* set of covered phones/simple diphones/clustered diphones */
    private Set phonesInCover;
    private Set simpleDiphonesInCover;
    private Set clusteredDiphonesInCover;
    /* number of simple/clustered prosodic variations in cover */
    private int numSimpleFeatVectsInCover;
    private int numClusteredFeatVectsInCover;
    /* the featureDefinition for the feature vectors */
    private FeatureDefinition featDef;
    /* the number of sentences in the corpus */
    private int numSentences;
    /* the phones that are not in the corpus and have to be ignored*/
    private List phonesToIgnore;
    /* if true, vectors stay in memory after they are read in*/
    private boolean holdVectorsInMemory;
    /* the vectors */
    private byte[][] vectorArray;   
    /* */
    private boolean needToReadVectors;
    /* the possible phone values */
    private String[] possiblePhoneArray;
    /* the possible next phone values */
    private String[] possibleNextPhoneArray;
    /* the possible next phone values */
    private String[] possibleNextPhoneClassArray;
    /* the possible next phone values */
    private String[] possibleProsodyArray;

    /**
     * Build a new coverage definition
     * and read in the config file
     * 
     * @param readConfigFile if true, read the config file,
     *                       else use default values
     * @param featDef the feature definition for the vectors
     * @param configFile the config file name
     * @param holdVectorsInMemory if true, vectors are stored in memory
     */
    public CoverageDefinition(FeatureDefinition featDef,
            String configFile,
            boolean holdVectorsInMemory,
            byte[][] vectorArray){
        this.holdVectorsInMemory = holdVectorsInMemory;
        this.vectorArray = vectorArray;
        if (vectorArray == null){
            needToReadVectors = true;
        } else {
            needToReadVectors = false;
        }
        this.featDef = featDef;

        //read config file             
        try{
            BufferedReader configIn = 
                new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(
                                        new File(configFile)),"UTF-8"));
            String line;
            int numparams = 0;
            //loop over the lines of the config file
            while((line = configIn.readLine()) != null){
                if (!line.startsWith("#") && !line.equals("")){
                    StringTokenizer tok = new StringTokenizer(line);
                    String key = tok.nextToken();
                    String value = tok.nextToken();
                    if (key.equals("simpleDiphones")){
                        if (value.equals("true")){
                            simpleDiphones = true;
                        } else {
                            simpleDiphones = false;
                        }
                        numparams++;
                        continue;
                    } 
                    if (key.equals("frequency")){
                        if (value.equals("none")){
                            considerFrequency = false;
                        } else {
                            considerFrequency = true;
                            frequencySetting = value;
                        }
                        numparams++;
                        continue;
                    }
                    if (key.equals("sentenceLength")){
                        if (value.equals("none")){
                            considerSentenceLength = false;
                        } else {
                            considerSentenceLength = true;
                            maxSentLengthAllowed = Integer.parseInt(value);
                            minSentLengthAllowed = Integer.parseInt(tok.nextToken());
                        }
                        numparams++;
                        continue;
                    }
                    if (key.equals("wantedWeight")){
                        phoneLevelWeight = Double.parseDouble(value);
                        diphoneLevelWeight = Double.parseDouble(tok.nextToken());
                        prosodyLevelWeight = Double.parseDouble(tok.nextToken());
                        numparams++;
                        continue;
                    }
                    if (key.equals("wantedWeightDecrease")){
                        wantedWeightDecrease = Double.parseDouble(value);
                        numparams++;
                        continue;
                    }                       
                    if (key.equals("missingPhones")){
                        phonesToIgnore = new ArrayList();
                        phoneFeatIndex = 
                            featDef.getFeatureIndex("mary_phoneme");
                        phonesToIgnore.add(
                                new Integer(featDef.getFeatureValueAsByte(phoneFeatIndex,value)));
                        while(tok.hasMoreTokens()){
                            phonesToIgnore.add(
                                    new Integer(featDef.getFeatureValueAsByte(
                                            phoneFeatIndex,tok.nextToken())));
                        }
                        numparams++;
                    }

                }
            }
            if (numparams<6){
                throw new Error("Error reading config file: there are only "
                        +numparams+" instead of 6 settings");
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Could not read config file");
        } 

    }


    /**
     * Compute the coverage of the corpus,
     * build and fill the cover sets
     * 
     * @param basenames the list of filenames 
     * @throws IOException
     */
    public void initialiseCoverage(String[] basenames)throws IOException{
        //stuff used for counting the phones and diphones
        possiblePhoneTypes = new HashSet();
        simpleDiphones2Frequency = new TreeMap();
        clusteredDiphones2Frequency = new TreeMap();
        Set simpleFeatVectTypes = new HashSet();
        Set clusteredFeatVectTypes = new HashSet();
        phoneFeatIndex = 
            featDef.getFeatureIndex("mary_phoneme");

        phoneClassesIndex = featDef.getFeatureIndex("mary_selection_next_phone_class");
        numPhoneClasses = featDef.getNumberOfValues(phoneClassesIndex);
        numPhoneValues = featDef.getNumberOfValues(phoneFeatIndex);
        numPhoneValuesMinusIgnored = numPhoneValues-phonesToIgnore.size()-1;

        diphoneFeatIndex = 
            featDef.getFeatureIndex("mary_next_phoneme");

        prosodyIndex = featDef.getFeatureIndex("mary_selection_prosody");
        int numPhoneTypes = 0;
        int numPhoneClassesTypes = 0;
        numSimpleDiphoneTypes = 0; numClusteredDiphoneTypes = 0;
        numTokens = 0;
        averageSentLength = 0.0; maxSentLength = 0; minSentLength = 20; 

        //get the features for the values
        possiblePhoneArray = featDef.getPossibleValues(phoneFeatIndex);
        possibleNextPhoneArray = featDef.getPossibleValues(diphoneFeatIndex);
        possibleNextPhoneClassArray = featDef.getPossibleValues(phoneClassesIndex);
        possibleProsodyArray = featDef.getPossibleValues(prosodyIndex);

        //build Cover
        buildCover();


        numSentences = basenames.length;
        trueNumSentences = numSentences;
        if (holdVectorsInMemory && needToReadVectors){
            vectorArray = new byte[numSentences][];
        }
        int tenPercent = numSentences/10; 
        //loop over the feature vectors  
        for (int index=0;index<numSentences;index++){
            if ((index % tenPercent) == 0 && index!=0){
                int percentage = index/tenPercent;
                System.out.print(" "+percentage+"0%");
            }
            //for each vector, get the values for the relevant features
            //add them to the list of possible values   
            //System.out.println(basenames[index]);
            String nextBasename = basenames[index];
            byte[] vectorBuf;
            int numFeatVects;
            if (needToReadVectors){
                FileInputStream fis;
                try{ 
                    fis =
                        new FileInputStream(new File(nextBasename));
                } catch (FileNotFoundException fnfe){
                    //skip file
                    System.out.println("Could not find file "+nextBasename);
                    if (holdVectorsInMemory){
                        vectorArray[index] = null;
                    }
                    trueNumSentences--;
                    basenames[index] = null;
                    continue;
                }
                //construct the number of vectors from the first 4 bytes
                byte[] vlength = new byte[4];
                fis.read(vlength);
                numFeatVects = (((vlength[0] & 0xff) << 24) 
                        | ((vlength[1] & 0xff) << 16) 
                        | ((vlength[2] & 0xff) << 8)
                        | (vlength[3] & 0xff));
                vectorBuf = new byte[4*numFeatVects];
                //read the feature vectors
                int yeah = fis.read(vectorBuf);
                //make sure we read all vectors
                assert (yeah == numFeatVects*4);
                fis.close();           

                if (holdVectorsInMemory){
                    vectorArray[index] = vectorBuf;
                }

            } else {
                vectorBuf = vectorArray[index];
                numFeatVects = vectorBuf.length;
            }
            //compute statistics of sentence length
            averageSentLength += numFeatVects;
            if (numFeatVects>maxSentLength)
                maxSentLength = numFeatVects;
            if(numFeatVects<minSentLength)
                minSentLength=numFeatVects;

            /* loop over the feature vectors */
            for (int i=0;i<numFeatVects;i++){
                numTokens++;

                //save feature vector in simple diphone tree                
                CoverLeaf leaf = goDownTree(true,vectorBuf,i,false);
                leaf.addPossibleInstance();

                //save feature vector in clustered diphone tree
                leaf = goDownTree(false,vectorBuf,i,false);
                leaf.addPossibleInstance();


                //first deal with current phone
                byte nextPhonebyte = getVectorValue(vectorBuf,i,phoneFeatIndex);

                //add 1 to the frequency value of the phone
                if (possiblePhoneTypes.add(possiblePhoneArray[nextPhonebyte]))
                    numPhoneTypes++;

                //deal with current diphone
                byte nextnextPhonebyte = getVectorValue(vectorBuf,i,diphoneFeatIndex);
                /**
				 String simpleDiphone = possiblePhoneArray[nextPhonebyte]+"_"
				 +possibleNextPhoneArray[nextnextPhonebyte];
                 **/
                StringBuffer buf = new StringBuffer(5);
                buf.append(possiblePhoneArray[nextPhonebyte]);
                buf.append("_");
                buf.append(possibleNextPhoneArray[nextnextPhonebyte]);  
                String simpleDiphone = buf.toString();

                nextnextPhonebyte = getVectorValue(vectorBuf,i,phoneClassesIndex);


                /**
				 String clusteredDiphone = possiblePhoneArray[nextPhonebyte]
				 +"_"+possibleNextPhoneClassArray[nextnextPhonebyte];
                 **/

                buf = new StringBuffer(5);
                buf.append(possiblePhoneArray[nextPhonebyte]);
                buf.append("_");
                buf.append(possibleNextPhoneClassArray[nextnextPhonebyte]);  
                String clusteredDiphone = buf.toString();

                //add 1 to the frequency value of the diphone
                if (simpleDiphones2Frequency.containsKey(simpleDiphone)){
                    int freq = 
                        ((Integer)simpleDiphones2Frequency.get(simpleDiphone)).intValue()+1;
                    simpleDiphones2Frequency.put(simpleDiphone,new Integer(freq));
                } else {
                    numSimpleDiphoneTypes++;
                    simpleDiphones2Frequency.put(simpleDiphone,new Integer(1));
                }

                //add 1 to the frequency value of the diphone
                if (clusteredDiphones2Frequency.containsKey(clusteredDiphone)){
                    int freq = 
                        ((Integer)clusteredDiphones2Frequency.get(clusteredDiphone)).intValue()+1;
                    clusteredDiphones2Frequency.put(clusteredDiphone,new Integer(freq));
                } else {
                    numClusteredDiphoneTypes++;
                    clusteredDiphones2Frequency.put(clusteredDiphone,new Integer(1));
                } 

                //deal with current diphone
                byte prosodyValue = getVectorValue(vectorBuf,i,prosodyIndex);
                /**
				 simpleFeatVectTypes.add(simpleDiphone+"_"
				 +possibleProsodyArray[prosodyValue]);
                 */
                buf = new StringBuffer(7);
                buf.append(simpleDiphone);
                buf.append("_");
                buf.append(prosodyValue);
                //add feature vector type if not already added
                simpleFeatVectTypes.add(buf.toString());

                /**
				 clusteredFeatVectTypes.add(clusteredDiphone+"_"
				 +possibleProsodyArray[prosodyValue]);
                 **/
                buf = new StringBuffer(7);
                buf.append(clusteredDiphone);
                buf.append("_");
                buf.append(prosodyValue); 
                clusteredFeatVectTypes.add(buf.toString());
                buf = null;
                simpleDiphone = null;
                clusteredDiphone = null;

            } 
        }//end of for-loop over feature vectors

        //compute average sentence length
        averageSentLength = averageSentLength/(double) trueNumSentences;

        //calculate cover size
        numPossibleSimpleDiphones = numPhoneValuesMinusIgnored*(numPhoneValuesMinusIgnored+1);
        numPossibleClusteredDiphones = numPhoneClasses*numPhoneValuesMinusIgnored;
        numSimpleLeaves = numPossibleSimpleDiphones*6;
        numClusteredLeaves = numPossibleClusteredDiphones*6;

        //number of feature vector types
        numSimpleFeatVectTypes = simpleFeatVectTypes.size();
        numClusteredFeatVectTypes = clusteredFeatVectTypes.size();
        //compute coverage of corpus
        possiblePhoneCoverage = 
            (double)numPhoneTypes/(double)(numPhoneValuesMinusIgnored);
        possibleSimpleDiphoneCoverage = 
            numSimpleDiphoneTypes/(double)numPossibleSimpleDiphones;
        possibleClusteredDiphoneCoverage = 
            numClusteredDiphoneTypes/(double)numPossibleClusteredDiphones;
        possibleOverallSimpleCoverage =  
            (double)numSimpleFeatVectTypes/(double)numSimpleLeaves;
        possibleOverallClusteredCoverage =  
            (double)numClusteredFeatVectTypes/(double)numClusteredLeaves;     

        //calculate relative frequency for each node
        //rel. freq. = freq / all tokens
        if (simpleDiphones){
            computeRelativeFrequency(simpleCover,numTokens);
        } else {
            computeRelativeFrequency(clusteredCover,numTokens);
        }

        //initialise several variables
        numSelectedFeatVects = 0;
        numSentencesInCover = 0;
        maxSentLengthInCover = 0;
        minSentLengthInCover = 20;
        phoneCoverageInTime = new ArrayList();
        diphoneCoverageInTime = new ArrayList();
        overallCoverageInTime = new ArrayList();
        phonesInCover = new HashSet();
        simpleDiphonesInCover = new HashSet();
        clusteredDiphonesInCover = new HashSet();
        numSimpleFeatVectsInCover = 0;
        numClusteredFeatVectsInCover = 0;
    }

    /**
     * Build the trees that represent the cover sets
     *
     */
    private void buildCover(){       
        simpleCover = 
            new CoverNode((byte)numPhoneValues,wantedWeightDecrease);
        clusteredCover = 
            new CoverNode((byte)numPhoneValues,wantedWeightDecrease);

        //compute all possible combinations
        for (int k=0;k<possiblePhoneArray.length;k++){
            if (phonesToIgnore.contains(new Integer(k))) continue;            
            //find out the index of the current phone
            byte nextIndex = (byte)k;

            //add a node for the phonetic identity of the next phone
            CoverNode nextSimpleChild = 
                new CoverNode((byte)numPhoneValues,wantedWeightDecrease);
            //add a node for the phone class of the next phone
            CoverNode nextClusteredChild = 
                new CoverNode((byte)numPhoneClasses,wantedWeightDecrease);

            //set the weight that determines how many instances 
            //are wanted of this phone
            nextSimpleChild.setWantedWeight(phoneLevelWeight);
            nextClusteredChild.setWantedWeight(phoneLevelWeight);

            simpleCover.addChild(nextSimpleChild,nextIndex);
            clusteredCover.addChild(nextClusteredChild,nextIndex);

            byte numGrandChildren = nextSimpleChild.getNumChildren();
            //go through the grandchildren of simpleCover
            for (byte i=0;i<numGrandChildren;i++){
                //each grandchild is a prosody node
                CoverNode prosodyNode = new CoverNode((byte)6,wantedWeightDecrease);

                //set the weight that determines how many instances 
                //are wanted of this diphone
                prosodyNode.setWantedWeight(diphoneLevelWeight);
                nextSimpleChild.addChild(prosodyNode,i);   
                //go through the children of the prosody node
                for (byte j=0;j<6;j++){
                    //each child is a leaf
                    CoverLeaf prosodyChild = 
                        new CoverLeaf(wantedWeightDecrease);
                    //set the weight that determines how many instances 
                    //are wanted of this prosody variation
                    prosodyChild.setWantedWeight(prosodyLevelWeight);
                    prosodyNode.addChild(prosodyChild,j);
                }                                   
            }

            numGrandChildren = nextClusteredChild.getNumChildren();
            //go through the grandchildren of clusteredCover
            for (byte i=0;i<numGrandChildren;i++){
                //each grandchild is a prosody node
                CoverNode prosodyNode = new CoverNode((byte)6,wantedWeightDecrease);

                //set the weight that determines how many instances 
                //are wanted of this diphone
                prosodyNode.setWantedWeight(diphoneLevelWeight);
                nextClusteredChild.addChild(prosodyNode,i);   
                //go through the children of the prosody node
                for (byte j=0;j<6;j++){
                    //each child is a leaf
                    CoverLeaf prosodyChild = 
                        new CoverLeaf(wantedWeightDecrease);
                    //set the weight that determines how many instances 
                    //are wanted of this prosody variation
                    prosodyChild.setWantedWeight(prosodyLevelWeight);
                    prosodyNode.addChild(prosodyChild,j);
                }                                   
            }

        }    

    }





    /**
     * Go down the cover tree according to the values
     * in the feature vector 
     * 
     * @param simpleDiphones if true, go down simple cover tree,
     *                      else go down clustered cover tree
     * @param vectors the feature vectors
     * @param index the index of the current feature vector
     * @param addNewFeatureVector if true, decrease wantedWeights 
     *                          of the nodes you pass
     * @return the leaf that you arrived at
     */
    private CoverLeaf goDownTree(boolean simpleDiphones,
            byte[] vectors,
            int index,
            boolean addNewFeatureVector){
        //go down to phone level
        byte nextIndex = getVectorValue(vectors,index,phoneFeatIndex);     
        CoverNode nextNode;
        if (simpleDiphones){
            nextNode = simpleCover.getChild(nextIndex);  
        } else {
            nextNode = clusteredCover.getChild(nextIndex); 
        }
        if (addNewFeatureVector)
            nextNode.decreaseWantedWeight();

        //go down to diphone level
        if (simpleDiphones){
            nextIndex = getVectorValue(vectors,index,diphoneFeatIndex);
        } else {
            nextIndex = getVectorValue(vectors,index,phoneClassesIndex);
        }        
        nextNode = nextNode.getChild(nextIndex);
        if (addNewFeatureVector)
            nextNode.decreaseWantedWeight();
        //go down to prosody level
        nextIndex = getVectorValue(vectors,index,prosodyIndex);
        nextNode = nextNode.getChild(nextIndex);
        if (addNewFeatureVector)
            nextNode.decreaseWantedWeight();
        if (!(nextNode instanceof CoverLeaf)){
            //something went wrong
            throw new Error("Went down cover tree for feature vector"
                    +" and did not end up on leaf!");
        }
        return (CoverLeaf)nextNode;
    }

    /**
     * Compute the relative frequency of each node in the corpus
     * 
     * @param node the node to compute the frequency for
     * @param allTokens total number of tokens in the corpus
     * @return the frequency for the given node
     */
    private double computeRelativeFrequency(CoverNode node, 
            double allTokens){
        double freq = 0;
        if (node instanceof CoverLeaf){
            //compute the relative frequency for this leaf
            int numPossInstances = ((CoverLeaf) node).maxNumFeatVects();
            freq = (double)numPossInstances / allTokens;
            if (considerFrequency){
                if(frequencySetting.equals("1minus")){
                    node.setFrequencyWeight(1-freq);
                } else {
                    if (frequencySetting.equals("inverse")){
                        node.setFrequencyWeight(1/freq);
                    } else {
                        node.setFrequencyWeight(freq);
                    }
                }
            }
        } else {
            //frequency is the sum of the frequency of the children
            byte numChildren = node.getNumChildren();
            //go through children
            for (byte i=0;i<numChildren;i++){
                CoverNode child = node.getChild(i);
                if (child == null) continue;
                freq += computeRelativeFrequency(child,allTokens);
            }
            if (considerFrequency){
                if(frequencySetting.equals("1minus")){
                    node.setFrequencyWeight(1-freq);
                } else {
                    if (frequencySetting.equals("inverse")){
                        node.setFrequencyWeight(1/freq);
                    } else {
                        node.setFrequencyWeight(freq);
                    }
                }
            }
        }
        return freq;

    }



    /**
     * Print a statistic of the unit distribution
     * in the corpus
     * 
     * @param filename the file to print to
     */
    public void printTextCorpusStatistics(String filename)throws Exception{
        DecimalFormat df = new DecimalFormat("0.00000");
        PrintWriter out =  
            new PrintWriter(
                    new FileWriter(
                            new File(filename)),true);
        out.println("*********************"+
                "\n* Unit distribution *"+
        "\n*********************\n\n");

        /* print out the sentence length statistics */
        out.println("Number of sentences : "+trueNumSentences);
        out.println("Average sentence length : "+averageSentLength);
        out.println("Maximum sentence length : "+maxSentLength);
        out.println("Minimum sentence length : "+minSentLength);

        /* print out coverage statistics */ 
        out.println("\nSimple Coverage:");
        out.println("phones: "+df.format(possiblePhoneCoverage));
        out.println("diphones: "+df.format(possibleSimpleDiphoneCoverage));
        out.println("overall: "+df.format(possibleOverallSimpleCoverage));

        out.println("\nClustered Coverage:");
        out.println("phones: "+df.format(possiblePhoneCoverage));
        out.println("diphones: "+df.format(possibleClusteredDiphoneCoverage));
        out.println("overall: "+df.format(possibleOverallClusteredCoverage)+"\n\n");

        if (possiblePhoneCoverage<100){
            out.println("The following phones are missing: ");
            for (int k=1;k<possiblePhoneArray.length;k++){
                String nextPhone = possiblePhoneArray[k]; 
                if (phonesToIgnore.contains(new Integer(k))) continue;
                if (!possiblePhoneTypes.contains(nextPhone)){
                    out.print(nextPhone+" ");
                }
            }
            out.print("\n");           
        }

        /* print out the diphone statistics */
        out.println("\n");
        out.println("Number of diphones           : "+numTokens);
        out.println("Number of different diphones : "+numSimpleDiphoneTypes);

        out.println("\n\nDiphones and their frequencies :\n");
        out.println("Simple diphones:\n");
        printDiphones(out,simpleDiphones2Frequency);    
        out.println("\nClustered diphones:\n");
        printDiphones(out,clusteredDiphones2Frequency);  

        simpleDiphones2Frequency = null;
        clusteredDiphones2Frequency = null;
        out.flush();
        out.close();    
    }

    /**
     * Print the settings of the config file
     * 
     *@param out the PrintWriter to print to
     */
    public void printSettings(PrintWriter out){
        /* print out setings */
        out.println("\nSettings of Coverage Definition:");
        out.println("simpleDiphones "+Boolean.toString(simpleDiphones));
        if (considerFrequency){
            out.println("frequency "+frequencySetting);
        } else {
            out.println("frequency none");
        }
        out.println("considerSentenceLength "+Boolean.toString(considerSentenceLength));

        out.println("phoneLevelWeight "+phoneLevelWeight);
        out.println("diphoneLevelWeight "+diphoneLevelWeight);
        out.println("prosodyLevelWeight "+prosodyLevelWeight);        
        out.println("divideWantedWeightBy "+wantedWeightDecrease);
        if (considerSentenceLength){
            out.println("maxSentenceLength "+maxSentLengthAllowed);
            out.println("minSentenceLength "+minSentLengthAllowed);
        }

    }

    /**
     * Print the diphone distribution of the corpus 
     * 
     * @param out the PrintWriter to print to
     * @param ph2Frequency maps from diphones to their frequency
     */
    private void printDiphones(PrintWriter out,
            Map ph2Frequency){
        DecimalFormat df = new DecimalFormat("0.00000");
        //Sort phones according to their frequencies
        TreeMap<Integer,List<String>> freq2Phones = new TreeMap<Integer,List<String>>(Collections.reverseOrder());
        Map<Integer,Double> freq2Prob = new HashMap<Integer,Double>();
        Set phones = ph2Frequency.keySet();
        for (Iterator it = phones.iterator();it.hasNext();){
            String nextPhone = (String) it.next();
            Integer nextFreq = (Integer)ph2Frequency.get(nextPhone);            
            if (!freq2Phones.containsKey(nextFreq)){
                List<String> phoneList = new ArrayList<String>();
                phoneList.add(nextPhone);
                freq2Phones.put(nextFreq,phoneList);
                int freq = nextFreq.intValue();
                double prob = (double)freq*100.0/(double)numTokens;
                freq2Prob.put(nextFreq,new Double(prob));
            } else {
                List<String> phoneList = freq2Phones.get(nextFreq);
                phoneList.add(nextPhone);
            }
        }
        //output phones and their frequencies
        Set<Integer> frequencies = freq2Phones.keySet();
        for (Integer nextFreq : frequencies){
            Double nextProb = freq2Prob.get(nextFreq);
            List<String> nextPhoneList = freq2Phones.get(nextFreq);
            for (int i=0; i<nextPhoneList.size();i++){
                out.print(nextPhoneList.get(i));
                out.print(" : ");
                out.print(nextFreq);
                out.print(", ");
                out.println(df.format(nextProb));
            }
        }
    }


    /**
     * Print statistics of the selected sentences
     * and a table of coverage development over time
     *  
     *@param distributionFile the file to print the statistics to
     *@param developmentFile the file to print the coverage development to 
     *@param logDevelopment if true, print development file
     *@throws Exception
     */
    public void printSelectionDistribution(String distributionFile,
            String developmentFile,
            boolean logDevelopment)
    throws Exception{        
        PrintWriter disOut =  
            new PrintWriter(
                    new FileWriter(
                            new File(distributionFile)));
        /* print settings */
        DecimalFormat df = new DecimalFormat("0.00000");
        disOut.println("\nSettings of Coverage Definition:");
        disOut.println("simpleDiphones "+Boolean.toString(simpleDiphones));
        if (considerFrequency){
            disOut.println("frequency "+frequencySetting);
        } else {
            disOut.println("frequency none");
        }
        disOut.println("considerSentenceLength "+Boolean.toString(considerSentenceLength));
        disOut.println("phoneLevelWeight "+phoneLevelWeight);
        disOut.println("diphoneLevelWeight "+diphoneLevelWeight);
        disOut.println("prosodyLevelWeight "+prosodyLevelWeight);
        disOut.println("maxSentenceLength "+maxSentLengthAllowed);
        disOut.println("minSentenceLength "+minSentLengthAllowed);
        disOut.println("divideWantedWeightBy "+wantedWeightDecrease);

        /* print results */
        disOut.println("\nResults:");        
        disOut.println("Num sent in cover : "+numSentencesInCover);
        double avSentLength = (double)numSelectedFeatVects/(double)numSentencesInCover;
        disOut.println("Avg sent length : "+df.format(avSentLength));
        disOut.println("Max sent length : "+maxSentLengthInCover);
        disOut.println("Min sent length : "+minSentLengthInCover); 

        /* print distribution info */
        double phoneCov = (double)phonesInCover.size()/(double)numPhoneValuesMinusIgnored;
        double simpleDiphoneCov = (double)simpleDiphonesInCover.size()/(double)numPossibleSimpleDiphones;
        double overallSimpleCov =  (double)numSimpleFeatVectsInCover/(double)numSimpleLeaves;
        double clusteredDiphoneCov = (double)clusteredDiphonesInCover.size()/(double)numPossibleClusteredDiphones;
        double overallClusteredCov =  (double)numClusteredFeatVectsInCover/(double)numClusteredLeaves;

        disOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");

        disOut.println("Simple Coverage:");
        //disOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");
        disOut.println("diphones: "+df.format(simpleDiphoneCov)
                +" ("+df.format(possibleSimpleDiphoneCoverage)+")");
        disOut.println("overall: "+df.format(overallSimpleCov)
                +" ("+df.format(possibleOverallSimpleCoverage)+")");

        disOut.println("Clustered Coverage:");
        //disOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");
        disOut.println("diphones: "+df.format(clusteredDiphoneCov)
                +" ("+df.format(possibleClusteredDiphoneCoverage)+")");
        disOut.println("overall: "+df.format(overallClusteredCov)
                +" ("+df.format(possibleOverallClusteredCoverage)+")");         
        disOut.flush();
        disOut.close();

        /* print coverage development over time */
        if (logDevelopment){
            PrintWriter devOut =  
                new PrintWriter(
                        new FileWriter(
                                new File(developmentFile)));
            devOut.println("\toverall coverage\tdiphone coverage\tphone coverage");
            for (int i=0;i<overallCoverageInTime.size();i++){
                devOut.print(i+"\t"+df.format(overallCoverageInTime.get(i))
                        +"\t"+df.format(diphoneCoverageInTime.get(i))
                        +"\t"+df.format(phoneCoverageInTime.get(i))+"\n");           
            }        
            devOut.flush();
            devOut.close();
        }
    }

    public void printResultToLog(PrintWriter logOut){
        /* print settings */
        DecimalFormat df = new DecimalFormat("0.00000");
        logOut.println("simpleDiphones "+Boolean.toString(simpleDiphones));
        if (considerFrequency){
            logOut.println("frequency "+frequencySetting);
        } else {
            logOut.println("frequency none");
        }
        logOut.println("considerSentenceLength "+Boolean.toString(considerSentenceLength));
        logOut.println("phoneLevelWeight "+phoneLevelWeight);
        logOut.println("diphoneLevelWeight "+diphoneLevelWeight);
        logOut.println("prosodyLevelWeight "+prosodyLevelWeight);
        logOut.println("maxSentenceLength "+maxSentLengthAllowed);
        logOut.println("minSentenceLength "+minSentLengthAllowed);
        logOut.println("divideWantedWeightBy "+wantedWeightDecrease);

        logOut.println("\nNum sent in cover : "+numSentencesInCover);
        double avSentLength = (double)numSelectedFeatVects/(double)numSentencesInCover;
        logOut.println("Avg sent length : "+df.format(avSentLength));
        logOut.println("Max sent length : "+maxSentLengthInCover);
        logOut.println("Min sent length : "+minSentLengthInCover); 

        /* print distribution info */
        double phoneCov = (double)phonesInCover.size()/(double)numPhoneValuesMinusIgnored;
        double simpleDiphoneCov = (double)simpleDiphonesInCover.size()/(double)numPossibleSimpleDiphones;
        double overallSimpleCov =  (double)numSimpleFeatVectsInCover/(double)numSimpleLeaves;
        double clusteredDiphoneCov = (double)clusteredDiphonesInCover.size()/(double)numPossibleClusteredDiphones;
        double overallClusteredCov =  (double)numClusteredFeatVectsInCover/(double)numClusteredLeaves;

        logOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");

        logOut.println("Simple Coverage:");
        //logOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");
        logOut.println("diphones: "+df.format(simpleDiphoneCov)
                +" ("+df.format(possibleSimpleDiphoneCoverage)+")");
        logOut.println("overall: "+df.format(overallSimpleCov)
                +" ("+df.format(possibleOverallSimpleCoverage)+")");

        logOut.println("Clustered Coverage:");
        //logOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");
        logOut.println("diphones: "+df.format(clusteredDiphoneCov)
                +" ("+df.format(possibleClusteredDiphoneCoverage)+")");
        logOut.println("overall: "+df.format(overallClusteredCov)
                +" ("+df.format(possibleOverallClusteredCoverage)+")\n\n");         


    }


    /**
     * Add the given feature vectors to the cover
     * 
     * @param coveredFVs the feature vectors to add 
     */
    public void updateCover(byte[] coveredFVs){
        int numNewFeatVects = coveredFVs.length/4;
        int newPhones = 0;
        int newDiphones = 0;
        //loop through the feature vectors
        for (int i=0;i<numNewFeatVects;i++){

            /* update simpleCover */
            CoverLeaf leaf = 
                goDownTree(true,coveredFVs,i,true);
            //if this is the first feature vector in this leaf
            //decrease cover size
            if (leaf.getNumFeatureVectors()==0){
                numSimpleFeatVectsInCover++;
            }
            leaf.addFeatureVector();
            String phone = possiblePhoneArray[getVectorValue(coveredFVs,i,phoneFeatIndex)];
            //update coverage statistics
            String diphone = phone+"_"
            +possibleNextPhoneArray[getVectorValue(coveredFVs,i,diphoneFeatIndex)];

            phonesInCover.add(phone);
            simpleDiphonesInCover.add(diphone);

            /* update clusteredCover */
            leaf = goDownTree(false,coveredFVs,i,true);
            //if this is the first feature vector in this leaf
            //decrease cover size
            if (leaf.getNumFeatureVectors()==0){
                numClusteredFeatVectsInCover++;
            }
            leaf.addFeatureVector();
            //update coverage statistics
            diphone = phone+"_"
            +possibleNextPhoneClassArray[getVectorValue(coveredFVs,i,phoneClassesIndex)];
            clusteredDiphonesInCover.add(diphone);
        } 
        //update phone coverage statistics
        double phoneCoverage = (double)phonesInCover.size()/(double)numPhoneValuesMinusIgnored;
        phoneCoverageInTime.add(new Double(phoneCoverage));
        //update diphone and overall coverage statistics
        if (simpleDiphones){
            double diphoneCoverage = 
                (double)simpleDiphonesInCover.size()/(double)numPossibleSimpleDiphones;
            double overallCoverage = 
                (double) numSimpleFeatVectsInCover/(double)numSimpleLeaves;
            diphoneCoverageInTime.add(new Double(diphoneCoverage));        
            overallCoverageInTime.add(new Double(overallCoverage));
        } else {
            double diphoneCoverage = 
                (double)clusteredDiphonesInCover.size()/(double)numPossibleClusteredDiphones;
            double overallCoverage = 
                (double) numClusteredFeatVectsInCover/(double)numClusteredLeaves;
            diphoneCoverageInTime.add(new Double(diphoneCoverage));        
            overallCoverageInTime.add(new Double(overallCoverage));
        }
        //compute statistics of sentence length
        numSentencesInCover ++;
        if (numNewFeatVects>maxSentLengthInCover)
            maxSentLengthInCover = numNewFeatVects;
        if(numNewFeatVects<minSentLengthInCover)
            minSentLengthInCover=numNewFeatVects;
        numSelectedFeatVects += numNewFeatVects;
    }



    /**
     * Check if cover has maximum simple diphone coverage
     * 
     * @return true if cover has maximum simple diphone coverage
     */
    public boolean reachedMaxSimpleDiphones(){
        return simpleDiphonesInCover.size() >= numSimpleDiphoneTypes;        
    }

    /**
     * Check if cover has maximum clustered diphone coverage
     * 
     * @return true if cover has maximum clustered diphone coverage
     */
    public boolean reachedMaxClusteredDiphones(){
        return clusteredDiphonesInCover.size() >= numClusteredDiphoneTypes;        
    }

    /**
     * Check if cover has maximum simple prosody coverage
     * 
     * @return true if cover has maximum simple prosody coverage
     */
    public boolean reachedMaxSimpleProsody(){
        boolean result = numSimpleFeatVectsInCover == numSimpleFeatVectTypes;
        return result;
    }

    /**
     * Check if cover has maximum clustered prosody coverage
     * 
     * @return true if cover has maximum clustered prosody coverage
     */
    public boolean reachedMaxClusteredProsody(){
        return numClusteredFeatVectsInCover == numClusteredFeatVectTypes;        
    }

    /**
     * Get the usefulness of the given feature vectors
     * Usefulness of a feature vector is defined as
     * the sum of the score for the feature vectors 
     * on all levels of the tree. On each level, 
     * the score is the product of the two weights 
     * of the node.
     * The first weight reflects the frequency/
     * inverted frequency of the value associated 
     * with the node in the corpus (=> frequencyWeight).
     * The second weight reflects how much an instance 
     * of a feature vector containing the associated 
     * value is wanted in the cover (=> wantedWeight).
     *  
     * @param featureVectors the feature vectors
     * @return the usefulness
     */
    public double usefulnessOfFVs(byte[] featureVectors){
        double usefulness = 0.0;
        int numFeatureVectors = featureVectors.length/4;
        if (considerSentenceLength){
            //too long sentences are useless
            if (numFeatureVectors>maxSentLengthAllowed)
                return -1.0;
            //too short sentences are useless as well
            if (numFeatureVectors<minSentLengthAllowed)
                return -1.0;
        }
        //loop over the feature vectors
        //System.out.print("Usefulness = ");
        for (int i=0;i<numFeatureVectors;i++){

            double u = 0;
            //get the associated leaf
            //go down to phone level
            byte nextIndex = getVectorValue(featureVectors,i,phoneFeatIndex); 
            CoverNode nextNode;
            if (simpleDiphones){
                nextNode = simpleCover.getChild(nextIndex); 
            }else{
                nextNode = clusteredCover.getChild(nextIndex); 
            }
            double relFreq = nextNode.getFrequencyWeight();
            double wantedWeight = nextNode.getWantedWeight();
            //System.out.print(" +"+relFreq+"*"+wantedWeight);
            u += relFreq*wantedWeight;
            //go down to diphone level
            if (simpleDiphones){
                nextIndex = getVectorValue(featureVectors,i,diphoneFeatIndex);
            } else {
                nextIndex = getVectorValue(featureVectors,i,phoneClassesIndex);
            }        
            nextNode = nextNode.getChild(nextIndex);
            relFreq = nextNode.getFrequencyWeight();
            wantedWeight = nextNode.getWantedWeight();
            //System.out.print(" +"+relFreq+"*"+wantedWeight);
            u += relFreq*wantedWeight;
            //go down to prosody level
            nextIndex = getVectorValue(featureVectors,i,prosodyIndex);
            nextNode = nextNode.getChild(nextIndex);            
            relFreq = nextNode.getFrequencyWeight();
            wantedWeight = nextNode.getWantedWeight();
            //System.out.print(" +"+relFreq+"*"+wantedWeight+"\n");
            u += relFreq*wantedWeight;
            usefulness += u;
        }
        //System.out.print(" = "+usefulness+"\n");
        double result = usefulness/(double)numFeatureVectors;
        return result;
    }


    public byte[][] getVectorArray(){
        return vectorArray;
    }

    public byte getVectorValue(byte[] vectors,int vectorIndex, int valueIndex){
        byte result = vectors[vectorIndex*4+valueIndex];
        return result;
    }

    /**
     * Print the cover sets to the given file
     * 
     * @param filename the file to print to
     */
    public void writeCoverageBin(String filename)throws Exception{
        DataOutputStream out = 
            new DataOutputStream(
                    new FileOutputStream(
                            new File(filename)));
        /* print all the relevant information */
        out.writeInt(numTokens);
        out.writeInt(numSimpleDiphoneTypes);
        out.writeInt(numClusteredDiphoneTypes);
        out.writeInt(numSimpleFeatVectTypes);
        out.writeInt(numClusteredFeatVectTypes);
        out.writeDouble(averageSentLength);
        out.writeInt(maxSentLength);
        out.writeInt(minSentLength);
        out.writeInt(numSimpleLeaves);
        out.writeInt(numClusteredLeaves);
        out.writeDouble(possiblePhoneCoverage);
        out.writeDouble(possibleSimpleDiphoneCoverage);
        out.writeDouble(possibleClusteredDiphoneCoverage);
        out.writeDouble(possibleOverallSimpleCoverage);
        out.writeDouble(possibleOverallClusteredCoverage);    
        out.writeInt(numSentences);
        /* print the coverage tree */
        writeTreeBin(out,simpleCover); 
        writeTreeBin(out,clusteredCover);
        out.flush();
        out.close();
    }



    /**
     * Print the cover tree
     * 
     * @param out the output stream to write to
     * @param cover the tree to print
     */
    private void writeTreeBin(DataOutputStream out, CoverNode cover)
    throws IOException{

        //go down to phone level
        byte numChildren = cover.getNumChildren();  
        double frequencyWeight = cover.getFrequencyWeight();
        double wantedWeight = cover.getWantedWeight();
        double wantedWeightDecrease = cover.getWantedWeightDecrease();
        out.writeByte(numChildren);
        for (byte i=0;i<numChildren;i++){
            if (phonesToIgnore.contains(new Integer(i))) continue;
            CoverNode phoneNode = cover.getChild(i);
            frequencyWeight = phoneNode.getFrequencyWeight();
            wantedWeight = phoneNode.getWantedWeight();
            wantedWeightDecrease = phoneNode.getWantedWeightDecrease();
            byte numNextChildren = phoneNode.getNumChildren();  
            out.writeByte(numNextChildren);
            //go down to diphone level        
            for (byte j=0;j<numNextChildren;j++){
                CoverNode diphoneNode = phoneNode.getChild(j);
                //go down to prosody level
                byte numNextNextChildren = diphoneNode.getNumChildren();  
                out.writeByte(numNextNextChildren);
                for (byte k=0;k<numNextNextChildren;k++){
                    CoverLeaf nextLeaf = (CoverLeaf)diphoneNode.getChild(k);
                    int numVectors = nextLeaf.maxNumFeatVects();
                    frequencyWeight = nextLeaf.getFrequencyWeight();
                    wantedWeight = nextLeaf.getWantedWeight();
                    wantedWeightDecrease = nextLeaf.getWantedWeightDecrease();
                    out.writeInt(numVectors);                    
                }

            }

        }

    }

    /**
     * Read the cover sets from the given file
     * 
     * @param filename the file containing the cover sets
     * @param featDef the feature definition for the features
     * @param basenames the list of basenames
     * @throws Exception
     */
    public void readCoverageBin(String filename,
            FeatureDefinition featDef,
            String[] basenames)throws Exception{
        this.featDef = featDef;
        phoneFeatIndex = 
            featDef.getFeatureIndex("mary_phoneme");        
        phoneClassesIndex = featDef.getFeatureIndex("mary_selection_next_phone_class");
        numPhoneClasses = featDef.getNumberOfValues(phoneClassesIndex);        
        diphoneFeatIndex = 
            featDef.getFeatureIndex("mary_next_phoneme");        
        prosodyIndex = featDef.getFeatureIndex("mary_selection_prosody");
        numPhoneValues = featDef.getNumberOfValues(phoneFeatIndex);
        numPhoneValuesMinusIgnored = numPhoneValues-phonesToIgnore.size()-1;
        numPossibleSimpleDiphones = numPhoneValuesMinusIgnored*(numPhoneValuesMinusIgnored+1);
        numPossibleClusteredDiphones = numPhoneClasses*numPhoneValuesMinusIgnored;

        possiblePhoneArray = featDef.getPossibleValues(phoneFeatIndex);
        possibleNextPhoneArray = featDef.getPossibleValues(diphoneFeatIndex);
        possibleNextPhoneClassArray = featDef.getPossibleValues(phoneClassesIndex);
        possibleProsodyArray = featDef.getPossibleValues(prosodyIndex);

        //initialise several variables
        numSelectedFeatVects = 0;
        numSentencesInCover = 0;
        maxSentLengthInCover = 0;
        minSentLengthInCover = 20;
        phoneCoverageInTime = new ArrayList();
        diphoneCoverageInTime = new ArrayList();
        overallCoverageInTime = new ArrayList();
        phonesInCover = new HashSet();
        simpleDiphonesInCover = new HashSet();
        clusteredDiphonesInCover = new HashSet();
        numSimpleFeatVectsInCover = 0;
        numClusteredFeatVectsInCover = 0;

        DataInputStream in =
            new DataInputStream(
                    new FileInputStream(
                            new File(filename)));
        /* read all the relevant information */
        numTokens = in.readInt();
        numSimpleDiphoneTypes = in.readInt();
        numClusteredDiphoneTypes = in.readInt();
        numSimpleFeatVectTypes = in.readInt();
        numClusteredFeatVectTypes = in.readInt();
        averageSentLength = in.readDouble();
        maxSentLength = in.readInt();
        minSentLength = in.readInt();
        numSimpleLeaves = in.readInt();
        numClusteredLeaves = in.readInt();
        possiblePhoneCoverage = in.readDouble();
        possibleSimpleDiphoneCoverage = in.readDouble();
        possibleClusteredDiphoneCoverage = in.readDouble();
        possibleOverallSimpleCoverage = in.readDouble();
        possibleOverallClusteredCoverage = in.readDouble();    
        numSentences = in.readInt();
        /* print the coverage tree */
        readTreeBin(in,true); 
        readTreeBin(in,false);
        in.close();
        System.out.print("Num Tokens: "+numTokens+"\n");
        if (holdVectorsInMemory && needToReadVectors){
            System.out.print("Reading feature vectors...");
            //read in the vectors
            int numSentences = basenames.length;
            trueNumSentences = numSentences;
            vectorArray = new byte[numSentences][];
            int tenPercent = numSentences/10; 
            for (int index=0;index<numSentences;index++){
                if ((index % tenPercent) == 0 && index!=0){
                    int percentage = index/tenPercent*10;
                    System.out.print(" "+percentage+"%");
                }
                FileInputStream fis;
                try{ fis =
                    new FileInputStream(new File(basenames[index]));
                } catch (FileNotFoundException fnfe){
                    System.out.println("Could not find file "+basenames[index]);
                    vectorArray[index] = null;
                    basenames[index] = null;
                    trueNumSentences--;
                    continue;
                }

                byte[] vlength = new byte[4];
                fis.read(vlength);

                int numFeatVects = (((vlength[0] & 0xff) << 24) 
                        | ((vlength[1] & 0xff) << 16) 
                        | ((vlength[2] & 0xff) << 8)
                        | (vlength[3] & 0xff));
                byte[] vectorBuf = new byte[4*numFeatVects];
                fis.read(vectorBuf);
                fis.close();

                vectorArray[index] = vectorBuf;
            }

        }
        System.out.println("True num sentences "+trueNumSentences);
    }


    /**
     * Read a cover tree
     * 
     * @param in the inputstream to read from
     * @param isSimpleCover if true, build the cover tree fro simpleDiphones
     * @throws Exception
     */
    private void readTreeBin(DataInputStream in, boolean isSimpleCover)throws Exception{
        byte numChildren = in.readByte();
        double wantedWeight = 0.0;
        CoverNode cover = 
            new CoverNode(numChildren,
                    wantedWeightDecrease,
                    wantedWeight);
        for (byte i=0;i<numChildren;i++){
            if (phonesToIgnore.contains(new Integer(i))) continue;
            byte nextNumChildren = in.readByte();

            CoverNode diphoneNode = 
                new CoverNode(nextNumChildren,
                        wantedWeightDecrease,
                        phoneLevelWeight);
            cover.addChild(diphoneNode,i);

            for (byte j=0;j<nextNumChildren;j++){
                byte nextNextNumChildren = in.readByte();
                CoverNode prosodyNode = 
                    new CoverNode(nextNextNumChildren,
                            wantedWeightDecrease,
                            diphoneLevelWeight);
                diphoneNode.addChild(prosodyNode,j);

                for (byte k=0;k<nextNextNumChildren;k++){     
                    int numVectors = in.readInt();
                    CoverLeaf leafNode =
                        new CoverLeaf(wantedWeightDecrease,
                                prosodyLevelWeight,
                                numVectors);
                    prosodyNode.addChild(leafNode,k);
                }

            }

        }
        computeRelativeFrequency(cover,numTokens);
        if (isSimpleCover){
            simpleCover = cover;
        } else {
            clusteredCover = cover;
        }
    }




    /**
     * A node in the cover tree
     * Represents a feature. 
     * Number of children is the number of possible values.
     * 
     * @author Anna Hunecke
     *
     */
    class CoverNode{

        /* children of this node */
        private CoverNode[] children;
        /* number of children of this node */
        private byte numChildren; 
        /* how much is this node and its children
         * wanted in the cover */
        protected double wantedWeight;
        /* frequency/inverted frequency of the node
         * in the corpus */
        protected double frequencyWeight;
        /* number by which the wantedWeight is divided */
        protected double wantedWeightDecrease;

        /**
         * Build a new CoverNode
         * Set frequency weight to 1
         */
        public CoverNode(){
            frequencyWeight = 1;
        }

        /**
         * Build a new CoverNode
         * 
         * @param numChildren the number of children
         * @param wantedWeightDecrease the value by which the wanted weight is divided
         * @param wantedWeight the wanted weight
         */
        public CoverNode(byte numChildren,
                double wantedWeightDecrease,
                double wantedWeight){
            this.numChildren = numChildren;
            children = new CoverNode[numChildren];
            this.wantedWeightDecrease = wantedWeightDecrease;
            this.wantedWeight = wantedWeight;
            frequencyWeight = 1;
        }




        /**
         * Build a new CoverNode
         * 
         * @param values the number of values
         * @param wantedWeightDecrease the wantedWeightDecrease
         */
        public CoverNode(byte values,
                double wantedWeightDecrease){
            children = new CoverNode[values];
            numChildren = (byte)children.length;
            frequencyWeight = 1;
            this.wantedWeightDecrease = wantedWeightDecrease;
        }

        /**
         * Add a new child 
         * 
         * @param child the child
         * @param value the position of the child in the children array
         */
        public void addChild(CoverNode child, byte value){
            children[value] = child;
        }

        /**
         * Get a child
         * 
         * @param value the position of the child in the children array
         * @return the child (null, if there is no child at this position)
         */
        public CoverNode getChild(byte value){
            return children[value];
        }

        /**
         * Get the number of children
         * 
         * @return the number of children
         */
        public byte getNumChildren(){
            return numChildren;
        }

        /**
         * Set the wantedWeight
         * 
         * @param wantedWeight the new wantedWeight
         */
        public void setWantedWeight(double wantedWeight){
            this.wantedWeight = wantedWeight;
        }

        /**
         * Get the wantedWeight
         * 
         * @return the wantedWeight
         */
        public double getWantedWeight(){
            return wantedWeight;
        }

        /**
         * Get the wantedWeightDecrease
         * 
         * @return the wantedWeightDecrease
         */
        public double getWantedWeightDecrease(){
            return wantedWeightDecrease;
        }

        /**
         * Decrease the wantedWeight
         * by dividing it by wantedWeightDecrease
         *
         */
        public void decreaseWantedWeight(){
            wantedWeight = wantedWeight/wantedWeightDecrease;
        }

        /**
         * Set the frequencyWeight
         * 
         * @param frequencyWeight the new frequencyWeight
         */
        public void setFrequencyWeight(double frequencyWeight){
            this.frequencyWeight = frequencyWeight;
        }

        /**
         * Get the frequencyWeight
         * 
         * @return the frequencyWeight
         */
        public double getFrequencyWeight(){
            return frequencyWeight;
        }



    }


    /**
     * A leaf in the cover tree.
     * Collects the feature vectors that
     * belong to the path that leads to the leaf.
     * 
     * @author Anna Hunecke
     *
     */
    class CoverLeaf extends CoverNode{

        /* the number of feature vectors in this node */
        private int numFeatVects;
        /* the maximimum number of feature vectors 
         * that could be in this node 
         * (according to the corpus)*/
        private int maxNumFeatVects;


        /**
         * Build a new cover leaf
         * 
         * @param wantedWeightDecrease the wantedWeightDecrease
         */
        public CoverLeaf(double wantedWeightDecrease){
            super();
            numFeatVects = 0;
            maxNumFeatVects = 0;
            this.wantedWeightDecrease = wantedWeightDecrease;
            frequencyWeight = 1;
        }

        /**
         * Build a new CoverLeaf
         * 
         * @param wantedWeightDecrease the wantedWeightDecrease
         * @param wantedWeight the wanted weight
         * @param maxNumFeatVects maximum number of feature vectors that
         *                        can be collected in this leaf
         */
        public CoverLeaf(double wantedWeightDecrease,
                double wantedWeight,
                int maxNumFeatVects){            
            this.wantedWeightDecrease = wantedWeightDecrease;
            this.wantedWeight = wantedWeight;
            this.maxNumFeatVects = maxNumFeatVects;
            frequencyWeight = 1;
        }

        /**
         * Add a new feature vector
         * 
         * @param featureVector the new feature vector
         */
        public void addFeatureVector(){
            numFeatVects++;
        }

        /**
         * Increase the maximum number of feature vectors by one
         * (because we have seen a feature vector for this node 
         * in the corpus)
         */
        public void addPossibleInstance(){
            maxNumFeatVects++;
        }

        /**
         * Get the number of feature vectors of this node
         * 
         * @return the number of feature vectors
         */
        public int getNumFeatureVectors(){
            return numFeatVects;
        }

        /**
         * Get the maximum number of feature vectors
         * of this node
         * 
         * @return the maximum number of feature vectors 
         */
        public int maxNumFeatVects(){
            return maxNumFeatVects;
        }        
    }
}
