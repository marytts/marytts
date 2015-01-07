package marytts.modules.phonemiser;

import java.io.InputStream;
import java.util.LinkedList;

import org.testng.annotations.*;

public class SyllabifierTest {

	private Syllabifier syllabifier;

	@BeforeTest
	public void setUp() throws Exception {
		InputStream resource = getClass().getResourceAsStream("/marytts/features/allophones.ROOT.xml");
		AllophoneSet allophoneSet = AllophoneSet.getAllophoneSet(resource, "");
		syllabifier = new Syllabifier(allophoneSet);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testSyllabfyNullList() {
		LinkedList<String> nullList = null;
		syllabifier.syllabify(nullList);
	}
}
