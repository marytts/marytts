package marytts.util.display;

import javax.swing.JFrame;

import marytts.signalproc.display.FunctionGraph;
import marytts.util.math.MathUtils;

public class DisplayUtils {

	public static void plot(float[] x) {
		if (x != null)
			plot(x, 0, x.length - 1);
	}

	public static void plot(double[] x) {
		if (x != null)
			plot(x, 0, x.length - 1);
	}

	public static void plot(float[] x, int startInd, int endInd) {
		plot(x, startInd, endInd, "");
	}

	public static void plot(double[] x, int startInd, int endInd) {
		plot(x, startInd, endInd, "");
	}

	public static void plot(float[] x, String strTitle) {
		if (x != null)
			plot(x, 0, x.length - 1, strTitle, false);
	}

	public static void plot(double[] x, String strTitle) {
		if (x != null)
			plot(x, 0, x.length - 1, strTitle, false);
	}

	public static void plot(float[] x, int startInd, int endInd, String strTitle) {
		plot(x, startInd, endInd, strTitle, false);
	}

	public static void plot(double[] x, int startInd, int endInd, String strTitle) {
		plot(x, startInd, endInd, strTitle, false);
	}

	public static void plot(float[] x, int startInd, int endInd, String strTitle, boolean bAutoClose) {
		plot(x, startInd, endInd, strTitle, bAutoClose, 3000);
	}

	public static void plot(double[] x, int startInd, int endInd, String strTitle, boolean bAutoClose) {
		plot(x, startInd, endInd, strTitle, bAutoClose, 3000);
	}

	public static void plotZoomed(float[] x, String strTitle, double minVal) {
		plotZoomed(x, strTitle, minVal, MathUtils.getMax(x));
	}

	public static void plotZoomed(double[] x, String strTitle, double minVal) {
		if (x != null)
			plotZoomed(x, strTitle, minVal, MathUtils.getMax(x));
	}

	public static void plotZoomed(float[] x, String strTitle, double minVal, double maxVal) {
		plotZoomed(x, strTitle, minVal, maxVal, false);
	}

	public static void plotZoomed(double[] x, String strTitle, double minVal, double maxVal) {
		plotZoomed(x, strTitle, minVal, maxVal, false);
	}

	public static void plotZoomed(float[] x, String strTitle, double minVal, double maxVal, boolean bAutoClose) {
		plotZoomed(x, strTitle, minVal, maxVal, bAutoClose, 3000);
	}

	public static void plotZoomed(double[] x, String strTitle, double minVal, double maxVal, boolean bAutoClose) {
		plotZoomed(x, strTitle, minVal, maxVal, bAutoClose, 3000);
	}

	public static void plotZoomed(float[] x, String strTitle, double minVal, double maxVal, boolean bAutoClose,
			int milliSecondsToClose) {
		if (x != null) {
			double[] xd = new double[x.length];
			for (int i = 0; i < x.length; i++)
				xd[i] = x[i];

			plotZoomed(xd, strTitle, minVal, maxVal, bAutoClose, milliSecondsToClose);
		}
	}

	public static void plotZoomed(double[] x, String strTitle, double minVal, double maxVal, boolean bAutoClose,
			int milliSecondsToClose) {
		if (x != null) {
			double[] y = null;
			if (minVal > maxVal) {
				double tmp = minVal;
				minVal = maxVal;
				maxVal = tmp;
			}
			y = new double[x.length];
			for (int i = 0; i < x.length; i++) {
				y[i] = x[i];
				if (y[i] < minVal)
					y[i] = minVal;
				else if (y[i] > maxVal)
					y[i] = maxVal;
			}

			plot(y, 0, y.length - 1, strTitle, bAutoClose, milliSecondsToClose);
		}
	}

	public static void plot(float[] xIn, int startInd, int endInd, String strTitle, boolean bAutoClose, int milliSecondsToClose) {
		if (xIn != null) {
			double[] xd = new double[endInd - startInd + 1];
			for (int i = startInd; i <= endInd; i++)
				xd[i - startInd] = xIn[i];

			FunctionGraph graph = new FunctionGraph(400, 200, 0, 1, xd);
			JFrame frame = graph.showInJFrame(strTitle, 500, 300, true, false);

			if (bAutoClose) {
				try {
					Thread.sleep(milliSecondsToClose);
				} catch (InterruptedException e) {
				}
				frame.dispose();
			}
		}
	}

	// Plots the values in x
	// If bAutoClose is specified, the figure is closed after milliSecondsToClose milliseconds
	// milliSecondsToClose: has no effect if bAutoClose is false
	public static void plot(double[] xIn, int startInd, int endInd, String strTitle, boolean bAutoClose, int milliSecondsToClose) {
		if (xIn != null) {
			endInd = MathUtils.CheckLimits(endInd, 0, xIn.length - 1);
			startInd = MathUtils.CheckLimits(startInd, 0, endInd);

			double[] x = new double[endInd - startInd + 1];
			System.arraycopy(xIn, startInd, x, 0, x.length);

			FunctionGraph graph = new FunctionGraph(400, 200, 0, 1, x);
			JFrame frame = graph.showInJFrame(strTitle, 500, 300, true, false);

			if (bAutoClose) {
				try {
					Thread.sleep(milliSecondsToClose);
				} catch (InterruptedException e) {
				}
				frame.dispose();
			}
		}
	}

}
