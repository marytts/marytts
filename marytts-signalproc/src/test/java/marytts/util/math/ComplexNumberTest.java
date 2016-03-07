/**
 *
 */
package marytts.util.math;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * @author marc
 *
 */
public class ComplexNumberTest {

	@Test
	public void isEqual() {
		ComplexNumber n1 = new ComplexNumber(1, 2);
		ComplexNumber n2 = new ComplexNumber(1, 2);
		Assert.assertEquals(n1, n2);
	}
}
