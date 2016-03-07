package marytts.util.data.text;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import marytts.util.data.text.PraatPitchTier.PitchTarget;
import marytts.util.dom.DomUtils;
import marytts.util.math.MathUtils;

import org.w3c.dom.Document;


import org.testng.Assert;
import org.testng.annotations.*;


public class PraatPitchTierTest {

	private PraatPitchTier pt;
	private PraatPitchTier spt;

	private void assertTimesStrictlyIncreasing(PitchTarget[] targets) {
		for (int i = 1; i < targets.length; i++) {
			Assert.assertTrue(targets[i].time > targets[i - 1].time);
		}
	}

	@BeforeMethod
	public void setUpDefault() throws Exception {
		Reader pitchTierFile = new InputStreamReader(getClass().getResourceAsStream("pop001.PitchTier"), "UTF-8");
		pt = new PraatPitchTier(pitchTierFile);
	}

	@BeforeMethod
	public void setUpShort() throws Exception {
		Reader shortPitchTierFile = new InputStreamReader(getClass().getResourceAsStream("pop001_short.PitchTier"), "UTF-8");
		spt = new PraatPitchTier(shortPitchTierFile);
	}

	@Test
	public void canReadPitchFileDefault() throws Exception {
		Assert.assertNotNull(pt);
		Assert.assertEquals(0, pt.getXmin(), 1.e-7);
		Assert.assertEquals(2.52, pt.getXmax(), 1.e-7);
		Assert.assertEquals(109, pt.getNumTargets());
		Assert.assertEquals(pt.getNumTargets(), pt.getPitchTargets().length);
		Assert.assertNotNull(pt.getPitchTargets()[0]);
	}

	@Test
	public void canReadPitchFileShort() throws Exception {
		Assert.assertNotNull(spt);
		Assert.assertEquals(0, spt.getXmin(), 1.e-7);
		Assert.assertEquals(2.52, spt.getXmax(), 1.e-7);
		Assert.assertEquals(109, spt.getNumTargets());
		Assert.assertEquals(spt.getNumTargets(), spt.getPitchTargets().length);
		Assert.assertNotNull(spt.getPitchTargets()[0]);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void chokeOnWrongFile1() throws Exception {
		String wrong = "this is a wrong first line";
		new PraatPitchTier(new StringReader(wrong));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void chokeOnWrongFile2() throws Exception {
		String wrong = PraatPitchTier.FIRSTLINE + "\nthis is a wrong second line";
		new PraatPitchTier(new StringReader(wrong));
	}

	@Test
	public void canWritePitchFileDefault() throws Exception {
		StringWriter sw = new StringWriter();
		pt.writeTo(sw);
		String newFile = sw.toString();
		PraatPitchTier newPT = new PraatPitchTier(new StringReader(newFile));
		Assert.assertEquals(pt.getPitchTargets().length, newPT.getPitchTargets().length);
	}

	@Test
	public void canWritePitchFileShort() throws Exception {
		StringWriter sw = new StringWriter();
		spt.writeTo(sw);
		String newFile = sw.toString();
		PraatPitchTier newPT = new PraatPitchTier(new StringReader(newFile));
		Assert.assertEquals(spt.getPitchTargets().length, newPT.getPitchTargets().length);
	}

	@Test
	public void canExportFramesDefault() {
		double step = 0.01;
		double[] frames = pt.toFrames(step);
		Assert.assertEquals((int) ((pt.getXmax() - pt.getXmin()) / step + 1), frames.length);
	}

	@Test
	public void canExportFramesShort() {
		double step = 0.01;
		double[] frames = spt.toFrames(step);
		Assert.assertEquals((int) ((spt.getXmax() - spt.getXmin()) / step + 1), frames.length);
	}

	@Test
	public void canComputeFrequency() throws Exception {
		String simpleFile = PraatPitchTier.FIRSTLINE + "\n" + PraatPitchTier.SECONDLINE + "\n\n";
		simpleFile += "0\n3\n" // from 0 to 3 seconds
            + "3\n" // four targets
            + "1\n100\n" // at 1 second, 100 Hz
            + "2\n200\n" // at 2 seconds, 200 Hz
            + "2.5\n300\n"; // at 2.5 seconds, 300 Hz
		PraatPitchTier simple = new PraatPitchTier(new StringReader(simpleFile));
		Assert.assertTrue(Double.isNaN(simple.getFrequency(0)));
		Assert.assertTrue(Double.isNaN(simple.getFrequency(0.99)));
		Assert.assertEquals(100, simple.getFrequency(1), 1.e-7);
		Assert.assertEquals(150, simple.getFrequency(1.5), 1.e-7);
		Assert.assertEquals(175, simple.getFrequency(1.75), 1.e-7);
		Assert.assertEquals(200, simple.getFrequency(2), 1.e-7);
		Assert.assertEquals(250, simple.getFrequency(2.25), 1.e-7);
		Assert.assertEquals(300, simple.getFrequency(2.5), 1.e-7);
		Assert.assertTrue(Double.isNaN(simple.getFrequency(2.51)));
		Assert.assertTrue(Double.isNaN(simple.getFrequency(3)));
	}

	@Test
	public void canImportFramesDefault() {
		double step = 0.01;
		double[] frames = pt.toFrames(step);
		PraatPitchTier newPT = new PraatPitchTier(pt.getXmin(), frames, step);
		Assert.assertEquals(pt.getXmin(), newPT.getXmin(), 1.e-7);
		Assert.assertEquals(pt.getXmax(), newPT.getXmax(), step); // xmax will be less
        // precisely equal
		Assert.assertEquals(newPT.getNumTargets(), newPT.getPitchTargets().length);
		Assert.assertNotNull(newPT.getPitchTargets()[0]);
		Assert.assertTrue(MathUtils.sumSquaredError(pt.toFrames(step), newPT.toFrames(step)) < 1.e-30);
	}

	@Test
	public void canImportFramesShort() {
		double step = 0.01;
		double[] frames = spt.toFrames(step);
		PraatPitchTier newPT = new PraatPitchTier(spt.getXmin(), frames, step);
		Assert.assertEquals(spt.getXmin(), newPT.getXmin(), 1.e-7);
		Assert.assertEquals(spt.getXmax(), newPT.getXmax(), step); // xmax will be less
        // precisely equal
		Assert.assertEquals(newPT.getNumTargets(), newPT.getPitchTargets().length);
		Assert.assertNotNull(newPT.getPitchTargets()[0]);
		Assert.assertTrue(MathUtils.sumSquaredError(spt.toFrames(step), newPT.toFrames(step)) < 1.e-30);
	}

	@Test
	public void canParseBothTextFormats() {
		Assert.assertEquals(pt, spt);
	}

	@Test
	public void canCreateFromMaryXML() throws Exception {
		Document acoustparams = DomUtils.parseDocument(getClass().getResourceAsStream("pop001.dfki-poppy-hsmm.ACOUSTPARAMS"));
		PraatPitchTier ptM = new PraatPitchTier(acoustparams);
		Assert.assertNotNull(ptM);
		Assert.assertNotNull(ptM.getPitchTargets());
		Assert.assertNotNull(ptM.toFrames(0.01));
		assertTimesStrictlyIncreasing(ptM.getPitchTargets());
	}

}
