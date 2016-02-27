package marytts.signalproc.effects;

import org.junit.Test;
import static org.junit.Assert.*;

public class AudioEffectsTest {
	@Test
	public void canGetEffects() {
		assertNotNull(AudioEffects.getEffects());
		assertTrue(AudioEffects.getEffects().iterator().hasNext());
	}

	@Test
	public void haveEffects() {
		assertTrue(AudioEffects.countEffects() > 0);
	}

	@Test
	public void canGetByName() {
		String name = "Robot";
		AudioEffect ae = AudioEffects.getEffect(name);
		assertNotNull(ae);
		assertEquals(name, ae.getName());
	}

	@Test
	public void canGetDefaultParams() {
		String name = "Robot";
		String expected = "amount:100.0;";
		// exercise
		String params = AudioEffects.getEffect(name).getExampleParameters();
		// verify
		assertEquals(expected, params);
	}

}
