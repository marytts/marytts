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
         
         //read in the features with feature file indexer
         System.out.println("Reading feature file ...");
         String featureFile = databaseLayout.targetFeaturesFileName();
         FeatureFileIndexer ffi = new FeatureFileIndexer(featureFile);
         System.out.println(" ... done!");
         //TODO: find a way to define the feature sequence
         FeatureDefinition featureDefinition = ffi.getFeatureDefinition();
         int[] featureSequence = new int[3];
         featureSequence[0] = featureDefinition.getFeatureIndex("mary_phoneme");
         featureSequence[1] = featureDefinition.getFeatureIndex("mary_next_is_pause"); 
         featureSequence[2] = featureDefinition.getFeatureIndex("mary_stressed"); 
         
         //sort the features according to feature sequence
         System.out.println("Sorting features ...");
         ffi.deepSort(featureSequence);
         System.out.println(" ... done!");
         //get the resulting tree
         MaryNode topLevelTree = ffi.getTree();
         
         //convert the top-level CART to Wagon Format
         System.out.println("Building CART from tree ...");
         CARTWagonFormat topLevelCART = new CARTWagonFormat(topLevelTree,ffi);
         System.out.println(" ... done!");
         //TODO: For each leaf of the top-level CART, call Wagon and replace leaf by new Wagon CART
         
         //dump big CART to binary file
         String destinationFile = databaseLayout.cartFileName();
         dumpCART(destinationFile,topLevelCART);
         
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
            System.out.println("Reading CART from "+filename);
            File cartFile = new File(filename);
            BufferedReader reader =
                        new BufferedReader(new 
                                InputStreamReader(new FileInputStream(cartFile)));
            //build and return CART
            return new CARTWagonFormat(reader,featDef);
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
                System.out.println("Dump CART to "+destFile);
        
                //Open the destination file (cart.bin) and output the header
                DataOutputStream out = new DataOutputStream(new
                        BufferedOutputStream(new 
                        FileOutputStream(destFile)));
                //create new CART-header and write it to output file
                MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
                hdr.write(out);
        
                //write number of CARTs
                out.writeInt(1);
                String name = "";
                //dump name and CART
                out.writeUTF(name);
                cart.dumpBinary(out);
                //for debugging
                cart.toStandardOut();
              
                //finish
                out.close();
                System.out.println("Done\n");
            } catch (IOException e){
                    e.printStackTrace();
                    throw new Error("Error dumping CARTS");
            }    
    }     
    
    
}
