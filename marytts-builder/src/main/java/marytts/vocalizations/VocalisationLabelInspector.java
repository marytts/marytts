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
package marytts.vocalizations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.tools.voiceimport.UnitLabel;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.io.BasenameList;

public class VocalisationLabelInspector {

	private String inLocation; // input directory
	private String outLocation; // output directory
	private AudioFormat format;

	/**
	 * 
	 * @param inLocation
	 *            inLocation
	 * @param outLocation
	 *            outLocation
	 */
	public VocalisationLabelInspector(String inLocation, String outLocation) {
		this.inLocation = inLocation;
		this.outLocation = outLocation;
	}

	public void process(String baseName) throws IOException {

		String labelFile = inLocation + File.separator + baseName + ".lab";
		String outlabelFile = outLocation + File.separator + baseName + ".lab";
		String waveFile = inLocation + File.separator + baseName + ".wav";
		String outwaveFile = outLocation + File.separator + baseName + ".wav";

		// read labels
		UnitLabel[] vocalLabels = UnitLabel.readLabFile(labelFile);

		// read waveform
		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(new File(waveFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
		format = audioInputStream.getFormat();
		double[] signal = MaryAudioUtils.getSamplesAsDoubleArray(audioInputStream);

		/*
		 * // get start and end pause durations from labels double sPauseDuration = getStartPauseDuration(vocalLabels); double
		 * ePauseDuration = getEndPauseDuration(vocalLabels);
		 * 
		 * try { int startStampIndex = (int) (sPauseDuration * format.getSampleRate()); int endStampIndex = signal.length - (int)
		 * (ePauseDuration * format.getSampleRate()) - 1; double[] newSignal = new double[endStampIndex-startStampIndex];
		 * System.arraycopy(signal, startStampIndex, newSignal, 0, endStampIndex-startStampIndex);
		 * MaryAudioUtils.writeWavFile(newSignal, outwaveFile, format); } catch(Exception e){
		 * System.out.println("problem : "+baseName); }
		 */

		// get start and end pause durations from labels
		double sPauseStamp = getStartTimeStamp(vocalLabels);
		double ePauseStamp = getEndTimeStamp(vocalLabels);

		int startStampIndex = (int) (sPauseStamp * format.getSampleRate());
		int endStampIndex = (int) (ePauseStamp * format.getSampleRate());
		double[] newSignal = new double[endStampIndex - startStampIndex];
		System.arraycopy(signal, startStampIndex, newSignal, 0, endStampIndex - startStampIndex);
		MaryAudioUtils.writeWavFile(newSignal, outwaveFile, format);
		UnitLabel[] newVocalLabels = removePausesFromLabels(vocalLabels);
		UnitLabel.writeLabFile(newVocalLabels, outlabelFile);

	}

	private UnitLabel[] removePausesFromLabels(UnitLabel[] vocalLabels) {

		ArrayList<UnitLabel> uLabels = new ArrayList<UnitLabel>();

		// put into a list
		for (int i = 0; i < vocalLabels.length; i++) {
			uLabels.add(vocalLabels[i]);
		}

		// remove back pauses first
		for (int i = (uLabels.size() - 1); i > 0; i--) {
			if ("_".equals(vocalLabels[i].unitName)) {
				uLabels.remove(i);
			} else {
				break;
			}
		}

		// remove front pauses next
		for (int i = 0; i < uLabels.size(); i++) {
			if ("_".equals(vocalLabels[i].unitName)) {
				uLabels.remove(i);
			} else {
				break;
			}
		}

		// post process unit labels
		double removedPauseTime = (uLabels.get(0)).getStartTime();
		for (int i = 0; i < uLabels.size(); i++) {

			UnitLabel ulab = uLabels.get(i);
			uLabels.remove(i);
			double sTime = ulab.getStartTime();
			double eTime = ulab.getEndTime();
			ulab.setStartTime(sTime - removedPauseTime);
			ulab.setEndTime(eTime - removedPauseTime);
			uLabels.add(i, ulab);
		}

		return uLabels.toArray(new UnitLabel[0]);
	}

	/**
	 * 
	 * @param vocalLabels
	 *            vocalLabels
	 * @return 0.0
	 */
	private double getStartPauseDuration(UnitLabel[] vocalLabels) {

		boolean isStartPause = false;
		for (int i = 0; i < vocalLabels.length; i++) {

			if (i == 0 && "_".equals(vocalLabels[i].unitName)) {
				isStartPause = true;
				continue;
			}

			if (isStartPause && !"_".equals(vocalLabels[i].unitName)) {
				return vocalLabels[i].startTime;
			}
		}

		return 0.0;
	}

	/**
	 * 
	 * @param vocalLabels
	 *            vocalLabels
	 * @return 0.0
	 */
	private double getEndPauseDuration(UnitLabel[] vocalLabels) {

		boolean isEndPause = false;

		for (int i = (vocalLabels.length - 1); i > 0; i--) {

			if (i == (vocalLabels.length - 1) && "_".equals(vocalLabels[i].unitName)) {
				isEndPause = true;
				continue;
			}

			if (isEndPause && !"_".equals(vocalLabels[i].unitName)) {
				return (vocalLabels[vocalLabels.length - 1].endTime - vocalLabels[i].endTime);
			}
		}

		return 0.0;
	}

	/**
	 * 
	 * @param vocalLabels
	 *            vocalLabels
	 * @return 0.0
	 */
	private double getStartTimeStamp(UnitLabel[] vocalLabels) {

		boolean isStartPause = false;
		for (int i = 0; i < vocalLabels.length; i++) {

			if (i == 0 && "_".equals(vocalLabels[i].unitName)) {
				isStartPause = true;
				continue;
			}

			if (isStartPause && !"_".equals(vocalLabels[i].unitName)) {
				return vocalLabels[i].startTime;
			}
		}

		return 0.0;
	}

	/**
	 * 
	 * @param vocalLabels
	 *            vocalLabels
	 * @return vocalLabels[vocalLabels.length -1].endTime
	 */
	private double getEndTimeStamp(UnitLabel[] vocalLabels) {

		boolean isEndPause = false;

		for (int i = (vocalLabels.length - 1); i > 0; i--) {

			if (i == (vocalLabels.length - 1) && "_".equals(vocalLabels[i].unitName)) {
				isEndPause = true;
				continue;
			}

			if (isEndPause && !"_".equals(vocalLabels[i].unitName)) {
				return vocalLabels[i].endTime;
			}
		}

		return vocalLabels[vocalLabels.length - 1].endTime;
	}

	/**
	 * @param args
	 *            args
	 * @throws IOException
	 *             IOException
	 */
	public static void main(String[] args) throws IOException {

		// String inDirName = "/home/sathish/phd/data/original-lab-wav-sync";
		// String outDirName = "/home/sathish/phd/data/pauseless-lab-wav-sync";
		String inDirName = "/home/sathish/phd/data/original_stimulus_sync";
		String outDirName = "/home/sathish/phd/data/pauseless_stimulus_sync";
		BasenameList bnl = new BasenameList(inDirName, ".wav");

		VocalisationLabelInspector vli = new VocalisationLabelInspector(inDirName, outDirName);

		for (int i = 0; i < bnl.getLength(); i++) {
			vli.process(bnl.getName(i));
		}

	}

}
