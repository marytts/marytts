/**
 * 
 */
package marytts.tools.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import marytts.datatypes.MaryXML;
import marytts.exceptions.InvalidDataException;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

/**
 * @author marc
 * 
 */
public class MaryTranscriptionAligner extends TranscriptionAligner {

	private boolean insertDummyDurations = false;

	public MaryTranscriptionAligner() {
		super(null);
	}

	/**
	 * @param allophoneSet
	 *            allophoneSet
	 */
	public MaryTranscriptionAligner(AllophoneSet allophoneSet) {
		super(allophoneSet);
	}

	/**
	 * @param allophoneSet
	 *            allophoneSet
	 * @param insertDummyDurations
	 *            if true, in any inserted items, a duration of 1 millisecond will be set.
	 */
	public MaryTranscriptionAligner(AllophoneSet allophoneSet, boolean insertDummyDurations) {
		super(allophoneSet);
		this.insertDummyDurations = insertDummyDurations;
	}

	/**
	 * 
	 * This changes the transcription of a MARYXML document in ALLOPHONES format to match the label sequence given as the "labels"
	 * parameter. The symbols of the original transcription are aligned to corrected ones, with which they are replaced in turn.
	 * 
	 * @param allophones
	 *            the MARYXML document, in ALLOPHONES format
	 * @param labels
	 *            the sequence of label symbols to use, separated by the entry separator as provided by getEntrySeparator().
	 * @throws InvalidDataException
	 *             if a manual label is encountered that is not in the AllophoneSet
	 */
	public void alignXmlTranscriptions(Document allophones, String labels) throws InvalidDataException {
		// get all t and boundary elements
		NodeIterator tokenIt = MaryDomUtils.createNodeIterator(allophones, MaryXML.TOKEN, MaryXML.BOUNDARY);
		List<Element> tokens = new ArrayList<Element>();
		Element e;
		while ((e = (Element) tokenIt.nextNode()) != null) {
			tokens.add(e);
		}

		String orig = this.collectTranscription(allophones);

		System.err.println("Orig   : " + orig);
		System.err.println("Correct: " + labels);

		// now we align the transcriptions and split it at the delimiters
		String al = this.distanceAlign(orig.trim(), labels.trim()) + " ";

		System.err.println("Alignments: " + al);
		String[] alignments = al.split("#");

		// change the transcription in xml according to the aligned one
		changeTranscriptions(allophones, alignments);

		if (allophoneSet == null) { // cannot verify
			return;
		}
		// assert that all alignments should be in the AllophoneSet for this locale:
		HashSet<String> manualLabelSet = new HashSet<String>(Arrays.asList(al.trim().split("[#\\s]+")));
		try {
			for (String label : manualLabelSet) {
				allophoneSet.getAllophone(label);
			}
		} catch (IllegalArgumentException iae) {
			throw new InvalidDataException(iae.getMessage());
		}
	}

	/**
	 * 
	 * This computes a string of phonetic symbols out of an allophones xml: - standard phones are taken from "ph" elements in the
	 * document - after each token-element (except those followed by a "boundary"-element), a "bnd" symbol is inserted (standing
	 * for a possible pause). Entries are separated by the entrySeparator character.
	 * 
	 * @param doc
	 *            the document to analyse
	 * @return orig, converted into string
	 */
	private String collectTranscription(Document doc) {
		// String storing the original transcription begins with a pause
		StringBuilder orig = new StringBuilder();

		NodeIterator ni = MaryDomUtils.createNodeIterator(doc, MaryXML.PHONE, MaryXML.BOUNDARY);
		Element e;
		Element prevToken = null;
		boolean prevWasBoundary = false;
		while ((e = (Element) ni.nextNode()) != null) {
			if (e.getTagName().equals(MaryXML.PHONE)) {
				Element token = (Element) MaryDomUtils.getAncestor(e, MaryXML.TOKEN);
				if (token != prevToken && !prevWasBoundary) {
					if (orig.length() > 0)
						orig.append(entrySeparator);
					orig.append(possibleBnd);
				}
				if (orig.length() > 0)
					orig.append(entrySeparator);
				orig.append(e.getAttribute("p"));
				prevToken = token;
				prevWasBoundary = false;
			} else { // boundary
				if (orig.length() > 0)
					orig.append(entrySeparator);
				orig.append(possibleBnd);
				prevWasBoundary = true;
			}
		}

		return orig.toString();
	}

