package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.util.PrintfFormat;

/**
 * A class which converts a text file in festvox format
 * into a one-file-per-utterance format in a given directory.
 * @author schroed
 *
 */
public class F0CARTTrainer implements VoiceImportComponent
{
    protected File f0Dir;
    protected File leftF0FeaturesFile;
    protected File midF0FeaturesFile;
    protected File rightF0FeaturesFile;
    protected File wagonDescFile;
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    protected int percent = 0;
    
    
    
    /**/
    public F0CARTTrainer( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;
        String rootDir = db.rootDirName();
        this.f0Dir = new File(rootDir, System.getProperty("db.f0dir", "f0carts"));
        if (!f0Dir.exists()) f0Dir.mkdir();
        this.leftF0FeaturesFile = new File(f0Dir, System.getProperty("db.f0.left.featuresfile", "f0.left.feats"));
        this.midF0FeaturesFile = new File(f0Dir, System.getProperty("db.f0.mid.featuresfile", "f0.mid.feats"));
        this.rightF0FeaturesFile = new File(f0Dir, System.getProperty("db.f0.right.featuresfile", "f0.right.feats"));
        this.wagonDescFile = new File(f0Dir, System.getProperty("db.f0.wagondescfile", "f0.desc"));
    }
    
    /**/
    public boolean compute() throws IOException
    {
        FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(db.targetFeaturesFileName());
        UnitFileReader unitFile = new UnitFileReader( db.unitFileName() );
        TimelineReader waveTimeline = new TimelineReader( db.waveTimelineFileName() );
        
        PrintWriter toLeftFeaturesFile = new PrintWriter(new FileOutputStream(leftF0FeaturesFile));
        PrintWriter toMidFeaturesFile = new PrintWriter(new FileOutputStream(midF0FeaturesFile));
        PrintWriter toRightFeaturesFile = new PrintWriter(new FileOutputStream(rightF0FeaturesFile));

        System.out.println("F0 CART trainer: exporting f0 features");
        
        FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
        byte isVowel = featureDefinition.getFeatureValueAsByte("mary_ph_vc", "+");
        int iVC = featureDefinition.getFeatureIndex("mary_ph_vc");
        int iSegsFromSylStart = featureDefinition.getFeatureIndex("mary_segs_from_syl_start");
        int iSegsFromSylEnd = featureDefinition.getFeatureIndex("mary_segs_from_syl_end");
        int iCVoiced = featureDefinition.getFeatureIndex("mary_ph_cvox");
        byte isCVoiced = featureDefinition.getFeatureValueAsByte("mary_ph_cvox", "+");

        int nSyllables = 0;
        for (int i=0, len=unitFile.getNumberOfUnits(); i<len; i++) {
            // We estimate that feature extraction takes 1/10 of the total time
            // (that's probably wrong, but never mind)
            percent = 10*i/len;
            FeatureVector fv = featureFile.getFeatureVector(i);
            if (fv.getByteFeature(iVC) == isVowel) {
                // Found a vowel, i.e. found a syllable.
                int mid = i;
                FeatureVector fvMid = fv;
                // Now find first/last voiced unit in the syllable:
                int first = i;
                for(int j=1, lookLeft = (int)fvMid.getByteFeature(iSegsFromSylStart); j<lookLeft; j++) {
                    fv = featureFile.getFeatureVector(mid-j); // units are in sequential order
                    // No need to check if there are any vowels to the left of this one,
                    // because of the way we do the search in the top-level loop.
                    if (fv.getByteFeature(iCVoiced) != isCVoiced) {
                        break; // mid-j is not voiced
                    }
                    first = mid-j; // OK, the unit we are currently looking at is part of the voiced syllable section
                }
                int last = i;
                for(int j=1, lookRight = (int)fvMid.getByteFeature(iSegsFromSylEnd); j<lookRight; j++) {
                    fv = featureFile.getFeatureVector(mid+j); // units are in sequential order

                    if (fv.getByteFeature(iVC) != isVowel && fv.getByteFeature(iCVoiced) != isCVoiced) {
                        break; // mid+j is not voiced
                    }
                    last = mid+j; // OK, the unit we are currently looking at is part of the voiced syllable section
                }
                // TODO: make this more robust, e.g. by fitting two straight lines to the data:
                Datagram[] midDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(mid), unitFile.getSampleRate());
                Datagram[] leftDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(first), unitFile.getSampleRate());
                Datagram[] rightDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(last), unitFile.getSampleRate());
                if (midDatagrams != null && midDatagrams.length > 0
                        && leftDatagrams != null && leftDatagrams.length > 0
                        && rightDatagrams != null && rightDatagrams.length > 0) {
                    float midF0 = waveTimeline.getSampleRate() / (float) midDatagrams[midDatagrams.length/2].getDuration();
                    float leftF0 = waveTimeline.getSampleRate() / (float) leftDatagrams[0].getDuration();
                    float rightF0 = waveTimeline.getSampleRate() / (float) rightDatagrams[rightDatagrams.length-1].getDuration();
                    System.out.println("Syllable at "+mid+" (length "+(last-first+1)+"): left = "+((int)leftF0)+", mid = "+((int)midF0)+", right = "+rightF0);
                    toLeftFeaturesFile.println(leftF0 + " "+ featureDefinition.toFeatureString(fvMid));
                    toMidFeaturesFile.println(midF0 + " "+ featureDefinition.toFeatureString(fvMid));
                    toRightFeaturesFile.println(rightF0 + " "+ featureDefinition.toFeatureString(fvMid));
                    nSyllables++;
                    
                }
                // Skip the part we just covered:
                i = last;
            }
        }
        toLeftFeaturesFile.close();
        toMidFeaturesFile.close();
        toRightFeaturesFile.close();
        System.out.println("F0 features extracted for "+nSyllables+" syllables");
        percent = 10; // estimated
        PrintWriter toDesc = new PrintWriter(new FileOutputStream(wagonDescFile));
        generateFeatureDescriptionForWagon(featureDefinition, toDesc);
        toDesc.close();
        
        // Now, call wagon
        WagonCaller wagonCaller = new WagonCaller(null);
        File wagonTreeFile = new File(f0Dir, "f0.left.tree");
        boolean ok = wagonCaller.callWagon("-data "+leftF0FeaturesFile.getAbsolutePath()
                +" -desc "+wagonDescFile.getAbsolutePath()
                +" -stop 10 "
                +" -output "+wagonTreeFile.getAbsolutePath());
        if (!ok) return false;
        percent = 40;
        wagonTreeFile = new File(f0Dir, "f0.mid.tree");
        ok = wagonCaller.callWagon("-data "+midF0FeaturesFile.getAbsolutePath()
                +" -desc "+wagonDescFile.getAbsolutePath()
                +" -stop 10 "
                +" -output "+wagonTreeFile.getAbsolutePath());
        if (!ok) return false;
        percent = 70;
        wagonTreeFile = new File(f0Dir, "f0.right.tree");
        ok = wagonCaller.callWagon("-data "+rightF0FeaturesFile.getAbsolutePath()
                +" -desc "+wagonDescFile.getAbsolutePath()
                +" -stop 10 "
                +" -output "+wagonTreeFile.getAbsolutePath());
        percent = 100;
        return ok;
    }
    
    private String[] align(String basename) throws IOException
    {
        BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.phoneUnitLabDirName() + basename + db.phoneUnitLabExt() )), "UTF-8"));
        BufferedReader features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.phoneUnitFeaDirName() + basename + db.phoneUnitFeaExt() )), "UTF-8")); 
        String line;
        // Skip label file header:
        while ((line = labels.readLine()) != null) {
            if (line.startsWith("#")) break; // line starting with "#" marks end of header
        }
        // Skip features file header:
        while ((line = features.readLine()) != null) {
            if (line.trim().equals("")) break; // empty line marks end of header
        }
        
        // Now go through all feature file units
        boolean correct = true;
        int unitIndex = -1;
        float prevEnd = 0;
        List aligned = new ArrayList();
        while (correct) {
            unitIndex++;
            String labelLine = labels.readLine();
            String featureLine = features.readLine();
            if (featureLine == null) { // incomplete feature file
                return null;
            } else if (featureLine.trim().equals("")) { // end of features
                if (labelLine == null) break; // normal end found
                else // label file is longer than feature file
                    return null;
            }
            // Verify that the two labels are the same:
            StringTokenizer st = new StringTokenizer(labelLine.trim());
            // The third token in each line is the label
            float end = Float.parseFloat(st.nextToken());
            st.nextToken(); // skip
            String labelUnit = st.nextToken();

            st = new StringTokenizer(featureLine.trim());
            // The expect that the first token in each line is the label
            String featureUnit = st.nextToken();
            if (!featureUnit.equals(labelUnit)) {
                // Non-matching units found
                return null;
            }
            // OK, now we assume we have two matching lines.
            if (!featureUnit.startsWith("_")) { // discard all silences
                // Output format: unit duration, followed by the feature line
                aligned.add(new PrintfFormat(Locale.ENGLISH, "%.3f").sprintf(end-prevEnd)+" "+featureLine.trim());
            }
            prevEnd = end;
        }
        return (String[]) aligned.toArray(new String[0]);
    }
    
    private void generateFeatureDescriptionForWagon(FeatureDefinition fd, PrintWriter out)
    {
        out.println("(");
        out.println("(segment_duration float)");
        int nDiscreteFeatures = fd.getNumberOfByteFeatures()+fd.getNumberOfShortFeatures();
        for (int i=0, n=fd.getNumberOfFeatures(); i<n; i++) {            
            out.print("( ");
            out.print(fd.getFeatureName(i));
            if (i<nDiscreteFeatures) { // list values
                if (fd.getNumberOfValues(i) == 20 && fd.getFeatureValueAsString(i, 19).equals("19")) {
                    // one of our pseudo-floats
                    out.println(" float )");
                } else { // list the values
                    for (int v=0, vmax=fd.getNumberOfValues(i); v<vmax; v++) {
                        out.print("  ");
                        String val = fd.getFeatureValueAsString(i, v);
                        if (val.indexOf('"') != -1) {
                            StringBuffer buf = new StringBuffer();
                            for (int c=0; c<val.length(); c++) {
                                char ch = val.charAt(c);
                                if (ch == '"') buf.append("\\\"");
                                else buf.append(ch);
                            }
                            val = buf.toString();
                        }
                        out.print("\""+val+"\"");
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
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }


}
