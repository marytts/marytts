/**
 * 
 */
package marytts.util.math;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author marc
 *
 */
public class ComplexNumberTest {

	@Test
	public void isEqual() {
		ComplexNumber n1 = new ComplexNumber(1,2);
		ComplexNumber n2 = new ComplexNumber(1,2);
		assertEquals(n1, n2);
	}
}
