import org.junit.Assert;
import org.junit.Test;

public class multiply {
	//Test Case 1: Input -> {3, 4}, Output -> 12
	@Test
	public void multiplyTest() {
		Assert.assertEquals(12, MathUtils.multiply(new float [] {3, 4}, 0));
	}
	//Test Case 2: Input -> {0, 9} or {9, 0}, Output -> 0
	@Test
	public void MuliplyTestwithZero() {
		Assert.assertEquals(0, MathUtils.multiply(new float [] {0, 9}, 0));
		Assert.assertEquals(0, MathUtils.multiply(new float [] {9, 0}, 0));
	}
}
