/*
 * Copyright (C) 2005 DFKI GmbH. All rights reserved.
 */
package marytts.signalproc.display;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         Display a 2d graph showing y(x), with labelled scales. This constructor is for subclasses only, which may need to
 *         perform some operations before calling initialise().
 * 
 */
public class Histogram extends FunctionGraph {

	protected Histogram() {
		super();
	}

	/**
	 * Display a histogram showing y(x), with labelled scales.
	 * 
	 * @param x0
	 *            x0
	 * @param xStep
	 *            xStep
	 * @param y
	 *            y
	 */
	public Histogram(double x0, double xStep, double[] y) {
		super(x0, xStep, y);
	}

	/**
	 * Display a histogram showing y(x), with labelled scales.
	 * 
	 * @param width
	 *            width
	 * @param height
	 *            height
	 * @param x0
	 *            x0
	 * @param xStep
	 *            xStep
	 * @param y
	 *            y
	 */
	public Histogram(int width, int height, double x0, double xStep, double[] y) {
		super(width, height, x0, xStep, y);
	}

	public void initialise(int width, int height, double newX0, double newXStep, double[] y) {
		super.initialise(width, height, newX0, newXStep, y);
		setPrimaryDataSeriesStyle(graphColor.get(0), DRAW_HISTOGRAM, -1);
	}

}