	/**
	 * 
	 * This changes the transcription according to a given sequence of phonetic symbols (including boundaries and pauses).
	 * Boundaries in doc are added or deleted as necessary to match the pause symbols in alignments.
	 * 
	 * @param doc
	 *            the document in which to change the transcriptions
	 * @param alignments
	 *            the aligned symbols to use in the update.
	 */
	private void changeTranscriptions(Document doc, String[] alignments) {
		// Algorithm:
		// * Go through <ph> and <boundary> elements in doc on the one hand,
		// and through alignments on the other hand.
		// - Special steps for the first <ph> in a token:
		// -> if the <ph> is the first <ph> in the current token,
		// and alignment is a pause symbol,
		// insert a new boundary before the token, and skip the alignment entry;
		// -> if the <ph> is the first <ph> in the current token,
		// and the alignment entry is empty, skip the alignment entry.
		// - for <ph> elements:
		// -> if the alignment entry is empty, delete the <ph> and,
		// if it was the only <ph> in the current <syllable>, also
		// delete the syllable;
		// -> else, use the current alignment entry, adding any <ph>
		// elements as necessary.
		// - for <boundary> elements:
		// -> if symbol is pause, keep boundary;
		// -> if symbol is word separator, delete boundary.

		NodeIterator ni = MaryDomUtils.createNodeIterator(doc, MaryXML.PHONE, MaryXML.BOUNDARY);
		List<Element> origPhonesAndBoundaries = new ArrayList<Element>();
		// We make a copy of the list of original entries, because when
		// we add/remove entries later, that get the node iterator confused.
		Element elt;
		while ((elt = (Element) ni.nextNode()) != null) {
			origPhonesAndBoundaries.add(elt);
		}
		int iAlign = 0;
		Element prevToken = null;
		boolean prevWasBoundary = false;
		for (Element e : origPhonesAndBoundaries) {
			if (e.getTagName().equals(MaryXML.PHONE)) {
				boolean betweenTokens = false;
				Element token = (Element) MaryDomUtils.getAncestor(e, MaryXML.TOKEN);
				if (token != prevToken && !prevWasBoundary) {
					betweenTokens = true;
				}
				if (betweenTokens) {
					assert !prevWasBoundary;
					if (alignments[iAlign].trim().equals(possibleBnd)) {
						// Need to insert a boundary before token
						System.err.println("  inserted boundary in xml");
						Element b = MaryXML.createElement(doc, MaryXML.BOUNDARY);
						b.setAttribute("breakindex", "3");
						if (insertDummyDurations) {
							b.setAttribute("duration", "1");
						}
						token.getParentNode().insertBefore(b, token);
					} else if (!alignments[iAlign].trim().equals("")) {
						// one or more phones were inserted into the transcription
						// -- treat them as word-final, i.e. insert them into the last syllable in prevToken
						Element syllable = null;
						Element ref = null; // insert before null = insert at the end
						NodeList prevSyllables = null;
						// if there is an insertion at the beginning, we don't have a prevToken!
						if (prevToken != null) {
							prevSyllables = prevToken.getElementsByTagNameNS(MaryXML.getNamespace(), MaryXML.SYLLABLE);
						}
						if (prevSyllables != null && prevSyllables.getLength() > 0) { // insert at end of previous token
							syllable = (Element) prevSyllables.item(prevSyllables.getLength() - 1);
							ref = null;
						} else { // insert at beginning of current token
							syllable = (Element) e.getParentNode();
							ref = e; // insert before current phone
						}
						String[] newPh = alignments[iAlign].trim().split("\\s+");
						for (int i = 0; i < newPh.length; i++) {
							Element newPhElement = MaryXML.createElement(doc, MaryXML.PHONE);
							newPhElement.setAttribute("p", newPh[i]);
							syllable.insertBefore(newPhElement, ref);
							System.err.println(" inserted phone from transcription: " + newPh[i]);
							if (insertDummyDurations) {
								newPhElement.setAttribute("d", "1");
							}
						}
					} // else it is an empty word boundary marker
					iAlign++; // move beyond the marker between tokens
				}
				prevToken = token;
				prevWasBoundary = false;
				System.err.println("Ph = " + e.getAttribute("p") + ", align = " + alignments[iAlign]);
				if (alignments[iAlign].trim().equals("")) {
					// Need to delete the current <ph> element
					Element syllable = (Element) e.getParentNode();
					assert syllable != null;
					assert syllable.getTagName().equals(MaryXML.SYLLABLE);
					syllable.removeChild(e);
					if (MaryDomUtils.getFirstElementByTagName(syllable, MaryXML.PHONE) == null) {
						// Syllable is now empty, need to delete it as well
						syllable.getParentNode().removeChild(syllable);
					}
				} else {
					// Replace <ph>, add siblings if necessary
					String[] newPh = alignments[iAlign].trim().split("\\s+");
					e.setAttribute("p", newPh[0]);
					if (newPh.length > 1) {
						// any ph to be added
						Element syllable = (Element) e.getParentNode();
						assert syllable != null;
						assert syllable.getTagName().equals(MaryXML.SYLLABLE);
						Node rightNeighbor = e.getNextSibling(); // can be null
						for (int i = 1; i < newPh.length; i++) {
							Element newPhElement = MaryXML.createElement(doc, MaryXML.PHONE);
							newPhElement.setAttribute("p", newPh[i]);
							syllable.insertBefore(newPhElement, rightNeighbor);
						}
					}
				}
			} else { // boundary
				System.err.println("Boundary, align = " + alignments[iAlign]);
				if (alignments[iAlign].trim().equals(possibleBnd)) {
					// keep boundary
				} else {
					// delete boundary
					System.err.println("  deleted boundary from xml");
					e.getParentNode().removeChild(e);
				}
				prevWasBoundary = true;
			}
			iAlign++;
		}
		updatePhAttributesFromPhElements(doc);
	}

