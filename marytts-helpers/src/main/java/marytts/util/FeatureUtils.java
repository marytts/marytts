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
package marytts.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import marytts.modeling.features.FeatureDefinition;

/**
 * @author marc
 *
 */
public class FeatureUtils {

    public static FeatureDefinition readFeatureDefinition(String targetFeaturesData) throws
        IOException {
        BufferedReader in = new BufferedReader(new StringReader(targetFeaturesData));
        try {
            return new FeatureDefinition(in, false);
        } finally {
            in.close();
        }
    }

    public static FeatureDefinition readFeatureDefinition(InputStream featureStream) throws
        IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(featureStream, "UTF-8"));
        try {
            return new FeatureDefinition(in, false);
        } finally {
            in.close();
        }
    }

}
