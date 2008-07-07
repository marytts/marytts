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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.io.FileWriter;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.modules.HTSContextTranslator;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.unitselection.select.JoinCostFeatures;
import marytts.unitselection.select.PrecompiledJoinCostReader;
import marytts.util.math.MathUtils;


public class JoinModeller extends VoiceImportComponent
{
    
    private DatabaseLayout db = null;
    private int percent = 0;
    
    private HTSContextTranslator contextTranslator = null;
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
    
    
    public JoinModeller()
    {
        contextTranslator = new HTSContextTranslator();
        featureList = new Vector<String>(Arrays.asList(new String[] {
                "mary_phoneme",
                "mary_prev_phoneme",
                "mary_next_phoneme",
                "mary_ph_vc",
                "mary_ph_cplace",
                "mary_ph_ctype",
                "mary_ph_cvox",
                "mary_ph_vfront",
                "mary_ph_vheight",
                "mary_ph_vlng",
                "mary_ph_vrnd",
                "mary_prev_vc",
                "mary_prev_cplace",
                "mary_prev_ctype",
                "mary_prev_cvox",
                "mary_prev_vfront",
                "mary_prev_vheight",
                "mary_prev_vlng",
                "mary_prev_vrnd",
                "mary_next_vc",
                "mary_next_cplace",
                "mary_next_ctype",
                "mary_next_cvox",
                "mary_next_vfront",
                "mary_next_vheight",
                "mary_next_vlng",
                "mary_next_vrnd",
                "mary_stressed",
                "mary_pos_in_syl",
                "mary_position_type",
                "mary_gpos",
                "mary_sentence_punc",
                "mary_sentence_numwords",
                "mary_words_from_sentence_start",
                "mary_words_from_sentence_end",
                "mary_word_numsyls",
                "mary_syls_from_word_start",
                "mary_syls_from_word_end",
                "mary_word_numsegs",
                "mary_segs_from_word_start",
                "mary_segs_from_word_end",
                "mary_syl_numsegs",
                "mary_segs_from_syl_start",
                "mary_segs_from_syl_end",
                "mary_syls_from_prev_stressed",
                "mary_syls_to_next_stressed",
                "mary_prev_punctuation",
                "mary_next_punctuation",
                "mary_words_from_prev_punctuation",
                "mary_words_to_next_punctuation",
                "mary_word_frequency",
                "mary_halfphone_lr"
        }));
        
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
        
        statsStream = new FileWriter(getProp(STATSFILE));
        mmfStream   = new FileWriter(getProp(MMFFILE));
        fullStream  = new FileWriter(getProp(FULLFILE));
        // output HTK model definition and dummy state-transition matrix, macro ~t "trP_1"
        int numFeatures = joinFeatures.getNumberOfFeatures();             
        int numFeaturesMcep = numFeatures - 1; 
        int numFeaturesF0 = 1;
        /* two streams, first of size 12 (MCEP) and the second of size 1 (F0) */
        mmfStream.write("~o\n" + "<VECSIZE> " + numFeatures 
                + " <USER><DIAGC>" 
                + "<STREAMINFO> 2 "+numFeaturesMcep+" "+numFeaturesF0 +"\n" 
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
                double aux;              
                for (int k=0, len=myRightFrame.length; k<len; k++) {
                    if(k==(len-1)){
                        // TODO: find out in JoinCostFeatureFileWriter why Infinity values occur, and fix it.
                      if(myRightFrame[k] == Float.POSITIVE_INFINITY || nextLeftFrame[k] == Float.POSITIVE_INFINITY ){    
                        aux= 0.0 ;
                        System.out.println("WARNING: numUnit="+ i + " myRightFrame[k]="+myRightFrame[k] + " nextLeftFrame[k]=" + nextLeftFrame[k]);
                      }else
                        aux = (double)myRightFrame[k] - nextLeftFrame[k];
                     
                      difference[k] = aux;
                      // if using logarithm ?
                      // if(aux == 0.0)
                      //   difference[k] = -1.0e10;
                      // else
                      //   difference[k] = Math.log(Math.abs(aux));
                      //System.out.println("k=" + k + " F0Right=" + myRightFrame[k] + " F0Left=" + nextLeftFrame[k] + "   log(abs(F0diff))=" + difference[k]);   
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
            m = diffVectors[0].length;
            double[][] diffVectorsMcep = new double[n][m-1];
            double[][] diffVectorsF0 = new double[n][1];
            for(i=0; i< n ; i++){
               for(j=0; j< m; j++){
                  if(j== m-1){
                    diffVectorsF0[i][0] = diffVectors[i][j];
                    //System.out.println("diff["+i+"]["+j+"]="+diffVectorsF0[i][0]);
                  }
                  else
                    diffVectorsMcep[i][j] = diffVectors[i][j]; 
                 //System.out.println("diff["+i+"]["+j+"]="+diffVectors[i][j]);
               }
            }
            // Compute means and variances of the features across difference vectors         
            double[] meansMcep;
            double[] variancesMcep;
            double[] meansF0;
            double[] variancesF0;
            double mix1, mix2;
            if (n == 1) {
                meansMcep = diffVectorsMcep[0];
                variancesMcep = MathUtils.zeros(meansMcep.length);
                
                meansF0 = diffVectorsF0[0];
                variancesF0 = MathUtils.zeros(meansF0.length);
            } else {
                meansMcep = MathUtils.mean(diffVectorsMcep, true);
                variancesMcep = MathUtils.variance(diffVectorsMcep, meansMcep, true);
                
                meansF0 = MathUtils.mean(diffVectorsF0, true);
                variancesF0 = MathUtils.variance(diffVectorsF0, meansF0, true);
            }
            
            assert meansMcep.length == numFeaturesMcep : "expected to have " + numFeaturesMcep + " features, got " + meansMcep.length;
                        
            
            fullStream.write(fvString + "\n");
            statsStream.write(numUniqueFea + " \"" + fvString + "\"    " + n + "    " + n + "\n");
            
            mmfStream.write("~h " + "\"" + fvString + "\"\n" );

            /* Stream 1*/
            mmfStream.write("<BEGINHMM>\n<NUMSTATES> 3\n<STATE> 2\n<STREAM> 1\n");
            mmfStream.write("<MEAN> " + numFeaturesMcep + "\n");
            for (i=0; i<meansMcep.length; i++)
              mmfStream.write(meansMcep[i] + " ");
            mmfStream.write("\n<VARIANCE> " + numFeaturesMcep + "\n");
            for (i=0; i<variancesMcep.length; i++)
              mmfStream.write(variancesMcep[i] + " ");

            /* Stream 2 */
            mmfStream.write("\n<STREAM> 2\n");
              mmfStream.write("<MEAN> " + numFeaturesF0 + "\n");
              for (i=0; i<meansF0.length; i++)
                mmfStream.write(meansF0[i] + " ");
              mmfStream.write("\n<VARIANCE> " + numFeaturesF0 + "\n");
              for (i=0; i<variancesF0.length; i++)
                mmfStream.write(variancesF0[i] + " ");
              
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
                    v = replacePunc(v);
                }
                pw.println("QS \""+f+"="+v+"\" {*|"+f+"="+v+"|*}");
            }
            pw.println();
        }
        pw.println("TR 3");
        pw.println();
        pw.println("// construct decision trees");
        pw.println("TB 000 join_mcep_s2_ {*.state[2].stream[1-1]}");
        pw.println("TB 000 join_f0_s2_ {*.state[2].stream[2-2]}");
        pw.println();
        pw.println("TR 1");
        pw.println();
        pw.println("// output constructed trees");
        pw.println("ST \"" + getProp(JOINTREEFILE) + "\"");
        pw.close();
        
        System.out.println("\n---- Tree-based context clustering for joinModeller\n");
        // here the input file is join_mmf.mry and the output is  join_mmf.mry.clustered    
        cmdLine = getProp(HHEDCOMMAND) + " -A -C " + getProp(TRNCONFFILE) + " -D -T 1 -p -i -H " + getProp(MMFFILE) + " -m -a 1.0 -w " + getProp(MMFFILE)+".clustered"  + " " + getProp(CXCHEDFILE) + " " + getProp(FULLFILE);
        launchProc(cmdLine, "HHEd", filedir);

        System.out.println("\n---- Creating conversion-to-hts command file for HHEd\n");
        pw = new PrintWriter(new File(getProp(CNVHEDFILE)));
        pw.println("TR 2");
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
        
        // the files trees.1 and pdf.1 produced by the previous command are renamed as tree-mcep-joinModeller.inf and mcep-joinModeller.pdf
        // the files trees.2 and pdf.2 produced by the previous command are renamed as tree-f0-joinModeller.inf and f0-joinModeller.pdf
        cmdLine = "mv " + filedir + "trees.1 " + filedir + "tree-mcep-joinModeller.inf";
        launchProc(cmdLine, "mv", filedir);
        cmdLine = "mv " + filedir + "trees.2 " + filedir + "tree-f0-joinModeller.inf";
        launchProc(cmdLine, "mv", filedir);
        
        cmdLine = "mv " + filedir + "pdf.1 " + filedir + "mcep-joinModeller.pdf";
        launchProc(cmdLine, "mv", filedir);
        cmdLine = "mv " + filedir + "pdf.2 " + filedir + "f0-joinModeller.pdf";
        launchProc(cmdLine, "mv", filedir);
        
        System.out.println("\n---- Created files: tree-mcep-joinModeller.inf, mcep-joinModeller.pdf");
        System.out.println("                      tree-f0-joinModeller.inf, f0-joinModeller.pdf\n");
        
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
    
    
    private String replacePunc(String lab){
        String s = lab;
           
        if(lab.contentEquals(".") )
          s = "pt";
        else if (lab.contentEquals(",") )
          s = "cm";
        else if (lab.contentEquals("(") )
            s = "op";
        else if (lab.contentEquals(")") )
            s = "cp";
        else if (lab.contentEquals("?") )
            s = "in";
        else if (lab.contentEquals("\"") )
            s = "qt";
        
        return s;
          
      }
    
    
}
