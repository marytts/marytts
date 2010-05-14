/**
 * Copyright 2010 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.cart.CART;
import marytts.cart.Node;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.MaryCARTWriter;
import marytts.cart.io.WagonCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

import marytts.signalproc.analysis.VoiceQuality;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.HnmTimelineReader;
import marytts.unitselection.data.MCepDatagram;
import marytts.unitselection.data.MCepTimelineReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.MaryUtils;

/**
 * A class which converts a text file in festvox format into a one-file-per-utterance format in a given directory.
 * 
 * @author steiner (based on DurationCARTTrainer by schroed)
 * 
 */
public class VoiceQualityCARTTrainer extends VoiceImportComponent {
    protected File unitfeatureDir;

    protected File tempDir;

    protected File vqFeatsFile;

    // protected File vqOQGDescFile;
    protected File vqDescFile;

    // protected File vqOQGTreeFile;

    protected DatabaseLayout db = null;

    protected int percent = 0;

    protected boolean useStepwiseTraining = false;

    private final String name = "VoiceQualityCARTTrainer";

    // public final String OQGTREE = name + ".vqOQGTree";

    // public final String VQFILE = name + ".vqFile";

    // public final String STEPWISETRAINING = name + ".stepwiseTraining";

    // public final String FEATUREFILE = name + ".featureFile";

    // public final String UNITFILE = name + ".unitFile";

    public final String ACFEATUREFILE = name + ".acFeatureFile";

    public final String ESTDIR = name + ".estDir";

    public String getName() {
        return name;
    }

    public void initialiseComp() {
        String rootDir = db.getProp(db.ROOTDIR);
        String tempDirName = db.getProp(db.TEMPDIR);
        this.tempDir = new File(tempDirName);
        if (!tempDir.exists()) {
            System.out.print("temp dir " + tempDirName + " does not exist; ");
            if (!tempDir.mkdir()) {
                throw new Error("Could not create TEMPDIR");
            }
            System.out.print("Created successfully.\n");
        }
        this.vqFeatsFile = new File(tempDirName + "vq.feats");
        // this.vqOQGDescFile = new File(tempDirName + "vq.oqg.desc");
        this.vqDescFile = new File(tempDirName + "vq.desc");
        // this.vqOQGTreeFile = new File(tempDirName + "vq.oqg.tree");
        // this.useStepwiseTraining = Boolean.valueOf(getProp(STEPWISETRAINING)).booleanValue();
    }

    public SortedMap<String, String> getDefaultProps(DatabaseLayout dbl) {
        this.db = dbl;
        if (props == null) {
            props = new TreeMap<String, String>();
            String fileSeparator = System.getProperty("file.separator");
            // props.put(STEPWISETRAINING, "false");
            // props.put(FEATUREFILE, db.getProp(db.FILEDIR) + "phoneFeatures" + db.getProp(db.MARYEXT));
            // props.put(UNITFILE, db.getProp(db.FILEDIR) + "phoneUnits" + db.getProp(db.MARYEXT));
            props.put(ACFEATUREFILE, db.getProp(db.FILEDIR) + "halfphoneFeatures_ac" + db.getProp(db.MARYEXT));
            // props.put(VQFILE, db.getProp(db.FILEDIR) + "timeline_vq" + db.getProp(db.MARYEXT));
            // props.put(OQGTREE, db.getProp(db.FILEDIR) + "vq.oqg.tree");
            String estdir = System.getProperty("ESTDIR");
            if (estdir == null) {
                estdir = "/project/mary/Festival/speech_tools/";
            }
            props.put(ESTDIR, estdir);
        }
        return props;
    }

    protected void setupHelp() {
        props2Help = new TreeMap<String, String>();
        // props2Help.put(STEPWISETRAINING, "\"false\" or \"true\" ???????????????????????????????????????????????????????????");
        props2Help.put(ACFEATUREFILE, "file containing all halfphone units and their target cost features");
        // props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features");
        // props2Help.put(UNITFILE, "file containing all phone units");
        // props2Help.put(VQFILE, "file containing all voice quality measurements");
        props2Help.put(ESTDIR, "directory containing the local installation of the Edinburgh Speech Tools");
        // props2Help.put(OQGTREE, "file containing the VQ CART. Will be created by this module");
    }

