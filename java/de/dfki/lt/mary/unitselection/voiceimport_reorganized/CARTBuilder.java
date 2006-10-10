/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.*;

import de.dfki.lt.mary.unitselection.FeatureFileIndexer;
import de.dfki.lt.mary.unitselection.MaryNode;
import de.dfki.lt.mary.unitselection.cart.CARTWagonFormat;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

public class CARTBuilder implements VoiceImportComponent {
    
    private DatabaseLayout databaseLayout;
    
    private String testFeatureFile = "";
    private String testDestinationFile = "/home/cl-home/hunecke/anna/openmary/CART.bin";
    
    public CARTBuilder(DatabaseLayout databaseLayout){
        this.databaseLayout = databaseLayout;
    }
    
    //for testing
    public static void main(String[] args){
        //build database layout
        DatabaseLayout db = new DatabaseLayout();
        //build CARTBuilder and run compute
        CARTBuilder cartBuilder = new CARTBuilder(db);
        try {
            cartBuilder.compute();
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Something went wrong");
        }
    }
    
     public boolean compute() throws Exception{
         long time = System.currentTimeMillis();
         //read in the features with feature file indexer
         System.out.println("Reading feature file ...");
         String featureFile = databaseLayout.targetFeaturesFileName();
         FeatureFileIndexer ffi = new FeatureFileIndexer(featureFile);
         System.out.println(" ... done!");
         //TODO: find a way to define the feature sequence
         FeatureDefinition featureDefinition = ffi.getFeatureDefinition();
         int[] featureSequence = new int[3];
         featureSequence[0] = featureDefinition.getFeatureIndex("mary_ph_vc");
         featureSequence[1] = featureDefinition.getFeatureIndex("mary_next_is_pause"); 
         featureSequence[2] = featureDefinition.getFeatureIndex("mary_stressed"); 
         //sort the features according to feature sequence
         System.out.println("Sorting features ...");
         ffi.deepSort(featureSequence);
         System.out.println(" ... done!");
         //get the resulting tree
         MaryNode topLevelTree = ffi.getTree();
         //topLevelTree.toStandardOut(ffi);
         
         //convert the top-level CART to Wagon Format
         System.out.println("Building CART from tree ...");
         CARTWagonFormat topLevelCART = new CARTWagonFormat(topLevelTree,ffi);
         System.out.println(" ... CART has "+topLevelCART.getNumNodes()+" nodes ...");
         System.out.println(" ... done!");
        
         //TODO: Write a dump method for the featureVectors; import and dump distance tables
         //replaceLeaves(topLevelCART,featureDefinition);
         //dump big CART to binary file
         String destinationFile = databaseLayout.cartFileName();
         dumpCART(destinationFile,topLevelCART);
         //say how long you took
         long timeDiff = System.currentTimeMillis() - time;
         System.out.println("Processing took "+timeDiff+" milliseconds.");
         
         //importCART(destinationFile,featureDefinition);
         return true;
     }
    
     
     /**
     * Read in the CARTs from festival/trees/ directory,
     * and store them in a CARTMap
     * 
     * @param festvoxDirectory the festvox directory of a voice
     */
    public CARTWagonFormat importCART(String filename,
                            FeatureDefinition featDef){
        try{
            //open CART-File
            System.out.println("Reading CART from "+filename+" ...");
            //build and return CART
            CARTWagonFormat cart = new CARTWagonFormat();
            cart.load(filename,featDef);
            //cart.toStandardOut();
            System.out.println(" ... done!");
            return cart;
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error reading CARTS");
        }
    }
       
    /**
     * Dump the CARTs in the cart map
     * to destinationDir/CARTS.bin
     * 
     * @param destDir the destination directory
     */
    public void dumpCART(String destFile,
                        CARTWagonFormat cart){
           try {
                System.out.println("Dumping CART to "+destFile+" ...");
        
                //Open the destination file (cart.bin) and output the header
                DataOutputStream out = new DataOutputStream(new
                        BufferedOutputStream(new 
                        FileOutputStream(destFile)));
                //create new CART-header and write it to output file
                MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
                hdr.write(out);
        
                //write number of nodes
                out.writeInt(cart.getNumNodes());
                String name = "";
                //dump name and CART
                out.writeUTF(name);
                //dump CART
                cart.dumpBinary(out);
              
                //finish
                out.close();
                System.out.println(" ... done\n");
            } catch (IOException e){
                    //e.printStackTrace();
                    throw new Error("Error dumping CARTS");
            }    
    }     
    
