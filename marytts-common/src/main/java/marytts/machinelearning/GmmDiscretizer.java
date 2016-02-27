/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.machinelearning;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * This discretizes values according to a gaussian mixture model (gmm). The result of discretization is the mean of the class that
 * contributed most probability to a point.
 * 
 * @author benjaminroth
 *
 */
public class GmmDiscretizer implements Discretizer {

	private GMM mixtureModel;
	private boolean extraZero;

	/**
	 * This trains a gaussian mixture model having the specified number of components.
	 * 
	 * @param values
	 *            the data the model is trained with
	 * @param nrClasses
	 *            number of components the mixture will have
	 * @param extraZero
	 *            specifies if zeroes are to be treated seperately from mixture model training and application.
	 * @return a discretizer that discretizes according to the trained model
	 */
	public static GmmDiscretizer trainDiscretizer(List<Integer> values, int nrClasses, boolean extraZero) {

		List<Integer> retained = new ArrayList<Integer>(values);

		Integer zero = new Integer(0);
		if (extraZero && retained.contains(zero)) {
			// remove all zeroes
			int i = 0;
			while (i < retained.size()) {
				if (retained.get(i).equals(zero))
					retained.remove(i);
				else
					i++;
			}
		}

		double[][] trainingData = new double[retained.size()][1];

		for (int i = 0; i < retained.size(); i++) {
			trainingData[i][0] = (double) retained.get(i);
		}

		int trainClasses;
		if (extraZero) {
			// one class is not trained but assigned
			trainClasses = nrClasses - 1;
		} else {
			trainClasses = nrClasses;
		}

		GMMTrainerParams gmmParams = new GMMTrainerParams();
		gmmParams.totalComponents = trainClasses;
		gmmParams.emMinIterations = 1000;
		gmmParams.emMaxIterations = 2000;
		GMM model = (new GMMTrainer().train(trainingData, gmmParams));

		return new GmmDiscretizer(model, extraZero);

	}

	/**
	 * This constructs a {@link Discretizer} using the specified mixture model.
	 * 
	 * @param model
	 *            GMM to be used
	 * @param extraZeroClass
	 *            specifies if zeros should be treated independently
	 */
	public GmmDiscretizer(GMM model, boolean extraZeroClass) {

		// TODO: debugging
		System.out.println("set model with the following components:");
		for (int i = 0; i < model.totalComponents; i++) {
			System.out.println("component " + i);
			System.out.println(" mean: " + model.components[i].meanVector[0]);
			System.out.println(" weight: " + model.weights[i]);
			System.out.println(" variance: " + model.components[i].covMatrix[0][0]);
		}

		this.mixtureModel = model;
		this.extraZero = extraZeroClass;
	}

	/**
	 * This discretizes a value by returning the mean of that gaussian component that has maximum probability for it.
	 * 
	 * @param value
	 *            the value to be discretized
	 * @return the discretization the value is mapped to
	 * 
	 */
	public int discretize(int value) {

		double[] x = new double[] { (double) value };
		double[] probs = this.mixtureModel.componentProbabilities(x);

		if (this.extraZero && value == 0)
			return 0;

		int maxClass = 0;
		double maxP = 0f;

		for (int i = 0; i < mixtureModel.totalComponents; i++) {
			if (probs[i] > maxP) {
				maxClass = i;
				maxP = probs[i];
			}
		}

		int maxClassMean = (int) mixtureModel.components[maxClass].meanVector[0];

		return maxClassMean;
	}

	/**
	 * Returns all poosible discretizations values can be mapped to.
	 * 
	 * @return all poosible discretizations values can be mapped to.
	 */
	public int[] getPossibleValues() {

		if (this.extraZero) {
			// TODO: space for optimization
			int[] retArr = new int[mixtureModel.components.length + 1];
			retArr[0] = 0;

			for (int i = 0; i < mixtureModel.components.length; i++) {
				retArr[i + 1] = (int) mixtureModel.components[i].meanVector[0];
			}

			return retArr;
		} else {
			int[] retArr = new int[mixtureModel.components.length];

			for (int i = 0; i < mixtureModel.components.length; i++) {
				retArr[i] = (int) mixtureModel.components[i].meanVector[0];
			}

			return retArr;
		}

	}

}
