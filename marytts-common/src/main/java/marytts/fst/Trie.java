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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * This class represents a trie, i.e. a symbol (or 'letter') tree. Each trie node has arcs, to each of which a symbol is attached.
 * The symbols guide the lookup of entries in the trie.
 * 
 * The main purpose of this particular trie implementation is not the direct use of the trie (e.g. lookup) but its conversion to a
 * finite-state machine that allows for an even more efficient storage and lookup.
 * 
 * We are very thankful to Andreas Eisele who had the idea of using tries and transducers for our purposes and who provided us
 * with c-code this particular implemention is based on.
 * 
 * @author benjaminroth
 *
 */
public class Trie<Symbol> {

	// class to store nodes of a trie
	class TrieNode {

		private boolean hashcodeFixed = false;
		int hashcode = -1;

		// maps a string to the node the corresponding arc leads to
		private Map<Integer, TrieNode> labelId2node = new HashMap<Integer, TrieNode>();

		// true if this state marks the end of an entry
		protected boolean isFinal = false;

		// id for transducer representation. -1 means no id asigned.
		private int id = -1;

		// pointer to mother node - needed for minimization
		private TrieNode backPointer = null;

		/**
		 * This constructs a TrieNode and specifies its predecessor.
		 * 
		 * @param predecessor
		 */
		public TrieNode(TrieNode predecessor, Map<Symbol, Integer> label2idMap, List<Symbol> labels) {
			this.backPointer = predecessor;

		}

		/**
		 * 
		 * This adds an entry (word...) to the node and its daughters. The entry is entered from the specified index on.
		 * 
		 * @param entry
		 *            word to be entered.
		 * @param index
		 *            position from which on the entry is to be enetered at this node.
		 * @return the final node of this entry.
		 */
		protected TrieNode add(Symbol[] entry, int index) {

			if (index == entry.length) {
				// index points to the end of the word: already everything entered.
				this.isFinal = true;

				return this;
			}

			Integer labelId = label2id.get(entry[index]);
			if (null == labelId) {
				labelId = labels.size();
				labels.add(entry[index]);
				label2id.put(entry[index], labelId);
			}

			// add rest of the entry to successors
			// get successor via id
			TrieNode successor = this.labelId2node.get(labelId);

			if (null == successor) {

				successor = new TrieNode(this, label2id, labels);
				this.labelId2node.put(labelId, successor);
			}

			return successor.add(entry, index + 1);

		}

		protected boolean hasSuccessor() {
			return this.labelId2node.size() > 0;
		}

		@Override
		public int hashCode() {

			if (this.hashcodeFixed)
				return this.hashcode;

			int hc = (this.isFinal) ? 1 : 0;

			// sortedIds =

			for (Integer labelId : this.labelId2node.keySet()) {
				hc += labelId ^ labelId2node.get(labelId).id;
			}

			return hc;
		}

		/*
		 * equals compares everything important but _not_ id
		 */
		public boolean equals(Object other) {

			TrieNode otherNode;

			try {
				otherNode = (TrieNode) other;
			} catch (ClassCastException e) {
				return false;
			}

			// System.out.println("comparing two TrieNodes");

			// both nodes have to be final
			if (this.isFinal != otherNode.isFinal)
				return false;

			// both nodes have to have same outgoing edges
			if (!this.labelId2node.keySet().equals(otherNode.labelId2node.keySet()))
				return false;

			// edges must lead to nodes of same equivalence class
			for (Integer labelId : this.labelId2node.keySet()) {
				if (labelId2node.get(labelId).id != otherNode.labelId2node.get(labelId).id)
					return false;
			}

			return true;
		}

		public int getId() {
			return this.id;
		}

		public void setId(int id2) {
			this.id = id2;
			this.hashcode = this.hashCode();
			this.hashcodeFixed = true;

		}

		public boolean hasId() {
			return this.id != -1;
		}

		public TrieNode getBackPointer() {
			return this.backPointer;
		}

