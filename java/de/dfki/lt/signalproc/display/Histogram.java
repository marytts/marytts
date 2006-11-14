/*
 * Copyright (C) 2005 DFKI GmbH. All rights reserved.
 */
package de.dfki.lt.signalproc.display;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class Histogram extends FunctionGraph
{

    /**
     * Display a 2d graph showing y(x), with labelled scales.
     * This constructor is for subclasses only, which may need
     * to perform some operations before calling initialise().
     */
    protected Histogram()
    {
        super();
    }

    /**
     * Display a histogram showing y(x), with labelled scales.
     */
    public Histogram(double x0, double xStep, double[] y)
    {
        super(x0, xStep, y);
    }

    /**
     * Display a histogram showing y(x), with labelled scales.
     */
    public Histogram(int width, int height, double x0, double xStep, double[] y)
    {
        super(width, height, x0, xStep, y);
    }

    public void initialise(int width, int height, 
            double x0, double xStep,  double[] y)
    {
        graphStyle = DRAW_HISTOGRAM;
        super.initialise(width, height, x0, xStep, y);
    }

}
