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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.*;
import java.util.*;

import de.dfki.lt.mary.unitselection.MaryNode;
import de.dfki.lt.mary.unitselection.cart.*;
import de.dfki.lt.mary.unitselection.cart.LeafNode.FeatureVectorLeafNode;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.FeatureArrayIndexer;
import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.MCepTimelineReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.MCepDatagram;

public class CARTBuilder extends VoiceImportComponent {
    
    private MCepTimelineReader mcepTimeline;
    private UnitFileReader unitFile;
    private String wagonDirName;
    private String wagonDescFile;
    private String wagonFeatsFile;
    private String wagonCartFile;
    private String wagonDisTabsFile;
    private DatabaseLayout db;
    private int percent = 0;
    public final String ACFEATUREFILE = "CARTBuilder.acFeatureFile";
    public final String FEATURESEQFILE = "CARTBuilder.featureSeqFile";
    public final String TOPLEVELTREEFILE = "CARTBuilder.topLevelTreeFile";
    public final String CARTFILE = "CARTBuilder.cartFile";
    
    public final String MCEPTIMELINE = "CARTBuilder.mcepTimeline";
    public final String UNITFILE = "CARTBuilder.unitFile";
    public final String READFEATURESEQUENCE = "CARTBuilder.readFeatureSequence";
    public final String MAXLEAFSIZE = "CARTBuilder.maxLeafSize";
    public final String ESTDIR = "CARTBuilder.estDir";
    
    
    public String getName(){
        return "CARTBuilder";
    }
    
