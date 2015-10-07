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
package marytts.signalproc.analysis;

import java.util.Arrays;
import java.util.Locale;

/**
 * A class to keep all information on a single EST format label
 * 
 * @author Oytun T&uuml;rk
 */
public class Label {
	public double time; // Ending time of phonetic label
	public int status; // Status
	public String phn; // Phone
	public double ll; // log likelihood
	public String[] rest; // If the label contains more fields, get them as text
	public double[] valuesRest; // If some of the <rest> are numbers, convert them to doubles and keep

	/**
	 * Simple constructor for simple cases: create a label from the given end time and phone symbol.
	 * 
	 * @param endTime
	 *            time where the phonetic label ends, in seconds.
	 * @param phoneSymbol
	 *            phonetic label, such as a phone symbol
	 */
	public Label(double endTime, String phoneSymbol) {
		this(endTime, 125 /* dummy value */, phoneSymbol, Double.NEGATIVE_INFINITY, null, null);
	}

	/**
	 * Create a new Label.
	 * 
	 * @param newTime
	 *            Ending time of phonetic label
	 * @param newStatus
	 *            status
	 * @param newPhn
	 *            phone symbol
	 * @param newll
	 *            log likelihood
	 */
	public Label(double newTime, int newStatus, String newPhn, double newll) {
		this(newTime, newStatus, newPhn, newll, null, null);
	}

	public Label() {
		this(-1.0, 0, "", Double.NEGATIVE_INFINITY, null, null);
	}

	public Label(double newTime, int newStatus, String newPhn, double newll, String[] restIn) {
		this(newTime, newStatus, newPhn, newll, restIn, null);
	}

	public Label(double newTime, int newStatus, String newPhn, double newll, String[] restIn, double[] valuesRestIn) {
		time = newTime;
		status = newStatus;
		phn = newPhn;
		ll = newll;

		if (restIn != null && restIn.length > 0) {
			rest = new String[restIn.length];
			for (int i = 0; i < restIn.length; i++)
				rest[i] = restIn[i];
		} else
			rest = null;

		if (valuesRestIn != null && valuesRestIn.length > 0) {
			valuesRest = new double[valuesRestIn.length];
			for (int i = 0; i < valuesRestIn.length; i++)
				valuesRest[i] = valuesRestIn[i];
		} else
			valuesRest = null;
	}

	public Label(Label lab) {
		if (lab != null)
			copyFrom(lab);
	}

	public void copyFrom(Label lab) {
		time = lab.time;
		status = lab.status;
		phn = lab.phn;
		ll = lab.ll;
		if (lab.rest != null && lab.rest.length > 0) {
			rest = new String[lab.rest.length];
			for (int i = 0; i < lab.rest.length; i++)
				rest[i] = lab.rest[i];
		} else
			rest = null;

		if (lab.valuesRest != null && lab.valuesRest.length > 0) {
			valuesRest = new double[lab.valuesRest.length];
			for (int i = 0; i < lab.valuesRest.length; i++)
				valuesRest[i] = lab.valuesRest[i];
		} else
			valuesRest = null;
	}

	// Display label entries
	public void print() {
		System.out.println("Time=" + String.valueOf(time) + " s. " + "Stat=" + String.valueOf(status) + " " + "Phone=" + phn
				+ " " + "Log-likelihood=" + String.valueOf(ll));
	}

	public String toString() {
		return String.format(Locale.US, "%f %d %s %f", time, status, phn, ll);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Label))
			return false;
		Label other = (Label) o;
		return Math.abs(time - other.time) < 1.e-7
				&& status == other.status
				&& (phn == null && other.phn == null || phn != null && phn.equals(other.phn))
				&& (ll == other.ll || Double.isInfinite(ll) && Double.isInfinite(other.ll) || Double.isNaN(ll)
						&& Double.isNaN(other.ll)) && Arrays.deepEquals(rest, other.rest);
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
