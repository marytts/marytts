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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mary.unitselection.FeatureFileIndexer;
import de.dfki.lt.mary.unitselection.MaryNode;
import de.dfki.lt.mary.unitselection.cart.CARTWagonFormat;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.MCepTimelineReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.MCepDatagram;

import de.dfki.lt.mary.MaryProperties;

public class CARTBuilder implements VoiceImportComponent {
    
    private DatabaseLayout databaseLayout;
    
    public CARTBuilder(DatabaseLayout databaseLayout){
        this.databaseLayout = databaseLayout;
    }
    
    //for testing
    public static void main(String[] args) throws Exception
    {
        //build database layout
        DatabaseLayout db = new DatabaseLayout();
        //build CARTBuilder and run compute
        CARTBuilder cartBuilder = new CARTBuilder(db);
        cartBuilder.compute();
    }
    
     public boolean compute() throws Exception{
         long time = System.currentTimeMillis();
         //read in the features with feature file indexer
         System.out.println("Reading feature file ...");
         String featureFile = databaseLayout.targetFeaturesFileName();
         FeatureFileIndexer ffi = new FeatureFileIndexer(featureFile);
         System.out.println(" ... done!");
        
         FeatureDefinition featureDefinition = ffi.getFeatureDefinition();
         
         //read in the feature sequence
         //open the file
         System.out.println("Reading feature sequence ...");
         String featSeqFile = databaseLayout.featSequenceFileName();
         BufferedReader buf = new BufferedReader(
                 new FileReader(new File(featSeqFile)));
         //each line contains one feature
         String line = buf.readLine();
         //collect features in a list
         List features = new ArrayList();
         while (line != null){
             // Skip empty lines and lines starting with #:
             if (!(line.trim().equals("") || line.startsWith("#"))){
                 features.add(line.trim());
             }
             line = buf.readLine();
         }
         //convert list to int array
         int[] featureSequence = new int[features.size()];
         for (int i=0;i<features.size();i++){
             featureSequence[i] = 
                 featureDefinition.getFeatureIndex((String)features.get(i));
         }
       System.out.println(" ... done!"); 

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
         System.out.println(" ... done!");
        
         //TODO: Write a dump method for the featureVectors; import and dump distance tables
         replaceLeaves(topLevelCART,featureDefinition);
         
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
                            FeatureDefinition featDef)
    throws IOException
    {
        try{
            //open CART-File
            System.out.println("Reading CART from "+filename+" ...");
            //build and return CART
            CARTWagonFormat cart = new CARTWagonFormat();
            cart.load(filename,featDef);
            //cart.toStandardOut();
            System.out.println(" ... done!");
            return cart;
        } catch (IOException ioe){
            IOException newIOE = new IOException("Error reading CART");
            newIOE.initCause(ioe);
            throw newIOE;
        }
    }
       
