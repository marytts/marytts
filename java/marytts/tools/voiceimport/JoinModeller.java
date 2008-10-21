/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.*;
import java.io.FileWriter;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.htsengine.HTSModelSet;
import marytts.htsengine.HTSTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.PhoneTranslator;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.unitselection.select.JoinCostFeatures;
import marytts.unitselection.select.JoinModelCost;
import marytts.unitselection.select.PrecompiledJoinCostReader;
import marytts.util.math.MathUtils;


public class JoinModeller extends VoiceImportComponent
{
    
    private DatabaseLayout db = null;
    private int percent = 0;
    
    private PhoneTranslator contextTranslator = null;  // CHECK this class, with the new version of HMM voices
                                                       // will not be needed.
    private HMMData htsData = null;  /* for using the function readFeatureList() */
    private Vector<String> featureList = null;
    private Map<String,String> feat2shortFeat = new HashMap<String, String>();
    
    private int numberOfFeatures = 0;
    private float[] fw = null;
    private String[] wfun = null;
    FileWriter statsStream = null;
    FileWriter mmfStream = null;
    FileWriter fullStream = null;

    
    public final String JOINCOSTFEATURESFILE = "JoinModeller.joinCostFeaturesFile";
    public final String UNITFEATURESFILE = "JoinModeller.unitFeaturesFile";
    public final String UNITFILE = "JoinModeller.unitFile";
    public final String STATSFILE = "JoinModeller.statsFile";
    public final String MMFFILE = "JoinModeller.mmfFile";
    public final String FULLFILE = "JoinModeller.fullFile";
    public final String CXCHEDFILE = "JoinModeller.cxcJoinFile";
    public final String JOINTREEFILE = "JoinModeller.joinTreeFile";
    public final String CNVHEDFILE = "JoinModeller.cnvJoinFile";
    public final String TRNCONFFILE = "JoinModeller.trnFile";
    public final String CNVCONFFILE = "JoinModeller.cnvFile"; 
    public final String HHEDCOMMAND = "JoinModeller.hhedCommand";
    public final String FEATURELISTFILE = "JoinModeller.featureListFile";
    
    public JoinModeller()
    {
        contextTranslator = new PhoneTranslator();
    }
    
    public String getName(){
        return "JoinModeller";
    }
 
