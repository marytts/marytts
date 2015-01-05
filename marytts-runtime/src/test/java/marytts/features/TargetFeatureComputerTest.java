package marytts.features;

import static org.junit.Assert.*;

import java.util.Locale;

import marytts.unitselection.select.Target;
import marytts.util.string.ByteStringTranslator;

import org.junit.Before;
import org.junit.Test;

public class TargetFeatureComputerTest {

	private TestByteValuedFeatureProcessor processor;
	private TargetFeatureComputer computer;

	private String[] valueProvider() {
		String[] values = new String[ByteStringTranslator.MAXNUM];
		for (int v = 0; v < values.length; v++) {
			values[v] = "val" + v;
		}
		return values;
	}

	@Before
	public void setUp() throws Exception {
		System.setProperty(".allophoneset", "jar:/marytts/features/allophones.ROOT.xml");
		FeatureProcessorManager manager = new FeatureProcessorManager(Locale.ROOT);
		processor = new TestByteValuedFeatureProcessor();
		manager.addFeatureProcessor(processor);
		computer = new TargetFeatureComputer(manager, processor.getName());
	}

	@Test
	public void testToStringValues() {
		String[] values = processor.getValues();
		ByteStringTranslator translator = new ByteStringTranslator(values);
		for (String expected : values) {
			byte feature = translator.get(expected);
			FeatureVector vector = new FeatureVector(new byte[] { feature }, new short[] {}, new float[] {}, 0);
			String actual = computer.toStringValues(vector);
			assertEquals(expected, actual);
		}
	}

	public class TestByteValuedFeatureProcessor implements ByteValuedFeatureProcessor {

		private ByteStringTranslator values;

		public TestByteValuedFeatureProcessor() {
			this.values = new ByteStringTranslator(valueProvider());
		}

		@Override
		public String getName() {
			return "test_feature";
		}

		@Override
		public byte process(Target target) {
			return 0;
		}

		@Override
		public String[] getValues() {
			return values.getStringValues();
		}
	}
}
