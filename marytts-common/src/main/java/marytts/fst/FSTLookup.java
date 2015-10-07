/**
 * Copyright 2003-2007 DFKI GmbH.
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
package marytts.fst;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a finite state transducer lookup.
 * 
 * @author Andreas Eisele
 */
public class FSTLookup {
	// ///////////////////// Static FST repository ////////////////////
	/**
	 * Map "filename encoding" or "filename" to FST.
	 */
	private static Map<String, FST> knownFSTs = new HashMap<String, FST>();

	// //////////////////// An individual FSTLookup class //////////////

	private FST fst;

	/**
	 * Initialise the finite state transducer lookup. This constructor will assume that the file contains a header indicating the
	 * proper encoding.
	 * 
	 * @param fileName
	 *            the name of the file from which to load the FST.
	 * @throws IOException
	 *             if the FST cannot be loaded from the given file.
	 */
	public FSTLookup(String fileName) throws IOException {
		InputStream inStream = new FileInputStream(fileName);
		try {
			init(inStream, fileName);
		} finally {
			inStream.close();
		}
	}

	/**
	 * Initialise the finite state transducer lookup. This constructor will assume that the stream contains a header indicating
	 * the proper encoding.
	 * 
	 * @param inStream
	 *            the stream from which to load the FST.
	 * @param identifier
	 *            an identifier by which the FST lookup can be retrieved.
	 * @throws IOException
	 *             if the FST cannot be loaded from the given file.
	 */
	public FSTLookup(InputStream inStream, String identifier) throws IOException {
		init(inStream, identifier);
	}

	private void init(InputStream inStream, String identifier) throws IOException {
		fst = knownFSTs.get(identifier);
		if (fst == null) {
			fst = new FST(inStream);
			knownFSTs.put(identifier, fst);
		}

	}

	/**
	 * Initialise the finite state transducer lookup. This is a constructor for legacy headerless FST files.
	 * 
	 * @param fileName
	 *            the name of the file from which to load the FST.
	 * @param encoding
	 *            the name of the encoding used in the file (e.g., UTF-8 or ISO-8859-1).
	 * @throws IOException
	 *             if the FST cannot be loaded from the given file.
	 * @throws UnsupportedEncodingException
	 *             if the encoding is not supported.
	 */
	public FSTLookup(String fileName, String encoding) throws IOException, UnsupportedEncodingException {
		InputStream inStream = new FileInputStream(fileName);
		try {
			init(inStream, fileName, encoding);
		} finally {
			inStream.close();
		}
	}

	/**
	 * Initialise the finite state transducer lookup. This is a constructor for legacy headerless FST files.
	 * 
	 * @param inStream
	 *            the stream from which to load the FST.
	 * @param identifier
	 *            an identifier by which the FST lookup can be retrieved.
	 * @param encoding
	 *            the name of the encoding used in the file (e.g., UTF-8 or ISO-8859-1).
	 * @throws IOException
	 *             if the FST cannot be loaded from the given file.
	 * @throws UnsupportedEncodingException
	 *             if the encoding is not supported.
	 */
	public FSTLookup(InputStream inStream, String identifier, String encoding) throws IOException, UnsupportedEncodingException {
		init(inStream, identifier, encoding);
	}

	private void init(InputStream inStream, String identifier, String encoding) throws IOException, UnsupportedEncodingException {
		String key = identifier + " " + encoding;
		fst = knownFSTs.get(key);
		if (fst == null) {
			fst = new FST(inStream, encoding);
			knownFSTs.put(key, fst);
		}
	}

	/**
	 * Look up a word in the FST. The FST runs in normal mode, i.e. it generates the expanded forms from the original forms. This
	 * method is thread-safe.
	 * 
	 * @param word
	 *            the word to look up.
	 * @return a string array containing all expansions of word. If no expansion is found, an array of length 0 is returned.
	 */
	public String[] lookup(String word) {
		return lookup(word, false);
	}

	/**
	 * Look up a word in the FST. This method is thread-safe.
	 * 
	 * @param word
	 *            the word to look up.
	 * @param generate
	 *            whether the FST is to run in inverse direction, i.e. generating the original form from the expanded form.
	 * @return a string array containing all expansions of word. If no expansion is found, an array of length 0 is returned.
	 */
	public String[] lookup(String word, boolean generate) {
		StringBuilder buffer2 = new StringBuilder();
		List<String> results = new ArrayList<String>();

		lookup(word, 0, 0, generate, buffer2, results);

		String[] resultArray = new String[results.size()];
		resultArray = (String[]) results.toArray(resultArray);
		return resultArray;
	}

	private void lookup(String word, int offset1, int arc, boolean generate, StringBuilder buffer2, List<String> results) {
		do {
			int label = fst.labels[arc];
			int offset2 = buffer2.length();
			if (label == 0) {
				if (offset1 == word.length()) {
					results.add(buffer2.toString());
				}
			} else {
				String s1;
				if (generate)
					s1 = (String) fst.strings.get(fst.mapping[fst.offsets[2 * label + 1]]);
				else
					s1 = (String) fst.strings.get(fst.mapping[fst.offsets[2 * label]]);
				if (word.startsWith(s1, offset1)) {
					String s2;
					if (generate)
						s2 = (String) fst.strings.get(fst.mapping[fst.offsets[2 * label]]);
					else
						s2 = (String) fst.strings.get(fst.mapping[fst.offsets[2 * label + 1]]);
					buffer2.append(s2);
					lookup(word, offset1 + s1.length(), fst.targets[arc], generate, buffer2, results);
					if (offset2 < buffer2.length())
						buffer2.delete(offset2, buffer2.length());
				}
			}
		} while (!fst.isLast[arc++]);
	}

	/**
	 * A simple command-line frontend for the FST.
	 * 
	 * @param args
	 *            args
	 * @throws IOException
	 *             IOException
	 */
	public static void main(String[] args) throws IOException {
		long iBegin = System.currentTimeMillis();

		if (args.length == 0) {
			System.err.println("usage: java marytts.fst.FSTLookup FstFile [-g] [word ...]");
			System.exit(-1);
		}

		FSTLookup fstLookup = new FSTLookup(args[0]);

		if (args.length == 1 || (args.length == 2 && args[1].equals("-g"))) {
			boolean generate = false;
			if (args.length == 2 && args[1].equals("-g"))
				generate = true;
			String line;
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				while ((line = in.readLine()) != null) {
					showResults(line, fstLookup.lookup(line, generate));
				}
			} catch (Exception e) {
				System.err.println("Invalid Input");
			}
		} else {
			int i = 1;
			boolean generate = false;
			if (args[1].equals("-g")) {
				generate = true;
				i = 2;
			}
			for (; i < args.length; i++) {
				showResults(args[i], fstLookup.lookup(args[i], generate));
			}
		}
		long iEnd = System.currentTimeMillis();
		System.err.println("processed in " + (iEnd - iBegin) + " ms.");
	}

	public static void showResults(String query, String[] args) {
		System.out.println("---- " + args.length + " result(s) for " + query + ":");
		int i;
		for (i = 0; i < args.length; i++)
			System.out.println(args[i]);
		System.out.println();
	}
}
