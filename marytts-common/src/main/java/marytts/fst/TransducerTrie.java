/**
 * Copyright 2000-2009 DFKI GmbH.
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * This is a particular Trie whose Symbols are Pairs of Strings, the first of which is interpreted as an input symbol and the
 * second as an output symbol. The transducer obtained by trie minimization can be written to a file in the transducer format used
 * by MARY.
 * 
 * To get the transducer representation first compute the minimization of the trie, then write the transducer to disk.
 * 
 * See main method for example usage.
 * 
 * @author benjaminroth
 *
 */
public class TransducerTrie extends Trie<StringPair> {
	static int ARCOFFSET_BITS = 20;

	static int OVERALL_BITS = 32;// for example
	static int LABELID_BITS = OVERALL_BITS - (ARCOFFSET_BITS + 1);

	public void writeFST(DataOutputStream out, String encoding) throws IOException {

		if (null == this.reprs)
			throw new IllegalStateException("Cannot write transducer: first compute minimization of trie.");

		// compute arc offsets
		int[] arcOffsets = new int[this.reprs.size() + 1];

		// first has offset one (consider additional start arc)
		arcOffsets[0] = 1;

		for (int i = 0; i < rlist.size(); i++) {
			arcOffsets[i + 1] = arcOffsets[i] + rlist.get(i).getArcMap().size();

			// if final, consider the added "final arc"
			if (rlist.get(i).isFinal) {
				arcOffsets[i + 1] += 1;
			}
		}

		// write number of arcs
		int maxAO = arcOffsets[arcOffsets.length - 1];

		// to ensure that number can be encoded:
		// shift to right by the number of available bits and look if something remains
		if ((maxAO >> ARCOFFSET_BITS) != 0) {
			int numBitsNeeded = (int) Math.ceil(Math.log(maxAO) / Math.log(2));
			throw new IOException("Cannot write transducer: too many arcs to be encoded in binary fst format (would need "
					+ numBitsNeeded + " bits, have " + ARCOFFSET_BITS + ")");
		}

		int maxLID = this.labels.size() + 2;
		if ((maxLID >> LABELID_BITS) != 0) {
			int numBitsNeeded = (int) Math.ceil(Math.log(maxLID) / Math.log(2));
			throw new IOException("Cannot write transducer: too many arc-labels to be encoded in binary fst format (would need "
					+ numBitsNeeded + " bits, have " + LABELID_BITS + ")");
		}

		if (!Charset.isSupported(encoding))
			throw new IOException("Cannot write transducer: encoding not supported.");

		// write encoding in UTF-8
		out.writeInt(encoding.length());
		out.write(encoding.getBytes("UTF-8"));
		// write overall bits
		out.writeInt(OVERALL_BITS);
		// write bits used for encoding arc_offsets
		out.writeInt(ARCOFFSET_BITS);

		out.writeInt(maxAO);

		// write starting arc:
		// pointing to start node offset - empty label - final
		int startArc = arcOffsets[root.getId()] | 1 << 20 | 1 << 31;

		out.writeInt(startArc);

		// write arcs, final nodes have final arc as last with empty label
		// dont forget to add one
		for (TrieNode repr : rlist) {

			List<Integer> arcVals = new ArrayList<Integer>();

			for (Integer labelId : repr.getArcMap().keySet()) {
				int targetId = repr.getArcMap().get(labelId).getId();
				arcVals.add(arcOffsets[targetId] | (labelId + 2) << 20);
			}

			// if final, consider the added "final arc"
			if (repr.isFinal) {
				arcVals.add(arcOffsets[repr.getId()] | 0 << 20 | 1 << 31);

			} else {
				// mark last of the arcs as "last"
				int last = arcVals.size() - 1;
				arcVals.set(last, arcVals.get(last) | 1 << 31);
			}

			for (Integer val : arcVals) {
				out.writeInt(val);
			}

		}

		// compute label offsets
		int[] labelOffsets = new int[this.labels.size() * 2];

		// first has offset two (input and output of empty arc)
		labelOffsets[0] = 4;

		for (int i = 0; i < labels.size(); i++) {

			StringPair ioSym = labels.get(i);

			// offset of outS determined by offset of inS
			labelOffsets[i * 2 + 1] = labelOffsets[i * 2];
			// offset increased by length of inS
			labelOffsets[i * 2 + 1] += ioSym.getString1().getBytes(encoding).length;
			// additionally increased by one because of stop byte
			labelOffsets[i * 2 + 1] += 1;

			if (i + 1 < labels.size()) {
				// offset of next inS determined by this outS
				labelOffsets[(i + 1) * 2] = labelOffsets[i * 2 + 1];
				labelOffsets[(i + 1) * 2] += ioSym.getString2().getBytes(encoding).length;
				labelOffsets[(i + 1) * 2] += 1;
			}

		}

		// write number of pairs
		out.writeInt(labels.size() + 2);

		// write empty label id/offset
		out.writeShort(0);
		out.writeShort(1);
		out.writeShort(2);
		out.writeShort(3);

		// write pair offsets
		for (int i = 0; i < labels.size(); i++) {
			out.writeShort(labelOffsets[i * 2]);
			out.writeShort(labelOffsets[i * 2 + 1]);
		}

		// write first two pairs: just empty symbols
		out.writeByte(0);
		out.writeByte(0);
		out.writeByte(0);
		out.writeByte(0);

		// write pairs
		for (int i = 0; i < labels.size(); i++) {

			StringPair ioSym = labels.get(i);

			out.write(ioSym.getString1().getBytes(encoding));
			out.writeByte(0);
			out.write(ioSym.getString2().getBytes(encoding));
			out.writeByte(0);
		}

	}

