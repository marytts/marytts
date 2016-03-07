package marytts.util.data;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.signalproc.process.InlineDataProcessor;
import marytts.util.data.audio.DDSAudioInputStream;


import org.testng.Assert;
import org.testng.annotations.*;


public class ProducingDoubleDataSourceTest {

	private static AudioFormat getTestAudioFormat() {
		float sampleRate = 16000; // 8000,11025,16000,22050,44100,48000
		int sampleSizeInBits = 16; // 8,16
		int channels = 1; // 1,2
		boolean signed = true; // true,false
		boolean bigEndian = false; // true,false
		AudioFormat af = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		return af;
	}

	@Test
	public void canReadZeroDoubles() {
		TestProducer producer = new TestProducer(0);
		producer.start();
		Assert.assertEquals(0, producer.available());
		Assert.assertEquals(0, producer.getDataLength());
		double[] data = producer.getAllData();
		Assert.assertEquals(0, data.length);
	}

	@Test
	public void canReadZeroFrames() throws IOException {
		TestProducer producer = new TestProducer(0);
		producer.start();
		AudioInputStream ais = new DDSAudioInputStream(producer, getTestAudioFormat());
		Assert.assertEquals(0, ais.getFrameLength());
	}

	@Test
	public void canReadTenDoubles() {
		int numDoubles = 10;
		TestProducer producer = new TestProducer(numDoubles);
		producer.start();
		Assert.assertEquals(numDoubles, producer.getDataLength());
		double[] data = producer.getAllData();
		Assert.assertEquals(numDoubles, data.length);
	}

	@Test
	public void canRead4000Doubles() {
		int numDoubles = 4000;
		TestProducer producer = new TestProducer(numDoubles);
		producer.start();
		Assert.assertEquals(numDoubles, producer.getDataLength());
		double[] data = producer.getAllData();
		Assert.assertEquals(numDoubles, data.length);
	}

	@Test
	public void canReadTenFrames() throws Exception {
		int numDoubles = 10;
		TestProducer producer = new TestProducer(numDoubles);
		producer.start();
		AudioInputStream ais = new DDSAudioInputStream(producer, getTestAudioFormat());
		Assert.assertEquals(numDoubles, ais.getFrameLength());
		int bytesRead = ais.read(new byte[16000]);
		Assert.assertEquals(2 * numDoubles, bytesRead);

	}

	@Test
	public void canRead4000Frames() throws Exception {
		int numDoubles = 4000;
		TestProducer producer = new TestProducer(numDoubles);
		producer.start();
		AudioInputStream ais = new DDSAudioInputStream(producer, getTestAudioFormat());
		Assert.assertEquals(numDoubles, ais.getFrameLength());
		int bytesRead = ais.read(new byte[16000]);
		Assert.assertEquals(2 * numDoubles, bytesRead);
	}

	@Test
	public void willApplyInlineProcessor() {
		InlineDataProcessor halver = new InlineDataProcessor() {
			public void applyInline(double[] data, int off, int len) {
				for (int i = off, max = off + len; i < max; i++) {
					data[i] *= .5;
				}
			}
		};
		int numDoubles = 100;
		TestProducer producer = new TestProducer(numDoubles, halver);
		producer.start();
		double[] result = producer.getAllData();
		for (int i = 0; i < numDoubles; i++) {
			Assert.assertEquals(producer.DUMMY * .5, result[i], 1.e-10);
		}
	}

	private static class TestProducer extends ProducingDoubleDataSource {
		double DUMMY = 0.23;

		public TestProducer(int numToSend) {
			super(numToSend);
		}

		public TestProducer(int numToSend, InlineDataProcessor dataProcessor) {
			super(numToSend, dataProcessor);
		}

		public void run() {
			long numToSend = getDataLength();
			while (numToSend > 0) {
				putOneDataPoint(DUMMY);
				numToSend--;
			}
			putEndOfStream();
		}

	}
}
