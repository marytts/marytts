/**
 * Copyright 2000-2008 DFKI GmbH.
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
package marytts.language.de.infostruct;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;


public class Stemmer {

    private String vowels = "aeiouyäöü";
    private String sEndings = "bdfghklmnrt";
    private String stEndings = "bdfghklmnt";
    private Logger logger;

    public Stemmer() {
        logger = MaryUtils.getLogger("Stemmer");
    }

    //extract the stemm of the original word
    public String findStem(String word) {
        word = prepareWord(word);
        // print prepared word

        int posR1 = calculateR(word, 0, 2); //(word, 0);
        int posR2 = calculateR(word, posR1, 0);
        // R1 is the region after the first non-vowel following a vowel
        String r1 = word.substring(posR1, word.length());
        // R2 is the region after the first non-vowel following a vowel in R1
        String r2 = word.substring(posR2, word.length());
        // print R1 and R2

        String wordWithoutR1 = word.substring(0, posR1);
        // print beginn of word

        //begin the cut-endings operation
        String r1AfterStep1 = step1(word, r1);
        // print cutted word after step1

        String r1AfterStep2 = step2(wordWithoutR1 + r1AfterStep1, r1AfterStep1);
        // print cutted word after step2

        //result = step3(wordWithoutR1+result,result,r2);
        //System.out.println("step3: " +wordWithoutR1 + result);
        //	return wordWithoutR1+result;
        String result = finalAdjust(wordWithoutR1 + r1AfterStep2);
        logger.debug(word + " => " + wordWithoutR1 + "|" + r1 +
          " => (step1)" + wordWithoutR1 + r1AfterStep1 +
          " => (step2)" + wordWithoutR1 + r1AfterStep2 +
          " => " + result);
        return result;
    } //findStemm

    private String prepareWord(String word) {
        String result = Character.toString(word.charAt(0));
        for (int i = 1; i < word.length(); i++) {
            switch (word.charAt(i)) {
                case 'u' :
                    {
                        if ((i != (word.length() - 1))
                            && (vowels.indexOf(word.charAt(i - 1)) > -1)
                            && (vowels.indexOf(word.charAt(i + 1)) > -1)) {
                            result = result + "U";
                        } //if
                        else
                            result = result + word.charAt(i);
                        break;
                    } //case
                case 'y' :
                    {
                        if ((i != (word.length() - 1))
                            && (vowels.indexOf(word.charAt(i - 1)) > -1)
                            && (vowels.indexOf(word.charAt(i + 1)) > -1)) {
                            result = result + "Y";
                        } //if	
                        else
                            result = result + word.charAt(i);
                        break;
                    } //case
                case 'ß' :
                    {
                        result = result + "ss";
                        break;
                    } //case
                default :
                    {
                        result = result + word.charAt(i);
                        break;
                    } //default
            } //switch
        } //for
        return result;
    } //prepareWord

    /**
     * Try to cut off a simple suffix, corresponding to adjective/noun inflection endings.
     * @param word
     * @param r1
     * @return
     */
    private String step1(String word, String r1) {
        String result = r1;
        if (r1.endsWith("ern")) {
            result = result.substring(0, result.length() - 3);
        } else if (r1.endsWith("er") || r1.endsWith("es") || r1.endsWith("em") || r1.endsWith("en")) {
            result = result.substring(0, result.length() - 2);
        } else if (r1.endsWith("e") || (r1.endsWith("s") && (sEndings.indexOf(word.charAt(word.length() - 2)) > -1))) {
            result = result.substring(0, result.length() - 1);
        } //else if
        return result;
    } //step1

    /**
     * Try to cut off a longer suffix, taking into account Komparativ and Superlativ forms
     * of adjectives.
     * @param word
     * @param r
     * @return
     */
    private String step2(String word, String r) {
        String result = r;
        if (r.endsWith("est")) {
            result = result.substring(0, result.length() - 3);
        } else if (
            r.endsWith("er")
                || r.endsWith("en")
                || (r.endsWith("st")
                    && (stEndings.indexOf(word.charAt(word.length() - 3)) > -1)
                    && (word.length() - 3 > 2))) {
            result = result.substring(0, result.length() - 2);
        } //else if
        return result;
    } //step2

    /**
     * Try to cut of adjective- and noun-building suffixes.
     * @param word
     * @param r1
     * @param r2
     * @return
     */
    private String step3(String word, String r1, String r2) {
        String result = r1;
        if (r2.endsWith("isch") && (r1.charAt(r1.length() - 4) != 'e')) {
            result = result.substring(0, result.length() - 4);
        } else if (r2.endsWith("lich") || r2.endsWith("heit")) {
            if (r1.endsWith("erlich") || r1.endsWith("enlich")) {
                result = result.substring(0, result.length() - 6);
            } else
                result = result.substring(0, result.length() - 4);
        } else if (r2.endsWith("keit")) {
            if (r2.endsWith("lichkeit")) {
                result = result.substring(0, result.length() - 8);
            } else if (r2.endsWith("igkeit")) {
                result = result.substring(0, result.length() - 6);
            } else
                result = result.substring(0, result.length() - 4);
        } else if (r2.endsWith("end") || r2.endsWith("ung")) {
            if ((r2.substring(0, r2.length() - 3)).endsWith("ig") && (r1.charAt(r1.length() - 5) != 'e')) {
                result = result.substring(0, result.length() - 5);
            } else
                result = result.substring(0, result.length() - 3);
        } else if ((r2.endsWith("ig") || r2.endsWith("ik")) && r1.charAt(r1.length() - 3) != 'e') {
            result = result.substring(0, result.length() - 2);
        } //else if
        return result;
    } // step3

    /**
     * In the given word, and starting from a given index, find the first consonant
     * after a vowel sequence and return the position after that consonant, or
     * constraint, whichever is larger.  
     * @param word the string in which to search for a V-C pattern
     * @param index index in word from which to start
     * @param constraint minimum return value
     * @return the position after the consonant, or constraint, whichever is larger.
     */
    private int calculateR(String word, int index, int constraint) {
        int result = index;
        for (int i = index; i < word.length(); i++) {
            if (vowels.indexOf(word.charAt(i)) > -1) {
                for (int j = i; j < word.length(); j++) {
                    if ((vowels.indexOf(word.charAt(j)) == -1) && j > 1) {
                        result = ++j;
                        if (result > constraint) {
                            break;
                        } else
                            result = constraint;
                    } //if 
                } // for:j       
                break;
            } //if
        } // for:i
        return result;
    } //calculateR

    private String finalAdjust(String word) {

        String result = Character.toString(word.charAt(0));

        for (int i = 1; i < word.length(); i++) {

            switch (word.charAt(i)) {
                case 'U' :
                    {
                        result = result + "u";
                        break;
                    } //case
                case 'Y' :
                    {
                        result = result + "y";
                        break;
                    } //case
                case 'ä' :
                    {
                        result = result + "a";
                        break;
                    } //case
                case 'ö' :
                    {
                        result = result + "o";
                        break;
                    } //case
                case 'ü' :
                    {
                        result = result + "u";
                        break;
                    } //case

                default :
                    {
                        result = result + word.charAt(i);
                        break;
                    } //default
            } //switch

        } //for
        //result = result.substring(0,word.length());
        return result;
    } //finalAdjust        

    public static void main(String args[]) {
        Stemmer stemmer = new Stemmer();
        System.out.println("RESULT:   " + stemmer.findStem(args[0]));
    } //main

} //Stemmer
