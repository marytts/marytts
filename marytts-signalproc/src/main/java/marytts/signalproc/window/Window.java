/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.window;

import java.util.Arrays;

import marytts.signalproc.process.CopyingDataProcessor;
import marytts.signalproc.process.InlineDataProcessor;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 *         Interface for windowing functions.
 */
public abstract class Window implements CopyingDataProcessor, InlineDataProcessor {
	public static final int RECT = 0;
	public static final int HAMMING = 1;
	public static final int BLACKMAN = 2;
	public static final int HANNING = 3;
	public static final int GAUSS = 4;
	public static final int BARTLETT = 5;
	public static final int FLATTOP = 6;

	protected double prescalingFactor;
	protected boolean evenLength;

	/**
	 * This array, whose length is the window length, holds the multiplication factors for each sample across the window. It must
	 * be initialised in the constructor, using the method #initialise().
	 */
	protected double[] window;

	/**
	 * Default constructor for subclasses that need to do something themselves before calling initialise().
	 * 
	 */
	protected Window() {
		prescalingFactor = 1.;
	}

	/**
	 * Create a new window of length length. This will call #initialise() in the respective subclass in order to fill the window
	 * array with meaningful multiplication values.
	 * 
	 * @param length
	 *            the window length, in samples; this should be an odd number, so that the window can be symmetric around the
	 *            center point.
	 * @throws IllegalArgumentException
	 *             if length is an even number.
	 */
	public Window(int length) {
		this(length, 1);
	}

	/**
	 * Create a new window of length length, and apply the given prescaling factor to each point. This will call #initialise() in
	 * the respective subclass in order to fill the window array with meaningful multiplication values.
	 * 
	 * @param length
	 *            the window length, in samples; this should be an odd number, so that the window can be symmetric around the
	 *            center point.
	 * @param prescalingFactor
	 *            a factor to apply to each window point.
	 * @throws IllegalArgumentException
	 *             if length is an even number.
	 */
	public Window(int length, double prescalingFactor) {
		window = new double[length];
		this.evenLength = (length % 2 == 0);
		this.prescalingFactor = prescalingFactor;
		initialise();
	}

	/**
	 * Apply this window on the given source data array, at the given position. This method returns the resulting data in a new
	 * array of the same length as this window (@see getLength()). If src has less than getLength() data points left at pos, zeros
	 * are added at the end.
	 * 
	 * @param src
	 *            the source data array to apply the windowing function to.
	 * @param pos
	 *            the position from which to apply the windowing function
	 * @return an array of the same length as this window, computed by applying this window to the source data.
	 * @throws IllegalArgumentException
	 *             if targetLength is smaller than this window's length as returned by #getLength().
	 */
	public double[] apply(final double[] src, int pos) {
		double target[] = new double[window.length];
		apply(src, pos, target, 0);
		return target;
	}

	public double[] apply(final double[] src) {
		return apply(src, 0);
	}

	/**
	 * Apply the window function in-line, i.e. by modifying the original data.
	 * 
	 * @param data
	 *            data
	 * @param pos
	 *            the position in the data array where to start applying the window function.
	 * @param len
	 *            the amount of data after position pos to process. len must be less than or equal to getLength(). If it is less
	 *            than getLength(), a truncated window will be applied.
	 * @throws IllegalArgumentException
	 *             if len != getLength().
	 */
	public void applyInline(double[] data, int pos, int len) {
		apply(data, pos, data, pos, len);
	}

	public void applyInline(double[] data, int pos) {
		applyInline(data, pos, window.length);
	}

	public void applyInline(double[] data) {
		applyInline(data, 0, window.length);
	}

	/**
	 * Apply this window on the given source data array, at the given position. This method returns the resulting data in the
	 * given target array, at the target position given by targetPos. If src has less than getLength() data points left at pos,
	 * zeros are added at the end.
	 * 
	 * @param src
	 *            the source data array to apply the windowing function to.
	 * @param srcPos
	 *            the position in the source array from which to apply the windowing function
	 * @param target
	 *            an array to receive the target data, computed by applying this window to the source data. The target array must
	 *            be long enough to receive getLength() bytes after targetPos. if target == source and targetPos == srcPos, then
	 *            the window function is applied in-place.
	 * @param targetPos
	 *            targetPos
	 * @throws IllegalArgumentException
	 *             if target.length-targetPos is smaller than this window's length as returned by #getLength().
	 */
	public void apply(final double[] src, int srcPos, double[] target, int targetPos) {
		apply(src, srcPos, target, targetPos, 0, window.length);
	}

