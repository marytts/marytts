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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * An implementation of a finite state transducer. This class does nothing but load and represent the FST. It is used by other
 * classes doing something reasonable with it.
 * 
 * @author Andreas Eisele
 */
public class FST {
	// The following variables are package-readable, so that they can be
	// directly accessed by all classes in this package.
	int[] targets;
	short[] labels;
	boolean[] isLast;

	short[] offsets;
	byte[] bytes;
	int[] mapping;
	ArrayList strings = new ArrayList();

	public FST(String fileName) throws IOException {
		FileInputStream fis = new FileInputStream(fileName);
		try {
			load(fis);
		} finally {
			fis.close();
		}
	}

	/**
	 * Load the fst from the given input stream. Assumes header.
	 * 
	 * @param inStream
	 *            in stream
	 * @throws IOException
	 *             IOException
	 */
	public FST(InputStream inStream) throws IOException {
		load(inStream);
	}

	/**
	 * Initialise the finite state transducer. Loads from headerless legacy file format.
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
	public FST(String fileName, String encoding) throws IOException, UnsupportedEncodingException {
		this(fileName, encoding, false);
	}

	/**
	 * Initialise the finite state transducer. This constructor will assume that the file uses the system default encoding.
	 * 
	 * @param fileName
	 *            the name of the file from which to load the FST.
	 * @param verbose
	 *            whether to write a report to stderr after loading.
	 * @throws IOException
	 *             if the FST cannot be loaded from the given file.
	 */
	public FST(String fileName, boolean verbose) throws IOException {
		this(fileName, null, verbose);
	}

	/**
	 * Initialise the finite state transducer.
	 * 
	 * @param fileName
	 *            the name of the file from which to load the FST.
	 * @param encoding
	 *            the name of the encoding used in the file (e.g., UTF-8 or ISO-8859-1).
	 * 
	 *            This constructor is to be used for old FST-files where the encoding was not yet specified in the header.
	 * 
	 * @param verbose
	 *            whether to write a report to stderr after loading.
	 * @throws IOException
	 *             if the FST cannot be loaded from the given file.
	 * @throws UnsupportedEncodingException
	 *             if the encoding is not supported.
	 */
	public FST(String fileName, String encoding, boolean verbose) throws IOException, UnsupportedEncodingException {
		FileInputStream fis = new FileInputStream(fileName);
		try {
			loadHeaderless(fis, encoding, verbose);
		} finally {
			fis.close();
		}
	}

	/**
	 * Load the fst from the given input stream. Assumes headerless legacy file format.
	 * 
	 * @param inStream
	 *            inStream
	 * @param encoding
	 *            encoding
	 * @throws IOException
	 *             IOException
	 * @throws UnsupportedEncodingException
	 *             UnsupportedEncodingException
	 */
	public FST(InputStream inStream, String encoding) throws IOException, UnsupportedEncodingException {
		loadHeaderless(inStream, encoding, false);
	}

	private void load(InputStream inStream) throws IOException, UnsupportedEncodingException {
		int i;
		DataInputStream in = new DataInputStream(new BufferedInputStream(inStream));
		// int fileSize= (int) f.length();
		int fileSize = in.available(); // TODO: how robust is this??

		int encLen = in.readInt();
		byte[] encBytes = new byte[encLen];

		in.read(encBytes, 0, encLen);
		String encoding = new String(encBytes, "UTF-8");

		if (!Charset.isSupported(encoding))
			throw new IOException("Encoding of FST file not correctly specified. Maybe file in old format.");

		int overallBits = in.readInt();
		int arcOffBits = in.readInt();

		// System.out.println("bits: " + overallBits + "-" + arcOffBits);

		// todo: allow for more flexibility
		if (overallBits != 32 || arcOffBits != 20) {
			throw new IOException("Cannot handle non-standard bit allocation for label and arc id's.");
		}

		int nArcs = in.readInt();
		// arcs = new int[nArcs];

		targets = new int[nArcs];
		labels = new short[nArcs];
		isLast = new boolean[nArcs];

		for (i = 0; i < nArcs; i++) {
			int thisArc = in.readInt();

			targets[i] = thisArc & 1048575;
			labels[i] = (short) ((thisArc >> 20) & 2047);
			isLast[i] = ((byte) (thisArc >> 31)) != 0;

		}

		int nPairs = in.readInt();
		offsets = new short[2 * nPairs];
		for (i = 0; i < 2 * nPairs; i++)
			offsets[i] = in.readShort();
		// int nBytes = fileSize - 8 - 4 * (nPairs + nArcs);
		int nBytes = fileSize - 20 - encLen - 4 * (nPairs + nArcs);
		mapping = new int[nBytes];
		bytes = new byte[nBytes];
		in.readFully(bytes);
		assert in.available() == 0 : "Partial file read... not good";

		in.close();
		createMapping(mapping, bytes, encoding);
	}

	private void loadHeaderless(InputStream inStream, String encoding, boolean verbose) throws IOException,
			UnsupportedEncodingException {
		int i;
		DataInputStream in = new DataInputStream(new BufferedInputStream(inStream));
		// int fileSize= (int) f.length();
		int fileSize = in.available(); // TODO: how robust is this??
		int nArcs = in.readInt();
		// arcs = new int[nArcs];

		targets = new int[nArcs];
		labels = new short[nArcs];
		isLast = new boolean[nArcs];

		for (i = 0; i < nArcs; i++) {
			int thisArc = in.readInt();

			targets[i] = thisArc & 1048575;
			labels[i] = (short) ((thisArc >> 20) & 2047);
			isLast[i] = ((byte) (thisArc >> 31)) != 0;

		}

		int nPairs = in.readInt();
		offsets = new short[2 * nPairs];
		for (i = 0; i < 2 * nPairs; i++)
			offsets[i] = in.readShort();
		int nBytes = fileSize - 8 - 4 * (nPairs + nArcs);
		mapping = new int[nBytes];
		bytes = new byte[nBytes];
		in.readFully(bytes);
		if (verbose) {
			System.err.println("FST (" + fileSize + " Bytes, " + nArcs + " Arcs, " + nPairs + " Labels)" + " loaded");
		}
		in.close();
		createMapping(mapping, bytes, encoding);
	}

	private void createMapping(int[] mapping, byte[] bytes, String encoding) throws UnsupportedEncodingException {
		mapping[0] = 0;
		int last0 = -1;
		String s;
		int len;
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == 0) {
				len = i - last0 - 1;
				if (len == 0)
					strings.add("");
				else {
					String str;
					if (encoding != null)
						str = new String(bytes, last0 + 1, len, encoding);
					else
						str = new String(bytes, last0 + 1, len);
					strings.add(str);
				}
				mapping[last0 + 1] = strings.size() - 1;
				last0 = i;
			}
		}
	}
}
