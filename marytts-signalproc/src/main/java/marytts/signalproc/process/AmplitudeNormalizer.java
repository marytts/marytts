package marytts.signalproc.process;

import marytts.util.math.MathUtils;

/**
 * The purpose of this class is to make sure that its data never exceeds amplitude +/-1. This is a simple and approximate
 * streaming-mode amplitude normalizer; it will use the maximum seen so far as the maximum amplitude, which means that audio at
 * the beginning of the file might play louder than audio at the end if the maximum is non-initial.
 * 
 * @author marc
 * 
 */
public class AmplitudeNormalizer implements InlineDataProcessor {

	private double max;

	public AmplitudeNormalizer(double initialMax) {
		this.max = initialMax;
	}

	public void applyInline(double[] data, int off, int len) {
		double localMax = MathUtils.absMax(data, off, len);
		if (localMax > max) {
			max = localMax;
		}
		for (int i = off; i < off + len; i++) {
			data[i] /= max;
		}
	}

}
