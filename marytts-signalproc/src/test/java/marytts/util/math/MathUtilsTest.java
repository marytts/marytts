package marytts.util.math;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilsTest {

	@Test
	public void testInterpolateNonZeroValues() {
		double[] contour = new double[] { 1, 0, 3 };
		double[] expected = new double[] { 1, 2, 3 };
		double[] actual = MathUtils.interpolateNonZeroValues(contour);
		Assert.assertArrayEquals(expected, actual, 0d);
	}
}
