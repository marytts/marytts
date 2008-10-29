/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.tools.en_US;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.cart.StringCART;
import marytts.cart.io.MaryCARTWriter;
import marytts.cart.io.WagonCARTWriter;
import marytts.features.FeatureDefinition;
import marytts.fst.AlignerTrainer;
import marytts.fst.FSTLookup;
import marytts.fst.TransducerTrie;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;
import marytts.server.MaryProperties;
import marytts.tools.newlanguage.LTSTrainer;

/**
 * This class does a one-time, offline conversion from the CMUDict in Festival format
 * (cmudict-0.4.scm and cmudict_extensions.scm) into MARY format.
 * Specifically, the following steps are performed:
 * <ol>
 * <li>conversion to a text format without brackets, using '|' as the delimiter between three fields:
 * <code>graphemes | allophones | part-of-speech(optional)</code>
 * </li>
 * <li>conversion of the phonetic alphabet used from MRPA to SAMPA</li>
 * <li>creation of a compact FST representing the lexicon</li>
 * <li>training of Letter-to-sound rules from the data</li>
 * </ol>
 * @author marc
 *
 */
public class CMUDict2MaryFST
{
    private static Map<String, String> mrpa2sampa;
    private static AllophoneSet usSampa;

    private static void fillSampaMap() throws Exception
    {
        // Any phoneme inventory mappings?
        String sampamapFilename = "lib/modules/en/synthesis/sampa2mrpa_en.map";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sampamapFilename), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.equals("") || line.startsWith("#")) {
                    continue; // ignore empty and comment lines
                }
                try {
                    addSampaMapEntry(line);
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException("Ignoring invalid entry in sampa map file "+sampamapFilename, iae);
                }
            }
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Cannot open file '"+sampamapFilename+"'", ioe);
        }
    }

    private static void addSampaMapEntry(String entry) throws IllegalArgumentException
    {
        boolean s2v = false;
        boolean v2s = false;
        String[] parts = null;
        // For one-to-many mappings, '+' can be used to group phoneme symbols.
        // E.g., the line "EI->E:+I" would map "EI" to "E:" and "I" 
        entry.replace('+', ' ');
        if (entry.indexOf("<->") != -1) {
            parts = entry.split("<->");
            s2v = true;
            v2s = true;
        } else if (entry.indexOf("->") != -1) {
            parts = entry.split("->");
            s2v = true;
        } else if (entry.indexOf("<-") != -1) {
            parts = entry.split("<-");
            v2s = true;
        }
        if (parts == null || parts.length != 2) { // invalid entry
            throw new IllegalArgumentException();
        }
        if (v2s) {
            mrpa2sampa.put(parts[1].trim(), parts[0].trim());
        }
    }

    /** Converts a single phonetic symbol in MRPA representation
     * representation into its equivalent in MARY sampa representation.
     * @return the converted phoneme, or the input string if no known conversion exists.
     */
    public static String mrpa2sampa(String voicePhoneme)
    {
        if (mrpa2sampa.containsKey(voicePhoneme))
            return mrpa2sampa.get(voicePhoneme);
        else
            return voicePhoneme;
    }

    public static String mrpaString2sampaString(String mrpaString)
    {
        StringTokenizer st = new StringTokenizer(mrpaString);
        LinkedList<String> sampaList = new LinkedList<String>();
        while (st.hasMoreTokens()) {
            String mrpa = st.nextToken();
            String sampa;
            if (mrpa.endsWith("1")) {
                sampa = mrpa2sampa(mrpa.substring(0, mrpa.length()-1))+"1";
            } else if (mrpa.endsWith("0")) {
                sampa = mrpa2sampa(mrpa.substring(0, mrpa.length()-1));
            } else {
                sampa = mrpa2sampa(mrpa);
            }
            sampaList.add(sampa);
        }
        usSampa.getSyllabifier().syllabify(sampaList);
        StringBuilder sb = new StringBuilder();
        for (String s : sampaList) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(s);
        }
        return sb.toString();
    }
    
    private static void convertToSampa(BufferedReader br, PrintWriter toSampa)
    throws IOException 
    {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            // skip comments:
            if (line.startsWith(";") || line.equals("")) continue;
            // expected line format:
            // ("acquirer" nil (ax k w ay1 er0 er0))
            int firstQuote = line.indexOf('"');
            if (!(firstQuote >= 0)) {
                System.err.println("Skipping strange line (no first quote): "+line);
            }
            int secondQuote = line.indexOf('"', firstQuote+1);
            if (!(secondQuote > firstQuote)) {
                System.err.println("Skipping strange line (no second quote): "+line);
            }
            int firstSpace = secondQuote+1;
            if (!(line.charAt(firstSpace) == ' ')) {
                System.err.println("Skipping strange line (no first space): "+line);
            }
            int secondSpace = line.indexOf(' ', firstSpace+1);
            if (!(secondSpace > firstSpace)) {
                System.err.println("Skipping strange line (no second space): "+line);
            }
            int firstBracket = secondSpace+1;
            if (!(line.charAt(firstBracket) == '(')) {
                System.err.println("Skipping strange line (no first bracket): "+line);
            }
            int secondBracket = line.indexOf(')', firstBracket+1);
            if (!(secondBracket > firstBracket)) {
                System.err.println("Skipping strange line (no second bracket): "+line);
            }
            String graphemes = line.substring(firstQuote+1, secondQuote);
            String pos = line.substring(firstSpace+1, secondSpace);
            if (pos.equals("nil")) pos = "";
            String allophones = line.substring(firstBracket+1, secondBracket);
            String sampaString = mrpaString2sampaString(allophones);
            toSampa.println(graphemes + " | " + sampaString + " | "+pos);
        }
    }


    
    /**
     * @param args
     */
    public static void main(String[] args)
    throws Exception
    {
        File cmudict = new File("lib/modules/en/us/lexicon/cmu/cmudict-0.4.scm");
        if (!cmudict.exists())
            throw new IllegalStateException("This program should be called from the MARY base directory.");
        File extensions = new File("lib/modules/en/us/lexicon/cmu/cmudict_extensions.scm");
        File cmudictSampa = File.createTempFile("cmudictSampa", ".txt");
        
        // Convert to SAMPA text dictionary
        System.err.println("Converting dictionary to MARY text format...");
        mrpa2sampa = new HashMap<String, String>();
        fillSampaMap();
        usSampa = AllophoneSet.getAllophoneSet("lib/modules/en/us/lexicon/allophones.en_US.xml");
        
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(cmudict), "ASCII"));
        PrintWriter toSampa = new PrintWriter(cmudictSampa, "UTF-8");
        convertToSampa(br, toSampa);
        br.close();
        br = new BufferedReader(new InputStreamReader(new FileInputStream(extensions), "ASCII"));
        convertToSampa(br, toSampa);
        br.close();
        toSampa.close();
        System.err.println("...done!");
        System.err.println();

        // Compress into FST
        System.err.println("Compressing into FST:");
        List<String> testGraphemes = new ArrayList<String>();
        List<String> testAllophones = new ArrayList<String>();
        List<String> testPos = new ArrayList<String>();
        int N = 100; // every N'th entry is put into tests...
        int n = 0;
        System.err.println(" - aligning graphemes and allophones...");
        br = new BufferedReader(new InputStreamReader(new FileInputStream(cmudictSampa), "UTF-8"));
        AlignerTrainer at = new AlignerTrainer(false, true);
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\s*\\|\\s*");
            String graphemes = parts[0];
            String allophones = parts[1];
            String pos = (parts.length > 2 && parts[2].length()>0) ? "("+parts[2]+")" : null;
            String[] graphemeList = new String[graphemes.length()];
            for (int i=0, len=graphemeList.length; i<len; i++) {
                graphemeList[i] = graphemes.substring(i, i+1);
            }
            String[] allophoneList = allophones.split(" ");
            // preserve space between allophones:
            for (int i=1, max=allophoneList.length; i<max; i++) {
                allophoneList[i] = " "+allophoneList[i];
            }
            at.addAlreadySplit(graphemeList, allophoneList, pos);
            n++;
            if (n == N) {
                testGraphemes.add(graphemes);
                testAllophones.add(allophones);
                testPos.add(pos);
                n = 0;
            }
        }
        br.close();
        // make some alignment iterations
        for ( int i = 0 ; i < 4 ; i++ ){
            System.err.println("     iteration " + (i+1));
            at.alignIteration();
        }
        System.err.println(" - entering alignments in trie...");
        TransducerTrie t = new TransducerTrie();
        for (int i = 0, size = at.lexiconSize(); i<size; i++){
            t.add(at.getAlignment(i));
            t.add(at.getInfoAlignment(i));
        }
        System.err.println(" - minimizing trie...");
        t.computeMinimization();
        System.err.println(" - writing transducer to disk...");
        String fstLocation = "lib/modules/en/us/lexicon/cmudict.fst";
        File of = new File(fstLocation);
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
        t.writeFST(os,"UTF-8");
        os.flush();
        os.close();

        System.err.println(" - looking up "+testGraphemes.size()+" test words...");
        FSTLookup fst = new FSTLookup(fstLocation);

        for (int i=0, max=testGraphemes.size(); i<max; i++) {
            String key = testGraphemes.get(i);
            String expected = testAllophones.get(i);
            String[] result = fst.lookup(key);
            if (testPos.get(i) != null) {
                String key2 = key +testPos.get(i);
                String[] result2 = fst.lookup(key2);
                if (!expected.equals(result2[0]))
                    System.err.println("    "+key2+" -> "+ Arrays.toString(result2)+ " (expected: "+expected+")");
                // in addition, expected should be one of the results of a lookup without pos
                boolean found = false;
                for (String r : result) {
                    if (expected.equals(r)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    System.err.println("    "+key+" -> "+ Arrays.toString(result)+ " (expected: "+expected+")");
            } else {
                if (!expected.equals(result[0]))
                    System.err.println("    "+key+" -> "+ Arrays.toString(result)+ " (expected: "+expected+")");
            }
        }
        System.err.println("...done!");
        System.err.println();
        at = null;
        t = null;
        fst = null;
        System.gc();
        
        // Train LTS
        System.err.println("Training letter-to-sound rules...");
        // initialize trainer 
        LTSTrainer tp = new LTSTrainer(usSampa, Locale.US);

        br = new BufferedReader(new InputStreamReader(new FileInputStream(cmudictSampa), "UTF-8"));
        
        System.err.println(" - reading lexicon...");
        // read lexicon for training
        tp.readLexicon(br, "\\s*\\|\\s*", /*convert to lowercase*/true, /*predict stress*/ true);

        System.err.println(" - aligning...");
        // make some alignment iterations
        for ( int i = 0 ; i < 5 ; i++ ){
            System.err.println("     iteration " + (i+1));
            tp.alignIteration();
            
        }
        System.err.println(" - training decision tree...");
        StringCART st = tp.trainTree(100);
        System.err.println(" - saving...");
        // new MARY cart format:
        MaryCARTWriter mcw = new MaryCARTWriter();
        PrintWriter pw = new PrintWriter("lib/modules/en/us/lexicon/cmudict.lts.tree.txt", "UTF-8");
        mcw.toTextOut(st, pw);
        pw.close();
        mcw.dumpMaryCART(st, "lib/modules/en/us/lexicon/cmudict.lts.tree.binary");
        // old wagon cart format:
        WagonCARTWriter wcw = new WagonCARTWriter();
        wcw.dumpWagonCART(st, "lib/modules/en/us/lexicon/cmudict.lts.wagontree.binary");
        pw = new PrintWriter("lib/modules/en/us/lexicon/cmudict.lts.wagontree.txt", "UTF-8");
        wcw.toTextOut(st, pw);
        pw.close();
        // and, separately, the feature definition.
        pw = new PrintWriter("lib/modules/en/us/lexicon/cmudict.lts.pfeats", "UTF-8");
        st.getFeatureDefinition().writeTo(pw, false);
        pw.close();
        st = null;
        System.err.println(" - loading LTS rules...");
        br = new BufferedReader(new InputStreamReader(new FileInputStream("lib/modules/en/us/lexicon/cmudict.lts.pfeats"), "UTF-8"));
        FeatureDefinition featureDef = new FeatureDefinition(br, false);
        br.close();
        br = new BufferedReader(new InputStreamReader(new FileInputStream("lib/modules/en/us/lexicon/cmudict.lts.wagontree.txt"), "UTF-8"));
        st = new StringCART(br, featureDef, featureDef.getFeatureIndex("target"));
        TrainedLTS lts = new TrainedLTS(usSampa, Locale.US, st);

        System.err.println(" - looking up "+testGraphemes.size()+" test words...");

        int max = testGraphemes.size();
        int correct = 0;
        for (int i=0; i<max; i++) {
            String key = testGraphemes.get(i);
            String expected = testAllophones.get(i);
            String result = lts.syllabify(lts.predictPronunciation(key));
            if (!expected.equals(result))
                System.err.println("    "+key+" -> "+ result + " (expected: "+expected+")");
            else 
                correct++;
        }
        System.err.println("   for "+correct+" out of "+max+" prediction is identical to lexicon entry.");
        System.err.println("...done!");
        System.err.println();

    }


}