		/**
		 * this checks if equivalent states in the right language of this node are already identified.
		 * 
		 * @return true iff so
		 */
		public boolean rightIdentified() {

			for (TrieNode n : this.labelId2node.values()) {
				if (!n.hasId())
					return false;
			}
			return true;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			if (this.backPointer == null) {
				sb.append(">");
			}

			if (this.isFinal) {
				sb.append("((" + this.id + "))");
			} else {
				sb.append("(" + this.id + ")");
			}

			for (Integer lId : this.labelId2node.keySet()) {

				String l = labels.get(lId).toString();

				sb.append("\n");
				sb.append("|-" + l);
				sb.append(" (" + this.labelId2node.get(lId).id + ")");
			}

			return sb.toString();
		}

		public Map<Integer, TrieNode> getArcMap() {
			return this.labelId2node;
		}

	}

	// node that point to the beginning of all words
	protected TrieNode root;

	// list of nodes that mark the end of a entry (word)
	protected List<TrieNode> finalNodes;

	// mapping from nodes to representatives in a minimized transducer view.

	protected Map<TrieNode, Integer> reprs = null;

	// mapping is done via list indices
	protected List<TrieNode> rlist = null;

	// back and forth mapping from labels to ids
	protected Map<Symbol, Integer> label2id;
	protected List<Symbol> labels;

	/**
	 * Standard constructor for a trie.
	 */
	public Trie() {

		this.label2id = new HashMap<Symbol, Integer>();
		this.labels = new ArrayList<Symbol>();

		this.root = new TrieNode(null, label2id, labels);

		this.finalNodes = new ArrayList<TrieNode>();
	}

	/**
	 * 
	 * This adds an entry to the trie.
	 * 
	 * @param entry
	 *            entry
	 */
	public void add(Symbol[] entry) {
		// add the entry and remember backpointer to final node
		this.finalNodes.add(this.root.add(entry, 0));
	}

	/**
	 * This computes the minimization of the trie, i.e. equivalent nodes are identified. This is necessary to store a compact
	 * version of this trie as a minimal transducer. The trie itself is not represented more compactly.
	 * 
	 */
	public void computeMinimization() {
		// core idea: identify nodes with identical right language.

		// candidates are first all final nodes without successors
		LinkedList<TrieNode> identityCandidates = new LinkedList<TrieNode>();

		for (TrieNode fn : this.finalNodes) {
			if (!fn.hasSuccessor()) {
				identityCandidates.add(fn);
			}
		}

		// store the representants of the equivalence classes
		this.rlist = new ArrayList<TrieNode>();

		// maps nodes to their Id/representative
		// this.reprs = new HashMap<TrieNode, TrieNode>();
		this.reprs = new HashMap<TrieNode, Integer>();

		// for each identity candidate check to which nodes it is identical,
		// make new equivalence classes when needed and produce new candidates

		while (!identityCandidates.isEmpty()) {
			// pop the head element
			TrieNode currCan = identityCandidates.remove();

			// does it belong to one of the already identified equiv. classes?
			if (this.reprs.containsKey(currCan)) {
				currCan.setId(reprs.get(currCan));
				// System.out.println("identifies identical nodes for class " + currCan.getId());
			}

			/*
			 * for (TrieNode repr : reprs){ if ( currCan.identicalTo(repr) ){ currCan.setId( repr.getId() ); break; } }
			 */

			// ... if not, let it represent a new class
			if (!currCan.hasId()) {
				currCan.setId(reprs.size());
				reprs.put(currCan, currCan.getId());
				rlist.add(currCan);
			}

			TrieNode pred = currCan.getBackPointer();

			// add the predecessor of this node if...
			if (null != pred && // 1. there is one
					!pred.hasId() && // 2. it is not already processed
					pred.rightIdentified() // 3. but its successors are processed
			) {
				identityCandidates.add(pred);
			}

		}

	}

	public String toString() {

		StringBuilder sb = new StringBuilder();

		for (TrieNode r : this.reprs.keySet()) {
			sb.append("\n");
			sb.append(r.toString());
		}

		return sb.toString();

	}

}
