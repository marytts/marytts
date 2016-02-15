/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.unitselection.weightingfunctions;

/**
 * Defines the applicable weighting functions.
 * 
 * @author sacha
 * 
 */
public class WeightingFunction {

	/**
	 * Linear weighting function: just computes the difference.
	 * 
	 * @author sacha
	 * 
	 */
	public static class linear implements WeightFunc {
		/**
		 * Cost computation: a simple absolute value of a subtraction.
		 * 
		 * @param a
		 *            a
		 * @param b
		 *            b
		 */
		public double cost(double a, double b) {
			return (a > b ? (a - b) : (b - a));
			/*
			 * (Measuring which one is bigger replaces a costly Math.abs() operation.)
			 */
		}

		/**
		 * Optional argument setting.
		 * 
		 * @param val
		 *            val
		 */
		public void setParam(String val) {
			/*
			 * Does nothing and is never called in the linear case.
			 */
		}

		/**
		 * Returns the string definition for this weight function.
		 */
		public String whoAmI() {
			return ("linear");
		}
	}

	/**
	 * Step weighting function: saturates above a given percentage of the input values.
	 * 
	 * @author sacha
	 * 
	 */
	public static class step implements WeightFunc {
		/** A percentage above which the function saturates to 0. */
		private double stepVal;

		/**
		 * Cost computation: the absolute value of a subtraction, with application of a saturation if the difference value reaches
		 * a certain percentage of the mean value of the arguments.
		 * 
		 * @param a
		 *            a
		 * @param b
		 *            b
		 * @return res if dev &lt; stepVal, 0.0 otherwise
		 */
		public double cost(double a, double b) {

			double res = (a > b ? (a - b) : (b - a));
			/*
			 * (Measuring which one is bigger replaces a costly Math.abs() operation.)
			 */

			double dev = res / ((a + b) / 2.0);
			if (dev < stepVal)
				return (res);
			else
				return (0.0);
		}

		/**
		 * Optional argument setting.
		 * 
		 * Syntax for the step function's parameter: the first number before the % sign is interpreted as a percentage for the
		 * step value.
		 * 
		 * @param val
		 *            val
		 * */
		public void setParam(String val) {
			stepVal = Double.parseDouble(val.substring(0, val.indexOf("%"))) / 100.0;
		}

		/**
		 * String definition of the function.
		 * 
		 * @return ("step " + Double.toString(100.0 * stepVal) + "%")
		 */
		public String whoAmI() {
			return ("step " + Double.toString(100.0 * stepVal) + "%");
		}
	}

}
