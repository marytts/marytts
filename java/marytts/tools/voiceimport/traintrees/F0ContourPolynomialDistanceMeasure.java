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

package marytts.tools.voiceimport.traintrees;

import java.io.File;
import java.io.IOException;

import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;
import marytts.util.math.Polynomial;

/**
 * @author marc
 *
 */
public class F0ContourPolynomialDistanceMeasure implements DistanceMeasure
{
    private float[][] contourCoeffs;
    
    public F0ContourPolynomialDistanceMeasure(FeatureFileReader contours)
    throws IOException
    {
        this.contourCoeffs = new float[contours.getNumberOfUnits()][];
        for (int i=0; i<contourCoeffs.length; i++) {
            contourCoeffs[i] = contours.getFeatureVector(i).getContinuousFeatures();
        }
    }
    
    /**
     * Compute the distance between the f0 contours corresponding to the given feature vectors.
     * From the feature vectors, only their unit index number is used.
     * @see marytts.tools.voiceimport.traintrees.DistanceMeasure#distance(marytts.features.FeatureVector, marytts.features.FeatureVector)
     */
    public float distance(FeatureVector fv1, FeatureVector fv2)
    {
        int i1 = fv1.getUnitIndex();
        int i2 = fv2.getUnitIndex();
        float dist = (float) Polynomial.polynomialDistance(contourCoeffs[i1], contourCoeffs[i2]);
        return dist;
    }

    /**
     * Compute the distance between the f0 contours corresponding to the given feature vectors.
     * From the feature vectors, only their unit index number is used.
     * @see marytts.tools.voiceimport.traintrees.DistanceMeasure#distance(marytts.features.FeatureVector, marytts.features.FeatureVector)
     */
    public float squaredDistance(FeatureVector fv1, FeatureVector fv2)
    {
        int i1 = fv1.getUnitIndex();
        int i2 = fv2.getUnitIndex();
        float dist = (float) Polynomial.polynomialSquaredDistance(contourCoeffs[i1], contourCoeffs[i2]);
        return dist;
    }
}
