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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import marytts.cart.CART;
import marytts.cart.LeafNode;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.WagonCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * A class providing the functionality to interface with an external wagon process.
 * 
 * @author marc
 * 
 */
public class Wagon implements Runnable {
	private static File wagonExecutable;

	public static void setWagonExecutable(File wagonExe) {
		wagonExecutable = wagonExe;
	}

	private Logger logger;
	private String id;
	private FeatureDefinition featureDefinition;
	private FeatureVector[] fv;
	private DistanceMeasure distMeasure;
	private File distFile;
	private File descFile;
	private File featFile;
	private File cartFile;
	private String systemCall;
	private boolean finished = false;
	private boolean success = false;
	private CART cart = null;

	/**
	 * Set up a new wagon process. Wagon.setWagonExecutable() must be called beforehand.
	 * 
	 * @param id
	 *            id
	 * @param featureDefinition
	 *            featureDefinition
	 * @param featureVectors
	 *            featureVectors
	 * @param aDistanceMeasure
	 *            aDistanceMeasure
	 * @param dir
	 *            dir
	 * @param balance
	 *            balance
	 * @param stop
	 *            stop
	 * @throws IOException
	 *             if there was no call to Wagon.setWagonExecutable() with an executable file before calling this constructor.
	 */
	public Wagon(String id, FeatureDefinition featureDefinition, FeatureVector[] featureVectors,
			DistanceMeasure aDistanceMeasure, File dir, int balance, int stop) throws IOException {
		if (wagonExecutable == null || !wagonExecutable.isFile()) {
			throw new IOException("No wagon executable set using Wagon.setExecutable()!");
		}
		this.logger = MaryUtils.getLogger("Wagon");
		this.id = id;
		this.featureDefinition = featureDefinition;
		this.fv = featureVectors;
		this.distMeasure = aDistanceMeasure;
		this.distFile = new File(dir, id + ".dist");
		this.descFile = new File(dir, id + ".desc");
		this.featFile = new File(dir, id + ".feat");
		this.cartFile = new File(dir, id + ".cart");
		this.systemCall = wagonExecutable.getAbsolutePath() + " -desc " + descFile.getAbsolutePath() + " -data "
				+ featFile.getAbsolutePath() + " -balance " + balance + " -distmatrix " + distFile.getAbsolutePath() + " -stop "
				+ stop + " -output " + cartFile.getAbsolutePath();
	}

