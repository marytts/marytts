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

package marytts.unitselection.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * Iterator to provide the sentences in a given feature file, in sequence.
 * 
 * @author marc
 */
public class SentenceIterator implements Iterator<Sentence> {

	private final FeatureFileReader features;
	private final int fiSentenceStart;
	private final int fiSentenceEnd;
	private final int fiWordStart;
	private final int fiWordEnd;
	private final boolean isHalfphone;
	private final int fiLR;
	private final int fvLR_L;
	private final int fvLR_R;

	private int i;
	private int len;
	private Sentence nextSentence = null;

	public SentenceIterator(FeatureFileReader features) {
		this.features = features;
		FeatureDefinition featureDefinition = features.getFeatureDefinition();
		fiSentenceStart = featureDefinition.getFeatureIndex("words_from_sentence_start");
		fiSentenceEnd = featureDefinition.getFeatureIndex("words_from_sentence_end");
		fiWordStart = featureDefinition.getFeatureIndex("segs_from_word_start");
		fiWordEnd = featureDefinition.getFeatureIndex("segs_from_word_end");
		String halfphoneFeature = "halfphone_lr";
		if (featureDefinition.hasFeature(halfphoneFeature)) {
			isHalfphone = true;
			fiLR = featureDefinition.getFeatureIndex(halfphoneFeature);
			fvLR_L = featureDefinition.getFeatureValueAsByte(fiLR, "L");
			fvLR_R = featureDefinition.getFeatureValueAsByte(fiLR, "R");
		} else {
			isHalfphone = false;
			fiLR = fvLR_L = fvLR_R = 0;
		}

		i = 0;
		len = features.getNumberOfUnits();
	}

	public synchronized boolean hasNext() {
		if (nextSentence == null) {
			prepareNextSentence();
		}
		return nextSentence != null;
	}

	public synchronized Sentence next() {
		if (nextSentence == null) {
			prepareNextSentence();
		}
		if (nextSentence == null) {
			// no more sentences
			throw new NoSuchElementException("no more sentences!");
		}
		Sentence retval = nextSentence;
		nextSentence = null;
		return retval;
	}

	public void remove() {
		throw new UnsupportedOperationException("This iterator cannot remove sentences");
	}

	/**
	 * Find the next sentence in the feature file, if possible.
	 */
	private void prepareNextSentence() {
		if (nextSentence != null) {
			return;
		}
		if (i >= len) {
			return;
		}
		// if we get here, then i is the index of a unit before or at a sentence start
		while (i < len && !isSentenceStart(i)) {
			i++;
		}
		if (i >= len) {
			return;
		}
		int iSentenceStart = i;
		while (i < len && !isSentenceEnd(i)) {
			i++;
		}
		if (i >= len) {
			return;
		}
		int iSentenceEnd = i;
		nextSentence = new Sentence(features, iSentenceStart, iSentenceEnd);
	}

	/**
	 * Check if the given unit index is a sentence start
	 * 
	 * @param index
	 *            the unit index
	 */
	private boolean isSentenceStart(int index) {
		FeatureVector fv = features.getFeatureVector(index);

		return fv.getByteFeature(fiSentenceStart) == 0 // first word in sentence
				&& fv.getByteFeature(fiWordStart) == 0 // first segment in word
				&& (!isHalfphone || fv.getByteFeature(fiLR) == fvLR_L); // for halfphones, it's the left half
	}

	/**
	 * Check if the given unit index is a sentence end
	 * 
	 * @param index
	 *            the unit index
	 */
	private boolean isSentenceEnd(int index) {
		FeatureVector fv = features.getFeatureVector(index);

		return fv.getByteFeature(fiSentenceEnd) == 0 // last word in sentence
				&& fv.getByteFeature(fiWordEnd) == 0 // last segment in word
				&& (!isHalfphone || fv.getByteFeature(fiLR) == fvLR_R); // for halfphones, it's the right half
	}

}