    public SortedMap<String,String> getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap<String,String>();
           String filedir = db.getProp(db.FILEDIR);
           String maryExt = db.getProp(db.MARYEXT);
           props.put(JOINCOSTFEATURESFILE,filedir+"joinCostFeatures"+maryExt);
           props.put(UNITFEATURESFILE,filedir+"halfphoneFeatures"+maryExt);
           props.put(UNITFILE,filedir+"halfphoneUnits"+maryExt);
           props.put(STATSFILE,filedir+"stats"+maryExt);
           props.put(MMFFILE,filedir+"join_mmf"+maryExt);
           props.put(FULLFILE, filedir+"fullList"+maryExt);
           props.put(FEATURELISTFILE, filedir+"featureListFile"+maryExt);
           props.put(CXCHEDFILE, filedir+"cxc_join.hed");
           props.put(JOINTREEFILE, filedir+"join_tree.inf");
           props.put(CNVHEDFILE, filedir+"cnv_join.hed");
           props.put(TRNCONFFILE, filedir+"trn.cnf");
           props.put(CNVCONFFILE, filedir+"cnv.cnf");
           props.put(HHEDCOMMAND, "/project/mary/marcela/sw/HTS_2.0.1/htk/bin/HHEd");
       }
       return props;
    }
    
    protected void setupHelp(){         
        props2Help = new TreeMap<String,String>();
        props2Help.put(JOINCOSTFEATURESFILE,"file containing all halfphone units and their join cost features");
        props2Help.put(UNITFEATURESFILE,"file containing all halfphone units and their target cost features");
        props2Help.put(UNITFILE,"file containing all halfphone units");      
        props2Help.put(STATSFILE,"output file containing statistics of the models in HTK stats format");
        props2Help.put(MMFFILE,"output file containing one state HMM models, HTK format, representing join models (mean and variances are calculated in this class)");
        props2Help.put(FULLFILE,"output file containing the full list of HMM model names");
        props2Help.put(FEATURELISTFILE,"feature list for making fullcontext names and questions");        
        props2Help.put(CXCHEDFILE,"HTK hed file used by HHEd, load stats file, contains questions for decision tree-based context clustering and outputs result in join-tree.inf");
        props2Help.put(CNVHEDFILE,"HTK hed file used by HHEd to convert trees and mmf into hts_engine format");
        props2Help.put(TRNCONFFILE,"HTK configuration file for context clustering");
        props2Help.put(CNVCONFFILE,"HTK configuration file for converting to hts_engine format");
        props2Help.put(HHEDCOMMAND,"HTS-HTK HHEd command, HTS version minimum HTS_2.0.1");
    }
    
    public boolean compute() throws IOException, Exception
    {
        System.out.println("\n---- Training join models\n");       
        
        FeatureFileReader unitFeatures = FeatureFileReader.getFeatureFileReader(getProp(UNITFEATURESFILE));
        JoinCostFeatures joinFeatures = new JoinCostFeatures(getProp(JOINCOSTFEATURESFILE));
        UnitFileReader units = new UnitFileReader(getProp(UNITFILE));
        
        featureList = new Vector<String>(); 
        readFeatureList(getProp(FEATURELISTFILE));
              
        statsStream = new FileWriter(getProp(STATSFILE));
        mmfStream   = new FileWriter(getProp(MMFFILE));
        fullStream  = new FileWriter(getProp(FULLFILE));
        // output HTK model definition and dummy state-transition matrix, macro ~t "trP_1"
        int numFeatures = joinFeatures.getNumberOfFeatures();             
        mmfStream.write("~o\n" + "<VECSIZE> " + numFeatures 
                + " <USER><DIAGC>\n"  
                + "~t \"trP_1\"\n<TRANSP> 3\n" + "0 1 0\n0 0 1\n0 0 0\n");
       
       
         
        if (unitFeatures.getNumberOfUnits() != joinFeatures.getNumberOfUnits())
            throw new IllegalStateException("Number of units in unit and join feature files does not match!");
        if (unitFeatures.getNumberOfUnits() != units.getNumberOfUnits())
            throw new IllegalStateException("Number of units in unit file and unit feature file does not match!");
        int numUnits = unitFeatures.getNumberOfUnits();
        FeatureDefinition def = unitFeatures.getFeatureDefinition();

        int iPhoneme = def.getFeatureIndex("mary_phoneme");
        int nPhonemes = def.getNumberOfValues(iPhoneme);
        int iLeftRight = def.getFeatureIndex("mary_halfphone_lr");
        byte vLeft = def.getFeatureValueAsByte(iLeftRight, "L");
        byte vRight = def.getFeatureValueAsByte(iLeftRight, "R");
        int iEdge = def.getFeatureIndex("mary_edge");
        byte vNoEdge = def.getFeatureValueAsByte(iEdge, "0");
        byte vStartEdge = def.getFeatureValueAsByte(iEdge, "start");
        byte vEndEdge = def.getFeatureValueAsByte(iEdge, "end");

        
        Map<String,Set<double[]>> uniqueFeatureVectors = new HashMap<String, Set<double[]>>();
        
        // Now look at all pairs of adjacent units
        FeatureVector fvNext = unitFeatures.getFeatureVector(0);
        for (int i=0; i<numUnits-1; i++) {
            percent = 100 * (i+1) / numUnits;
            
            FeatureVector fv = fvNext; 
            byte edge = fv.getByteFeature(iEdge);
            fvNext = unitFeatures.getFeatureVector(i+1);
            byte edgeNext = fvNext.getByteFeature(iEdge);

            if (edge == vNoEdge && edgeNext == vNoEdge) {
                // TODO: do we need this?
                int phoneme = fv.getFeatureAsInt(iPhoneme);
                assert 0 <= phoneme && phoneme < nPhonemes;
                byte lr = fv.getByteFeature(iLeftRight);
                int phonemeNext = fvNext.getFeatureAsInt(iPhoneme);
                byte lrNext = fvNext.getByteFeature(iLeftRight);
                String pair = def.getFeatureValueAsString(iPhoneme, phoneme) +
                    "_" + def.getFeatureValueAsString(iLeftRight, lr) +
                    "-" +
                    def.getFeatureValueAsString(iPhoneme, phonemeNext) +
                    "_" + def.getFeatureValueAsString(iLeftRight, lrNext);

                // Compute the difference vector
                float[] myRightFrame = joinFeatures.getRightJCF(i);
                float[] nextLeftFrame = joinFeatures.getLeftJCF(i+1);
 
                double[] difference = new double[myRightFrame.length];                 
                for (int k=0, len=myRightFrame.length; k<len; k++) {
                    if(k==(len-1) && (myRightFrame[k] == Float.POSITIVE_INFINITY || nextLeftFrame[k] == Float.POSITIVE_INFINITY) ){
                      // TODO: find out in JoinCostFeatureFileWriter why Infinity values occur, and fix it.
                      difference[k] = 0.0;  
                      System.out.println("WARNING: numUnit="+ i + " myRightFrame[k]="+myRightFrame[k] + " nextLeftFrame[k]=" + nextLeftFrame[k]);
                    }
                    else
                      difference[k] = ((double)myRightFrame[k]) - nextLeftFrame[k];                   
                 }   
                

                // Group the units with the same feature vectors
                String contextName = contextTranslator.features2LongContext(def, fv, featureList);
                Set<double[]> unitsWithFV = uniqueFeatureVectors.get(contextName);
                if (unitsWithFV == null) {
                    unitsWithFV = new HashSet<double[]>();
                    uniqueFeatureVectors.put(contextName, unitsWithFV);
                }
                unitsWithFV.add(difference);
            }
        }
        
        int numUniqueFea = 1;
        int n,i,j,m;
        for (String fvString : uniqueFeatureVectors.keySet()) {
            double[][] diffVectors = uniqueFeatureVectors.get(fvString).toArray(new double[0][]);
            n = diffVectors.length;
            
            // Compute means and variances of the features across difference vectors         
            double[] means;
            double[] variances;
            if (n == 1) {
                means = diffVectors[0];
                variances = MathUtils.zeros(means.length);
               
            } else {
                means = MathUtils.mean(diffVectors, true);
                variances = MathUtils.variance(diffVectors, means, true);
      
            }
            
            assert means.length == numFeatures : "expected to have " + numFeatures + " features, got " + means.length;            
            
            fullStream.write(fvString + "\n");
            statsStream.write(numUniqueFea + " \"" + fvString + "\"    " + n + "    " + n + "\n");
            
            mmfStream.write("~h " + "\"" + fvString + "\"\n" );

            /* Stream 1*/
            mmfStream.write("<BEGINHMM>\n<NUMSTATES> 3\n<STATE> 2\n");
            mmfStream.write("<MEAN> " + numFeatures + "\n");
            for (i=0; i<means.length; i++)
              mmfStream.write(means[i] + " ");
            mmfStream.write("\n<VARIANCE> " + numFeatures + "\n");
            for (i=0; i<variances.length; i++)
              mmfStream.write(variances[i] + " ");
  
            mmfStream.write("\n~t \"trP_1\"\n<ENDHMM>\n");
            numUniqueFea++;
        }
        fullStream.close();
        statsStream.close();
        mmfStream.close();
        
        Process proc = null;
        String cmdLine = null;
        BufferedReader procStdout = null;
        String line = null;
        String filedir = db.getProp(db.FILEDIR);
        
        System.out.println(uniqueFeatureVectors.keySet().size() + " unique feature vectors, "+numUnits +" units");
        System.out.println("Generated files: " + getProp(STATSFILE) + " " + getProp(MMFFILE) + " " + getProp(FULLFILE));
        
        System.out.println("\n---- Creating tree clustering command file for HHEd\n");
        PrintWriter pw = new PrintWriter(new File(getProp(CXCHEDFILE)));
        pw.println("// load stats file");
        pw.println("RO 000 \"" + getProp(STATSFILE)+"\"");
        pw.println();
        pw.println("TR 0");
        pw.println();
        pw.println("// questions for decision tree-based context clustering");
        for (String f : featureList) {               
            int featureIndex = def.getFeatureIndex(f);
            String[] values = def.getPossibleValues(featureIndex);
            for (String v : values) {
                if (f.endsWith("phoneme")) {
                    v = contextTranslator.replaceTrickyPhones(v);
                } else if (f.endsWith("sentence_punc") || f.endsWith("punctuation")) {
                    v = PhoneTranslator.replacePunc(v);
                }
                pw.println("QS \""+f+"="+v+"\" {*|"+f+"="+v+"|*}");
            }
            pw.println();
        }
        pw.println("TR 3");
        pw.println();
        pw.println("// construct decision trees");
        pw.println("TB 000 join_ {*.state[2]}");
        pw.println();
        pw.println("TR 1");
        pw.println();
        pw.println("// output constructed tree");
        pw.println("ST \"" + getProp(JOINTREEFILE) + "\"");
        pw.close();
        
        System.out.println("\n---- Tree-based context clustering for joinModeller\n");
        // here the input file is join_mmf.mry and the output is  join_mmf.mry.clustered    
        cmdLine = getProp(HHEDCOMMAND) + " -A -C " + getProp(TRNCONFFILE) + " -D -T 2 -p -i -H " + getProp(MMFFILE) + " -m -a 1.0 -w " + getProp(MMFFILE)+".clustered"  + " " + getProp(CXCHEDFILE) + " " + getProp(FULLFILE);
        launchProc(cmdLine, "HHEd", filedir);

        System.out.println("\n---- Creating conversion-to-hts command file for HHEd\n");
        pw = new PrintWriter(new File(getProp(CNVHEDFILE)));
        pw.println("TR 3");
        pw.println();
        pw.println("// load trees for joinModeller");
        pw.println("LT \"" + getProp(JOINTREEFILE) + "\"");
        pw.println();
        pw.println("// convert loaded trees for hts_engine format");
        pw.println("CT \""+filedir+"\"");
        pw.println();
        pw.println("// convert mmf for hts_engine format");
        pw.println("CM \"" + filedir + "\"");
        pw.close();
        
        
        System.out.println("\n---- Converting mmfs to the hts_engine file format\n");
        // the input of this command are: join_mmf.mry and join_tree.inf and the output: trees.1 and pdf.1
        cmdLine = getProp(HHEDCOMMAND) + " -A -C " + getProp(CNVCONFFILE) + " -D -T 1 -p -i -H " + getProp(MMFFILE)+".clustered" + " " + getProp(CNVHEDFILE) + " " + getProp(FULLFILE);
        launchProc(cmdLine, "HHEd", filedir);
        
        // the files trees.1 and pdf.1 produced by the previous command are renamed as tree-joinModeller.inf and joinModeller.pdf
        cmdLine = "mv " + filedir + "trees.1 " + filedir + "tree-joinModeller.inf";
        launchProc(cmdLine, "mv", filedir);
        
        cmdLine = "mv " + filedir + "pdf.1 " + filedir + "joinModeller.pdf";
        launchProc(cmdLine, "mv", filedir);
        
        System.out.println("\n---- Created files: tree-joinModeller.inf, joinModeller.pdf");
        
        return true;
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


    /**
     * A general process launcher for the various tasks
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;
        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( cmdLine );
            
            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            while( true ) {
                line = procStdout.readLine();
                if ( line == null ) break;
                System.out.println( line );
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if ( proc.exitValue() != 0 ) {
                throw new RuntimeException( task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "]." );
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( task + " computation provoked an IOException on file [" + baseName + "].", e );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException( task + " computation interrupted on file [" + baseName + "].", e );
        }
        
    }
    
    
    /** This function reads the feature list file, for example feature_list_en_05.pl
     * and fills in a vector the elements in that list that are un-commented 
     */
    private void readFeatureList(String featureListFile) throws FileNotFoundException {
      String line;
      int i;
  
      //System.out.println("featureListFile: " + featureListFile);
      Scanner s = null;
      try {
        s = new Scanner(new BufferedReader(new FileReader(featureListFile))).useDelimiter("\n");
          
        while (s.hasNext()) {
          line = s.next();
          //System.out.println("fea: "+ line);
          if(!line.contains("#") && line.length()>0){    /* if it is not commented */
            String[] elem = line.split(",");
            for(i=0; i<elem.length; i++)
              if(elem[i].contains("mary_")){  /* if starts with mary_ */                 
                featureList.addElement(elem[i].substring(elem[i].indexOf("\"")+1, elem[i].lastIndexOf("\"")));
                //System.out.println("  -->  "+ featureList.lastElement()); 
              }
          }
        }
                
        if (s != null) { 
          s.close();
        }
        
      } catch (FileNotFoundException e) {
          System.out.println("readFeatureList:  " + e.getMessage());
      }
      
      System.out.println("readFeatureList: loaded " + featureList.size() + " context features from " + featureListFile);
      
    } /* method ReadFeatureList */

 
    
    public static void main(String[] args) throws IOException, InterruptedException{
        try {
            /* configure log info */
            org.apache.log4j.BasicConfigurator.configure();
            
            /* These trees are for halphone features */
            String joinPdfFile = "/project/mary/marcela/HMM-voices/DFKI_German_Poker/mary_files/joinModeller.pdf";
            String joinTreeFile = "/project/mary/marcela/HMM-voices/DFKI_German_Poker/mary_files/tree-joinModeller.inf";
            
            /* Load PDFs*/
            HTSModelSet joinPdf = new HTSModelSet();        
            joinPdf.loadJoinModelSet(joinPdfFile);
            
            JoinModelCost jm = new JoinModelCost();
            
            
            //String contextFile = "/project/mary/marcela/HMM-voices/DFKI_German_Poker/phonefeatures/m0001.pfeats";
            /*halphone example */
            String contextFile = "/project/mary/marcela/unitselection-halfphone.pfeats";
            Scanner context = new Scanner(new BufferedReader(new FileReader(contextFile)));
            String strContext="";
            while (context.hasNext()) {
              strContext += context.nextLine(); 
              strContext += "\n";
            }
            context.close();
            //System.out.println(strContext);
            FeatureDefinition def = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);
            jm.setFeatureDefinition(def);
            
            int numTrees = 1;  /* just JoinModeller will be loaded */
            HTSTreeSet joinTree = new HTSTreeSet(numTrees);            
            joinTree.loadTreeSetGeneral(joinTreeFile, 0, def);
            
            
            /* Given a context feature model name, find its join PDF mean and variance */
            /* first, find an index in the tree and then find the mean and variance that corresponds to that index in joinPdf */
            int indexPdf;
            int vectorSize = joinPdf.getJoinVsize();
            double[] mean = new double[vectorSize];
            double[] variance = new double[vectorSize];
            
            //String modelName = "0^f-t+_=0||str=0|pos_syl=3|pos_type=final|pos=NN|snt_punc=.|snt_numwrds=7|wrds_snt_stt=2|wrds_snt_end=4|wrd_numsyls=3|syls_wrd_stt=2|syls_wrd_end=0|wrd_numsegs=9|segs_wrd_stt=8|segs_wrd_end=0|syl_numsegs=4|segs_syl_stt=3|segs_syl_end=0|syls_p_str=2|syls_n_str=1|p_punc=0|n_punc=.|wrds_p_punc=2|wrds_n_punc=4|wrd_freq=0|";
            //String modelName = "0^i:-o:+_=0||str=0|pos_syl=0|pos_type=final|pos=FM|snt_punc=.|snt_numwrds=17|wrds_snt_stt=16|wrds_snt_end=0|wrd_numsyls=4|syls_wrd_stt=3|syls_wrd_end=0|wrd_numsegs=7|segs_wrd_stt=6|segs_wrd_end=0|syl_numsegs=1|segs_syl_stt=0|segs_syl_end=0|syls_p_str=2|syls_n_str=0|p_punc=:|n_punc=.|wrds_p_punc=4|wrds_n_punc=0|wrd_freq=0|";
            //String modelName = "aI^R-a:+t=@||wrds_n_punc=0|syl_numsegs=2|segs_wrd_stt=5|syls_wrd_stt=2|str=0|wrds_snt_end=0|pos_syl=1|p_punc=0|wrd_freq=3|snt_punc=dt|segs_wrd_end=3|wrds_snt_stt=4|wrds_p_punc=4|n_punc=dt|segs_syl_end=0|syls_wrd_end=1|pos_type=mid|syls_n_str=0|POS=VVPP|snt_numwrds=5|wrd_numsyls=4|wrd_numsegs=9|syls_p_str=1|segs_syl_stt=1||";
            //String feaLine = "24 0 1 0 0 1 2 3 1 0 30 2 2 1 1 0 0 0 0 43 2 2 4 1 0 0 0 0 1 1 0 1 3 0 0 2 2 0 0 0 0 0 1 1 1 0 0 5 1 3 5 1 3 10 1 9 1 3 1 1 3 0 2 6 1 4 2 1 0 1 3 1 7 9 0 1 6 0 1 1 3 0";
            //String feaLine = "23 0 2 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0 34 2 4 2 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0";
            /* halfphone example */
            String feaLine = "8 3 2 0 1 0 6 1 0 0 4 2 38 1 0 0 1 1 1 2 2 35 2 3 1 1 0 0 0 0 1 2 1 0 1 0 0 13 0 0 0 0 0 0 0 0 1 0 0 6 0 5 6 0 5 9 0 9 0 5 0 1 1 0 0 3 2 0 3 2 0 0 1 0 7 9 0 2 0 0 0 0 6 8";
            FeatureVector fv;
            
            context = new Scanner(new BufferedReader(new FileReader(contextFile)));
            while (context.hasNext()) {
              feaLine = context.nextLine(); 
              if (feaLine.trim().equals("")) break;
            }
            while (context.hasNext()) {
                feaLine = context.nextLine(); 
                if (feaLine.trim().equals("")) break;
            }
            while (context.hasNext()) {
                feaLine = context.nextLine(); 
                //System.out.println(feaLine);
            
            fv = def.toFeatureVector(0, feaLine);
            indexPdf = joinTree.searchTreeGeneral(fv, def, joinTree.getTreeHead(0).getRoot(), false);
            
            //--- old indexPdf = joinTree.searchTree(modelName, joinTree.getTreeHead(0).getRoot(), false);
            
            System.out.println("model: " + feaLine + "\nindexPdf: " + indexPdf);
            joinPdf.findJoinPdf(indexPdf, mean, variance);
            
            System.out.print("mean: ");
            for(int i=0; i<vectorSize; i++)
               System.out.print(mean[i] + " ");
            System.out.print("\nvariance: ");
            for(int i=0; i<vectorSize; i++)
               System.out.print(variance[i] + " ");
            System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
        
      }
    
    
}
