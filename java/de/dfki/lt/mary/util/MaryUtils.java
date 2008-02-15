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
package de.dfki.lt.mary.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * A collection of useful static little utility methods.
 *
 * @author Marc Schr&ouml;der
 */

public class MaryUtils {
    private static long lowMemoryThreshold = -1;
    private static Timer maintenanceTimer = new Timer(true); // a daemon timer which will not prohibit system exits.
    
    /**
     * Create a map from an Object array which contains paired entries
     * (key, value, key, value, ....).
     */
    public static Map ArrayToMap(Object[] a) {
        Map m = new HashMap();
        for (int i = 0; i < a.length; i += 2) {
            m.put(a[i], a[i + 1]);
        }
        return m;
    }

    public static Character[] StringToCharacterArray(String s) {
        Character[] cArray = new Character[s.length()];
        for (int i = 0; i < s.length(); i++) {
            cArray[i] = new Character(s.charAt(i));
        }
        return cArray;
    }

    /**
     * Join a collection of strings into a single String object, in the order
     * indicated by the collection's iterator.
     * @param strings a collection containing exclusively String objects
     * @return a single String object
     * @throws IllegalArgumentException if the collection contains an element
     * that is not a String
     */
    public static String joinStrings(Collection strings)
    {
        StringBuffer buf = new StringBuffer();
        if (strings == null) {
            throw new NullPointerException("Received null collection");
        }
        for (Iterator it=strings.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if (obj == null) {
                throw new NullPointerException("Received collection containing null object");
            }
            if (!(obj instanceof String)) {
                throw new IllegalArgumentException("Received collection containing non-String object: "+obj.toString());
            }
            buf.append((String)obj);
        }
        return buf.toString();
    }
    
    /**
     * Check if bytes contains a subsequence identical with pattern,
     * and return the index position.
     * Assumes that pattern.length ^&lt; bytes.length.
     * @param bytes
     * @param pattern
     * @return the index position in bytes where pattern starts, or -1 if bytes
     * does not contain pattern.
     */
    public static int indexOf(byte[] bytes, byte[] pattern)
    {
        if (bytes == null || pattern == null || bytes.length < pattern.length || pattern.length == 0) {
            return -1;
        }
        int j=0; // index in pattern
        int start=-1;
        for (int i=0; i<bytes.length; i++) {
            if (bytes[i] == pattern[j]) {
                if (j==0) start = i;
                j++;
                if (j == pattern.length) return start; // found it!
            } else {
                j=0;
                start = -1;
            }
        }
        return -1;
    }

    
    public static String[] splitIntoSensibleXMLUnits(String s) {
        List result = new ArrayList(); 
        boolean inLetters = false;
        int partstart = 0;
        final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜabcdefghijklmnopqrstuvwxyzäöüß";
        final String splitters = "\n \t\"=<>&;";
        
        for (int i=0; i < s.length(); i++) {
            if (splitters.indexOf(s.charAt(i)) != -1) {
                // s[i] is a splitter character; it will always trigger a break
                // and be on its own
                if (i>partstart) {
                    result.add(s.substring(partstart, i));
                }
                result.add(s.substring(i, i+1));
                partstart = i+1; // to move past the current char
            } else if (letters.indexOf(s.charAt(i)) == -1) {
                // s[i] is non-letter
                if (inLetters && i>partstart) {
                    result.add(s.substring(partstart, i));
                    partstart = i;
                }
                inLetters = false;
            } else {
                // s[i] is letter
                if (!inLetters && i>partstart) {
                    result.add(s.substring(partstart, i));
                    partstart = i;
                }
                inLetters = true;
            }
        }
        return (String[]) result.toArray(new String[0]);
    }
    

