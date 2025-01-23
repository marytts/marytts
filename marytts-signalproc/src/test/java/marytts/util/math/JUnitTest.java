package marytts.util.math;

import static org.junit.Assert;
import org.junit.Test;

public class MultiplyTest {
	@Test
	public void multiplyTest() {
		assertEquals(12, Multiply.multiply(new float [] {3, 4}), "Multiplication should work");
	}
	public void MuliplyTestwithZero() {
		assertEquals(0, Multiply.multiply(new float [] {0, 9}), "Multiplication with zero should be 0");
		assertEquals(0, Multiply.multiply(new float [] {9, 0}), "Multiplication with zero should be 0");
	}
}