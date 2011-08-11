package marytts.util.data.text;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import marytts.util.math.MathUtils;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PraatPitchTierTest {

	private PraatPitchTier pt;
	
	@Before
	public void setUp() throws Exception {
		Reader shortPitchTierFile = new InputStreamReader(getClass().getResourceAsStream("pop001_short.PitchTier"), "UTF-8");
		pt = new PraatPitchTier(shortPitchTierFile);
	}
	
	@Test
	public void canReadPitchFile() throws Exception {
		assertNotNull(pt);
		assertEquals(0, pt.getXmin(), 1.e-7);
		assertEquals(2.52, pt.getXmax(), 1.e-7);
		assertEquals(109, pt.getNumTargets());
		assertEquals(pt.getNumTargets(), pt.getPitchTargets().length);
		assertNotNull(pt.getPitchTargets()[0]);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void chokeOnWrongFile1() throws Exception {
		String wrong = "this is a wrong first line";
		new PraatPitchTier(new StringReader(wrong));
	}

	@Test(expected=IllegalArgumentException.class)
	public void chokeOnWrongFile2() throws Exception {
		String wrong = PraatPitchTier.FIRSTLINE + "\nthis is a wrong second line";
		new PraatPitchTier(new StringReader(wrong));
	}

	@Test
	public void canWritePitchFile() throws Exception {
		StringWriter sw = new StringWriter();
		pt.writeTo(sw);
		String newFile = sw.toString();
		PraatPitchTier newPT = new PraatPitchTier(new StringReader(newFile));
		assertEquals(pt.getPitchTargets().length, newPT.getPitchTargets().length);
	}
	
	@Test
	public void canExportFrames() {
		double step = 0.01;
		double[] frames = pt.toFrames(step);
		assertEquals((int) ((pt.getXmax()-pt.getXmin())/step+1), frames.length);
	}
	
	
	@Test
	public void canComputeFrequency() throws Exception {
		String simpleFile = PraatPitchTier.FIRSTLINE+"\n"+PraatPitchTier.SECONDLINE+"\n\n"
			+"0\n3\n" // from 0 to 3 seconds
			+"3\n" // four targets
			+"1\n100\n" // at 1 second, 100 Hz
			+"2\n200\n" // at 2 seconds, 200 Hz
			+"2.5\n300\n"; // at 2.5 seconds, 300 Hz
		PraatPitchTier simple = new PraatPitchTier(new StringReader(simpleFile));
		assertTrue(Double.isNaN(simple.getFrequency(0)));
		assertTrue(Double.isNaN(simple.getFrequency(0.99)));
		assertEquals(100, simple.getFrequency(1), 1.e-7);
		assertEquals(150, simple.getFrequency(1.5), 1.e-7);
		assertEquals(175, simple.getFrequency(1.75), 1.e-7);
		assertEquals(200, simple.getFrequency(2), 1.e-7);
		assertEquals(250, simple.getFrequency(2.25), 1.e-7);
		assertEquals(300, simple.getFrequency(2.5), 1.e-7);
		assertTrue(Double.isNaN(simple.getFrequency(2.51)));
		assertTrue(Double.isNaN(simple.getFrequency(3)));
	}
	
	@Test
	public void canImportFrames() {
		double step = 0.01;
		double[] frames = pt.toFrames(step);
		PraatPitchTier newPT = new PraatPitchTier(pt.getXmin(), frames, step);
		assertEquals(pt.getXmin(), newPT.getXmin(), 1.e-7);
		assertEquals(pt.getXmax(), newPT.getXmax(), step); // xmax will be less precisely equal
		assertEquals(newPT.getNumTargets(), newPT.getPitchTargets().length);
		assertNotNull(newPT.getPitchTargets()[0]);
		assertTrue(MathUtils.sumSquaredError(pt.toFrames(step), newPT.toFrames(step)) < 1.e-30);
	}
	
}
