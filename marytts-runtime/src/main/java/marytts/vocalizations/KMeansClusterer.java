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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.machinelearning.KMeansClusteringTrainerParams;
import marytts.machinelearning.PolynomialCluster;
import marytts.machinelearning.PolynomialKMeansClusteringTrainer;
import marytts.util.math.Polynomial;

public class KMeansClusterer {

	private ArrayList<String> baseNames;
	Polynomial[] f0Polynomials;
	private int polynomialOrder = 3;
	private int numberOfSamples = 0;
	private HashMap<Polynomial, String> mapPolynomialBaseNames;

	public KMeansClusterer() {
		baseNames = new ArrayList<String>();
		mapPolynomialBaseNames = new HashMap<Polynomial, String>();
	}

	public void loadF0Polynomials(String fileName) throws IOException {

		BufferedReader bfr = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		ArrayList<String> lines = new ArrayList<String>();
		while ((line = bfr.readLine()) != null) {
			lines.add(line.trim());
		}
        bfr.close();
		double[][] f0PolynomialCoeffs;
		String[] words = lines.get(0).trim().split("\\s+");
		polynomialOrder = words.length - 1;
		numberOfSamples = lines.size();
		f0PolynomialCoeffs = new double[numberOfSamples][polynomialOrder];
		f0Polynomials = new Polynomial[numberOfSamples];

		for (int i = 0; i < lines.size(); i++) {
			line = lines.get(i);
			words = line.trim().split("\\s+");
			baseNames.add(words[0].trim());

			for (int j = 0; j < polynomialOrder; j++) {
				f0PolynomialCoeffs[i][j] = Double.valueOf(words[j + 1].trim());
			}

			// System.out.println(f0PolynomialCoeffs[i][0]+" "+f0PolynomialCoeffs[i][1]+" "+f0PolynomialCoeffs[i][2]+" "+f0PolynomialCoeffs[i][3]);
			// System.out.println(words[0]+" "+words[1]+" "+words[2]+" "+words[3]+" "+words[4]);
		}

		// testing
		for (int i = 0; i < numberOfSamples; i++) {
			f0Polynomials[i] = new Polynomial(f0PolynomialCoeffs[i]);
			mapPolynomialBaseNames.put(f0Polynomials[i], baseNames.get(i));
			System.out.print(baseNames.get(i) + " ");
			for (int j = 0; j < polynomialOrder; j++) {
				System.out.print(f0PolynomialCoeffs[i][j] + " ");
			}
			System.out.println();

			double coeff[] = f0Polynomials[i].coeffs;
			System.out.print(baseNames.get(i) + " ");
			for (int j = 0; j < coeff.length; j++) {
				System.out.print(coeff[j] + " ");
			}
			System.out.println();
		}

	}

	public void trainer(int numClusters) throws UnsupportedAudioFileException, IOException {
		KMeansClusteringTrainerParams params = new KMeansClusteringTrainerParams();
		params.numClusters = numClusters;
		params.maxIterations = 10000;
		// Train:
		PolynomialCluster[] clusters = PolynomialKMeansClusteringTrainer.train(f0Polynomials, params);
        }
}