	/**
	 * Apply this window on the given source data array, at the given position. This method returns the resulting data in the
	 * given target array, at the target position given by targetPos. If src has less than getLength() data points left at pos,
	 * zeros are added at the end.
	 * 
	 * @param src
	 *            the source data array to apply the windowing function to.
	 * @param srcPos
	 *            the position in the source array from which to apply the windowing function. If srcPos is negative, abs(srcPos)
	 *            zeroes will be pre-pended before the first data from src is taken into account; if it is greater than
	 *            src.length-getLength(), the result will be filled up with trailing zeroes behind the last data.
	 * @param target
	 *            an array to receive the target data, computed by applying this window to the source data. The target array must
	 *            be long enough to receive getLength() bytes after targetPos. if target == source and targetPos == srcPos, then
	 *            the window function is applied in-place.
	 * @param targetPos
	 *            target Pos
	 * @param len
	 *            the number of samples of the window to apply; this must be less than or equal getLength(). If it is less than
	 *            getLength(), a truncated window will be applied.
	 * @throws IllegalArgumentException
	 *             if target.length-targetPos is smaller than this window's length as returned by #getLength(), or if len is
	 *             greater or equals to getLength.
	 */
	public void apply(final double[] src, int srcPos, double[] target, int targetPos, int len) {
		apply(src, srcPos, target, targetPos, 0, len);
	}

	/**
	 * Apply a part of this window on the given source data array, at the given position. For example, by setting off to
	 * getLength()/2 and len to getLength()/2, only the right half of the window will be applied. This method returns the
	 * resulting data in the given target array, at the target position given by targetPos. If src has less than len data points
	 * left at srcPos, zeros are added at the end.
	 * 
	 * @param src
	 *            the source data array to apply the windowing function to.
	 * @param srcPos
	 *            the position in the source array from which to apply the windowing function. If srcPos is negative, abs(srcPos)
	 *            zeroes will be pre-pended before the first data from src is taken into account; if it is greater than
	 *            src.length-getLength(), the result will be filled up with trailing zeroes behind the last data.
	 * @param target
	 *            an array to receive the target data, computed by applying this window to the source data. The target array must
	 *            be long enough to receive getLength() bytes after targetPos. if target == source and targetPos == srcPos, then
	 *            the window function is applied in-place.
	 * @param targetPos
	 *            targetPos
	 * @param off
	 *            the offset from the start of the window from where on the window is to be applied.
	 * @param len
	 *            the number of samples of the window to apply; off+len must be less than or equal getLength().
	 * @throws IllegalArgumentException
	 *             if target.length-targetPos is smaller than this window's length as returned by #getLength(), or if len is
	 *             greater or equals to getLength.
	 */
	public void apply(final double[] src, int srcPos, double[] target, int targetPos, int off, int len) {
		if (len < 0 || off < 0 || off + len > window.length)
			throw new IllegalArgumentException("Requested offset " + off + " or length " + len
					+ " does not fit into window length " + window.length);
		if (target.length < targetPos + len)
			throw new IllegalArgumentException("Target array cannot hold enough data");
		int start, end; // actual positions in src to apply the window to.
		// If these deviate from srcPos and srcPos+len, resp., zeroes need to be pre-/appended.
		start = srcPos;
		if (start < 0) {
			start = 0;
			Arrays.fill(target, targetPos, targetPos + (start - srcPos), 0);
		}
		end = srcPos + len;
		if (end > src.length) {
			end = src.length;
			Arrays.fill(target, targetPos + end - srcPos, targetPos + len, 0);
		}
		for (int i = start; i < end; i++) {
			target[targetPos + i - srcPos] = src[i] * window[off + i - srcPos];
		}
	}

	/**
	 * The initialisation of the window array with multiplication factors corresponding to the specific windowing function. This
	 * needs to be implemented by each subclass.
	 */
	protected abstract void initialise();

	/**
	 * Return the length of this window, in samples.
	 * 
	 * @throws NullPointerException
	 *             if the window has not yet been initialised.
	 * @return window length
	 */
	public int getLength() {
		if (window == null) {
			throw new NullPointerException("The window has not yet been initialised");
		}
		return window.length;
	}

	/**
	 * Get the value of the window function at index position i
	 * 
	 * @param i
	 *            the index position in the window for which to return the value
	 * @return the value of the window function, between 0 and 1.
	 * @throws IllegalArgumentException
	 *             if i&lt;0 or i&gt;getLength().
	 * @throws NullPointerException
	 *             if the window has not yet been initialised.
	 */
	public double value(int i) {
		if (window == null) {
			throw new NullPointerException("The window has not yet been initialised");
		}
		if (i < 0 || i > window.length) {
			throw new IllegalArgumentException("Can only return values for index positions 0 to " + window.length);
		}
		return window[i];
	}

	/**
	 * Return this window's type, as defined by the constants in Window, or -1 if the window is not of a known type.
	 * 
	 * @return -1
	 */
	public int type() {
		if (this instanceof RectWindow)
			return RECT;
		else if (this instanceof HammingWindow)
			return HAMMING;
		else if (this instanceof BlackmanWindow)
			return BLACKMAN;
		else if (this instanceof HanningWindow)
			return HANNING;
		else if (this instanceof GaussWindow)
			return GAUSS;
		else if (this instanceof BartlettWindow)
			return BARTLETT;
		else if (this instanceof FlattopWindow)
			return FLATTOP;
		else
			return -1;
	}

