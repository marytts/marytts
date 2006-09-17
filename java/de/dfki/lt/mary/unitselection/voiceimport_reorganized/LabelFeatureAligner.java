package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import de.dfki.lt.mary.util.FileUtils;

/**
 * Compare unit label and unit feature files.
 * If they don't align, flag a problem; let the user
 * decide how to fix it -- either by editing the unit label
 * file or by editing a rawmaryxml file and recomputing the
 * features file.
 * @author schroed
 *
 */
public class LabelFeatureAligner
{
    protected File unitlabelDir;
    protected File unitfeatureDir;
    protected UnitFeatureComputer featureComputer;
    protected String pauseSymbol;
    
    public LabelFeatureAligner(File unitlabelDir, File unitfeatureDir, UnitFeatureComputer featureComputer, String pauseSymbol)
    {
        this.unitlabelDir = unitlabelDir;
        this.unitfeatureDir = unitfeatureDir;
        this.featureComputer = featureComputer;
        this.pauseSymbol = pauseSymbol;
    }
    
    public void compute() throws IOException
    {
        String[] basenames = FileUtils.listBasenames(unitlabelDir, ".unitlab");
        System.out.println("Verifying feature-label alignment for "+basenames.length+" files");
        List problems = new ArrayList();
        for (int i=0; i<basenames.length; i++) {
            boolean correct = verifyAlignment(basenames[i]);
            System.out.print("    "+basenames[i]);
            if (correct) {
                System.out.println(" OK");
            } else {
                problems.add(basenames[i]);
                System.out.println(" does not align properly");
            }
        }
        System.out.println("Found "+problems.size() + " problems");
        
        for (int i=0, len = problems.size(); i<len; i++) {
            String basename = (String) problems.get(i);
            boolean correct;
            do {
                System.out.print("    "+basename);
                letUserCorrect(basename);
                correct = verifyAlignment(basename);
                if (correct) {
                    System.out.println(" OK");
                } else {
                    System.out.println(" still does not align properly");
                }
            } while (!correct);
        }
    }
    
    protected boolean verifyAlignment(String basename) throws IOException
    {
        BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(unitlabelDir, basename+".unitlab")), "UTF-8"));
        BufferedReader features = new BufferedReader(new InputStreamReader(new FileInputStream(new File(unitfeatureDir, basename+".feats")), "UTF-8"));
        String line;
        // Skip label file header:
        while ((line = labels.readLine()) != null) {
            if (line.startsWith("#")) break; // line starting with "#" marks end of header
        }
        // Skip features file header:
        while ((line = features.readLine()) != null) {
            if (line.trim().equals("")) break; // empty line marks end of header
        }

        String firstLabelUnit = null;
        // Skip initial pauses in label file:
        do {
            firstLabelUnit = getLabelUnit(labels);
        } while (firstLabelUnit.equals(pauseSymbol));

        String firstFeatureUnit = null;
        // Skip initial pauses in features file (in the unexpected case there are any):
        do {
            firstFeatureUnit = getFeatureUnit(features);
        } while (firstFeatureUnit.equals(pauseSymbol));
        
        if (firstLabelUnit == null || !firstLabelUnit.equals(firstFeatureUnit)) {
            System.out.println("Non-matching initial units found: '"+firstLabelUnit+"' vs. '"+firstFeatureUnit+"'");
            return false;
        }
        // Now go through all feature file units
        boolean correct = true;
        while (correct) {
            String labelUnit = getLabelUnit(labels);
            String featureUnit = getFeatureUnit(features);
            // when featureUnit is the empty string, we have found an empty line == end of feature section
            if ("".equals(featureUnit)) break;
            if (!labelUnit.equals(featureUnit)) {
                System.out.println("Non-matching units found: '"+labelUnit+"' vs. '"+featureUnit+"'");
                return false;
            }
        }
        return true;
    }
    
    private String getLabelUnit(BufferedReader labelReader)
    throws IOException
    {
        String line = labelReader.readLine();
        if (line == null) return null;
        StringTokenizer st = new StringTokenizer(line.trim());
        // The third token in each line is the label
        st.nextToken(); st.nextToken();
        String unit = st.nextToken();
        return unit;
    }
    
    private String getFeatureUnit(BufferedReader featureReader)
    throws IOException
    {
        String line = featureReader.readLine();
        if (line == null) return null;
        if (line.trim().equals("")) return ""; // empty line -- signal end of section
        StringTokenizer st = new StringTokenizer(line.trim());
        // The expect that the first token in each line is the label
        String unit = st.nextToken();
        return unit;
        
    }
    
    protected void letUserCorrect(String basename)
    {
        try {
            Thread.sleep(10000);
        }catch (InterruptedException e) {}
    }
    
    

    public static void main(String[] args) throws IOException
    {
        String text = System.getProperty("text.dir", "text");
        String unitfeatures = System.getProperty("unitfeatures.dir", "unitfeatures");
        String locale = System.getProperty("locale", "en");
        UnitFeatureComputer ufc = new UnitFeatureComputer(new File(text), new File(unitfeatures), locale);
        String unitlab = System.getProperty("unitlab.dir", "unitlab");
        String pauseSymbol = System.getProperty("pause.symbol", "pau");
        LabelFeatureAligner lfa = new LabelFeatureAligner(new File(unitlab), new File(unitfeatures), ufc, pauseSymbol);
        lfa.compute();
    }

}