     public void initialiseComp()
    {       
         wagonDirName = db.getProp(db.TEMPDIR);
         wagonDescFile = wagonDirName+"wagon.desc";
         wagonFeatsFile = wagonDirName+"wagon.feats";
         wagonCartFile = wagonDirName+"wagon.cart";
         wagonDisTabsFile = wagonDirName+"wagon.distabs";
        //make sure that we have at least a feature sequence file
        File featSeqFile = new File(getProp(FEATURESEQFILE));
        if (!featSeqFile.exists()){
            File topLevelTreeFile = new File(getProp(TOPLEVELTREEFILE));
            if (!topLevelTreeFile.exists()){
                try{
                    PrintWriter featSeqOut =
                        new PrintWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(featSeqFile),"UTF-8"));
                    featSeqOut.println("# Automatically generated feature sequence file for CARTBuilder\n"
                            +"# Add features to refine\n"
                            +"# Defines the feature sequence used to build the top-level CART\n"
                            +"mary_phoneme\nmary_stressed\nmary_next_phoneme");
                    featSeqOut.flush();
                    featSeqOut.close();
                } catch (Exception e){
                    System.out.println("Warning: no feature sequence file "
                            +getProp(FEATURESEQFILE)
                            +" and no top level tree file "
                            +getProp(TOPLEVELTREEFILE)
                            +"; CARTBuilder will not run.");
                }
            }
        }
    }
    
     public SortedMap getDefaultProps(DatabaseLayout db){
         this.db = db;
       if (props == null){
           props = new TreeMap();
           String filedir = db.getProp(db.FILEDIR);
           String maryext = db.getProp(db.MARYEXT);
           props.put(ACFEATUREFILE,filedir
                        +"halfphoneFeatures_ac"+maryext);
           props.put(FEATURESEQFILE, db.getProp(db.CONFIGDIR)
                        +"featureSequence.txt");
           props.put(TOPLEVELTREEFILE, db.getProp(db.CONFIGDIR)
                        +"topLevel.tree");
           props.put(CARTFILE, filedir
                        +"cart"+maryext);
           
           props.put(MCEPTIMELINE, filedir
                        +"timeline_mcep"+maryext);
           props.put(UNITFILE,filedir
                        +"halfphoneUnits"+maryext);
           props.put(READFEATURESEQUENCE,"true");
           props.put(MAXLEAFSIZE,"3300");
           String estdir = System.getProperty("ESTDIR");
           if ( estdir == null ) {
               estdir = "/project/mary/Festival/speech_tools/";
           }
           props.put(ESTDIR,estdir);
       }
       
       return props;
   }
     
     
     protected void setupHelp(){
         props2Help = new TreeMap();
         props2Help.put(ACFEATUREFILE,"file containing all halfphone units and their target cost features"
                 +" plus the acoustic target cost features");
         props2Help.put(FEATURESEQFILE, "file containing the feature sequence for the basic tree");
         props2Help.put(TOPLEVELTREEFILE,"file containing the basic tree");
         props2Help.put(CARTFILE, "file containing the preselection CART. Will be created by this module");
         props2Help.put(MCEPTIMELINE,"file containing the mcep files");
         props2Help.put(UNITFILE,"file containing all halfphone units");
         props2Help.put(READFEATURESEQUENCE,"if \"true\", basic tree is read from feature sequence file;"
                 +" if \"false\", basic tree is read from top level tree file.");
         props2Help.put(MAXLEAFSIZE,"the maximum number of units in a leaf of the basic tree");
         props2Help.put(ESTDIR,"directory containing the local installation of the Edinburgh Speech Tools");
     }
     
     public boolean compute() throws Exception{
         long time = System.currentTimeMillis();
         //read in the features with feature file indexer
         System.out.println("Reading feature file ...");
         String featureFile = getProp(ACFEATUREFILE);
         FeatureFileReader ffr = FeatureFileReader.getFeatureFileReader(featureFile);
         FeatureVector[] featureVectorsCopy = ffr.getCopyOfFeatureVectors();
         FeatureDefinition featureDefinition = ffr.getFeatureDefinition(); 
         //remove the feature vectors of edge units
         List fVList = new ArrayList();
         int edgeIndex = 
             featureDefinition.getFeatureIndex(FeatureDefinition.EDGEFEATURE);
         for (int i=0;i<featureVectorsCopy.length;i++){
             FeatureVector nextFV = featureVectorsCopy[i];
             if (!nextFV.isEdgeVector(edgeIndex)) fVList.add(nextFV);
         }
         int fVListSize = fVList.size();
         int removed = featureVectorsCopy.length - fVListSize;
         System.out.println("Removed "+removed+" edge vectors; "
                 +"remaining vectors : "+fVListSize);
         FeatureVector[] featureVectors = new FeatureVector[fVListSize];
         for (int i=0;i<featureVectors.length;i++){
             featureVectors[i] = (FeatureVector) fVList.get(i);
         }
         CART topLevelCART;
         boolean fromFeatureSequence = 
             Boolean.valueOf(getProp(READFEATURESEQUENCE)).booleanValue();
         if (fromFeatureSequence){
             /* Build the top level tree from a feature sequence */
             FeatureArrayIndexer fai = new FeatureArrayIndexer(featureVectors, featureDefinition);
             System.out.println(" ... done!");         
             //read in the feature sequence
             //open the file
             System.out.println("Reading feature sequence ...");
             String featSeqFile = getProp(FEATURESEQFILE);
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
             fai.deepSort(featureSequence);
             System.out.println(" ... done!");
             //get the resulting tree
             MaryNode topLevelTree = fai.getTree();
             
             //convert the top-level CART to Wagon Format
             System.out.println("Building CART from tree ...");
             topLevelCART = new FeatureVectorCART(topLevelTree, fai);
             PrintWriter pw = 
                 new PrintWriter(
                         new FileWriter(
                                 new File("./test.txt")));
             topLevelCART.toTextOut(pw);
             System.out.println(" ... done!");
             
         }else {
             /* read in the top-level tree from file */
             String filename = getProp(TOPLEVELTREEFILE);
             System.out.println("Reading empty top-level tree from file "+filename+" ...");
             BufferedReader reader = 
                 new BufferedReader(
                         new InputStreamReader(
                                 new FileInputStream(
                                         new File(filename)),"UTF-8"));
             topLevelCART = new TopLevelTree(reader, featureDefinition);
             System.out.println(" ... done!");
             
             //fill in the leafs of the tree
             System.out.println("Filling leafs of top-level tree ...");
             ((TopLevelTree)topLevelCART).fillLeafs(featureVectors);
             System.out.println(" ... done!");
         }
         
         System.out.println("Checking top-level CART for reasonable leaf sizes ...");
         int minSize = 5;
         int maxSize = Integer.parseInt(getProp(MAXLEAFSIZE));
         int nTooSmall = 0;
         int nTooBig = 0;
         int nLeaves = 0;
         for (LeafNode leaf = topLevelCART.getFirstLeafNode(); leaf != null; leaf = leaf.getNextLeafNode()) {
             if (leaf.getNumberOfData() < minSize) {
                 // Ignore a few meaningless combinations:
                 String path = leaf.getDecisionPath();
                 if (path.indexOf("phoneme==0") == -1
                         && path.indexOf("vc==0") == -1
                         && !(path.indexOf("prev_vc==+") != -1 && path.indexOf("prev_c") != -1)
                         && !(path.indexOf("prev_vc==-") != -1 && path.indexOf("prev_vheight") != -1)
                 ) {
                    
                     //System.out.println("leaf too small: "+leaf.getDecisionPath());
                     nTooSmall++;
                     }
       
             } else if (leaf.getNumberOfData() > maxSize) {
                 System.out.println("               LEAF TOO BIG: "+leaf.getDecisionPath());
                 nTooBig++;
             }
             nLeaves++;
         }
         if (nTooSmall > 0 || nTooBig > 0) {
             System.out.println("Bad top-level cart: "+nTooSmall+"/"+nLeaves+" leaves are too small, "+nTooBig+"/"+nLeaves+" are too big");
             System.out.println("Cutting down the big leaves to size "+maxSize);
             for (LeafNode leaf = topLevelCART.getFirstLeafNode(); leaf != null; leaf = leaf.getNextLeafNode()) {
                 if (leaf.getNumberOfData() > maxSize) {
                     FeatureVectorLeafNode fvleaf = (FeatureVectorLeafNode)leaf;
                     FeatureVector[] fv = fvleaf.getFeatureVectors();
                     FeatureVector[] newfv = new FeatureVector[maxSize];
                     System.arraycopy(fv, 0, newfv, 0, maxSize);
                     fvleaf.setFeatureVectors(newfv);
                 }
             }
             
         } else {
             System.out.println("... OK!");
         }
         
         boolean callWagon = System.getProperty("db.cartbuilder.callwagon", "true").equals("true");
         
         if (callWagon) {
             boolean ok = replaceLeaves(topLevelCART,featureDefinition);
             if(!ok) {
                 System.out.println("Could not replace leaves");
                 return false;
             }
         }
         
         //dump big CART to binary file
         String destinationFile = getProp(CARTFILE);
         dumpCART(destinationFile,topLevelCART);
         
         /**
          //Dump the resulting Cart to text file
           PrintWriter pw = 
           new PrintWriter(
           new FileWriter(
           new File("./mary_files/cartTextDump.txt")));
           topLevelCART.toTextOut(pw);**/
         //say how long you took
         long timeDiff = System.currentTimeMillis() - time;
         System.out.println("Processing took "+timeDiff+" milliseconds.");
         
         return true;
     }
    
     
     /**
     * Read in the CARTs from festival/trees/ directory,
     * and store them in a CARTMap
     * 
     * @param festvoxDirectory the festvox directory of a voice
     */
    public CART importCART(String filename,
                            FeatureDefinition featDef)
    throws IOException
    {
        try{
            //open CART-File
            System.out.println("Reading CART from "+filename+" ...");
            //build and return CART
            CART cart = new ExtendedClassificationTree();
            cart.load(filename,featDef,null);
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
                        CART cart)
    throws IOException
    {
        System.out.println("Dumping CART to "+destFile+" ...");
        
        //Open the destination file (cart.bin) and output the header
        DataOutputStream out = new DataOutputStream(new
                BufferedOutputStream(new 
                FileOutputStream(destFile)));
        //create new CART-header and write it to output file
        MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
        hdr.writeTo(out);

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
    public boolean replaceLeaves(CART cart,
            				FeatureDefinition featureDefinition)
    throws IOException
    {
        try {
            System.out.println("Replacing Leaves ...");
            
            System.out.println("Cart has "+cart.getNumNodes()+" nodes");
            
            
            //create wagon dir if it does not exist
            File wagonDir = new File(wagonDirName);
            if (!wagonDir.exists()){
                wagonDir.mkdir();
            }
            //get the filenames for the various files used by wagon
            String featureDefFile = wagonDescFile;
            String featureVectorsFile = wagonFeatsFile;
            String cartFile = wagonCartFile;
            String distanceTableFile = wagonDisTabsFile;
            //dump the feature definitions
            PrintWriter out = new PrintWriter(new 
                			FileOutputStream(new 
                			        File(featureDefFile)));
            Set featuresToIgnore = new HashSet();
            featuresToIgnore.add("mary_unit_logf0");
            featuresToIgnore.add("mary_unit_duration");
            featureDefinition.generateAllDotDescForWagon(out, featuresToIgnore);
            out.close();

            int numProcesses = Integer.getInteger("wagon.numProcesses", 1).intValue();
            System.out.println("Will run "+numProcesses+" wagon processes in parallel");
            WagonCallerThread[] wagons = new WagonCallerThread[numProcesses];
            
            int stop = 50; // do not want leaves smaller than this
            List leaves = new ArrayList();
            for (LeafNode leaf = cart.getFirstLeafNode(); leaf != null; leaf = leaf.getNextLeafNode()) {
                leaves.add(leaf);
            }
            int nLeaves = leaves.size();
            System.out.println("Computing acoustic subtrees for "+nLeaves+" unit clusters");
            /* call Wagon successively */
            //go through the CART
            int wagonID = 0;
            for (int i=0; i<nLeaves; i++) {
                long startTime = System.currentTimeMillis();
                percent = 100*i/nLeaves;
                LeafNode leaf = (LeafNode) leaves.get(i);
                FeatureVector[] featureVectors = ((LeafNode.FeatureVectorLeafNode)leaf).getFeatureVectors();
                if (featureVectors.length <= stop) continue;
                wagonID++;
                System.out.println("Leaf replacement no. "+wagonID+" started at "+new Date());
                //dump the feature vectors
                System.out.println(wagonID+"> Dumping "+featureVectors.length+" feature vectors...");
                String featureFileName = featureVectorsFile+wagonID;
                dumpFeatureVectors(featureVectors, featureDefinition, featureFileName);
                long endTime = System.currentTimeMillis();
                System.out.println(wagonID+">... dumping feature vectors took "+(endTime-startTime)+" ms");
                startTime = endTime;
                //dump the distance tables
                System.out.println(wagonID+"> Computing distance tables...");
                String distanceFileName = distanceTableFile+wagonID;
                buildAndDumpDistanceTables(featureVectors, distanceFileName, featureDefinition);
                endTime = System.currentTimeMillis();
                System.out.println(wagonID+"> ... computing distance tables took "+(endTime-startTime)+" ms");
                startTime = endTime;
                // Dispatch call to Wagon to one of the wagon callers:
                WagonCallerThread wagon = new WagonCallerThread(String.valueOf(wagonID), 
                        leaf, featureDefinition, featureVectors,
                        featureDefFile, 
                        featureFileName,
                        distanceFileName,
                        cartFile+wagonID,
                        0,
                        stop,
                        getProp(ESTDIR));
                boolean dispatched = false;
                while (!dispatched) {
                    for (int w=0; w<numProcesses && !dispatched; w++) {
                        if (wagons[w] == null) {
                            System.out.println("Dispatching wagon "+wagonID+" as process "+(w+1)+" out of "+numProcesses);
                            wagons[w] = wagon;
                            wagon.start();
                            dispatched = true;
                        } else if (wagons[w].finished()) {
                            if (!wagons[w].success()) {
                                System.out.println("Wagon "+wagons[w].id()+" failed. Aborting");
                                return false;
                            }
                            System.out.println("Dispatching wagon "+wagonID+" as process "+(w+1)+" out of "+numProcesses);
                            wagons[w] = wagon;
                            wagon.start();
                            dispatched = true;
                        }
                    }
                    if (!dispatched) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {}
                    }
                }
            }           
            // Now make sure we wait for all wagons to finish
            for (int w=0; w<numProcesses; w++) {
                if (wagons[w] != null) {
                    while (!wagons[w].finished()) {
                        try {
                            wagons[w].join();
                        } catch (InterruptedException ie) {}
                    }
                    if (!wagons[w].success()) {
                        System.out.println("Wagon "+wagons[w].id()+" failed. Aborting");
                        return false;
                    }
                }
            }
            
        } catch (IOException ioe) {
            IOException newIOE = new IOException("Error replacing leaves");
            newIOE.initCause(ioe);
            throw newIOE;
        }
        System.out.println(" ... done!");
        return true;
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
            // Print the feature string
            out.print( i+" "+featDef.toFeatureString( featureVectors[i] ) );
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
    public void buildAndDumpDistanceTables (FeatureVector[] featureVectors, String filename,
            FeatureDefinition featDef ) throws IOException {
        /* Load the MelCep timeline and the unit file */
        if (mcepTimeline == null) {
            try {
                mcepTimeline = new MCepTimelineReader(getProp(MCEPTIMELINE) );
            }
            catch ( IOException e ) {
                throw new RuntimeException( "Failed to read the Mel-Cepstrum timeline [" 
                        +getProp(MCEPTIMELINE)
                        + "] due to the following IOException: ", e );
            }
        }
        if (unitFile == null) {
            try {
                unitFile = new UnitFileReader(getProp(UNITFILE) );
            }
            catch ( IOException e ) {
                throw new RuntimeException( "Failed to read the unit file [" 
                        +getProp(UNITFILE)
                        + "] due to the following IOException: ", e );
            }
        }

        /* Dereference the number of units once and for all */
        int numUnits = featureVectors.length;
        /* Read the Mel Cepstra for each unit, and cumulate
         * their sufficient statistics in the same loop */
        double[][][] melCep = new double[numUnits][][];
        double val = 0;
        double[] sum = new double[mcepTimeline.getOrder()];
        double[] sumSq = new double[mcepTimeline.getOrder()];
        double[] sigma2 = new double[mcepTimeline.getOrder()];
        double N = 0.0;
        for ( int i = 0; i < numUnits; i++ ) {
            //System.out.println( "FEATURE_VEC_IDX=" + i + " UNITIDX=" + featureVectors[i].getUnitIndex() );
            /* Read the datagrams for the current unit */
            Datagram[] buff = null;
            MCepDatagram[] dat = null;
            //System.out.println( featDef.toFeatureString( featureVectors[i] ) );
            try {
                buff = mcepTimeline.getDatagrams( unitFile.getUnit(featureVectors[i].getUnitIndex()), unitFile.getSampleRate() );
                //System.out.println( "NUMFRAMES=" + buff.length );
                dat = new MCepDatagram[buff.length];
                for ( int d = 0; d < buff.length; d++ ) {
                    dat[d] = (MCepDatagram)( buff[d] );
                }
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Failed to read the datagrams for unit number [" + featureVectors[i].getUnitIndex()
                        + "] from the Mel-cepstrum timeline due to the following Exception: ", e );
            }
            N += (double)(dat.length); // Update the frame counter
            melCep[i] = new double[dat.length][];
            for ( int j = 0; j < dat.length; j++ ) {
                melCep[i][j] = dat[j].getCoeffsAsDouble();
                /* Cumulate the sufficient statistics */
                for ( int k = 0; k < mcepTimeline.getOrder(); k++ ) {
                    val = melCep[i][j][k];
                    sum[k] += val;
                    sumSq[k] += (val*val);
                }
            }
        }
        /* Finalize the variance calculation */
        for ( int k = 0; k < mcepTimeline.getOrder(); k++ ) {
            val = sum[k];
            sigma2[k] = ( sumSq[k] - (val*val)/N ) / N;
        }
        //System.out.println("Read MFCCs, now computing distances");
        /* Compute the unit distance matrix */
        double[][] dist = new double[numUnits][numUnits];
        for ( int i = 0; i < numUnits; i++ ) {
            dist[i][i] = 0.0; // <= Set the diagonal to 0.0
            for ( int j = 1; j < numUnits; j++ ) {
                /* Get the DTW distance between the two sequences: 
                System.out.println( "Entering DTW : "
                        + featDef.getFeatureName( 0 ) + " "
                        + featureVectors[i].getFeatureAsString( 0, featDef )
                        + ".length=" + melCep[i].length + " ; "
                        + featureVectors[j].getFeatureAsString( 0, featDef )
                        + ".length=" + melCep[j].length + " ." );
                System.out.flush(); */
                if (melCep[i].length == 0 || melCep[j].length == 0) {
                    if (melCep[i].length == melCep[j].length) { // both 0 length
                        dist[i][j] = dist[j][i] = 0;
                    } else {
                        dist[i][j] = dist[j][i] = 100000; // a large number
                    }
                } else {
                    //dist[i][j] = dist[j][i] = dtwDist( melCep[i], melCep[j], sigma2 );
                    //System.out.println("Using Mahalanobis distance\n"+
                      //      			"Distance is "+dist[i][j]);
                    
                    double f0Weight = 100; // ad hoc value
                    double durWeight = 1000; // ad hoc value

                    double spectralDist = stretchDist(melCep[i], melCep[j], sigma2);
                    double f0Dist = f0Weight * f0Dist(featureVectors[i], featureVectors[j], featDef);
                    double durDist = durWeight * durDist(featureVectors[i], featureVectors[j], featDef);
                    //System.out.println("Spectral distance: "+spectralDist+" -- F0 distance: "+f0Dist+" -- Duration distance: "+durDist);
                    dist[i][j] = dist[j][i] = spectralDist + f0Dist + durDist;
                }
            }
        }
        /* Write the matrix to disk */
        //System.out.println( "Writing distance matrix to file [" + filename + "]");
        PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(filename)));
        for ( int i = 0; i < numUnits; i++ ) {
            for ( int j = 0; j < numUnits; j++ ) {
                out.print( (float)(dist[i][j]) + " " );
            }
            out.print("\n");
        }
        out.flush();
        out.close();
        
    }
    
    
    private double f0Dist(FeatureVector fv1, FeatureVector fv2, FeatureDefinition fd)
    {
        int iLogF0 = fd.getFeatureIndex("mary_unit_logf0");
        float logf0_1 = fv1.getContinuousFeature(iLogF0);
        float logF0_2 = fv2.getContinuousFeature(iLogF0);
        return Math.abs(logf0_1-logF0_2);
    }

    private double durDist(FeatureVector fv1, FeatureVector fv2, FeatureDefinition fd)
    {
        int iLogF0 = fd.getFeatureIndex("mary_unit_duration");
        float logf0_1 = fv1.getContinuousFeature(iLogF0);
        float logF0_2 = fv2.getContinuousFeature(iLogF0);
        return Math.abs(logf0_1-logF0_2);
    }

    /**
     * Computes an average Mahalanobis distance along the simple time-stretched
     * correspondence between two frame sequences.
     * @param seq1 a frame sequence
     * @param seq2 another frame sequence
     * @param sigma2 the variance of the vectors
     * @return the average Mahalanobis distance between the two frame sequences
     */
    private double stretchDist(double[][] seq1, double[][] seq2, double[] sigma2)
    {
        double[][] shorter;
        double[][] longer;
        if (seq1.length < seq2.length) {
            shorter = seq1;
            longer = seq2;
        } else {
            shorter = seq2;
            longer = seq1;
        }
        float lengthFactor = shorter.length / (float) longer.length;
        double totalDist = 0;
        for (int i=0; i<longer.length; i++) {
            int iShorter = (int)(lengthFactor*i);
            double dist = mahalanobis(longer[i], shorter[iShorter], sigma2);
            if (Double.isInfinite(dist) || Double.isNaN(dist))
                dist = 100000; // a large number
            totalDist += dist;
        }
        return totalDist / longer.length;
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
    private double dtwDist( double[][] seq1, double[][] seq2, double[] sigma2 ) {
        
        if ( ( seq1.length <= 0 ) || ( seq2.length <= 0 ) ) {
            throw new RuntimeException( "Can't compute a DTW distance from a sequence with length 0 or negative. "
                    + "(seq1.length=" + seq1.length + "; seq2.length=" + seq2.length + ")" );
        }
        
        int l1 = seq1.length;
        int l2 = seq2.length;
        double[][] d = new double[l1][l2];
        double[][] D = new double[l1][l2];
        int[][] Nd = new int[l1][l2]; // <= Number of cumulated distances, for the final averaging
        double[] minV = new double[3];
        int[] minNd = new int[3];
        int minIdx = 0;
        /* Fill the local distance matrix */
        for ( int i = 0; i < l1; i++ ) {
            for ( int j = 0; j < l2; j++ ) {
                d[i][j] = mahalanobis( seq1[i],   seq2[j],   sigma2 );
            }
        }
        /* Compute the optimal DTW distance: */
        /* - 1st row/column: */
        /* (This part works for 1 frame or more in either sequence.) */
        D[0][0] = 2*d[0][0];
        for ( int i = 1; i < l1; i++ ) {
            D[i][0] = d[i][0];
            Nd[i][0] = 1;
        } 
        for ( int i = 1; i < l2; i++ ) {
                D[0][i] = d[0][i];
                Nd[0][i] = 1;
        }
        /* - 2nd row/column: */
        /* (This part works for 2 frames or more in either sequence.) */
        /* corner: i==1, j==1 */
        if ( (l1 > 1) && (l2 > 1) ) {
            minV[0] = 2*d[0][1] + d[1][1];  minNd[0] = 3;
            minV[1] = D[0][0] + 2*d[1][1];  minNd[1] = Nd[0][0] + 2;
            minV[2] = 2*d[1][0] + d[1][1];  minNd[2] = 3;
            minIdx = minV[0] < minV[1] ? 0 : 1;
            minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
            D[1][1] = minV[minIdx];
            Nd[1][1] = minNd[minIdx];

            /* 2nd row: j==1 ; 2nd col: i==1 */
            for ( int i = 2; i < l1; i++ ) {
                // Row: 
                minV[0] = D[i-2][0] + 2*d[i-1][1] + d[i][1];  minNd[0] = Nd[i-2][0] + 3;
                minV[1] = D[i-1][0] + 2*d[i][1];              minNd[1] = Nd[i-1][0] + 2;
                minV[2] = 2*d[i][0] + d[i][1];                minNd[2] = 3;
                minIdx = minV[0] < minV[1] ? 0 : 1;
                minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
                D[i][1] = minV[minIdx];
                Nd[i][1] = minNd[minIdx];
                }
            for ( int i = 2; i < l2; i++ ) {
                // Column: 
                minV[0] = 2*d[0][i] + d[1][i];                minNd[0] = 3;
                minV[1] = D[0][i-1] + 2*d[1][i];              minNd[1] = Nd[0][i-1] + 2;
                minV[2] = D[0][i-2] + 2*d[1][i-1] + d[1][i];  minNd[2] = Nd[0][i-2] + 3;
                minIdx = minV[0] < minV[1] ? 0 : 1;
                minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
                D[1][i] = minV[minIdx];
                Nd[1][i] = minNd[minIdx];
            }

        }
        /* - Rest of the matrix: */
        /* (This part works for 3 frames or more in either sequence.) */
        if ( (l1 > 2) && (l2 > 2) ) {
            for ( int i = 2; i < l1; i++ ) {
                for ( int j = 2; j < l2; j++ ) {
                    minV[0] = D[i-2][j-1] + 2*d[i-1][j] + d[i][j];  minNd[0] = Nd[i-2][j-1] + 3;
                    minV[1] = D[i-1][j-1] + 2*d[i][j];              minNd[1] = Nd[i-1][j-1] + 2;
                    minV[2] = D[i-1][j-2] + 2*d[i][j-1] + d[i][j];  minNd[0] = Nd[i-1][j-2] + 3;
                    minIdx = minV[0] < minV[1] ? 0 : 1;
                    minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
                    D[i][j] = minV[minIdx];
                    Nd[i][j] = minNd[minIdx];
                }
            }
        }
        /* Return */
        return( D[l1-1][l2-1] / (double)(Nd[l1-1][l2-1]) );
    }
    
    /**
     * Mahalanobis distance between two feature vectors.
     * 
     * @param v1 A feature vector.
     * @param v2 Another feature vector.
     * @param sigma2 The variance of the distribution of the considered feature vectors.
     * @return The mahalanobis distance between v1 and v2.
     */
    private double mahalanobis( double[] v1, double[] v2, double[] sigma2 ) {
        double sum = 0.0;
        double diff = 0.0;
        for ( int i = 0; i < v1.length; i++ ) {
            diff = v1[i] - v2[i];
            sum += ( (diff*diff) / sigma2[i] );
        }
        return( sum );
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }

    
    
    public static class WagonCallerThread extends Thread
    {
        //the Edinburgh Speech tools directory
        protected String ESTDIR;
        protected String arguments;
        protected File cartFile;
        protected File valueFile;
        protected File distanceTableFile;
        protected String id;
        protected LeafNode leafToReplace;
        protected FeatureDefinition featureDefinition;
        protected FeatureVector[] featureVectors;
        protected boolean finished = false;
        protected boolean success = false;
        
        public WagonCallerThread(String id,
                LeafNode leafToReplace, FeatureDefinition featureDefinition,
                FeatureVector[] featureVectors,
                String descFilename, String valueFilename, 
                String distanceTableFilename,
                String cartFilename,
                int balance,
                int stop,
                String ESTDIR)
        {
            this.id = id;
            this.leafToReplace = leafToReplace;
            this.featureDefinition = featureDefinition;
            this.featureVectors = featureVectors;
            this.ESTDIR = ESTDIR;

            this.valueFile = new File(valueFilename);
            this.distanceTableFile = new File(distanceTableFilename);
            this.cartFile = new File(cartFilename);
            
            this.arguments = "-desc " + descFilename
                + " -data " + valueFilename
                + " -balance " + balance 
                + " -distmatrix " + distanceTableFilename
                + " -stop " + stop 
                + " -output " + cartFilename;
        }
        
        public void run()
        {
            try {
                long startTime = System.currentTimeMillis();
                System.out.println(id+"> Calling wagon as follows:");
                System.out.println(ESTDIR + "/main/wagon " + arguments);
                Process p = Runtime.getRuntime().exec( ESTDIR + "/main/wagon " + arguments);
                //collect the output
                //read from error stream
                StreamGobbler errorGobbler = new 
                    StreamGobbler(p.getErrorStream(), id+" err");            
            
                //read from output stream
                StreamGobbler outputGobbler = new 
                    StreamGobbler(p.getInputStream(), id+" out");        
                //start reading from the streams
                errorGobbler.start();
                outputGobbler.start();
                p.waitFor();
                if (p.exitValue()!=0) {
                    finished = true;
                    success = false;
                } else {
                    success = true;
                    System.out.println(id+"> Wagon call took "+(System.currentTimeMillis()-startTime)+" ms");
                    
                    //read in the resulting CART
                    System.out.println(id+"> Reading CART");
                    BufferedReader buf = new BufferedReader(
                            new FileReader(cartFile));
                    CART newCART = new ExtendedClassificationTree(buf, featureDefinition);    
                    buf.close();
                    // Fix the new cart's leaves:
                    // They are currently the index numbers in featureVectors;
                    // but what we need is the unit index numbers!
                    for (LeafNode leaf = newCART.getFirstLeafNode(); leaf != null; leaf = leaf.getNextLeafNode()) {
                        int[] data = (int[])leaf.getAllData();
                        for (int i=0; i<data.length; i++) {
                            data[i] = featureVectors[data[i]].getUnitIndex();
                        }
                    }
                    
                    //replace the leaf by the CART
                    System.out.println(id+"> Replacing leaf");
                    System.out.println(id+"> (before: "+leafToReplace.getRootNode().getNumberOfNodes()+" nodes, adding "+newCART.getNumNodes()+")");
                    Node newNode = CART.replaceLeafByCart(newCART, leafToReplace);
                    System.out.println(id+"> done -- cart now has "+newNode.getRootNode().getNumberOfNodes()+" nodes.");

                    finished = true;
                }
                if (!Boolean.getBoolean("wagon.keepfiles")) {
                    valueFile.delete();
                    distanceTableFile.delete();
                }
                
            } catch (Exception e){
                e.printStackTrace();
                finished = true;
                success = false;
                throw new RuntimeException("Exception running wagon");
            }

        }
        
        public boolean finished() { return finished; }
        public boolean success() { return success; }
        public String id() { return id; }

    }
    
    static class StreamGobbler extends Thread
    {
        InputStream is;
        String type;
        
        StreamGobbler(InputStream is, String type)
        {
            this.is = is;
            this.type = type;
        }
        
        public void run()
        {
            try
            {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line=null;
                while ( (line = br.readLine()) != null)
                    System.out.println(type + ">" + line);    
                } catch (IOException ioe)
                  {
                    ioe.printStackTrace();  
                  }
        }
    }

    public static void main(String[] args) throws Exception
    {
        CARTBuilder cartBuilder = new CARTBuilder();
        DatabaseLayout db = new DatabaseLayout(cartBuilder);
        //compute        
        boolean ok = cartBuilder.compute();
        if (ok) System.out.println("Finished successfully!");
        else System.out.println("Failed.");
    }
    
}
