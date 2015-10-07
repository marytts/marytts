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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import marytts.machinelearning.KMeansClusteringTrainerParams;
import marytts.machinelearning.PolynomialCluster;
import marytts.machinelearning.PolynomialKMeansClusteringTrainer;
import marytts.signalproc.display.FunctionGraph;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.Polynomial;

public class KMeansClusterer {

	private ArrayList<String> baseNames;
	Polynomial[] f0Polynomials;
	private int polynomialOrder = 3;
	private int numberOfSamples = 0;
	private int numberOfClusters;
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
				f0PolynomialCoeffs[i][j] = (new Double(words[j + 1].trim())).doubleValue();
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
		this.numberOfClusters = numClusters;
		KMeansClusteringTrainerParams params = new KMeansClusteringTrainerParams();
		params.numClusters = numClusters;
		params.maxIterations = 10000;
		// Train:
		PolynomialCluster[] clusters = PolynomialKMeansClusteringTrainer.train(f0Polynomials, params);

		// Visualise:
		FunctionGraph clusterGraph = new FunctionGraph(0, 1, new double[1]);
		clusterGraph.setYMinMax(-550, 500);
		clusterGraph.setPrimaryDataSeriesStyle(Color.BLUE, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_FULLCIRCLE);
		JFrame jf = clusterGraph.showInJFrame("", false, true);
		for (int i = 0; i < clusters.length; i++) {
			double[] meanValues = clusters[i].getMeanPolynomial().generatePolynomialValues(100, 0, 1);
			clusterGraph.updateData(0, 1. / meanValues.length, meanValues);

			Polynomial[] members = clusters[i].getClusterMembers();
			System.out.print("Cluster " + i + " : ");
			for (int m = 0; m < members.length; m++) {
				double[] pred = members[m].generatePolynomialValues(meanValues.length, 0, 1);
				clusterGraph.addDataSeries(pred, Color.GRAY, FunctionGraph.DRAW_LINE, -1);
				String baseName = mapPolynomialBaseNames.get(members[m]);
				System.out.print(baseName + " ");
				jf.repaint();

				String waveFile = "/home/sathish/Work/phd/voices/f0desc-listener/vocalizations/wav/" + File.separator + baseName
						+ ".wav";
				AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(waveFile));

				// Enforce PCM_SIGNED encoding
				if (!inputAudio.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
					inputAudio = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, inputAudio);
				}

				int audioSampleRate = (int) inputAudio.getFormat().getSampleRate();
				AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
				double[] sentenceAudio = signal.getAllData();
				AudioPlayer ap = new AudioPlayer(new DDSAudioInputStream(new BufferedDoubleDataSource(sentenceAudio),
						new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, // samples per second
								16, // bits per sample
								1, // mono
								2, // nr. of bytes per frame
								audioSampleRate, // nr. of frames per second
								true))); // big-endian;))
				ap.start();

				try {
					ap.join();
					Thread.sleep(10);
				} catch (InterruptedException ie) {
				}

			}
			System.out.println();
			jf.setTitle("Cluster " + (i + 1) + " of " + clusters.length + ": " + members.length + " members");
			jf.repaint();

			try {
				Thread.sleep(5000);
			} catch (InterruptedException ie) {
			}
		}
	}

	/**
	 * @param args
	 *            args
	 * @throws IOException
	 *             IOException
	 * @throws UnsupportedAudioFileException
	 *             UnsupportedAudioFileException
	 */
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
		KMeansClusterer kmc = new KMeansClusterer();
		// String fileName = "/home/sathish/phd/voices/en-GB-listener/vocal-polynomials/all.listener.polynomials.txt";
		// String fileName = "/home/sathish/phd/voices/en-GB-listener/vocal-polynomials/SpiVocalizationF0PolyFeatureFile.txt";
		String fileName = "/home/sathish/phd/voices/en-GB-listener/yeahPrudenceVocalizationF0PolyFeatureFile.txt";
		kmc.loadF0Polynomials(fileName);
		kmc.trainer(5);
		System.exit(0);
	}

}
