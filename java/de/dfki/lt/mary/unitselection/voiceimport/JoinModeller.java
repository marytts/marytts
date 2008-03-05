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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.io.FileWriter;

import de.dfki.lt.mary.modules.HTSContextTranslator;
import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.JoinCostFeatures;
import de.dfki.lt.mary.unitselection.PrecompiledJoinCostReader;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.signalproc.util.MathUtils;

public class JoinModeller extends VoiceImportComponent
{
    
    private DatabaseLayout db = null;
    private int percent = 0;
    
    private HTSContextTranslator contextTranslator = null;
    private Vector<String> featureList = null;
    
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
    //public final String CXCHEDFILE = "JoinModeller.cxcJoinFile";
    //public final String CNVHEDFILE = "JoinModeller.cnvJoinFile";
    //public final String TRNCONFFILE = "JoinModeller.trnFile";
    //public final String CNVCONFFILE = "JoinModeller.cnvFile";   
    
    
    public JoinModeller()
    {
        contextTranslator = new HTSContextTranslator();
        featureList = new Vector<String>(Arrays.asList(new String[] {
                "mary_stressed",
                "mary_pos_in_syl",
                "mary_position_type",
                "mary_pos",
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
           props.put(MMFFILE,filedir+"mmf"+maryExt);
           props.put(FULLFILE, filedir+"fullList"+maryExt);
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
    }
    
    public boolean compute() throws IOException, Exception
    {
        System.out.println("---- Training join models");
        
        
        FeatureFileReader unitFeatures = FeatureFileReader.getFeatureFileReader(getProp(UNITFEATURESFILE));
        JoinCostFeatures joinFeatures = new JoinCostFeatures(getProp(JOINCOSTFEATURESFILE));
        UnitFileReader units = new UnitFileReader(getProp(UNITFILE));
        
        statsStream = new FileWriter(getProp(STATSFILE));
        mmfStream   = new FileWriter(getProp(MMFFILE));
        fullStream  = new FileWriter(getProp(FULLFILE));
        // output HTK model definition and dummy state-transition matrix, macro ~t "trP_1"
        // is there a way to know the lenght of MFCC at this point? so then there is no need
        // of hard coding 12.
        mmfStream.write("~o\n" + "<VECSIZE> 12 <MFCC><DIAGC>\n" + "~t \"trP_1\"\n<TRANSP> 3\n" + "0 1 0\n0 0 1\n0 0 0\n");
       
         
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
                    difference[k] = ((double)myRightFrame[k]) - nextLeftFrame[k];
                }


                // Group the units with the same feature vectors
                String contextName = contextTranslator.features2context(def, fv, featureList);
                Set<double[]> unitsWithFV = uniqueFeatureVectors.get(contextName);
                if (unitsWithFV == null) {
                    unitsWithFV = new HashSet<double[]>();
                    uniqueFeatureVectors.put(contextName, unitsWithFV);
                }
                unitsWithFV.add(difference);
            }
        }
        
        int numUniqueFea = 1;
        for (String fvString : uniqueFeatureVectors.keySet()) {
            double[][] diffVectors = uniqueFeatureVectors.get(fvString).toArray(new double[0][]);
            int n = diffVectors.length;
            //System.out.println(numUniqueFea + " " + n + " of " + fvString);
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
            fullStream.write(fvString + "\n");
            statsStream.write(numUniqueFea + " \"" + fvString + "\"    " + n + "    " + n + "\n");
            
            mmfStream.write("~h " + "\"" + fvString + "\"\n" );
            mmfStream.write("<BEGINHMM>\n<NUMSTATES> 3\n<STATE> 2\n");
            mmfStream.write("<MEAN> " + (means.length-1) + "\n");
            //System.out.print("means: ");
            //for (int i=0; i<means.length; i++) System.out.printf("  %.3f", means[i]);
            for (int i=0; i<(means.length-1); i++)
                mmfStream.write(means[i] + " ");
            mmfStream.write("\n<VARIANCE> " + (means.length-1) + "\n");
            //System.out.println();
            //System.out.print("vars : ");
            //for (int i=0; i<means.length; i++) System.out.printf("  %.3f", variances[i]);
            for (int i=0; i<(means.length-1); i++)
                mmfStream.write(variances[i] + " ");
            mmfStream.write("\n~t \"trP_1\"\n<ENDHMM>\n");
            //System.out.println();
            numUniqueFea++;
        }
        
        System.out.println(uniqueFeatureVectors.keySet().size() + " unique feature vectors, "+numUnits +" units");
        fullStream.close();
        statsStream.close();
        mmfStream.close();
        
        System.out.println("\nTree-based context clustering for joinModeller\n");
        
        
        System.out.println("\nConverting mmfs to the hts_engine file format");
        
        
        
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


}
