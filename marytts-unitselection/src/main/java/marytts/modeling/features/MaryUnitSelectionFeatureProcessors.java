package marytts.modeling.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import marytts.datatypes.MaryXML;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.ByteStringTranslator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class MaryUnitSelectionFeatureProcessors extends MaryGenericFeatureProcessors
{

	/**
	 * Returns the duration of the given segment, in seconds.
	 */
	public static class UnitDuration implements ContinuousFeatureProcessor {
		public String getName() {
			return "unit_duration";
		}

		public float process(Target target) {
			if (target instanceof DiphoneTarget) {
				DiphoneTarget diphone = (DiphoneTarget) target;
				return process(diphone.left) + process(diphone.right);
			}
			Element seg = target.getMaryxmlElement();
			if (seg == null) {
				return 0;
			}
			float phoneDuration = 0;
			String sDur;
			if (seg.getTagName().equals(MaryXML.PHONE))
				sDur = seg.getAttribute("d");
			else {
				assert seg.getTagName().equals(MaryXML.BOUNDARY) : "segment should be a phone or a boundary, but is a "
						+ seg.getTagName();
				sDur = seg.getAttribute("duration");
			}
			if (sDur.equals("")) {
				return 0;
			}
			try {
				// parse duration string, and convert from milliseconds into seconds:
				phoneDuration = Float.parseFloat(sDur) * 0.001f;
			} catch (NumberFormatException nfe) {
			}
			if (target instanceof HalfPhoneTarget)
				return phoneDuration / 2;
			return phoneDuration;
		}
	}

	/**
	 * Calculates the log of the fundamental frequency in the middle of a unit segment. This processor should be used by target
	 * items only -- for unit features during voice building, the actual measured values should be used.
	 */
	public static class UnitLogF0 implements ContinuousFeatureProcessor {
		public String getName() {
			return "unit_logf0";
		}

		public float process(Target target) {
			return process(target, false);
		}

		/**
		 * Compute log f0 and log f0 delta for the given target.
		 *
		 * @param target
		 *            target
		 * @param delta
		 *            if true, return the delta, i.e. the logF0 slope; if false, return the log f0 value itself.
		 * @return 0 if seg is null, or if !seg.getTagName().equals(MaryXML.PHONE), 0 if lastPos == null or nextPos == null,
		 *         return lastF0 otherwise; if delta return slope, return f0 otherwise
		 */
		protected float process(Target target, boolean delta) {
			// Note: all variables in this method with "f0" in their name
			// actually represent log f0 values.
			if (target instanceof DiphoneTarget) {
				DiphoneTarget diphone = (DiphoneTarget) target;
				return (process(diphone.left) + process(diphone.right)) / 2;
			}
			// Idea: find the closest f0 targets in the current syllable, left and right of our middle;
			// linearly interpolate between them to find the value in the middle of this unit.
			Element seg = target.getMaryxmlElement();
			if (seg == null) {
				return 0;
			}
			if (!seg.getTagName().equals(MaryXML.PHONE)) {
				return 0;
			}
			// get mid position of segment wrt phone start (phone start = 0, phone end = phone duration)
			float mid;
			float phoneDuration = getDuration(seg);
			if (target instanceof HalfPhoneTarget) {
				if (((HalfPhoneTarget) target).isLeftHalf()) {
					mid = .25f;
				} else {
					mid = .75f;
				}
			} else { // phone target
				mid = .5f;
			}

			// Now mid is the middle of the unit relative to the phone start, in percent
			Float lastPos = null; // position relative to mid, in milliseconds (negative)
			float lastF0 = 0;
			Float nextPos = null; // position relative to mid, in milliseconds
			float nextF0 = 0;
			Float[] f0values = getLogF0Values(seg);

			assert f0values != null;
			// values are position, f0, position, f0, etc.;
			// position is in percent of phone duration between 0 and 1, f0 is in Hz
			for (int i = 0; i < f0values.length; i += 2) {
				float pos = f0values[i];
				if (pos <= mid) {
					lastPos = (pos - mid) * phoneDuration; // negative or zero
					lastF0 = f0values[i + 1];
				} else if (pos > mid) {
					nextPos = (pos - mid) * phoneDuration; // positive
					nextF0 = f0values[i + 1];
					break; // no point looking further to the right
				}
			}
			if (lastPos == null) { // need to look to the left
				float msBack = -mid * phoneDuration;
				Element e = seg;

				// get all phone units in the same phrase
				Element phraseElement = (Element) MaryDomUtils.getAncestor(seg, MaryXML.PHRASE);
				TreeWalker tw = MaryDomUtils.createTreeWalker(seg.getOwnerDocument(), phraseElement, MaryXML.PHONE);
				Element en;
				while ((en = (Element) tw.nextNode()) != null) {
					if (en == seg) {
						break;
					}
				}

				while ((e = (Element) tw.previousNode()) != null) {
					float dur = getDuration(e);
					f0values = getLogF0Values(e);
					if (f0values.length == 0) {
						msBack -= dur;
						continue;
					}
					assert f0values.length > 1;
					float pos = f0values[f0values.length - 2];
					lastPos = msBack - (1 - pos) * dur;
					lastF0 = f0values[f0values.length - 1];
					break;
				}
			}

			if (nextPos == null) { // need to look to the right
				float msForward = (1 - mid) * phoneDuration;
				Element e = seg;

				// get all phone units in the same phrase
				Element phraseElement = (Element) MaryDomUtils.getAncestor(seg, MaryXML.PHRASE);
				TreeWalker tw = MaryDomUtils.createTreeWalker(seg.getOwnerDocument(), phraseElement, MaryXML.PHONE);
				Element en;
				while ((en = (Element) tw.nextNode()) != null) {
					if (en == seg) {
						break;
					}
				}

				while ((e = (Element) tw.nextNode()) != null) {
					float dur = getDuration(e);
					f0values = getLogF0Values(e);
					if (f0values.length == 0) {
						msForward += dur;
						continue;
					}
					assert f0values.length > 1;
					float pos = f0values[0];
					nextPos = msForward + pos * dur;
					nextF0 = f0values[1];
					break;
				}
			}

			if (lastPos == null && nextPos == null) {
				// no info
				return 0;
			} else if (lastPos == null) {
				// have only nextF0;
				if (delta)
					return 0;
				else
					return nextF0;
			} else if (nextPos == null) {
				// have only lastF0
				if (delta)
					return 0;
				else
					return lastF0;
			}
			assert lastPos <= 0 && 0 <= nextPos : "unexpected: lastPos=" + lastPos + ", nextPos=" + nextPos;
			// build a linear function (f(x) = slope*x+intersectionYAxis)
			float f0;
			float slope;
			if (lastPos - nextPos == 0) {
				f0 = (lastF0 + nextF0) / 2;
				slope = 0;
			} else {
				slope = (nextF0 - lastF0) / (nextPos - lastPos);
				// calculate the pitch
				f0 = lastF0 + slope * (-lastPos);
			}
			assert !Float.isNaN(f0) : "f0 is not a number";
			assert lastF0 <= f0 && nextF0 >= f0 || lastF0 >= f0 && nextF0 <= f0 : "f0 should be between last and next values";

			if (delta)
				return slope;
			else
				return f0;
		}

		private Float[] getLogF0Values(Element ph) {
			String mbrTargets = ph.getAttribute("f0");
			if (mbrTargets.equals("")) {
				return new Float[0];
			}
			ArrayList<Float> values = new ArrayList<Float>();
			try {
				// mbrTargets contains one or more pairs of numbers,
				// either enclosed by (a,b) or just separated by whitespace.
				StringTokenizer st = new StringTokenizer(mbrTargets, " (,)");
				while (st.hasMoreTokens()) {
					String posString = "";
					while (st.hasMoreTokens() && posString.equals(""))
						posString = st.nextToken();
					String f0String = "";
					while (st.hasMoreTokens() && f0String.equals(""))
						f0String = st.nextToken();

					float pos = Float.parseFloat(posString) * 0.01f;
					assert 0 <= pos && pos <= 1 : "invalid position:" + pos + " (pos string was '" + posString
							+ "' coming from '" + mbrTargets + "')";
					float f0 = Float.parseFloat(f0String);
					float logF0 = (float) Math.log(f0);
					values.add(pos);
					values.add(logF0);
				}
			} catch (Exception e) {
				return new Float[0];
			}
			return values.toArray(new Float[0]);
		}

		private float getDuration(Element ph) {
			float phoneDuration = 0;
			String sDur = ph.getAttribute("d");
			if (!sDur.equals("")) {
				try {
					phoneDuration = Float.parseFloat(sDur);
				} catch (NumberFormatException nfe) {
				}
			}
			return phoneDuration;
		}
	}

	/**
	 * Calculates the slope of a linear approximation of the fundamental frequency, in the log domain. The slope is computed by
	 * linearly connecting the two log f0 values closest to the middle of the unit segment. This processor should be used by
	 * target items only -- for unit features during voice building, the actual measured values should be used.
	 */
	public static class UnitLogF0Delta extends UnitLogF0 {
		@Override
		public String getName() {
			return "unit_logf0delta";
		}

		public float process(Target target) {
			return process(target, true);
		}
	}

	/**
	 * Returns the value of the given feature for the given segment.
	 */
	public static class GenericContinuousFeature implements ContinuousFeatureProcessor {
		private String name;
		private String attributeName;

		public GenericContinuousFeature(String featureName, String attributeName) {
			this.name = featureName;
			this.attributeName = attributeName;
		}

		public String getName() {
			return name;
		}

		public float process(Target target) {
			if (target instanceof DiphoneTarget) {
				DiphoneTarget diphone = (DiphoneTarget) target;
				// return mean of left and right costs:
				return (process(diphone.left) + process(diphone.right)) / 2.0f;
			}
			Element seg = target.getMaryxmlElement();
			if (seg == null) {
				return 0;
			}
			float value = 0;
			String valueString;
			if (seg.getTagName().equals(MaryXML.PHONE)) {
				valueString = seg.getAttribute(attributeName);
			} else {
				assert seg.getTagName().equals(MaryXML.BOUNDARY) : "segment should be a phone or a boundary, but is a "
						+ seg.getTagName();
				valueString = seg.getAttribute(attributeName);
			}
			if (valueString.equals("")) {
				return 0;
			}
			try {
				value = Float.parseFloat(valueString);
			} catch (NumberFormatException nfe) {
				return 0;
			}
			return value;
		}
	}



	/**
	 * The unit name for the given half phone target.
	 *
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class HalfPhoneUnitName implements ByteValuedFeatureProcessor {
		protected String name;
		protected ByteStringTranslator values;
		protected String pauseSymbol;

		/**
		 * Initialise a UnitName feature processor.
		 *
		 * @param possiblePhonemes
		 *            the possible phonemes
		 * @param pauseSymbol
		 *            the pause symbol
		 */
		public HalfPhoneUnitName(String[] possiblePhonemes, String pauseSymbol) {
			this.name = "halfphone_unitname";
			this.pauseSymbol = pauseSymbol;
			String[] possibleValues = new String[2 * possiblePhonemes.length + 1];
			possibleValues[0] = "0"; // the "n/a" value
			for (int i = 0; i < possiblePhonemes.length; i++) {
				possibleValues[2 * i + 1] = possiblePhonemes[i] + "_L";
				possibleValues[2 * i + 2] = possiblePhonemes[i] + "_R";
			}
			this.values = new ByteStringTranslator(possibleValues);
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			if (!(target instanceof HalfPhoneTarget))
				return 0;
			HalfPhoneTarget hpTarget = (HalfPhoneTarget) target;
			Element segment = target.getMaryxmlElement();
			String phoneLabel;
			if (segment == null) {
				phoneLabel = pauseSymbol;
			} else if (!segment.getTagName().equals(MaryXML.PHONE)) {
				phoneLabel = pauseSymbol;
			} else {
				phoneLabel = segment.getAttribute("p");
			}
			if (phoneLabel.equals(""))
				return values.get("0");
			String unitLabel = phoneLabel + (hpTarget.isLeftHalf() ? "_L" : "_R");
			return values.get(unitLabel);
		}
	}

	/**
	 * Is the given half phone target a left or a right half?
	 *
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class HalfPhoneLeftRight implements ByteValuedFeatureProcessor {
		protected ByteStringTranslator values;

		/**
		 * Initialise a HalfPhoneLeftRight feature processor.
		 */
		public HalfPhoneLeftRight() {
			this.values = new ByteStringTranslator(new String[] { "0", "L", "R" });
		}

		public String getName() {
			return "halfphone_lr";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			if (!(target instanceof HalfPhoneTarget))
				return 0;
			HalfPhoneTarget hpTarget = (HalfPhoneTarget) target;
			String value = (hpTarget.isLeftHalf() ? "L" : "R");
			return values.get(value);
		}
	}
}


/* MaryUnitSelectionFeatureProcessors.java ends here */
