import org.junit.Assert;
import org.junit.Test;

public class multiply {
	@Test
	public void multiplyTest() {
		Assert.assertEquals(12, MathUtils.multiply(new float [] {3, 4}, 0));
	}
	@Test
	public void MuliplyTestwithZero() {
		Assert.assertEquals(0, MathUtils.multiply(new float [] {0, 9}, 0));
		Assert.assertEquals(0, MathUtils.multiply(new float [] {9, 0}, 0));
	}
}