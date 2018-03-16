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
package marytts.todisappear;

import marytts.todisappear.FeatureVector;
import marytts.todisappear.MaryGenericFeatureProcessors;
import marytts.modules.nlp.phonemiser.Allophone;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.util.MaryRuntimeUtils;
import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

/**
 * A representation of a target representing the ideal properties of a unit in a
 * target utterance.
 *
 * @author Marc Schr&ouml;der
 *
 */
public class Target {
    protected String name;
    protected Element maryxmlElement;

    protected FeatureVector featureVector = null;

    protected float duration = -1;
    protected float f0 = -1;
    protected int isSilence = -1;

    /**
     * Create a target associated to the given element in the MaryXML tree.
     *
     * @param name
     *            a name for the target, which may or may not coincide with the
     *            segment name.
     * @param maryxmlElement
     *            the phone or boundary element in the MaryXML tree to be
     *            associated with this target.
     */
    public Target(String name, Element maryxmlElement) {
        this.name = name;
        this.maryxmlElement = maryxmlElement;
    }

    public Element getMaryxmlElement() {
        return maryxmlElement;
    }

    public String getName() {
        return name;
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    public void setFeatureVector(FeatureVector featureVector) {
        this.featureVector = featureVector;
    }

    public float getTargetDurationInSeconds() {
        if (duration != -1) {
            return duration;
        } else {
            if (maryxmlElement == null) {
                return 0;
            }
            // throw new NullPointerException("Target "+name+" does not have a
            // maryxml element.");
            duration = new MaryGenericFeatureProcessors.UnitDuration().process(this);
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
                assert maryxmlElement.getTagName()
                .equals(MaryXML.BOUNDARY) : "segment should be a phone or a boundary, but is a "
                + maryxmlElement.getTagName();
                maryxmlElement.setAttribute("duration", Float.toString(newDuration));
            }
        }
    }

    public float getTargetF0InHz() {
        if (f0 != -1) {
            return f0;
        } else {
            if (maryxmlElement == null) {
                throw new NullPointerException("Target " + name + " does not have a maryxml element.");
            }
            float logf0 = new MaryGenericFeatureProcessors.UnitLogF0().process(this);
            if (logf0 == 0) {
                f0 = 0;
            } else {
                f0 = (float) Math.exp(logf0);
            }
            return f0;
        }
    }

    public boolean hasFeatureVector() {
        return featureVector != null;
    }

    public static UserDataHandler targetFeatureCloner = new UserDataHandler() {
        public void handle(short operation, String key, Object data, Node src, Node dest) {
            if (operation == UserDataHandler.NODE_CLONED && key == "target") {
                dest.setUserData(key, data, this);
            }
        }
    };

    /**
     * Determine whether this target is a silence target
     *
     * @return true if the target represents silence, false otherwise
     */
    public boolean isSilence() {

        if (isSilence == -1) {
            // TODO: how do we know the silence symbol here?
            String silenceSymbol = "_";
            if (name.startsWith(silenceSymbol)) {
                isSilence = 1; // true
            } else {
                isSilence = 0; // false
            }
        }
        return isSilence == 1;
    }

    public String toString() {
        return name;
    }
}
