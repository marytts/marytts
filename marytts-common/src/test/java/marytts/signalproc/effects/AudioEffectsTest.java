package marytts.signalproc.effects;

import org.testng.Assert;
import org.testng.annotations.*;

public class AudioEffectsTest {
	@Test
	public void canGetEffects() {
		Assert.assertNotNull(AudioEffects.getEffects());
		Assert.assertTrue(AudioEffects.getEffects().iterator().hasNext());
	}

	@Test
	public void haveEffects() {
		Assert.assertTrue(AudioEffects.countEffects() > 0);
	}

	@Test
	public void canGetByName() {
		String name = "Robot";
		AudioEffect ae = AudioEffects.getEffect(name);
		Assert.assertNotNull(ae);
		Assert.assertEquals(name, ae.getName());
	}

	@Test
	public void canGetDefaultParams() {
		String name = "Robot";
		String expected = "amount:100.0;";
		// exercise
		String params = AudioEffects.getEffect(name).getExampleParameters();
		// verify
		Assert.assertEquals(expected, params);
	}

}