	/**
	 * Convenience method for requesting a window of the requested type.
	 * 
	 * @param windowType
	 *            one of the constants defined in Window.
	 * @param length
	 *            window length (should be an odd number)
	 * @return a window of the requested type and length
	 * @throws IllegalArgumentException
	 *             if windowType is not a valid window type, or if length is an even number
	 */
	public static Window get(int windowType, int length) {
		return get(windowType, length, 1.);
	}

	/**
	 * Convenience method for requesting a window of the requested type.
	 * 
	 * @param windowType
	 *            one of the constants defined in Window.
	 * @param length
	 *            window length (should be an odd number)
	 * @param prescale
	 *            a prescaling factor applied to all points in the window
	 * @return a window of the requested type and length
	 * @throws IllegalArgumentException
	 *             if windowType is not a valid window type, or if length is an even number
	 */
	public static Window get(int windowType, int length, double prescale) {
		switch (windowType) {
		case RECT:
			return new RectWindow(length, prescale);
		case HAMMING:
			return new HammingWindow(length, prescale);
		case BLACKMAN:
			return new BlackmanWindow(length, prescale);
		case HANNING:
			return new HanningWindow(length, prescale);
		case GAUSS:
			return new GaussWindow(length, prescale);
		case BARTLETT:
			return new BartlettWindow(length, prescale);
		case FLATTOP:
			return new FlattopWindow(length, prescale);
		default:
			throw new IllegalArgumentException("Unknown window type requested.");
		}
	}

	/**
	 * List all available window types
	 * 
	 * @return an integer corresponding to the constants defined in Window.
	 */
	public static int[] getAvailableTypes() {
		return new int[] { RECT, HAMMING, BLACKMAN, HANNING, GAUSS, BARTLETT, FLATTOP };
	}

	/**
	 * For a given type name (e.g., "Hanning window", or "BARTLETT"), return the type code. Matching is done as case-insensitive
	 * prefix matching.
	 * 
	 * @param typeName
	 *            the type name.
	 * @return the type code corresponding to typeName, or -1 if none could be determined.
	 */
	public static int getTypeByName(String typeName) {
		String tomatch = typeName.toUpperCase();
		if (tomatch.startsWith("HAMMING"))
			return HAMMING;
		else if (tomatch.startsWith("RECT"))
			return RECT;
		else if (tomatch.startsWith("BLACKMAN"))
			return BLACKMAN;
		else if (tomatch.startsWith("HANNING"))
			return HANNING;
		else if (tomatch.startsWith("GAUSS"))
			return GAUSS;
		else if (tomatch.startsWith("BARTLETT"))
			return BARTLETT;
		else if (tomatch.startsWith("FLATTOP"))
			return FLATTOP;
		else
			return -1;
	}

	/**
	 * Get the type name of a window type.
	 * 
	 * @param windowType
	 *            a valid window type
	 * @return a string representing the type name
	 * @throws IllegalArgumentException
	 *             if windowType is not a valid window type
	 */
	public static String getTypeName(int windowType) {
		Window w = get(windowType, 1);
		return w.toString();
	}

	// Normalize window coefficients to sum up to unity
	public void normalize() {
		normalize(1.0f);
	}

	// Normalize window coefficients to sum up to val
	public void normalizePeakValue(float desiredPeakValue) {
		double maxVal = MathUtils.getMax(window);

		double scale = desiredPeakValue / maxVal;

		for (int i = 0; i < window.length; i++)
			window[i] *= scale;
	}

	// Normalize window coefficients to sum up to val
	public void normalize(float val) {
		float total = 0.0f;
		int i;
		for (i = 0; i < window.length; i++)
			total += window[i];

		float scale = val / total;

		for (i = 0; i < window.length; i++)
			window[i] *= scale;
	}

	// Normalize window coefficients such that squared sum of coefficients is equal to val
	public void normalizeSquaredSum(float val) {
		float total = 0.0f;
		int i;
		for (i = 0; i < window.length; i++)
			total += window[i] * window[i];

		float scale = (float) (Math.sqrt(val) / Math.sqrt(total));

		for (i = 0; i < window.length; i++)
			window[i] *= scale;
	}

	public void normalizeRange(float minVal, float maxVal) {
		MathUtils.adjustRange(window, minVal, maxVal);
	}

	public double[] getCoeffs() {
		return window;
	}

	public double[] getCoeffsLeftHalf() {
		return ArrayUtils.subarray(window, 0, (int) Math.floor(0.5 * window.length + 0.5));
	}

	public double[] getCoeffsRightHalf() {
		int off = (int) Math.floor(0.5 * window.length + 0.5);
		int len = window.length - off;
		return ArrayUtils.subarray(window, off, len);
	}
}
