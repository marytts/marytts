/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.modules;

import java.util.Iterator;
import java.util.List;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;

/**
 * A link to the synthesis part of festival.
 *
 * @author Marc Schr&ouml;der
 */

public class FreeTTS2FestivalUtt extends InternalModule
{
    public static final String UTTMARKER = "===Utterance===";
    public static final String VOICEMARKER = "voice=";
    
    public FreeTTS2FestivalUtt() throws Exception
    {
        super("FreeTTS2FestivalUtt",
              MaryDataType.get("FREETTS_ACOUSTPARAMS"),
              MaryDataType.get("FESTIVAL_UTT")
              );
    }


    public MaryData process(MaryData d)
    {
    	StringBuffer buf = new StringBuffer();
    	List utts = d.getUtterances();
    	for (Iterator it = utts.iterator(); it.hasNext(); ) {
            Utterance utt = (Utterance) it.next();
            buf.append(convertUtt(utt));
    	}
    	
    	MaryData result = new MaryData(outputType());
    	result.setPlainText(buf.toString());
    	return result;
    }

    /**
     * Convert one utterance from FreeTTS representation to a string in FESTIVAL_UTT
     * format, i.e. as a sequence of Festival Relation files.
     * @param utt the FreeTTS Utterance object ot be converted.
     * @return a String representing utt in FESTIVAL_UTT format.
     */
    public String convertUtt(Utterance utt) {
        StringBuffer buf = new StringBuffer();
        buf.append("===Utterance===\n");
        buf.append(VOICEMARKER + FreeTTSVoices.getMaryVoice(utt.getVoice()).getName() + "\n");
        // Segment relation for this utterance
        buf.append("==Segment==\n");
        buf.append("#\n");
        Relation segmentRelation = utt.getRelation(Relation.SEGMENT);
        assert segmentRelation != null;
        Item segmentItem = segmentRelation.getHead();
        while (segmentItem != null) {
        	float endInSeconds = segmentItem.getFeatures().getFloat("end");
        	String segmentString = segmentItem.toString();
        	buf.append(String.valueOf(endInSeconds) + " 100 " + segmentString + "\n");
        	segmentItem = segmentItem.getNext();
        }
        
        // Target relation for this utterance
        buf.append("==Target==\n");
        buf.append("#\n");
        Relation targetRelation = utt.getRelation(Relation.TARGET);
        assert targetRelation != null;
        Item targetItem = targetRelation.getHead();
        while(targetItem != null){
        	float posInSeconds = targetItem.getFeatures().getFloat("pos");
        	float f0Value = targetItem.getFeatures().getFloat("f0");
        	buf.append(String.valueOf(posInSeconds) + " 100 " + String.valueOf(f0Value) + "\n");
        	targetItem = targetItem.getNext();
        }
        // Syllable Word IntEvent and Phrase relations for this utterance
        buf.append("==Syllable==\n");
        buf.append("#\n");
        Relation syllStrucRelation = utt.getRelation(Relation.SYLLABLE_STRUCTURE);
        assert syllStrucRelation != null;
        StringBuffer word = new StringBuffer();
        word.append("==Word==\n");
        word.append("#\n");
        StringBuffer iE = new StringBuffer();
        iE.append("==IntEvent==\n");
        iE.append("#\n");
        Item syllStrucItem = syllStrucRelation.getHead();
        while (syllStrucItem != null){
        	Item syllItem = syllStrucItem.getDaughter();
        	float end = 0;
        	while(syllItem != null){
        		end = syllItem.getLastDaughter().getFeatures().getFloat("end");
        		StringBuffer syllable = new StringBuffer();
        		Item segItem = syllItem.getDaughter();
        		while(segItem != null){
        			syllable.append(segItem.toString());
        			segItem = segItem.getNext();
        		}
        		String stress = syllItem.getFeatures().getString("stress");
        		buf.append(String.valueOf(end) + " 100 "+ syllable.toString() +" ; stress " + stress + "\n");
                if(syllItem.getFeatures().isPresent("accent")){
                    iE.append(String.valueOf(end) + " 100 " + syllItem.getFeatures().getString("accent") + "\n");
                }
        		if(syllItem.getFeatures().isPresent("endtone")){
        			iE.append(String.valueOf(end) + " 100 " +syllItem.getFeatures().getString("endtone") + "\n");
        		}
        		syllItem = syllItem.getNext();
        	}
        	word.append(String.valueOf(end) + " 100 " + syllStrucItem.toString() + "\n");
        	syllStrucItem = syllStrucItem.getNext();
        }
        buf.append(word.toString());
        buf.append(iE.toString());
        buf.append("==Phrase==\n");
        buf.append("#\n");
        Relation phraseRelation = utt.getRelation(Relation.PHRASE);
        assert phraseRelation != null;
        Item phraseItem = phraseRelation.getHead();
        while (phraseItem != null) {
            float end = 0;
            int phraseBreak;
            if (phraseItem.getFeatures().getString("name").equals("BB")) {
                phraseBreak = 4; // major IP break
            } else if (phraseItem.getFeatures().getString("name").equals("B")) {
                phraseBreak = 3; // minor ip break
            } else {
                logger.debug("Unexpected phrase name: '" + phraseItem.getFeatures().getString("name") + "'");
                phraseBreak = 1; // Fallback: word break
            }
            // now loop over the word daughters of this phrase item:
            Item wordItemInPhrase = phraseItem.getDaughter();
            while (wordItemInPhrase != null) {
                Item next = wordItemInPhrase.getNext();
                Item lastSegmentItem = wordItemInPhrase.findItem("R:SylStructure.daughtern.daughtern");
                if (lastSegmentItem != null) {
                    end = lastSegmentItem.getFeatures().getFloat("end");
                    int thisBreak = 1; 
                    if (next == null) {
                        thisBreak = phraseBreak;
                    }
                    buf.append(end + " 100 " + thisBreak + "\n");
                }
                wordItemInPhrase = next;
            }
            phraseItem = phraseItem.getNext();
        }
        return buf.toString();
    }
    	
