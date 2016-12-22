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
package marytts.client;

/**
 * Data for an audio effect control.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class AudioEffectControlData {
	private String effectName;
	private String helpText;
	private String exampleParams;
	private String params;
	private boolean isSelected;

	public AudioEffectControlData(String strEffectNameIn, String strExampleParams, String strHelpTextIn) {
		init(strEffectNameIn, strExampleParams, strHelpTextIn);
	}

	public void init(String strEffectNameIn, String strExampleParamsIn, String strHelpTextIn) {
		setEffectName(strEffectNameIn);
		setExampleParams(strExampleParamsIn);
		setHelpText(strHelpTextIn);
		setEffectParamsToExample();
	}

	public void setEffectName(String strEffectName) {
		effectName = strEffectName;
	}

	public String getEffectName() {
		return effectName;
	}

	public void setHelpText(String strHelpText) {
		helpText = strHelpText;
	}

	public String getHelpText() {
		return helpText;
	}

	public void setParams(String strParams) {
		params = strParams;
	}

	public String getParams() {
		return params;
	}

	public void setExampleParams(String strExampleParams) {
		exampleParams = strExampleParams;
	}

	public String getExampleParams() {
		return exampleParams;
	}

	public void setEffectParamsToExample() {
		setParams(exampleParams);
	}

	public void setSelected(boolean bSelected) {
		isSelected = bSelected;
	}

	public boolean getSelected() {
		return isSelected;
	}

}
