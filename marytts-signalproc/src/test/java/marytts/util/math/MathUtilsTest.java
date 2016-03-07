package marytts.util.math;


import org.testng.Assert;
import org.testng.annotations.*;

public class MathUtilsTest {

	@Test
	public void testInterpolateNonZeroValues() {
		double[] contour = new double[] { 1, 0, 3 };
		double[] expected = new double[] { 1, 2, 3 };
		double[] actual = MathUtils.interpolateNonZeroValues(contour);
		Assert.assertEquals(expected, actual);
	}
}