	public static void main(String[] args) throws IOException {
		// example usage

		String path = "/Users/benjaminroth/Desktop/mary/fst/german/";

		// specify location of lexicon you want to encode
		BufferedReader lexReader = new BufferedReader(new InputStreamReader(new FileInputStream(path + "lexicon.txt"),
				"ISO-8859-1"));

		// specify location of output
		String fstLocation = path + "lexicon.fst";

		// initialize trainer
		// AlignerTrainer at = new AlignerTrainer(PhonemeSet.getPhonemeSet(phFileLoc), Locale.ENGLISH);
		AlignerTrainer at = new AlignerTrainer(false, true);

		System.out.println("reading lexicon...");

		// read lexicon for training
		at.readLexicon(lexReader, "\\\\");

		System.out.println("...done!");

		System.out.println("aligning...");

		long start = System.currentTimeMillis();

		// make some alignment iterations
		for (int i = 0; i < 4; i++) {
			System.out.println(" iteration " + (i + 1));
			at.alignIteration();

		}

		long time = System.currentTimeMillis() - start;

		System.out.println("...done!");

		System.out.println("alignment took " + time + "ms");

		TransducerTrie t = new TransducerTrie();

		System.out.println("entering alignments in trie...");
		for (int i = 0; i < at.lexiconSize(); i++) {
			t.add(at.getAlignment(i));
			t.add(at.getInfoAlignment(i));
		}
		System.out.println("...done!");

		System.out.println("minimizing trie...");
		t.computeMinimization();
		System.out.println("...done!");

		System.out.println("writing transducer to disk...");
		File of = new File(fstLocation);

		DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));

		t.writeFST(os, "UTF-8");
		os.flush();
		os.close();
		System.out.println("...done!");

		System.out.println("looking up test words...");
		FSTLookup fst = new FSTLookup(fstLocation);

		System.out.println(" Fahrrad -> " + Arrays.toString(fst.lookup("Fahrrad")));
		System.out.println(" fahren -> " + Arrays.toString(fst.lookup("fahren")));
		System.out.println(" Umwelt -> " + Arrays.toString(fst.lookup("Umwelt")));
		System.out.println(" schonen -> " + Arrays.toString(fst.lookup("schonen")));
		System.out.println(" abgerechnet -> " + Arrays.toString(fst.lookup("abgerechnet")));
		System.out.println(" abgerechnet(A) -> " + Arrays.toString(fst.lookup("abgerechnet(A)")));
		System.out.println(" absorbieren -> " + Arrays.toString(fst.lookup("absorbieren")));
		System.out.println(" absorbieren(WV1b) -> " + Arrays.toString(fst.lookup("absorbieren(WV1b)")));
		System.out.println(" übersetzen -> " + Arrays.toString(fst.lookup("übersetzen")));
		System.out.println("...done!");

	}

}