	/**
	 * Export this feature definition in the "all.desc" format which can be read by wagon.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	private void createDescFile() throws IOException {
		PrintWriter out = new PrintWriter(new FileOutputStream(descFile));
		Set<String> featuresToIgnore = new HashSet<String>();
		featuresToIgnore.add("unit_logf0");
		featuresToIgnore.add("unit_duration");

		int numDiscreteFeatures = featureDefinition.getNumberOfByteFeatures() + featureDefinition.getNumberOfShortFeatures();
		out.println("(");
		out.println("(occurid cluster)");
		for (int i = 0, n = featureDefinition.getNumberOfFeatures(); i < n; i++) {
			out.print("( ");
			String featureName = featureDefinition.getFeatureName(i);
			out.print(featureName);
			if (featuresToIgnore != null && featuresToIgnore.contains(featureName)) {
				out.print(" ignore");
			}
			if (i < numDiscreteFeatures) { // list values
				for (int v = 0, vmax = featureDefinition.getNumberOfValues(i); v < vmax; v++) {
					out.print("  ");
					// Print values surrounded by double quotes, and make sure any
					// double quotes in the value are preceded by a backslash --
					// otherwise, we get problems e.g. for sentence_punc
					String val = featureDefinition.getFeatureValueAsString(i, v);
					if (val.indexOf('"') != -1) {
						StringBuilder buf = new StringBuilder();
						for (int c = 0; c < val.length(); c++) {
							char ch = val.charAt(c);
							if (ch == '"')
								buf.append("\\\"");
							else
								buf.append(ch);
						}
						val = buf.toString();
					}
					out.print("\"" + val + "\"");
				}
				out.println(" )");
			} else { // float feature
				out.println(" float )");
			}
		}
		out.println(")");
		out.close();
	}

	private void dumpFeatureVectors() throws IOException {
		// open file
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(featFile)));
		for (int i = 0; i < fv.length; i++) {
			// Print the feature string
			out.print(i + " " + featureDefinition.toFeatureString(fv[i]));
			// print a newline if this is not the last vector
			if (i + 1 != fv.length) {
				out.print("\n");
			}
		}
		// dump and close
		out.flush();
		out.close();
	}

	/*
	 * Save in "ancient" text format. Extremely inefficient for large files. Keeping this for documentation purposes only.
	 */
	private void saveDistanceMatrix() throws IOException {
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(distFile)));
		for (int i = 0; i < fv.length; i++) {
			for (int j = 0; j < fv.length; j++) {
				float distance = (i == j ? 0f : distMeasure.squaredDistance(fv[i], fv[j]));
				out.printf(Locale.US, "%.1f ", distance);
			}
			out.print("\n");
		}
		out.flush();
		out.close();
	}

	/* Save in efficient binary format. */
	private void binarySaveDistanceMatrix() throws IOException {
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(distFile)));
		out.writeBytes("EST_File fmatrix\n");
		out.writeBytes("version 1\n");
		out.writeBytes("DataType binary\n");
		out.writeBytes("ByteOrder BigEndian\n");
		out.writeBytes("rows " + fv.length + "\n");
		out.writeBytes("columns " + fv.length + "\n");
		out.writeBytes("EST_Header_End\n");
		for (int i = 0; i < fv.length; i++) {
			for (int j = 0; j < fv.length; j++) {
				float distance = (i == j ? 0f : distMeasure.squaredDistance(fv[i], fv[j]));
				out.writeFloat(distance);
			}
		}
		out.flush();
		out.close();
	}

	public void run() {
		try {
			long startTime = System.currentTimeMillis();

			logger.debug(id + "> Creating " + descFile.getName());
			createDescFile();

			logger.debug(id + "> Dumping features to " + featFile.getName());
			dumpFeatureVectors();

			logger.debug(id + "> Dumping distance matrix to " + distFile.getName());
			binarySaveDistanceMatrix();

			logger.debug(id + "> Calling wagon as follows:");
			logger.debug(systemCall);
			Process p = Runtime.getRuntime().exec(systemCall);
			// collect the output
			// read from error stream
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), id + " err");

			// read from output stream
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), id + " out");
			// start reading from the streams
			errorGobbler.start();
			outputGobbler.start();
			p.waitFor();
			if (p.exitValue() != 0) {
				finished = true;
				success = false;
			} else {
				success = true;
				logger.debug(id + "> Wagon call took " + (System.currentTimeMillis() - startTime) + " ms");

				// read in the resulting CART
				logger.debug(id + "> Reading CART");
				BufferedReader buf = new BufferedReader(new FileReader(cartFile));
				WagonCARTReader wagonReader = new WagonCARTReader(LeafType.IntAndFloatArrayLeafNode);
				cart = new CART(wagonReader.load(buf, featureDefinition), featureDefinition);
				buf.close();

				// Fix the new cart's leaves:
				// They are currently the index numbers in featureVectors;
				// but what we need is the unit index numbers!
				for (LeafNode leaf : cart.getLeafNodes()) {
					int[] data = (int[]) leaf.getAllData();
					for (int i = 0; i < data.length; i++) {
						data[i] = fv[data[i]].getUnitIndex();
					}
				}

				logger.debug(id + "> completed in " + (System.currentTimeMillis() - startTime) + " ms");
				finished = true;
			}
			if (!Boolean.getBoolean("wagon.keepfiles")) {
				featFile.delete();
				distFile.delete();
			}

		} catch (Exception e) {
			e.printStackTrace();
			finished = true;
			success = false;
			throw new RuntimeException("Exception running wagon");
		}

	}

	public boolean finished() {
		return finished;
	}

	public boolean success() {
		return success;
	}

	public String id() {
		return id;
	}

	public CART getCART() {
		return cart;
	}

	static class StreamGobbler extends Thread {
		InputStream is;
		String type;

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					System.out.println(type + ">" + line);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}
