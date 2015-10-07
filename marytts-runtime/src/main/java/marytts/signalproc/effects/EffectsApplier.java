/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.effects;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

/**
 * @author Oytun T&uuml;rk
 */
public class EffectsApplier {
	public BaseAudioEffect[] audioEffects;
	public int[] optimumEffectIndices;
	public static char chEffectSeparator = '+';
	private ArrayList<String> optimumOrderedEffectNames;

	public EffectsApplier() {
		getOptimizedEffectOrdering();
	}

	public AudioInputStream apply(AudioInputStream input, String param) {
		AudioFormat audioformat = input.getFormat();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(input);
		DoubleDataSource tmpSignal = null;

		parseEffectsAndParams(param, (int) audioformat.getSampleRate());
		boolean bFirstEffect = true;

		if (audioEffects != null) // There are audio effects to apply
		{
			int index;

			for (int i = 0; i < audioEffects.length; i++) {
				if (optimumEffectIndices != null && optimumEffectIndices[i] >= 0 && optimumEffectIndices[i] < audioEffects.length)
					index = optimumEffectIndices[i];
				else
					index = i;

				if (audioEffects[index] != null) {
					if (bFirstEffect) {
						if (audioEffects[index] != null) {
							tmpSignal = audioEffects[index].apply(signal);
							bFirstEffect = false;
						}
					} else {
						if (audioEffects[index] != null)
							tmpSignal = audioEffects[index].apply(tmpSignal);
					}
				}
			}

			if (tmpSignal != null) {
				if (tmpSignal.getDataLength() == DoubleDataSource.NOT_SPECIFIED) {
					double[] data = tmpSignal.getAllData();
					tmpSignal = new BufferedDoubleDataSource(data);
				}
				assert tmpSignal.getDataLength() != DoubleDataSource.NOT_SPECIFIED;
				return new DDSAudioInputStream(tmpSignal, audioformat);
			} else
				return input;
		} else
			return input;
	}

	// Extract effects and parameters and create the corresponding effects at a default sampling rate
	public void parseEffectsAndParams(String param) {
		parseEffectsAndParams(param, 16000);
	}

	// Extract effects and parameters and create the corresponding effects
	public void parseEffectsAndParams(String param, int samplingRate) {
		audioEffects = null;
		optimumEffectIndices = null;

		if (param != null && param.length() > 0) {
			param = StringUtils.deblank(param);
			int[] effectInds = StringUtils.find(param, chEffectSeparator);
			int numEffects = 0;

			if (effectInds != null) {
				numEffects = effectInds.length;
				if (effectInds[effectInds.length - 1] != param.length())
					numEffects++;
			} else {
				if (param.length() != 0)
					numEffects = 1;
			}

			if (numEffects > 0) {
				int totalNonEmptyEffects = 0;
				String[] strEffectNames = new String[numEffects];
				String[] strParamsAlls = new String[numEffects];

				String strEffectName, strParams;
				int[] paramInds;
				int i;
				for (i = 0; i < numEffects; i++) {
					if (i == 0) {
						if (numEffects == 1 || effectInds == null)
							strEffectName = param;
						else
							strEffectName = param.substring(0, effectInds[0]);
					} else {
						if (effectInds == null)
							strEffectName = param;
						else if (i < effectInds.length)
							strEffectName = param.substring(effectInds[i - 1] + 1, effectInds[i]);
						else
							strEffectName = param.substring(effectInds[i - 1] + 1, param.length());
					}

					strEffectName = StringUtils.deblank(strEffectName);

					if (strEffectName != null && strEffectName != "") {
						paramInds = StringUtils.find(strEffectName, BaseAudioEffect.chEffectParamStart);
						if (paramInds != null) {
							int stParam = MathUtils.max(paramInds);
							paramInds = StringUtils.find(strEffectName, BaseAudioEffect.chEffectParamEnd);
							if (paramInds != null) {
								int enParam = MathUtils.min(paramInds);

								strParams = strEffectName.substring(stParam + 1, enParam);
								strParams = StringUtils.deblank(strParams);
							} else
								strParams = "";

							strEffectName = strEffectName.substring(0, stParam);
							strEffectName = StringUtils.deblank(strEffectName);
						} else
							strParams = "";
					} else
						strParams = "";

					if (strEffectName != null && strEffectName != "") {
						strEffectNames[i] = strEffectName;
						strParamsAlls[i] = strParams;
						totalNonEmptyEffects++;
					}
				}

				int index = 0;
				if (totalNonEmptyEffects > 0) {
					audioEffects = new BaseAudioEffect[totalNonEmptyEffects];
					for (i = 0; i < numEffects; i++) {
						if (isEffectAvailable(strEffectNames[i])) {
							if (index < totalNonEmptyEffects) {
								audioEffects[index] = string2AudioEffect(strEffectNames[i], samplingRate);
								audioEffects[index].setName(strEffectNames[i]);
								audioEffects[index].setParams(strParamsAlls[i]);
								index++;
							} else
								break;
						}
					}

					optimizeEffectsOrdering();
				} else {
					audioEffects = null;
					optimumEffectIndices = null;
				}
			} else {
				audioEffects = null;
				optimumEffectIndices = null;
			}
		}
	}

