package marytts.signalproc.analysis;

import java.io.InputStreamReader;
import java.util.Arrays;

import marytts.util.data.text.XwavesLabelfileReader;
import marytts.util.io.FileUtils;

import org.junit.Test;
import static org.junit.Assert.*;

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
		assertEquals(10, l.items.length);
	}

	@Test
	public void labelArrayConstructor() {
		Label[] items = createTestLabels();
		Labels l = new Labels(items);
		assertNotSame(l.items, items);
		assertArrayEquals(l.items, items);
	}

	@Test
	public void lineArrayConstructor() throws Exception {
		String[] lines = FileUtils.getStreamAsString(getClass().getResourceAsStream("pop001.lab"), "ASCII").split("\n");
		Labels l = new Labels(lines);
		assertEquals(10, l.items.length);
	}

	@Test
	public void copyConstructor() {
		Labels l1 = new Labels(createTestLabels());
		Labels l2 = new Labels(l1);
		assertNotSame(l1.items, l2.items);
		assertEquals(l1, l2);
	}

	@Test
	public void indexFromTime() {
		Labels l = new Labels(createTestLabels());
		assertEquals(2, l.getLabelIndexAtTime(0.019));
	}

	@Test
	public void canReadWithXwavesLabelfileReader() throws Exception {
		Labels l = new Labels(getClass().getResourceAsStream("pop001.lab"));
		XwavesLabelfileReader xr = new XwavesLabelfileReader(new InputStreamReader(getClass().getResourceAsStream("pop001.lab"),
				"ASCII"));
		Labels xl = xr.getLabels();
		assertEquals(l.items.length, xl.items.length);
		assertArrayEquals(l.items, xl.items);
		assertEquals(l, xl);
		assertArrayEquals(l.getLabelSymbols(), xl.getLabelSymbols());
	}
}