    /**
     * Dump the CARTs in the cart map
     * to destinationDir/CARTS.bin
     * 
     * @param destDir the destination directory
     */
    public void dumpCART(String destFile,
                        CARTWagonFormat cart)
    throws IOException
    {
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
            				FeatureDefinition featureDefinition)
    throws IOException
    {
        try {
            //create wagon dir if it does not exist
            File wagonDir = new File(databaseLayout.wagonDirName());
            if (!wagonDir.exists()){
                wagonDir.mkdir();
            }
            //get the filenames for the various files used by wagon
            String wagonDirName = databaseLayout.wagonDirName();
            String featureDefFile = wagonDirName + "/" 
                                + databaseLayout.wagonDescFile();
            String featureVectorsFile = databaseLayout.wagonFeatsFile();
            String cartFile = databaseLayout.wagonCartFile();
            String distanceTableFile = databaseLayout.wagonDistTabsFile();
            //dump the feature definitions
            PrintWriter out = new PrintWriter(new 
                			FileOutputStream(new 
                			        File(featureDefFile)));
            featureDefinition.generateAllDotDesc(out);
            out.close();

            //build new WagonCaller
            WagonCaller wagonCaller = new WagonCaller(featureDefFile);
           
            int numProcesses = 1;
            String np = MaryProperties.getProperty("numProcesses");
            if (np != null){
                numProcesses = Integer.parseInt(np);
            }
            if (numProcesses > 1){
                /* run several Wagon calls in parallel */
                Process[] wagonProcesses = new Process[numProcesses];
                CARTWagonFormat.LeafNode[] leaves = new CARTWagonFormat.LeafNode[numProcesses];
                int index = 0;
                //go through the CART
                FeatureVector[] featureVectors = 
                    cart.getNextFeatureVectors();
                while (featureVectors != null){
                    //reset index if out of array range
                    if (index == wagonProcesses.length){
                        index =0;
                    }
                    //determine if there is a process running
                    if (!(wagonProcesses[index] == null)){
                        //we already have a Process
                        //determine the state of the Process
                        Process p = wagonProcesses[index];
                        try {
                            p.exitValue();
                            //the process has finished,
                            //read in the resulting CART
                            BufferedReader buf = new BufferedReader(
                                new FileReader(new File(wagonDirName+"/"+index+"_"+cartFile)));
                            CARTWagonFormat newCART = 
                                new CARTWagonFormat(buf,featureDefinition);
                            //replace the leaf by the CART
                            cart.replaceLeafByCart(newCART,leaves[index]);
                            //now we can start a new process in this slot
                        } catch (IllegalThreadStateException e){
                            //the process has not finished, try the next array slot
                            index++;
                            continue;
                        }
                    } 
                    String filePrefix = wagonDirName+"/"+index+"_";
                    //dump the feature vectors
                    dumpFeatureVectors(featureVectors, featureDefinition,filePrefix+featureVectorsFile);
                    //dump the distance tables
                    buildAndDumpDistanceTables(featureVectors,filePrefix+distanceTableFile);
                    //call Wagon and store the process
                    wagonProcesses[index] = wagonCaller.callWagon(filePrefix+featureVectorsFile,filePrefix+distanceTableFile,filePrefix+cartFile);
                    //store the leaf we want to replace
                    leaves[index] = cart.getNextLeafToReplace();
                    //get the next featureVectors
                    featureVectors = 
                        cart.getNextFeatureVectors();
                    index++;
                } 
            } else {
                /* call Wagon successively */
                //go through the CART
                FeatureVector[] featureVectors = 
                    cart.getNextFeatureVectors();
                while (featureVectors != null){
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
            }
              
        } catch (IOException ioe) {
            IOException newIOE = new IOException("Error replacing leaves");
            newIOE.initCause(ioe);
            throw newIOE;
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
                BufferedOutputStream(new 
                        FileOutputStream(filename)));
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
    public void buildAndDumpDistanceTables (FeatureVector[] featureVectors, String filename) throws FileNotFoundException {
        
        System.out.println( "Computing the distance matrix for file[" + filename + "]...");
        
        /* Dereference the number of units once and for all */
        int numUnits = featureVectors.length;
        /* Load the MelCep timeline and the unit file */
        MCepTimelineReader tlr = null;
        try {
            tlr = new MCepTimelineReader( databaseLayout.melcepTimelineFileName() );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Failed to read the Mel-Cepstrum timeline [" + databaseLayout.melcepTimelineFileName()
                    + "] due to the following IOException: ", e );
        }
        UnitFileReader ufr = null;
        try {
            ufr = new UnitFileReader( databaseLayout.unitFileName() );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Failed to read the unit file [" + databaseLayout.unitFileName()
                    + "] due to the following IOException: ", e );
        }
        /* Read the Mel Cepstra for each unit, and cumulate
         * their sufficient statistics in the same loop */
        double[][][] melCep = new double[numUnits][][];
        double val = 0;
        double[] sum = new double[tlr.getOrder()];
        double[] sumSq = new double[tlr.getOrder()];
        double[] sigma2 = new double[tlr.getOrder()];
        double N = 0.0;
        for ( int i = 0; i < numUnits; i++ ) {
            /* Read the datagrams for the current unit */
            MCepDatagram[] dat = null;
            try {
                dat = (MCepDatagram[]) tlr.getDatagrams( ufr.getUnit(featureVectors[i].getUnitIndex()), ufr.getSampleRate() );
            }
            catch ( IOException e ) {
                throw new RuntimeException( "Failed to read the datagrams for unit number [" + featureVectors[i].getUnitIndex()
                        + "] from the Mel-cepstrum timeline due to the following IOException: ", e );
            }
            N += (double)(dat.length); // Update the frame counter
            for ( int j = 0; j < dat.length; j++ ) {
                melCep[i][j] = dat[j].getCoeffsAsDouble();
                /* Cumulate the sufficient statistics */
                for ( int k = 0; k < tlr.getOrder(); k++ ) {
                    val = melCep[i][j][k];
                    sum[k] += val;
                    sumSq[k] += (val*val);
                }
            }
        }
        /* Finalize the variance calculation */
        for ( int k = 0; k < tlr.getOrder(); k++ ) {
            val = sum[k];
            sigma2[k] = ( sumSq[k] - (val*val)/N ) / N;
        }
        /* Compute the unit distance matrix */
        double[][] dist = new double[numUnits][featureVectors.length];
        for ( int i = 0; i < numUnits; i++ ) {
            dist[i][i] = 0.0; // <= Set the diagonal to 0.0
            for ( int j = 1; j < numUnits; j++ ) {
                /* Get the DTW distance between the two sequences: */
                dist[i][j] = dist[j][i] = dtwDist( melCep[i], melCep[j], sigma2 );
            } 
        }
        /* Write the matrix to disk */
        System.out.print( "Writing the distance matrix to file[" + filename + "]...");
        PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(filename)));
        for ( int i = 0; i < numUnits; i++ ) {
            for ( int j = 0; j < numUnits; j++ ) {
                out.print( (float)(dist[i][j]) + " " );
            }
            out.print("\n");
        }
        out.flush();
        out.close();
        
        System.out.println( "Done.");
    }
    
    
    /**
     * Computes an average Mahalanobis distance along the optimal DTW path
     * between two vector sequences.
     * 
     * The DTW constraint used here is:
     * D(i,j) = min {
     * D(i-2,j-1) + 2*d(i-1,j) + d(i,j) ;
     * D(i-1,j-1) + 2*d(i,j) ;
     * D(i-1,j-2) + 2*d(i,j-1) + d(i,j)
     * }
     * 
     * At the end of the DTW, the cumulated distance is normalized by the number
     * of local distances cumulated along the optimal path. Hence, the resulting
     * unit distance is homogeneous to an average having the order of magnitude
     * of a single Mahalanobis distance, and that for each pair of units.
     * 
     * @param seq1 The first sequence of (Mel-cepstrum) vectors.
     * @param seq2 The second sequence of (Mel-cepstrum) vectors.
     * @param sigma2 The variance of the vectors.
     * @return The average Mahalanobis distance along the optimal DTW path.
     */
    double dtwDist( double[][] seq1, double[][] seq2, double[] sigma2 ) {
        double[][] d = new double[seq1.length][seq2.length];
        double[][] D = new double[seq1.length][seq2.length];
        int[][] Nd = new int[seq1.length][seq2.length]; // <= Number of cumulated distances, for the final averaging
        double[] minV = new double[3];
        int[] minNd = new int[3];
        int minIdx = 0;
        /* Fill the local distance matrix */
        for ( int i = 0; i < seq1.length; i++ ) {
            for ( int j = 0; j < seq2.length; j++ ) {
                d[i][j] = mahalanobis( seq1[i],   seq2[j],   sigma2 );
            }
        }
        /* Compute the optimal DTW distance: */
        /* - 1st row/column: */
        D[0][0] = 2*d[0][0];
        for ( int i = 1; i < seq1.length; i++ ) {
            D[i][0] = d[i][0];
            D[0][i] = d[0][i];
            Nd[0][i] = Nd[i][0] = 1;
        }
        /* - 2nd row/column: */
        /* corner: i==1, j==1 */
        minV[0] = 2*d[0][1] + d[1][1];  minNd[0] = 3;
        minV[1] = D[0][0] + 2*d[1][1];  minNd[1] = Nd[0][0] + 2;
        minV[2] = 2*d[1][0] + d[1][1];  minNd[2] = 3;
        minIdx = minV[0] < minV[1] ? 0 : 1;
        minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
        D[1][1] = minV[minIdx];
        Nd[1][1] = minNd[minIdx];
        /* 2nd row: j==1 ; 2nd col: i==1 */
        for ( int i = 2; i < seq1.length; i++ ) {
            /* Row: */
            minV[0] = D[i-2][0] + 2*d[i-1][1] + d[i][1];  minNd[0] = Nd[i-2][0] + 3;
            minV[1] = D[i-1][0] + 2*d[i][1];              minNd[1] = Nd[i-1][0] + 2;
            minV[2] = 2*d[i][0] + d[i][1];                minNd[2] = 3;
            minIdx = minV[0] < minV[1] ? 0 : 1;
            minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
            D[i][1] = minV[minIdx];
            Nd[i][1] = minNd[minIdx];
            /* Column: */
            minV[0] = 2*d[0][i] + d[1][i];                minNd[0] = 3;
            minV[1] = D[0][i-1] + 2*d[1][i];              minNd[1] = Nd[0][i-1] + 2;
            minV[2] = D[0][i-2] + 2*d[1][i-1] + d[1][i];  minNd[2] = Nd[0][i-2] + 3;
            minIdx = minV[0] < minV[1] ? 0 : 1;
            minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
            D[1][i] = minV[minIdx];
            Nd[1][i] = minNd[minIdx];
        }
        /* - Rest of the matrix: */
        for ( int i = 2; i < seq1.length; i++ ) {
            for ( int j = 2; j < seq2.length; j++ ) {
                minV[0] = D[i-2][j-1] + 2*d[i-1][j] + d[i][j];  minNd[0] = Nd[i-2][j-1] + 3;
                minV[1] = D[i-1][j-1] + 2*d[i][j];              minNd[1] = Nd[i-1][j-1] + 2;
                minV[2] = D[i-1][j-2] + 2*d[i][j-1] + d[i][j];  minNd[0] = Nd[i-1][j-2] + 3;
                minIdx = minV[0] < minV[1] ? 0 : 1;
                minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
                D[i][j] = minV[minIdx];
                Nd[i][j] = minNd[minIdx];
            }
        }
        /* Return */
        return( D[seq1.length-1][seq2.length-1] / (double)(Nd[seq1.length-1][seq2.length-1]) );
    }
    
    /**
     * Mahalanobis distance between two feature vectors.
     * 
     * @param v1 A feature vector.
     * @param v2 Another feature vector.
     * @param sigma2 The variance of the distribution of the considered feature vectors.
     * @return The mahalanobis distance between v1 and v2.
     */
    double mahalanobis( double[] v1, double[] v2, double[] sigma2 ) {
        double sum = 0.0;
        double diff = 0.0;
        for ( int i = 0; i < v1.length; i++ ) {
            diff = v1[i] - v2[i];
            sum += ( (diff*diff) / sigma2[i] );
        }
        return( sum );
    }
    
}