	public BaseAudioEffect string2AudioEffect(String strEffectName, int samplingRate) {
		if (strEffectName.compareToIgnoreCase("Volume") == 0)
			return new VolumeEffect();
		else if (strEffectName.compareToIgnoreCase("Robot") == 0)
			return new RobotiserEffect(samplingRate);
		else if (strEffectName.compareToIgnoreCase("Chorus") == 0)
			return new ChorusEffectBase(samplingRate);
		else if (strEffectName.compareToIgnoreCase("Stadium") == 0)
			return new StadiumEffect(samplingRate);
		else if (strEffectName.compareToIgnoreCase("FIRFilter") == 0)
			return new FilterEffectBase(samplingRate);
		else if (strEffectName.compareToIgnoreCase("JetPilot") == 0)
			return new JetPilotEffect(samplingRate);
		else if (strEffectName.compareToIgnoreCase("Whisper") == 0)
			return new LpcWhisperiserEffect(samplingRate);
		else if (strEffectName.compareToIgnoreCase("TractScaler") == 0)
			return new VocalTractLinearScalerEffect(samplingRate);
		else if (strEffectName.compareToIgnoreCase("F0Add") == 0)
			return new HMMF0AddEffect();
		else if (strEffectName.compareToIgnoreCase("F0Scale") == 0)
			return new HMMF0ScaleEffect();
		else if (strEffectName.compareToIgnoreCase("Rate") == 0)
			return new HMMDurationScaleEffect();
		else
			return null;
	}

	// Check whether a desired effect is available in the optimum ordered list
	// If there are no optimum ordered lists available, then the EffectsApplier object is constructed with the null constructor
	// In this case, check if any audio effect is null before actually applying it
	public boolean isEffectAvailable(String effectName) {
		boolean returnVal = false;

		if (effectName != null && effectName != "") {
			if (optimumOrderedEffectNames != null) {
				for (Iterator it = optimumOrderedEffectNames.iterator(); it.hasNext();) {
					if (effectName.compareToIgnoreCase((String) it.next()) == 0) {
						returnVal = true;
						break;
					}
				}
			} else
				returnVal = true;
		}

		return returnVal;
	}

	// Get optimized effect ordering from marybase.config in case of multiple effects being applied one after another
	// The "optimal" ordering should be determined by testing and the audioeffects.classes.list entry in marybase.config
	// should be arranged in the same order
	public void getOptimizedEffectOrdering() {
		// Get optimal ordering from .config file
		optimumOrderedEffectNames = new ArrayList<String>();
		for (AudioEffect effect : AudioEffects.getEffects()) {
			optimumOrderedEffectNames.add(effect.getName());
		}
	}

	// In case of multiple effects, use a pre-determined order to sort the effects in order to minimize distortion
	// when applying effects consecutively.
	// This ordering should be the one given "audioeffects.classes.list" in "marybase.config"
	public void optimizeEffectsOrdering() {
		if (optimumOrderedEffectNames != null && optimumOrderedEffectNames.size() > 0 && audioEffects != null
				&& audioEffects.length > 0) {
			optimumEffectIndices = new int[audioEffects.length];
			int index = -1;
			int i;
			boolean bBroke = false;
			String tmpName;
			for (Iterator it = optimumOrderedEffectNames.iterator(); it.hasNext();) {
				tmpName = (String) it.next();
				for (i = 0; i < audioEffects.length; i++) {
					if (audioEffects[i] != null && audioEffects[i].getName() == tmpName) {
						index++;
						if (index > audioEffects.length - 1) {
							bBroke = true;
							break;
						} else
							optimumEffectIndices[index] = i;
					}
				}
			}
		} else
			optimumEffectIndices = null;
	}

	// Check if any effects are selected for which the corresponding parameters should be fed to the HMM synthesizer
	public void setHMMEffectParameters(Voice voice, String currentEffect) {

		if (voice instanceof HMMVoice) {
			// Just create dummy effects to set default values for HMM voices
			HMMF0AddEffect dummy1 = new HMMF0AddEffect();
			HMMF0ScaleEffect dummy2 = new HMMF0ScaleEffect();
			HMMDurationScaleEffect dummy3 = new HMMDurationScaleEffect();
			((HMMVoice) voice).setF0Mean(dummy1.NO_MODIFICATION);
			((HMMVoice) voice).setF0Std(dummy2.NO_MODIFICATION);
			((HMMVoice) voice).setDurationScale(dummy3.NO_MODIFICATION);
			//

			parseEffectsAndParams(currentEffect);

			if (audioEffects != null) {
				for (int i = 0; i < audioEffects.length; i++) {
					if (audioEffects[i] instanceof HMMF0AddEffect)
						((HMMVoice) voice).setF0Mean((double) ((HMMF0AddEffect) audioEffects[i]).f0Add);
					else if (audioEffects[i] instanceof HMMF0ScaleEffect)
						((HMMVoice) voice).setF0Std(((HMMF0ScaleEffect) audioEffects[i]).f0Scale);
					else if (audioEffects[i] instanceof HMMDurationScaleEffect)
						((HMMVoice) voice).setDurationScale(((HMMDurationScaleEffect) audioEffects[i]).durScale);
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		EffectsApplier e = new EffectsApplier();

		// String strEffectsAndParams = "FIRFilter+Robot(amount=50)";
		String strEffectsAndParams = "Robot(amount=100)+Chorus(delay1=866, amp1=0.24, delay2=300, amp2=-0.40,)";
		// String strEffectsAndParams = "Robot(amount=80)+Stadium(amount=50)";
		// String strEffectsAndParams = "FIRFilter(type=3,fc1=6000, fc2=10000) + Robot";
		// String strEffectsAndParams =
		// "Stadium(amount=40) + Robot(amount=87) + Whisper(amount=65)+FIRFilter(type=1,fc1=1540;)++";

		AudioInputStream input = AudioSystem.getAudioInputStream(new File(args[0]));

		AudioInputStream output = e.apply(input, strEffectsAndParams);

		AudioSystem.write(output, AudioFileFormat.Type.WAVE, new File(args[1]));
	}
}
