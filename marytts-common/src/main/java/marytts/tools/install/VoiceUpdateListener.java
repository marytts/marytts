/**
 * Copyright 2009 DFKI GmbH.
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

/*
 * VoiceUpdateListener.java
 *
 * Created on 21. September 2009, 10:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package marytts.tools.install;

/**
 * 
 * @author marc
 */
public interface VoiceUpdateListener {
	/**
	 * Take note of the fact that the current language has changed and the list of voices needs updating.
	 * 
	 * @param currentLanguage
	 *            currentLanguage
	 * @param forceUpdate
	 *            forceUpdate
	 */
	public void updateVoices(LanguageComponentDescription currentLanguage, boolean forceUpdate);

}
