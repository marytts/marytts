/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.modeling.features;

import marytts.datatypes.MaryXML;
import marytts.modeling.features.FeatureVector;
import marytts.modeling.features.MaryGenericFeatureProcessors;
import marytts.modules.nlp.phonemiser.Allophone;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;


/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class TargetUnit extends Target
{
	protected float duration = -1;
	protected float f0 = -1;

    public TargetUnit(String name, Element maryxmlElement) {
        super(name, maryxmlElement);
    }

	public float getTargetDurationInSeconds() {
		if (duration != -1) {
			return duration;
		} else {
			if (maryxmlElement == null)
				return 0;
			// throw new NullPointerException("Target "+name+" does not have a maryxml element.");
			duration = new MaryUnitSelectionFeatureProcessors.UnitDuration().process(this);
			return duration;
		}
	}

	/**
	 *
	 *
	 * @param newDuration
	 *            newDuration
	 */
	public void setTargetDurationInSeconds(float newDuration) {
		if (maryxmlElement != null) {
			if (maryxmlElement.getTagName().equals(MaryXML.PHONE)) {
				maryxmlElement.setAttribute("d", Float.toString(newDuration));
			} else {
				assert maryxmlElement.getTagName().equals(MaryXML.BOUNDARY) : "segment should be a phone or a boundary, but is a "
						+ maryxmlElement.getTagName();
				maryxmlElement.setAttribute("duration", Float.toString(newDuration));
			}
		}
	}

	public float getTargetF0InHz() {
		if (f0 != -1) {
			return f0;
		} else {
			if (maryxmlElement == null)
				throw new NullPointerException("Target " + name + " does not have a maryxml element.");
			float logf0 = new MaryUnitSelectionFeatureProcessors.UnitLogF0().process(this);
			if (logf0 == 0)
				f0 = 0;
			else
				f0 = (float) Math.exp(logf0);
			return f0;
		}
	}
}


/* TargetUnit.java ends here */