    /**
     * For each leaf in the CART, 
     * run Wagon on the feature vectors in this CART,
     * and replace leaf by resulting CART
     *  
     * @param topLevelCART the CART
     * @param featureDefinition the definition of the features
     */
    public void replaceLeaves(CARTWagonFormat cart,
            				FeatureDefinition featureDefinition){
        try {
            //TODO: enter meaningful file names; probably get them from DatabaseLayout 
            String featureDefFile = "enter Definition here";
            String featureVectorsFile = "enter Definition here";
            String cartFile = "enter Definition here";
            String distanceTableFile = "enter Definition here";
            //dump the feature definitions
            PrintWriter out = new PrintWriter(new 
                			FileOutputStream(new 
                			        File(featureDefFile)));
            featureDefinition.generateAllDotDesc(out);
            out.close();

            //build new WagonCaller
            WagonCaller wagonCaller = new WagonCaller(featureDefFile);
            
            //go through the CART
            FeatureVector[] featureVectors = 
                cart.getNextFeatureVectors();
            int index = 1;
            while (featureVectors != null){
                index++;
                //dump the feature vectors
                dumpFeatureVectors(featureVectors, featureDefinition,featureVectorsFile);
                //dump the distance tables
                buildAndDumpDistanceTables(featureVectors,distanceTableFile);
                //call Wagon
                wagonCaller.callWagon(featureVectorsFile,distanceTableFile,cartFile);
                //read in the resulting CART
                BufferedReader buf = new BufferedReader(
                        new FileReader(new File(cartFile)));
                CARTWagonFormat newCART = 
                    new CARTWagonFormat(buf,featureDefinition);
                //replace the leaf by the CART
                cart.replaceLeafByCart(newCART);
                //get the next featureVectors
                featureVectors = 
                    cart.getNextFeatureVectors();
            }
            
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error replacing leaves");
        }
    }
    
    /**
     * Dump the given feature vectors to a file with the given filename
     * @param featureVectors the feature vectors
     * @param featDef the feature definition
     * @param filename the filename
     */
    public void dumpFeatureVectors(FeatureVector[] featureVectors,
            					FeatureDefinition featDef,
            					String filename) throws FileNotFoundException{
        //open file 
        PrintWriter out = new PrintWriter(new 
    			FileOutputStream(new 
    			        File(filename)));
        //get basic feature info
        int numByteFeats = featDef.getNumberOfByteFeatures();
        int numShortFeats = featDef.getNumberOfShortFeatures();
        int numFloatFeats = featDef.getNumberOfContinuousFeatures();
        //loop through the feature vectors
        for (int i=0; i<featureVectors.length;i++){
            //get the next feature vector
            FeatureVector nextFV = featureVectors[i];
            //dump unit index
            out.print(nextFV.getUnitIndex()+" ");
            //dump the byte features
            for (int j=0; j<numByteFeats;j++){
                int feat = nextFV.getFeatureAsInt(j);
                out.print(featDef.getFeatureValueAsString(j,feat)+" ");
            }
            //dump the short features
            for (int j=0; j<numShortFeats;j++){
                int feat = nextFV.getFeatureAsInt(j);
                out.print(featDef.getFeatureValueAsString(j,feat)+" ");
            }
            //dump the float features
            for (int j=0; j<numFloatFeats;j++){
                out.print(nextFV.getContinuousFeature(j)+" ");
            }
            //print a newline if this is not the last vector
            if (i+1 != featureVectors.length){
                out.print("\n");
            }
        }
        //dump and close
        out.flush();
        out.close();
    }
    
    /**
     * Build the distance tables for the units 
     * from which we have the feature vectors
     * and dump them to a file with the given filename
     * @param featureVectors the feature vectors of the units
     * @param filename the filename
     */
    public void buildAndDumpDistanceTables(FeatureVector[] featureVectors,
							String filename){
    }
    
    
}