	private void updatePhAttributesFromPhElements(Document doc) {
		NodeIterator ni = MaryDomUtils.createNodeIterator(doc, MaryXML.TOKEN);
		Element t;
		while ((t = (Element) ni.nextNode()) != null) {
			updatePhAttributesFromPhElements(t);
		}
	}

	private void updatePhAttributesFromPhElements(Element token) {
		if (token == null)
			throw new NullPointerException("Got null token");
		if (!token.getTagName().equals(MaryXML.TOKEN)) {
			throw new IllegalArgumentException("Argument should be a <" + MaryXML.TOKEN + ">, not a <" + token.getTagName() + ">");
		}
		StringBuilder tPh = new StringBuilder();
		TreeWalker sylWalker = MaryDomUtils.createTreeWalker(token, MaryXML.SYLLABLE);
		Element syl;
		while ((syl = (Element) sylWalker.nextNode()) != null) {
			StringBuilder sylPh = new StringBuilder();
			String stress = syl.getAttribute("stress");
			if (stress.equals("1"))
				sylPh.append("'");
			else if (stress.equals("2"))
				sylPh.append(",");
			TreeWalker phWalker = MaryDomUtils.createTreeWalker(syl, MaryXML.PHONE);
			Element ph;
			while ((ph = (Element) phWalker.nextNode()) != null) {
				if (sylPh.length() > 0)
					sylPh.append(" ");
				sylPh.append(ph.getAttribute("p"));
			}
			String sylPhString = sylPh.toString();
			syl.setAttribute("ph", sylPhString);
			if (tPh.length() > 0)
				tPh.append(" - ");
			tPh.append(sylPhString);
			if (syl.hasAttribute("tone")) {
				tPh.append(" " + syl.getAttribute("tone"));
			}
		}
		if (tPh.toString().length() > 0) {
			token.setAttribute("ph", tPh.toString());
		}
	}

}
