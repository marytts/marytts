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
package marytts.signalproc.adaptation.gmm.classicalgmm;

import marytts.signalproc.adaptation.BaselineTrainerParams;

/**
 * Parameters if classical GMM training
 *  
 * @author Oytun T&uumlrk
 */
public class ClassicalGMMTrainerParams extends BaselineTrainerParams {
    //
    public int trainingType;
    public static final int FULL_CONVERSION = 1;
    public static final int DIAGONAL_CONVERSION = 2;
    public static final int VQ_CONVERSION = 3;
    //

}