        /*
        String defaultFestivalVoiceCmd = "voice_german_de1_os"; // female
        if (mbrola.startsWith("; speaker=male"))
            defaultFestivalVoiceCmd = "voice_german_de2_os"; // male
        festivalSynthesise(basename, defaultFestivalVoiceCmd);

        AudioInputStream sound =
            AudioSystem.getAudioInputStream(new File(System.getProperty("mary.base") +
                                                     File.separator + "tmp" +
                                                     File.separator + basename +
                                                     ".wav"));
        MaryData result = new MaryData(MaryDataType.get("AUDIO"));
        result.readFrom(sound, null);
        return result; 

    private String writeRelationFiles(Document doc, String mbrola)
        throws IOException, Exception
    {
        Pattern targetPattern = Pattern.compile("\\(([0-9]+),([0-9]+)\\)");
        ArrayList symbols = new ArrayList();
        ArrayList endTimes = new ArrayList();
        ArrayList targets = new ArrayList();
        ArrayList targetTimes = new ArrayList();

        // Read mbrola data into lists:
        StringTokenizer lines = new StringTokenizer(mbrola, "\n");
        float endTime = 0;
        while(lines.hasMoreTokens()) {
            String line = lines.nextToken();
            // Ignore lines starting with ';' (comments) and '#' (flush symbol)
            if (!line.startsWith(";") && !line.startsWith("#")) {
                StringTokenizer parts = new StringTokenizer(line);
                String symbol = null;
                if (parts.hasMoreTokens()) {
                    symbol = parts.nextToken();
                }
                int durMS = 0;
                if (parts.hasMoreTokens()) {
                    String helper = parts.nextToken();
                    try {
                        durMS = Integer.parseInt(helper);
                    } catch (NumberFormatException e) {}
                }
                float durS = durMS / (float)1000;
                endTime += durS;
                if (symbol != null && durMS != 0) { // valid entry
                    symbols.add(symbol);
                    endTimes.add(new Float(endTime));
                }
                if (parts.hasMoreTokens()) { // there is a target
                    Matcher m = targetPattern.matcher(parts.nextToken());
                    m.find();
                    String helper = m.group(1);
                    int percent = 0;
                    try {
                        percent = Integer.parseInt(helper);
                    } catch (NumberFormatException e) {}
                    helper = m.group(2);
                    int f0 = 0;
                    try {
                        f0 = Integer.parseInt(helper);
                    } catch (NumberFormatException e) {}
                    if (f0 != 0) { // valid target
                        targets.add(new Integer(f0));
                        targetTimes.add(new Float(endTime - durS +
                                                  (percent/(float)100 * durS)));
                    }
                }
            }
        }

        // Write the Segment relation file:
        String relationsDirectory = System.getProperty("mary.base") +
            File.separator + "tmp";
        File segmentFile = File.createTempFile("mary", ".Segment",
                                               new File(relationsDirectory));
        PrintWriter segmentWriter = new PrintWriter(new FileWriter(segmentFile));
        segmentWriter.println("#");
        String basename = segmentFile.getName();
        basename = basename.substring(0, basename.indexOf(".Segment"));
        PrintWriter syllableWriter =
            new PrintWriter(new FileWriter(relationsDirectory + File.separator +
                                           basename + ".Syllable"));
        syllableWriter.println("#");
        PrintWriter wordWriter =
            new PrintWriter(new FileWriter(relationsDirectory + File.separator +
                                           basename + ".Word"));
        wordWriter.println("#");
        PrintWriter intEventWriter =
            new PrintWriter(new FileWriter(relationsDirectory + File.separator +
                                           basename + ".IntEvent"));
        intEventWriter.println("#");
        PrintWriter phraseWriter =
            new PrintWriter(new FileWriter(relationsDirectory + File.separator +
                                           basename + ".Phrase"));
        phraseWriter.println("#");
        PrintWriter targetWriter =
            new PrintWriter(new FileWriter(relationsDirectory + File.separator +
                                           basename + ".Target"));
        targetWriter.println("#");

        // Segment:
        for (int i = 0; i < symbols.size(); i++) {
            segmentWriter.println(endTimes.get(i) + " 100 " + symbols.get(i));
            //            System.err.println("Segment: " + endTimes.get(i) + " 100 " + symbols.get(i));
        }
        segmentWriter.close();

        // Target:
        for (int i = 0; i < targets.size(); i++) {
            targetWriter.println(targetTimes.get(i) + " 100 " + targets.get(i));
            //            System.err.println("Target: " + targetTimes.get(i) + " 100 " + targets.get(i));
        }
        targetWriter.close();

        // Word, Syllable, Phrase, and IntEvent
        TreeWalker tw = ((DocumentTraversal)doc).
            createTreeWalker(doc, NodeFilter.SHOW_ELEMENT,
                             new RENodeFilter("(t|boundary)"), false);
        int i = 0; // index in 'symbols' list
        Element e = null;
        int lastSymbolInSyllable = -1; // index i for last symbol in current syllable
        int phraseNumber = 0;
        while ((e = (Element)tw.nextNode()) != null) {
            if (e.getTagName().equals(MaryXML.TOKEN) && e.hasAttribute("sampa")) {
                String sampa = e.getAttribute("sampa");
                int j = 0; // index in sampa
                while (j < sampa.length() && i < symbols.size()) {
                    // one syllable:
                    StringBuffer syllable = new StringBuffer();
                    lastSymbolInSyllable = -1;
                    char sylStress = '0';
                    if (sampa.charAt(j) == '\'' || // primary stress
                        sampa.charAt(j) == ',') { // secondary stress
                        sylStress = '1';
                        j++;
                    }
                    while (j < sampa.length() && i < symbols.size() &&
                           sampa.charAt(j) != '-' && sampa.charAt(j) != '_') {
                        // Now the segments as in list 'symbols'
                        String symbol = (String) symbols.get(i);
                        //System.err.println("Now looking at symbol '" + symbol +
                        //                 "', sampa '" + sampa + "', j = " + j);
                        if (sampa.startsWith(symbol, j)) { // simple match
                            j += symbol.length();
                            syllable.append(symbol);
                            lastSymbolInSyllable = i; // last one found so far
                            i++;
                        } else if (symbol.equals("_") && sampa.startsWith("?", j) ||
                                   symbol.equals("R") && sampa.startsWith("r", j) ||
                                   symbol.equals("z") && sampa.startsWith("D", j) ||
                                   symbol.equals("s") && sampa.startsWith("T", j) ||
                                   symbol.equals("n") && sampa.startsWith("=m", j) ||
                                   symbol.equals("n") && sampa.startsWith("=n", j) ||
                                   symbol.equals("n") && sampa.startsWith("=N", j) ||
                                   symbol.equals("l") && sampa.startsWith("=l", j) ||
                                   symbol.equals("6") && sampa.startsWith("=6", j) ||
                                   symbol.equals("i:") && sampa.startsWith("i", j) ||
                                   symbol.equals("y:") && sampa.startsWith("y", j) ||
                                   symbol.equals("e:") && sampa.startsWith("e", j) ||
                                   symbol.equals("E:") && sampa.startsWith("E", j) ||
                                   symbol.equals("2:") && sampa.startsWith("2", j) ||
                                   symbol.equals("u:") && sampa.startsWith("u", j) ||
                                   symbol.equals("o:") && sampa.startsWith("o", j) ||
                                   symbol.equals("E") && sampa.startsWith("{", j) ||
                                   symbol.equals("a") && sampa.startsWith("A", j) ||
                                   symbol.equals("9^") && sampa.startsWith("9~", j) ||
                                   symbol.equals("E~") && sampa.startsWith("E~", j) ||
                                   symbol.equals("a~") && sampa.startsWith("O~", j) ||
                                   symbol.equals("o~") && sampa.startsWith("o~", j)) {
                            // direct match as well
                            // (this corresponds to segment_list.txt)
                            if (sampa.charAt(j) == '=')
                                j++;
                            j++;
                            if (j < sampa.length() &&
                                (sampa.charAt(j) == ':' || sampa.charAt(j) == '~' ||
                                 sampa.charAt(j) == '^'))
                                j++;
                            syllable.append(symbol);
                            lastSymbolInSyllable = i; // last one found so far
                            i++;
                        } else if (symbol.equals("_")) {
                            // try skipping silence
                            i++;
                        } else {
                            throw new Exception("Cannot align sampa '" + sampa +
                                                "' with mbrola\n" + mbrola +
                                                "\nsampa position = " + j +
                                                ", mbrola position = " + i);
                        }
                    } // while inside syllable
                    if (lastSymbolInSyllable > -1) {
                        syllableWriter.println(endTimes.get(lastSymbolInSyllable) +
                                               " 100 " + syllable + " ; stress " +
                                               sylStress + " ;");
                        //                        System.err.println("Syllable: " + endTimes.get(lastSymbolInSyllable) +
                        //                                               " 100 " + syllable + " ; stress " +
                        //                                               sylStress + " ;");
                    }
                    if (j < sampa.length() &&
                        (sampa.charAt(j) == '-' || sampa.charAt(j) == '_'))
                        j++;
                } // while inside sampa
                // now the end of the last syllable in sampa is also
                // the end of the word
                if (lastSymbolInSyllable > -1) {
                    wordWriter.println(endTimes.get(lastSymbolInSyllable) + " 100 " +
                                       MaryDomUtils.tokenText(e));
                    //                    System.err.println("Word: " + endTimes.get(lastSymbolInSyllable) + " 100 " +
                    //                                       MaryDomUtils.tokenText(e));
                }
                if (e.hasAttribute("accent")) {
                    intEventWriter.println(endTimes.get(lastSymbolInSyllable)
                                           + " 100 " + e.getAttribute("accent"));
                    //                    System.err.println("IntEvent: " + endTimes.get(lastSymbolInSyllable)
                    //                                           + " 100 " + e.getAttribute("accent"));
                }
            } // if token and hasAttribute sampa
            else if (e.getTagName().equals(MaryXML.BOUNDARY)) {
                try {
                    if (Integer.parseInt(e.getAttribute("breakindex")) >= 3) {
                        // a sufficiently large boundary
                        // Put a phrase end here:
                        phraseWriter.println(endTimes.get(lastSymbolInSyllable) +
                                             " 100 B" + phraseNumber);
                        //                        System.err.println("Phrase: " + endTimes.get(lastSymbolInSyllable) +
                        //                                             " 100 " + phraseNumber);
                        phraseNumber++;
                        // A ToBI label?
                        if (e.hasAttribute("tobi")) {
                            intEventWriter.println(endTimes.get(lastSymbolInSyllable)
                                                   + " 100 " + e.getAttribute("tobi"));
                            //                            System.err.println("IntEvent: " + endTimes.get(lastSymbolInSyllable)
                            //                                                   + " 100 " + e.getAttribute("tobi"));
                        }
                    }
                } catch (NumberFormatException ex) {}
            }
        } // for all tokens and boundaries
        syllableWriter.close();
        wordWriter.close();
        phraseWriter.close();
        intEventWriter.close();
        return basename;
    }

 
    private void festivalSynthesise(String basename, String defaultFestVoiceCmd)
    throws IOException
    {
        String festVoiceCmd = defaultFestVoiceCmd;
        festVoiceCmd = "voice_german_de2_os";
        String[] cmdArray = new String[4];
        cmdArray[0] = System.getProperty("mary.base") + File.separator + "bin" +
            File.separator + "festival";
        cmdArray[1] = "--batch";
        cmdArray[2] = "festvox/dfki_test_schroed_ldom.scm";
        cmdArray[3] = "(begin " +
            "(load \"" + System.getProperty("mary.base") + "/src/modules/FreeTTS2FestivalUtt/make_utt.scm\") " +
            "(" + festVoiceCmd + ") " +
            "(set! utt1 (make_utt \"" + System.getProperty("mary.base") + "/tmp\" \"" + basename + "\")) " +
            "(utt.save utt1 \"" + System.getProperty("mary.base") + "/tmp/" + basename + ".utt\") " +
            "(Wave_Synth utt1) " +
            "(utt.save.wave utt1 \"" + System.getProperty("mary.base") + "/tmp/" + basename + ".wav\" \'riff)" +
            ")";
        String workingDirectory = "/local/home/schroed/neca/testRecordings";
        logger.info("Starting Festival with command: " + cmdArray[0] + " " +
                     cmdArray[1] + " " + cmdArray[2] + " in directory " +
                     workingDirectory);
    }
    */



}
