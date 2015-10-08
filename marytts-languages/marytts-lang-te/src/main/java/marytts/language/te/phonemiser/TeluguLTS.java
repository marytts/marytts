/**
 * Copyright 2002-2008 DFKI GmbH.
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
package marytts.language.te.phonemiser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Telugu letter to sound(LTS) module - It converts UTF8 graphemes to IT3 pronunciation
 * 
 * @author sathish, adopted from student work by Jyotsna and Chandu
 */
public class TeluguLTS {

	private HashMap<String, String> UTF8toPhoneSymbols;
	private HashMap<String, String> UTF8toPhoneTypes;
	private ArrayList<String> listPhoneSym;
	private ArrayList<String> listPhoneTypes;

	/**
	 * TeluguLTS constructor
	 * 
	 * @param utf8toit3mapStream
	 *            utf8toit3mapStream
	 * @throws IOException
	 *             IOException
	 */
	public TeluguLTS(InputStream utf8toit3mapStream) throws IOException {
		this.loadPhoneSymbolsAndTypes(utf8toit3mapStream);
	}

	/**
	 * Get it3 pronunciation for a word
	 * 
	 * @param word
	 *            word
	 * @return getStringfromArrayList(listPhoneSym)
	 * @throws IOException
	 *             IOException
	 */
	public String phonemise(String word) throws IOException {
		ArrayList<String> utf8CharList = readUTF8String(word);
		listPhoneSym = new ArrayList<String>();
		listPhoneTypes = new ArrayList<String>();

		Iterator<String> listrun = utf8CharList.iterator();
		while (listrun.hasNext()) {
			String utf8Char = listrun.next();
			String phoneSymbol = UTF8toPhoneSymbols.get(utf8Char);
			String phoneType = UTF8toPhoneTypes.get(utf8Char);
			if (phoneSymbol == null)
				phoneSymbol = getAsciiChar(utf8Char);
			if (phoneType == null)
				phoneType = "#";
			listPhoneSym.add(phoneSymbol);
			listPhoneTypes.add(phoneType);
			// System.out.println(utf8Char+" "+phoneSymbol+" "+phoneType);
		}

		removeUnknownSymbols();
		schwaHandler();
		removeHal();
		syllabify();
		putStressMark();

		return getStringfromArrayList(listPhoneSym);
	}

	/**
	 * Add stress mark on first syllable
	 * 
	 * @return listPhoneSym
	 */
	private ArrayList<String> putStressMark() {
		listPhoneSym.add(0, "'");
		return listPhoneSym;
	}

	/**
	 * Add syllable symbols at proper places
	 */
	private void syllabify() {

		for (int i = 0; i < listPhoneTypes.size(); i++) {
			if (isVowel(i)) {
				boolean isVowelLater = isVowelLater(i);
				boolean isNextSemiCon = isNextSemiConsonant(i);
				if (isVowelLater) {
					if (isNextSemiCon) {
						listPhoneSym.add(i + 2, "-");
						listPhoneTypes.add(i + 2, "SYM");
					} else {
						listPhoneSym.add(i + 1, "-");
						listPhoneTypes.add(i + 1, "SYM");
					}
				}
			}
		}
	}

	/**
	 * Check whether the character is Vowel or not
	 * 
	 * @param pos
	 *            pos
	 * @return true if listPhoneTypes.get(pos).equals("VOW"), false otherwise
	 */
	private boolean isVowel(int pos) {
		if (listPhoneTypes.get(pos).equals("VOW")) {
			return true;
		}
		return false;
	}