    /**
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static int romanToInt(String romanNumber) {
        if (!romanNumber.matches("[IVXLCDM]+")) {
            // Not a roman number
            throw new NumberFormatException(
                "Not a roman number: " + romanNumber);
        }

        String num = "IVXLCDM";
        int[] value = { 1, 5, 10, 50, 100, 500, 1000 };
        int sum = 0;
        for (int i = romanNumber.length() - 1; i >= 0;) {
            int posR = num.indexOf(romanNumber.charAt(i));
            if (i > 0) {
                int posL = num.indexOf(romanNumber.charAt(i - 1));
                if (posR <= posL) {
                    sum += value[posR];
                    i--;
                } else {
                    sum += value[posR] - value[posL];
                    i -= 2;
                }
            } else { // i==0
                sum += value[posR];
                i--;
            }
        }
        // So now <code>sum</code> is the resulting number.
        return sum;
    }

    /**
     * Read from Reader until end of file and return the content as one single String.
     */
    public static String getContentAsString(Reader r) throws IOException {
        BufferedReader br = new BufferedReader(r);
        StringBuffer buf = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            buf.append(line);
            buf.append("\n");
        }
        return buf.toString();
    }
    
    /**
      * Tell whether the string contains a positive or negative percentage
      * delta, i.e., a percentage number with an obligatory + or - sign.
      */
     public static boolean isPercentageDelta(String string) {
         String s = string.trim();
         if (s.length() < 3)
             return false;
         if (s.substring(s.length() - 1).equals("%") && isNumberDelta(s.substring(0, s.length() - 1)))
             return true;
         else
             return false;
     }

     /**
      * For a string containing a percentage delta as judged by
      * <code>isPercentageDelta()</code>, return the numerical value, rounded to
      * an integer.
      * @return the numeric part of the percentage, rounded to an integer, or 0
      * if the string is not a valid percentage delta.
      */
     public static int getPercentageDelta(String string) {
         String s = string.trim();
         if (!isPercentageDelta(s))
             return 0;
         return getNumberDelta(s.substring(0, s.length() - 1));
     }

     /**
      * Tell whether the string contains a positive or negative semitones delta,
      * i.e., a semitones number with an obligatory + or - sign, such as
      * "+3.2st" or "-13.2st".
      */
     public static boolean isSemitonesDelta(String string) {
         String s = string.trim();
         if (s.length() < 4)
             return false;
         if (s.substring(s.length() - 2).equals("st") && isNumberDelta(s.substring(0, s.length() - 2)))
             return true;
         else
             return false;
     }

     /**
      * For a string containing a semitones delta as judged by
      * <code>isSemitonesDelta()</code>, return the numerical value, as a
      * double.
      * @return the numeric part of the semitones delta, or 0
      * if the string is not a valid semitones delta.
      */
     public static double getSemitonesDelta(String string) {
         String s = string.trim();
         if (!isSemitonesDelta(s))
             return 0;
         String num = s.substring(0, s.length() - 2);
         double value = 0;
         try {
             value = Double.parseDouble(num);
         } catch (NumberFormatException e) {
             //logger.warn("Unexpected number value `" + num + "'");
         }
         return value;
     }

     /**
      * Tell whether the string contains a positive or negative number
      * delta, i.e., a number with an obligatory + or - sign.
      */
     public static boolean isNumberDelta(String string) {
         String s = string.trim();
         if (s.length() < 2)
             return false;
         if ((s.charAt(0) == '+' || s.charAt(0) == '-') && isUnsignedNumber(s.substring(1)))
             return true;
         else
             return false;
     }

     /**
      * For a string containing a number delta as judged by
      * <code>isNumberDelta()</code>, return the numerical value, rounded to
      * an integer.
      * @return the numeric value, rounded to an integer, or 0
      * if the string is not a valid number delta.
      */
     public static int getNumberDelta(String string) {
         String s = string.trim();
         if (!isNumberDelta(s))
             return 0;
         double value = 0;
         try {
             value = Double.parseDouble(s);
         } catch (NumberFormatException e) {
             //logger.warn("Unexpected number value `" + s + "'");
         }
         return (int) Math.round(value);

     }

     /**
      * Tell whether the string contains an unsigned semitones expression, such
      * as "12st" or "5.4st".
      */
     public static boolean isUnsignedSemitones(String string) {
         String s = string.trim();
         if (s.length() < 3)
             return false;
         if (s.substring(s.length() - 2).equals("st") && isUnsignedNumber(s.substring(0, s.length() - 2)))
             return true;
         else
             return false;
     }

     /**
      * For a string containing an unsigned semitones expression as judged by
      * <code>isUnsignedSemitones()</code>, return the numerical value as a
      * double.
      * @return the numeric part of the semitones expression, or 0 if the string
      * is not a valid unsigned semitones expression.
      */
     public static double getUnsignedSemitones(String string) {
         String s = string.trim();
         if (!isUnsignedSemitones(s))
             return 0;
         String num = s.substring(0, s.length() - 2);
         double value = 0;
         try {
             value = Double.parseDouble(num);
         } catch (NumberFormatException e) {
             //logger.warn("Unexpected number value `" + num + "'");
         }
         return value;
     }

     /**
      * Tell whether the string contains an unsigned number.
      */
     public static boolean isUnsignedNumber(String string) {
         String s = string.trim();
         if (s.length() < 1)
             return false;
         if (s.charAt(0) != '+' && s.charAt(0) != '-') {
             double value = 0;
             try {
                 value = Double.parseDouble(s);
             } catch (NumberFormatException e) {
                 return false;
             }
             return true;
         }
         return false;
     }

     /**
      * For a string containing an unsigned number as judged by
      * <code>isUnsignedNumber()</code>, return the numerical value, rounded to
      * an integer.
      * @return the numeric value, rounded to an integer, or 0
      * if the string is not a valid unsigned number.
      */
     public static int getUnsignedNumber(String string) {
         String s = string.trim();
         if (!isUnsignedNumber(s))
             return 0;
         double value = 0;
         try {
             value = Double.parseDouble(s);
         } catch (NumberFormatException e) {
             //logger.warn("Unexpected number value `" + s + "'");
         }
         return (int) Math.round(value);
     }

     /**
      * Tell whether the string contains a number.
      */
     public static boolean isNumber(String string) {
         String s = string.trim();
         if (s.length() < 1)
             return false;
         double value = 0;
         try {
             value = Double.parseDouble(s);
         } catch (NumberFormatException e) {
             return false;
         }
         return true;
     }

     /**
      * For a string containing a number as judged by
      * <code>isNumber()</code>, return the numerical value, rounded to
      * an integer.
      * @return the numeric value, rounded to an integer, or 0
      * if the string is not a valid number.
      */
     public static int getNumber(String string) {
         String s = string.trim();
         if (!isNumber(s))
             return 0;
         double value = 0;
         try {
             value = Double.parseDouble(s);
         } catch (NumberFormatException e) {
             //logger.warn("Unexpected number value `" + s + "'");
         }
         return (int) Math.round(value);
     }

    public static Locale string2locale(String localeString)
    {
        Locale locale = null;
        StringTokenizer localeST = new StringTokenizer(localeString, "_");
        String language = localeST.nextToken();
        String country = "";
        String variant = "";
        if (localeST.hasMoreTokens()) {
            country = localeST.nextToken();
            if (localeST.hasMoreTokens()) {
                variant = localeST.nextToken();
            }
         }
        locale = new Locale(language, country, variant);
        return locale;
    }

    /**
     * Test for lax Locale equality. More precisely, returns true if
     * (a) both are equal; (b) general only specifies language, and
     * specific has the same language; (c) general specifies language and
     * country, and specific has the same language and country. Else returns false.
     */
    public static boolean subsumes(Locale general, Locale specific)
    {
        if (general == null || specific == null) return false;
        if (general.equals(specific)) return true;
        else if (general.getVariant().equals("")) {
            if (general.getCountry().equals("")) {
                if (general.getLanguage().equals(specific.getLanguage())) return true;
            } else {
                if (general.getLanguage().equals(specific.getLanguage())
                && general.getCountry().equals(specific.getCountry())) return true;
            }
        }
        return false;
    }

    /**
     * Determine the amount of available memory. "Available" memory is calculated as
     * <code>(max - total) + free</code>.
     * @return the number of bytes of memory available according to the above algorithm.
     */
    public static long availableMemory()
    {
        Runtime rt = Runtime.getRuntime();
        return rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
    }

    /**
     * Verify if the java virtual machine is in a low memory condition.
     * The memory is considered low if less than a specified value is
     * still available for processing. "Available" memory is calculated using
     * <code>availableMemory()</code>.The threshold value can be specified
     * as the Mary property mary.lowmemory (in bytes). It defaults to 20000000 bytes.
     * @return a boolean indicating whether or not the system is in low memory condition.
     */
    public static boolean lowMemoryCondition()
    {
        return availableMemory() < lowMemoryThreshold();
    }

    /**
     * Verify if the java virtual machine is in a very low memory condition.
     * The memory is considered very low if less than half a specified value is
     * still available for processing. "Available" memory is calculated using
     * <code>availableMemory()</code>.The threshold value can be specified
     * as the Mary property mary.lowmemory (in bytes). It defaults to 20000000 bytes.
     * @return a boolean indicating whether or not the system is in very low memory condition.
     */
    public static boolean veryLowMemoryCondition()
    {
        return availableMemory() < lowMemoryThreshold()/2;
    }

    private static long lowMemoryThreshold()
    {
        if (lowMemoryThreshold < 0) // not yet initialised
            lowMemoryThreshold = (long) MaryProperties.getInteger("mary.lowmemory", 10000000);
        return lowMemoryThreshold;
    }

    /**
     * Create a temporary file that will be deleted after a specified number of seconds.
     * The file will be deleted regardless of whether it is still used or not,
     * so be sure to specify a sufficiently large value.
     * @param lifetimeInSeconds the number of seconds after which the file will be
     * deleted -- e.g., 3600 means that the file will be deleted one hour after creation.
     * @return the File that was created.
     * @throws IOException
     */
    public static File createSelfDeletingTempFile(int lifetimeInSeconds)
    throws IOException
    {
        final File f = File.createTempFile("mary", "temp");
        maintenanceTimer.schedule(new TimerTask() {
            public void run() {
                f.delete();
            }
        }, lifetimeInSeconds * 1000l);
        return f;
    }
    
    /**
     * Normalise the Unicode text by mapping "exotic" punctuation characters to
     * "standard" ones.
     * @param unicodeText a string that may include "exotic" punctuation characters
     * @return the string in which exotic characters have been replaced with their
     * closest relative that can be handled by the TTS. 
     */
    public static String normaliseUnicodePunctuation(String unicodeText)
    {
        return normaliseUnicode(unicodeText, punctuationTable);
    }
    protected static final char[] punctuationTable = new char[] {
        8220, '"', // „
        8222, '"', // “
        8211, '-', // -
        '\u2033', '"', // ″
        '\u2036', '"', // ‶
        '\u201c', '"', // “
        '\u201d', '"', // ”
        '\u201e', '"', // „
        '\u201f', '"', // ‟
        '\u00ab', '"', // «
        '\u00bb', '"', // »
        '\u2018', '\'', // ‘
        '\u2019', '\'', // ’
        '\u201a', '\'', // ‚
        '\u201b', '\'', // ‛
        '\u2032', '\'', // ′
        '\u2035', '\'', // ‵
        '\u2039', '\'', // ‹
        '\u203a', '\'', // ›
        '\u2010', '-', // ‐
        '\u2011', '-', // ‑
        '\u2012', '-', // ‒
        '\u2013', '-', // –
        '\u2014', '-', // —
        '\u2015', '-', // ―
    };


    /**
     * Normalise the Unicode text by mapping "exotic" letter characters to
     * "standard" ones. Standard is locale-dependent.
     * For GERMAN: ascii plus german umlauts and ß. For other locales: ascii.
     * @param unicodeText a string that may include "exotic" letter characters
     * @param targetLocale the locale against which normalisation is to be performed. 
     * @return the string in which exotic characters have been replaced with their
     * closest relative that can be handled by the TTS. 
     */
    public static String normaliseUnicodeLetters(String unicodeText, Locale targetLocale)
    {
        if (subsumes(Locale.GERMAN, targetLocale)) {
            return normaliseUnicode(unicodeText, toGermanLetterTable);
        } else {
            String german = normaliseUnicode(unicodeText, toGermanLetterTable);
            return normaliseUnicode(german, germanToAsciiLetterTable);
        }
    }
    protected static final char[] toGermanLetterTable = new char[] {
        '\u00c0', 'A', // À
        '\u00c1', 'A', // Á
        '\u00c2', 'A', // Â
        '\u00c3', 'A', // Ã
        '\u00c5', 'A', // Å
        '\u00c6', 'Ä', // Æ
        '\u00c7', 'C', // Ç
        '\u00c8', 'E', // È
        '\u00c9', 'E', // É
        '\u00ca', 'E', // Ê
        '\u00cb', 'E', // Ë
        '\u00cc', 'I', // Ì
        '\u00cd', 'I', // Í
        '\u00ce', 'I', // Î
        '\u00cf', 'I', // Ï
        '\u00d1', 'N', // Ñ
        '\u00d2', 'O', // Ò
        '\u00d3', 'O', // Ó
        '\u00d4', 'O', // Ô
        '\u00d5', 'O', // Õ
        '\u00d8', 'Ö', // Ø
        '\u00d9', 'U', // Ù
        '\u00da', 'U', // Ú
        '\u00db', 'U', // Û
        '\u00dd', 'Y', // Ý
        '\u00e0', 'a', // à
        '\u00e1', 'a', // á
        '\u00e2', 'a', // â
        '\u00e3', 'a', // ã
        '\u00e5', 'a', // å
        '\u00e6', 'ä', // æ
        '\u00e7', 'c', // ç
        '\u00e8', 'e', // è
        '\u00e9', 'e', // é
        '\u00ea', 'e', // ê
        '\u00eb', 'e', // ë
        '\u00ec', 'i', // ì
        '\u00ed', 'i', // í
        '\u00ee', 'i', // î
        '\u00ef', 'i', // ï
        '\u00f1', 'n', // ñ
        '\u00f2', 'o', // ò
        '\u00f3', 'o', // ó
        '\u00f4', 'o', // ô
        '\u00f5', 'o', // õ
        '\u00f8', 'ö', // ø
        '\u00f9', 'u', // ù
        '\u00fa', 'u', // ú
        '\u00fb', 'u', // û
        '\u00fd', 'y', // ý
        '\u00ff', 'y', // ÿ
    };

    protected static final char[] germanToAsciiLetterTable = new char[] {
        'Ä', 'A',
        'Ö', 'O',
        'Ü', 'U',
        'ä', 'a',
        'ö', 'o',
        'ü', 'u',
        'ß', 's'
    };

    private static String normaliseUnicode(String unicodeText, char[] mappings)
    {
        String result = unicodeText;
        for (int i=0; i<mappings.length; i+=2) {
            result = result.replace(mappings[i], mappings[i+1]);
        }
        return result;
    }

    
    /**
     * Apply the toString() method recursively to this throwable and all its causes.
     * The idea is to get cause information as in printStackTrace() without the stack trace.
     * @param t the throwable to print.
     * @return
     */
    public static String getThrowableAndCausesAsString(Throwable t)
    {
       StringBuffer buf = new StringBuffer();
       buf.append(t.toString());
       if (t.getCause() != null) {
           buf.append("\nCaused by: ");
           buf.append(getThrowableAndCausesAsString(t.getCause()));
       }
       return buf.toString();
    }
    
    /**
     * Given an array of values, compute the median of these values.
     * @param values
     * @return
     */
    public static double median(double[] values)
    {
        double[] a = (double[]) values.clone();
        Arrays.sort(a);
        int len = a.length;
        if (len % 2 == 0) {
            return (a[len/2-1]+a[len/2])/2;
        } else {
            return a[(len-1)/2];
        }
    }

    /**
     * Given an array of values, compute the median of these values.
     * @param values
     * @return
     */
    public static float median(float[] values)
    {
        float[] a = (float[]) values.clone();
        Arrays.sort(a);
        int len = a.length;
        if (len % 2 == 0) {
            return (a[len/2-1]+a[len/2])/2;
        } else {
            return a[(len-1)/2];
        }
    }
    
    /**
     * Given an array of int values, compute the median of these values.
     * @param values
     * @return
     */
    public static int median(int[] values)
    {
        int[] a = (int[]) values.clone();
        Arrays.sort(a);
        int len = a.length;
        if (len % 2 == 0) {
            return (a[len/2-1]+a[len/2])/2;
        } else {
            return a[(len-1)/2];
        }
    }

    /**
     * Given an array of long values, compute the median of these values.
     * @param values
     * @return
     */
    public static long median(long[] values)
    {
        long[] a = (long[]) values.clone();
        Arrays.sort(a);
        int len = a.length;
        if (len % 2 == 0) {
            return (a[len/2-1]+a[len/2])/2;
        } else {
            return a[(len-1)/2];
        }
    }

    /**
     * Compute the mean of all elements in the array. No missing values (NaN) are allowed.
     * @throws IllegalArgumentException if the array contains NaN values. 
     */
    public static double mean(double[] data)
    {
        double mean = 0;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i]))
                throw new IllegalArgumentException("NaN not allowed in mean calculation");
            mean += data[i];
        }
        mean /= data.length;
        return mean;
    }

    /**
     * Compute the mean of all elements in the array. No missing values (NaN) are allowed.
     * @throws IllegalArgumentException if the array contains NaN values. 
     */
    public static float mean(float[] data)
    {
        float mean = 0;
        for (int i=0; i<data.length; i++) {
            if (Float.isNaN(data[i]))
                throw new IllegalArgumentException("NaN not allowed in mean calculation");
            mean += data[i];
        }
        mean /= data.length;
        return mean;
    }
    
    /**
     * Compute the standard deviation of the given data
     * @param data
     * @return
     */
    public static double stdDev(double[] data) {
        // Pseudocode from wikipedia, which cites Knuth:
        // n = 0
        // mean = 0
        // S = 0
        // foreach x in data:
        //   n = n + 1
        //   delta = x - mean
        //   mean = mean + delta/n
        //   S = S + delta*(x - mean)      // This expression uses the new value of mean
        // end for
        // variance = S/(n - 1)
        double mean = 0;
        double S = 0;
        for (int i=0; i< data.length; i++) {
            double delta = data[i] - mean;
            mean += delta / (i+1);
            S += delta * (data[i] - mean);
        }
        return Math.sqrt(S/data.length);
    }

    /**
     * Compute the standard deviation of the given data
     * @param data
     * @return
     */
    public static double stdDev(float[] data) {
        // Pseudocode from wikipedia, which cites Knuth:
        // n = 0
        // mean = 0
        // S = 0
        // foreach x in data:
        //   n = n + 1
        //   delta = x - mean
        //   mean = mean + delta/n
        //   S = S + delta*(x - mean)      // This expression uses the new value of mean
        // end for
        // variance = S/(n - 1)
        double mean = 0;
        double S = 0;
        for (int i=0; i< data.length; i++) {
            double delta = data[i] - mean;
            mean += delta / (i+1);
            S += delta * (data[i] - mean);
        }
        return Math.sqrt(S/data.length);
    }

    //Return true if the operating system is Windows
    // return false for other OSs
    public static boolean isWindows()
    {
        String osName = System.getProperty("os.name");
        osName = osName.toLowerCase();
        
        if (osName.indexOf("windows")!=-1)
            return true;
        else
            return false;
    }
    
    public static void plot(double [] x)
    {
        plot(x, "");
    }
    
    public static void plot(double [] x, String strTitle)
    {
        plot(x, strTitle, false);
    }
    
    public static void plot(double [] x, String strTitle, boolean bAutoClose)
    {
        plot(x, strTitle, bAutoClose, 3000);
    }
    
    public static void plotZoomed(double [] x, String strTitle, double minVal)
    {
        plotZoomed(x, strTitle, minVal, MathUtils.getMax(x));
    }
    
    public static void plotZoomed(double [] x, String strTitle, double minVal, double maxVal)
    {
        plotZoomed(x, strTitle, minVal, maxVal, false);
    }
    
    public static void plotZoomed(double [] x, String strTitle, double minVal, double maxVal, boolean bAutoClose)
    {
        plotZoomed(x, strTitle, minVal, maxVal, bAutoClose, 3000);
    }
    
    public static void plotZoomed(double [] x, String strTitle, double minVal, double maxVal, boolean bAutoClose, int milliSecondsToClose)
    {
        double[] y = null;
        if (x!=null)
        {
            if (minVal>maxVal)
            {
                double tmp = minVal;
                minVal = maxVal;
                maxVal = tmp;
            }
            y = new double[x.length];
            for (int i=0; i<x.length; i++)
            {
                y[i] = x[i];
                if (y[i]<minVal)
                    y[i] = minVal;
                else if (y[i]>maxVal)
                    y[i] = maxVal;
            }
                
            plot(y, strTitle, bAutoClose, milliSecondsToClose);
        }
    }
    
    // Plots the values in x
    // If bAutoClose is specified, the figure is closed after milliSecondsToClose milliseconds
    // milliSecondsToClose: has no effect if bAutoClose is false
    public static void plot(double [] x, String strTitle, boolean bAutoClose, int milliSecondsToClose)
    {
        FunctionGraph graph = new FunctionGraph(400, 200, 0, 1, x);
        JFrame frame = graph.showInJFrame(strTitle, 500, 300, true, false);
        
        if (bAutoClose)
        {
            try { Thread.sleep(milliSecondsToClose); } catch (InterruptedException e) {}
            frame.dispose();
        }
    }
}
