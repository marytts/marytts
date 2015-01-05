/**
 * 
 */
package marytts.tools.analysis;

import static org.junit.Assert.*;

import java.io.IOException;

import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.analysis.Labels;
import marytts.util.dom.DomUtils;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author marc
 *
 */
public class CopySynthesisTest {

	private AllophoneSet getAllophoneSet() throws MaryConfigurationException {
		return AllophoneSet.getAllophoneSet(getClass()
				.getResourceAsStream("/marytts/language/en_GB/lexicon/allophones.en_GB.xml"), "dummy");
	}

	private Labels getReferenceLabels() throws IOException {
		return new Labels(getClass().getResourceAsStream("pop001.lab"));
	}

	private Document getTestDocument() throws Exception {
		return DomUtils.parseDocument(getClass().getResourceAsStream("pop001.dfki-poppy.ACOUSTPARAMS"));
	}

	private void assertSimilarDurations(Labels l1, Labels l2) {
		// label durations are "similar" if for each label pair the difference is less than one millisecond
		assertEquals(l1.items.length, l2.items.length);
		assertEquals(l1.items[0].time, l2.items[0].time, 0.001);
		for (int i = 1; i < l1.items.length; i++) {
			double d1 = l1.items[i].time - l1.items[i - 1].time;
			double d2 = l2.items[i].time - l2.items[i - 1].time;
			assertEquals(d1, d2, 0.001);
		}
	}

	@Test
	public void imposeSegments() throws Exception {
		Labels source = getReferenceLabels();
		Document target = getTestDocument();
		CopySynthesis cs = new CopySynthesis(getAllophoneSet());
		cs.imposeSegments(source, target);
		assertArrayEquals(source.getLabelSymbols(), new Labels(target).getLabelSymbols());
	}

	@Test
	public void imposeDurations() throws Exception {
		Labels source = getReferenceLabels();
		Document target = getTestDocument();
		System.out.println("Document before imposing durations:");
		System.out.println(DomUtils.document2String(target));
		CopySynthesis cs = new CopySynthesis(getAllophoneSet());
		cs.imposeDurations(source, target);
		System.out.println("\n\n\nDocument after imposing durations:");
		System.out.println(DomUtils.document2String(target));
		assertSimilarDurations(source, new Labels(target));
	}
}