	/**
	 * Check whether the word has vowels after given position
	 * 
	 * @param pos
	 *            pos
	 * @return true if listPhoneTypes.get(i).equals("VOW"), false otherwise
	 */
	private boolean isVowelLater(int pos) {
		for (int i = (pos + 1); i < listPhoneTypes.size(); i++) {
			if (listPhoneTypes.get(i).equals("VOW")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * check next position is semiconsonant
	 * 
	 * @param pos
	 *            pos
	 * @return true listPhoneSym.get(pos + 1).equals("n:") || listPhoneSym.get(pos + 1).equals("a:"), false otherwise
	 */
	private boolean isNextSemiConsonant(int pos) {
		if ((pos + 1) >= listPhoneSym.size())
			return false;
		if (listPhoneSym.get(pos + 1).equals("n:") || listPhoneSym.get(pos + 1).equals("a:")) {
			return true;
		}
		return false;
	}

	/**
	 * Get a string from arraylist
	 * 
	 * @param aList
	 *            aList
	 * @return result in string format
	 */
	private String getStringfromArrayList(ArrayList<String> aList) {
		Iterator<String> listrun = aList.iterator();
		StringBuilder result = new StringBuilder();
		while (listrun.hasNext()) {
			result.append(listrun.next());
		}
		return result.toString();
	}

	/**
	 * Hex-decimal representation for a given string
	 * 
	 * @param ch
	 *            ch
	 * @return hex
	 */
	private String toHex4(int ch) {
		String hex = Integer.toHexString(ch).toUpperCase();
		switch (hex.length()) {
		case 3:
			return "0" + hex;
		case 2:
			return "00" + hex;
		case 1:
			return "000" + hex;
		default:
			return hex;
		}
	}

	private void loadPhoneSymbolsAndTypes(InputStream inStream) throws IOException {
		String line;
		BufferedReader bfr = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
		UTF8toPhoneSymbols = new HashMap<String, String>();
		UTF8toPhoneTypes = new HashMap<String, String>();
		while ((line = bfr.readLine()) != null) {
			String[] words = line.split("\\|");
			UTF8toPhoneSymbols.put(words[0], words[1]);
			UTF8toPhoneTypes.put(words[0], words[2]);
		}
		bfr.close();
	}

	public ArrayList<String> readUTF8String(String word) throws IOException {
		CharBuffer cbuf = CharBuffer.wrap(word);
		ArrayList<String> utf8CharList = new ArrayList<String>();
		for (int i = 0; i < cbuf.length(); i++) {
			char ch = cbuf.get(i);
			utf8CharList.add(toHex4((int) ch));
		}
		return utf8CharList;
	}

	public ArrayList<String> readUTF8File(String filename) throws IOException {
		int ch;
		ArrayList<String> utf8CharList = new ArrayList<String>();
		InputStreamReader ins = new InputStreamReader(new FileInputStream(filename), "UTF8");
		while ((ch = ins.read()) >= 0) {
			utf8CharList.add(toHex4(ch));
		}
		return utf8CharList;
	}

	private void printData(String filename) throws IOException {
		ArrayList<String> utf8CharList = readUTF8File(filename);

		Iterator<String> listrun = utf8CharList.iterator();
		while (listrun.hasNext()) {
			String utf8Char = listrun.next();
			String phoneSymbol = UTF8toPhoneSymbols.get(utf8Char);
			String phoneType = UTF8toPhoneTypes.get(utf8Char);
			if (phoneSymbol == null)
				phoneSymbol = "SPACE";
			if (phoneType == null)
				phoneType = "#";
			System.out.println(utf8Char + " " + phoneSymbol + " " + phoneType);
		}
	}

	public void makeProperIt3(String filename) throws IOException {

		ArrayList<String> utf8CharList = readUTF8File(filename);
		ArrayList<String> lPhoneSym = new ArrayList<String>();
		ArrayList<String> lPhoneTypes = new ArrayList<String>();

		Iterator<String> listrun = utf8CharList.iterator();
		while (listrun.hasNext()) {
			String utf8Char = listrun.next();
			String phoneSymbol = UTF8toPhoneSymbols.get(utf8Char);
			String phoneType = UTF8toPhoneTypes.get(utf8Char);
			if (phoneSymbol == null)
				phoneSymbol = getAsciiChar(utf8Char);
			if (phoneType == null)
				phoneType = "#";
			lPhoneSym.add(phoneSymbol);
			lPhoneTypes.add(phoneType);
			System.out.println(utf8Char + " " + phoneSymbol + " " + phoneType);
		}

		printArrayList(lPhoneSym);
		printArrayList(lPhoneTypes);

		lPhoneSym = schwaHandler(lPhoneSym, lPhoneTypes);
		lPhoneSym = removeHal(lPhoneSym, lPhoneTypes);
		printArrayList(lPhoneSym);

	}

	/**
	 * Remove Halanth from telugu characters
	 * 
	 * @param lPhoneSym
	 *            lPhoneSym
	 * @param lPhoneTypes
	 *            lPhoneTypes
	 * @return lPhoneSym
	 */
	private ArrayList<String> removeHal(ArrayList<String> lPhoneSym, ArrayList<String> lPhoneTypes) {

		for (int i = 0; i < lPhoneTypes.size(); i++) {
			if (lPhoneTypes.get(i).equals("HLT")) {
				lPhoneTypes.remove(i);
				lPhoneSym.remove(i);
				i--;
			}
		}
		return lPhoneSym;
	}

	/**
	 * Remove Halanth from telugu characters
	 */
	private void removeHal() {

		for (int i = 0; i < listPhoneTypes.size(); i++) {
			if (listPhoneTypes.get(i).equals("HLT")) {
				listPhoneTypes.remove(i);
				listPhoneSym.remove(i);
				i--;
			}
		}
	}

	/**
	 * Remove unknown symbols
	 */
	private void removeUnknownSymbols() {

		for (int i = 0; i < listPhoneTypes.size(); i++) {
			if (listPhoneTypes.get(i).equals("#")) {
				listPhoneTypes.remove(i);
				listPhoneSym.remove(i);
				i--;
			}
		}
	}

	/**
	 * get ascii values for utf8 characters
	 * 
	 * @param utf8Char
	 *            utf8Char
	 * @return Character.toString(dec)
	 */
	private String getAsciiChar(String utf8Char) {
		int intValue = Integer.parseInt(utf8Char, 16);
		char dec = (char) intValue;
		return Character.toString(dec);
	}

	/**
	 * Schwa handler for telugu
	 * 
	 * @param lPhoneSym
	 *            lPhoneSym
	 * @param lPhoneTypes
	 *            lPhoneTypes
	 * @return lPhoneSym
	 */
	private ArrayList<String> schwaHandler(ArrayList<String> lPhoneSym, ArrayList<String> lPhoneTypes) {

		String prev, next;
		for (int i = 0; i < lPhoneTypes.size(); i++) {
			prev = lPhoneTypes.get(i);
			if ((i + 1) < lPhoneTypes.size()) {
				next = lPhoneTypes.get(i + 1);
			} else {
				next = lPhoneTypes.get(i);
			}

			if ((prev.equals("CON") && next.equals("CON")) || (prev.equals("CON") && next.equals("SYM"))
					|| (prev.equals("CON") && next.equals("#"))) {
				lPhoneTypes.add(i + 1, "VOW");
				lPhoneSym.add(i + 1, "a");
			}
		}
		return lPhoneSym;
	}

	/**
	 * Schwa handler for telugu
	 */
	private void schwaHandler() {

		String prev, next;
		for (int i = 0; i < listPhoneTypes.size(); i++) {

			// if(listPhoneTypes.get(i) == null) continue;
			// if(listPhoneTypes.get(i+1) == null) ;
			prev = listPhoneTypes.get(i);

			if ((i + 1) < listPhoneTypes.size()) {
				next = listPhoneTypes.get(i + 1);
			} else {
				next = listPhoneTypes.get(i);
			}

			if ((prev.equals("CON") && next.equals("CON")) || (prev.equals("CON") && next.equals("SYM"))
					|| (prev.equals("CON") && next.equals("#"))) {
				listPhoneTypes.add(i + 1, "VOW");
				listPhoneSym.add(i + 1, "a");
			}

		}
	}

	/**
	 * print array list
	 * 
	 * @param aList
	 *            aList
	 */
	private void printArrayList(ArrayList<String> aList) {
		Iterator<String> listrun = aList.iterator();
		System.out.println();
		while (listrun.hasNext()) {
			// System.out.print(listrun.next()+" ");
			System.out.print(listrun.next());
		}
		System.out.println();
	}

	/**
	 * @param args
	 *            args
	 * @throws IOException
	 *             IOException
	 */
	public static void main(String[] args) throws IOException {

		TeluguLTS utf8r = new TeluguLTS(new FileInputStream("~/openmary/lib/modules/te/lexicon/UTF8phone.te.list"));

		// utf8r.makeProperIt3("/home/sathish/Desktop/telugu-utf8-txt.done.data");
		// String nameString = "\u0C05\u0C38\u0C1F\u0C08\u0C05\u0C37";
		// PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("telugu-utf.txt"), "UTF8"));
		// pw.print(nameString);
		// pw.flush();
		// pw.close();

		System.out.println("Result : " + utf8r.phonemise("ప్రకారం"));
	}

}
