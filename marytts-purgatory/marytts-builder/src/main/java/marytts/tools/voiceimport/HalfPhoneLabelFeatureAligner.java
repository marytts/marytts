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
package marytts.tools.voiceimport;

/**
 * @author marc
 *
 */
public class HalfPhoneLabelFeatureAligner extends PhoneLabelFeatureAligner {

	public String getName() {
		return "HalfPhoneLabelFeatureAligner";
	}

	public HalfPhoneLabelFeatureAligner() {
	}

	@Override
	protected void customInitialisation() {
		featureComputer = (HalfPhoneUnitFeatureComputer) db.getComponent("HalfPhoneUnitFeatureComputer");
		allophoneExtractor = (AllophonesExtractor) db.getComponent("AllophonesExtractor");
		labelComputer = (HalfPhoneUnitLabelComputer) db.getComponent("HalfPhoneUnitLabelComputer");
		transcriptionAligner = (TranscriptionAligner) db.getComponent("TranscriptionAligner");
		featsExt = ".hpfeats";
		labExt = ".hplab";
		featsDir = db.getProp(db.HALFPHONEFEATUREDIR);
		labDir = db.getProp(db.HALFPHONELABDIR);
	}

}
