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

package marytts.modules.acoustic.model;

import java.io.InputStream;
import java.util.List;

import marytts.modeling.features.FeatureProcessorManager;
import marytts.features.FeatureMap;

import marytts.data.item.Item;
import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;

/**
 * Model which currently predicts only a flat 400 ms duration for each boundary Element
 * <p>
 * Could be replaced by a PauseTree or something else, but that would require a CARTModel instead of this.
 *
 * @author steiner
 *
 */
public class BoundaryModel extends Model {
	public BoundaryModel(FeatureProcessorManager featureManager, String voiceName, InputStream dataStream)
    {
		super(featureManager, voiceName, dataStream);
	}

	@Override
	public void applyTo(Utterance utt, SupportedSequenceType seq_type, List<Integer> item_indexes)
    {
        throw new UnsupportedOperationException();
		// for (Element element : elements) {
		// 	if (!element.hasAttribute(targetAttributeName)) {
		// 		element.setAttribute(targetAttributeName, "400");
		// 	}
		// }
	}

	/**
	 * For boundaries, this does nothing;
	 */
	@Override
	protected float evaluate(FeatureMap target) {
		return Float.NaN;
	}

	/**
	 * For boundaries, this does nothing;
	 */
	@Override
	public void loadData() {
		return;
	}
}
