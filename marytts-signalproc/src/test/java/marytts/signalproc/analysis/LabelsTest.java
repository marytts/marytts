package marytts.signalproc.analysis;

import java.io.InputStreamReader;
import java.util.Arrays;

import marytts.util.data.text.XwavesLabelfileReader;
import marytts.util.io.FileUtils;

import org.testng.Assert;
import org.testng.annotations.*;

public class LabelsTest {
	private Label[] createTestLabels() {
		Label[] items = new Label[5];
		for (int i = 0; i < items.length; i++) {
			items[i] = new Label(0.01 * i, 0, "a" + i, 0);
		}
		return items;
	}

	@Test
	public void canReadLabFile() throws Exception {
		Labels l = new Labels(getClass().getResourceAsStream("pop001.lab"));
		Assert.assertEquals(10, l.items.length);
	}

	@Test
	public void labelArrayConstructor() {
		Label[] items = createTestLabels();
		Labels l = new Labels(items);
		Assert.assertNotSame(l.items, items);
		Assert.assertEquals(l.items, items);
	}

	@Test
	public void lineArrayConstructor() throws Exception {
		String[] lines = FileUtils.getStreamAsString(getClass().getResourceAsStream("pop001.lab"), "ASCII").split("\n");
		Labels l = new Labels(lines);
		Assert.assertEquals(10, l.items.length);
	}

	@Test
	public void copyConstructor() {
		Labels l1 = new Labels(createTestLabels());
		Labels l2 = new Labels(l1);
		Assert.assertNotSame(l1.items, l2.items);
		Assert.assertEquals(l1, l2);
	}

	@Test
	public void indexFromTime() {
		Labels l = new Labels(createTestLabels());
		Assert.assertEquals(2, l.getLabelIndexAtTime(0.019));
	}

	@Test
	public void canReadWithXwavesLabelfileReader() throws Exception {
		Labels l = new Labels(getClass().getResourceAsStream("pop001.lab"));
		XwavesLabelfileReader xr = new XwavesLabelfileReader(new InputStreamReader(getClass().getResourceAsStream("pop001.lab"),
                                                                                   "ASCII"));
		Labels xl = xr.getLabels();
		Assert.assertEquals(l.items.length, xl.items.length);
		Assert.assertEquals(l.items, xl.items);
		Assert.assertEquals(l, xl);
		Assert.assertEquals(l.getLabelSymbols(), xl.getLabelSymbols());
	}
}
