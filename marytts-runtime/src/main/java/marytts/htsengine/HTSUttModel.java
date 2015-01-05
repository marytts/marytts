/* ----------------------------------------------------------------- */
/*           The HMM-Based Speech Synthesis Engine "hts_engine API"  */
/*           developed by HTS Working Group                          */
/*           http://hts-engine.sourceforge.net/                      */
/* ----------------------------------------------------------------- */
/*                                                                   */
/*  Copyright (c) 2001-2010  Nagoya Institute of Technology          */
/*                           Department of Computer Science          */
/*                                                                   */
/*                2001-2008  Tokyo Institute of Technology           */
/*                           Interdisciplinary Graduate School of    */
/*                           Science and Engineering                 */
/*                                                                   */
/* All rights reserved.                                              */
/*                                                                   */
/* Redistribution and use in source and binary forms, with or        */
/* without modification, are permitted provided that the following   */
/* conditions are met:                                               */
/*                                                                   */
/* - Redistributions of source code must retain the above copyright  */
/*   notice, this list of conditions and the following disclaimer.   */
/* - Redistributions in binary form must reproduce the above         */
/*   copyright notice, this list of conditions and the following     */
/*   disclaimer in the documentation and/or other materials provided */
/*   with the distribution.                                          */
/* - Neither the name of the HTS working group nor the names of its  */
/*   contributors may be used to endorse or promote products derived */
/*   from this software without specific prior written permission.   */
/*                                                                   */
/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            */
/* CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       */
/* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          */
/* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS */
/* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          */
/* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   */
/* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     */
/* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON */
/* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   */
/* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    */
/* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           */
/* POSSIBILITY OF SUCH DAMAGE.                                       */
/* ----------------------------------------------------------------- */
/**
 * Copyright 2011 DFKI GmbH.
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

package marytts.htsengine;

import java.util.Vector;

/**
 * list of Model objects for current utterance.
 * 
 * Java port and extension of HTS engine API version 1.04 Extension: mixed excitation
 * 
 * @author Marcela Charfuelan
 */
public class HTSUttModel {

	private int numModel; /* # of models for current utterance */
	private int numState; /* # of HMM states for current utterance */
	private int totalFrame; /* # of frames for current utterance */
	private int lf0Frame; /* # of frames that are voiced or non-zero */
	private Vector<HTSModel> modelList; /* This will be a list of Model objects for current utterance */
	private String realisedAcoustParams; /* list of phones and actual realised durations for each one */

	public HTSUttModel() {
		numModel = 0;
		numState = 0;
		totalFrame = 0;
		lf0Frame = 0;
		modelList = new Vector<HTSModel>();
		realisedAcoustParams = "";
	}

	public void setNumModel(int val) {
		numModel = val;
	}

	public int getNumModel() {
		return numModel;
	}

	public void setNumState(int val) {
		numState = val;
	}

	public int getNumState() {
		return numState;
	}

	public void setTotalFrame(int val) {
		totalFrame = val;
	}

	public int getTotalFrame() {
		return totalFrame;
	}

	public void setLf0Frame(int val) {
		lf0Frame = val;
	}

	public int getLf0Frame() {
		return lf0Frame;
	}

	public void addUttModel(HTSModel newModel) {
		modelList.addElement(newModel);
	}

	public HTSModel getUttModel(int i) {
		return (HTSModel) modelList.elementAt(i);
	}

	public int getNumUttModel() {
		return modelList.size();
	}

	public void setRealisedAcoustParams(String str) {
		realisedAcoustParams = str;
	}

	public String getRealisedAcoustParams() {
		return realisedAcoustParams;
	}

	public void concatRealisedAcoustParams(String str) {
		realisedAcoustParams = realisedAcoustParams + str;
	}

}
