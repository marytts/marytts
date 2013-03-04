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
package marytts.tests.modules;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.Syllabifier;
import marytts.modules.phonemiser.TrainedLTS;

public class CompareLexiconLTS {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println("java marytts.tests.modules.CompareLexiconLTS allophones.xml lts-model.lts lexicon.txt [RemoveTrailingOneFromPhones]");
            System.exit(0);
        }
        String allophoneFile = args[0];
        String ltsFile = args[1];
        String dictFile = args[2];
        boolean myRemoveTrailingOneFromPhones = true;
        if(args.length > 3){
        	myRemoveTrailingOneFromPhones = Boolean.getBoolean(args[3]);
        }
        
        TrainedLTS lts = new TrainedLTS(AllophoneSet.getAllophoneSet(allophoneFile), new FileInputStream(ltsFile), myRemoveTrailingOneFromPhones, new Syllabifier(AllophoneSet.getAllophoneSet(allophoneFile),
        		myRemoveTrailingOneFromPhones));

        int count = 0;
        int correct = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dictFile), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().startsWith("#")) {
                continue;
            }
            String[] parts = line.split(" ");
            if (parts.length < 2) {
                continue;
            }
            count++;
            String grapheme = parts[0];
            String lexPhoneme = parts[1];
            String lexPhonemeStripped = lexPhoneme.replaceAll("[-']+", "");
            String pron = lts.predictPronunciation(grapheme);
            String syl = lts.syllabify(pron);
            String sylStripped = syl.replaceAll("[-' ]+", "");
            if (lexPhonemeStripped.equals(sylStripped)) {
                correct++;
            } else {
                String sylStrippedSpaces = syl.replaceAll(" ", "");
                System.out.println(grapheme+" "+lexPhoneme+"\t\t# lts: "+sylStrippedSpaces);
            }
            
        }
    }

}