    public boolean compute() throws IOException {
        // FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
        // UnitFileReader unitFile = new UnitFileReader(getProp(UNITFILE));
        // String vqTimelineFile = getProp(VQFILE);
        // MCepTimelineReader vqTimeline = new MCepTimelineReader(vqTimelineFile);
        FeatureFileReader acFeatureFile = FeatureFileReader.getFeatureFileReader(getProp(ACFEATUREFILE));

        PrintWriter toFeaturesFile = new PrintWriter(new FileOutputStream(vqFeatsFile));
        System.out.println("VQ CART trainer: exporting VQ features");

        // FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
        FeatureDefinition featureDefinition = acFeatureFile.getFeatureDefinition();

        // init some variables
        // int sampleRate = unitFile.getSampleRate();
        String prevBasename = ""; // whatever, as long it's not the first basename or null
        // long prevBasenameDuration = 0;
        // long basenameStartSample = 0;
        int u = 0;
        // for (u = 0; u < unitFile.getNumberOfUnits(); u++) {
        String vqFeatureName = "unit_vq_oqg";
        int vqFeatureIndex = featureDefinition.getFeatureIndex(vqFeatureName);
        for (u = 0; u < acFeatureFile.getNumberOfUnits(); u++) {
            // We estimate that feature extraction takes 1/10 of the total time
            // (that's probably wrong, but never mind)
            // percent = 10 * u / unitFile.getNumberOfUnits();
            // Unit unit = unitFile.getUnit(u);
            percent = 10 * u / acFeatureFile.getNumberOfUnits();

            // TODO nan should only be excluded where it occurs and is not ignored, but some lines will have nan only in some fields!
            FeatureVector unitFV = acFeatureFile.getFeatureVector(u);
            float unitQV_OQG = unitFV.getContinuousFeature(vqFeatureIndex);
            // exclude NaN, which is not handled properly by wagon!
            if (!Float.isNaN(unitQV_OQG)) {
                /*
                 * Note: the featureString contains the continuous features at the end. We don't want any of these except the VQ
                 * values, which should should by default be at the beginning of the featureString. But instead of reordering and
                 * cherry-picking the features, we will lazily leave them as returned and add -predictee and -ignore arguments to
                 * the wagon call below, which has the same effect
                 */
                String featureString = featureDefinition.toFeatureString(unitFV);
                toFeaturesFile.println(featureString);
            }

            // do not process units with null duration (e.g. null units at basename edges)
            // if (unit.duration <= 0) {
            // continue;
            // }

            // Datagram[] vqDatagrams = vqTimeline.getDatagrams(unit, sampleRate);
            // double[] vqData = new double[vqDatagrams.length];
            // for(int d = 0; d < vqDatagrams.length; d++) {
            // MCepDatagram vqDatagram = (MCepDatagram) vqDatagrams[d];
            // vqData[d] = vqDatagram.getCoeffsAsDouble()[0]; // use only first field for now (OQG)
            // }
            // double meanOQG = MaryUtils.mean(vqData, true);

            // if (!Double.isNaN(meanOQG)) { // exclude NaN, which is not handled properly by wagon!
            // String line = meanOQG + " " + featureDefinition.toFeatureString(featureFile.getFeatureVector(u));
            // toFeaturesFile.println(line);
            // }
        }
        if (useStepwiseTraining)
            percent = 1;
        else
            percent = 10;
        toFeaturesFile.close();
        System.out.println("VQ features extracted for " + u + " units");

        // PrintWriter toDesc = new PrintWriter(new FileOutputStream(vqOQGDescFile));
        PrintWriter toDesc = new PrintWriter(new FileOutputStream(vqDescFile));
        generateFeatureDescriptionForWagon(featureDefinition, toDesc);
        toDesc.close();

        boolean ok = false;

        // select predictees (continuous features) for which to train CARTs (hard-coded for now):
        String[] predictees = { "unit_vq_oqg", "basename_vq_oqg" };

        // Now, call wagon once for each predictee:
        WagonCaller wagonCaller = new WagonCaller(getProp(ESTDIR), null);

        for (String predictee : predictees) {
            /*
             * apart from predictee, all continuous features should be ignored. However, somewhere between WagonCaller and
             * Runtime.exec, a StringTokenizer breaks the CLI argument to -ignore, which must be a bracketed list, and wagon
             * fails. Workaround is to write ignore fields into file and give filename as -ignore arg
             */
            String ignoreFilename = "ignore_fields_except_" + predictee;
            File ignoreFile = new File(tempDir + System.getProperty("file.separator") + ignoreFilename);
            PrintWriter ignoreFileWriter = new PrintWriter(ignoreFile);
            for (String featureName : featureDefinition.getContinuousFeatureNameArray()) {
                if (!featureName.equals(predictee)) {
                    ignoreFileWriter.println(featureName);
                }
            }
            ignoreFileWriter.close();

            // tree file to be created by wagon:
            File wagonTreeFile = new File(tempDir + System.getProperty("file.separator") + predictee + ".tree");

            // call wagon
            ok = wagonCaller.callWagon("-data " + vqFeatsFile.getAbsolutePath() + " -desc " + vqDescFile.getAbsolutePath()
                    + " -stop 10 " + " -output " + wagonTreeFile.getAbsolutePath() + " -predictee " + predictee + " -ignore "
                    + ignoreFile.getAbsolutePath());

            if (ok) {
                // String destinationFile = getProp(OQGTREE);
                String maryTreeFile = db.getProp(db.FILEDIR) + predictee + ".tree";
                WagonCARTReader wagonOQGReader = new WagonCARTReader(LeafType.FloatLeafNode);
                Node rootNode = wagonOQGReader.load(new BufferedReader(new FileReader(wagonTreeFile)), featureDefinition);
                CART oqgCart = new CART(rootNode, featureDefinition);
                MaryCARTWriter wwoqg = new MaryCARTWriter();
                wwoqg.dumpMaryCART(oqgCart, maryTreeFile);
            }
        }
        percent = 100;
        return ok;
    }

    private void generateFeatureDescriptionForWagon(FeatureDefinition fd, PrintWriter out) {
        out.println("(");
        int nDiscreteFeatures = fd.getNumberOfByteFeatures() + fd.getNumberOfShortFeatures();
        for (int i = 0, n = fd.getNumberOfFeatures(); i < n; i++) {
            out.print("( ");
            out.print(fd.getFeatureName(i));
            if (i < nDiscreteFeatures) { // list values
                if (fd.getNumberOfValues(i) == 20 && fd.getFeatureValueAsString(i, 19).equals("19")) {
                    // one of our pseudo-floats
                    out.println(" float )");
                } else { // list the values
                    for (int v = 0, vmax = fd.getNumberOfValues(i); v < vmax; v++) {
                        out.print("  ");
                        String val = fd.getFeatureValueAsString(i, v);
                        if (val.indexOf('"') != -1) {
                            StringBuilder buf = new StringBuilder();
                            for (int c = 0; c < val.length(); c++) {
                                char ch = val.charAt(c);
                                if (ch == '"')
                                    buf.append("\\\"");
                                else
                                    buf.append(ch);
                            }
                            val = buf.toString();
                        }
                        out.print("\"" + val + "\"");
                    }
                    out.println(" )");
                }
            } else { // float feature
                out.println(" float )");
            }
        }
        out.println(")");
    }

    /**
     * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
     * 
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress() {
        return percent;
    }
    /*
     * public static void main(String[] args) throws Exception { VoiceQualityCARTTrainer vqct = new VoiceQualityCARTTrainer();
     * DatabaseLayout db = new DatabaseLayout(vqct); vqct.compute(); }
     */
}
