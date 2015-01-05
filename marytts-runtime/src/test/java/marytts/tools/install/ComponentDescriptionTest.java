package marytts.tools.install;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComponentDescriptionTest {

	@Test
	public void versionNewer() {
		assertTrue(ComponentDescription.isVersionNewerThan("0.2", "0.1"));
	}

	@Test
	public void snapshotNotNewer() {
		assertFalse(ComponentDescription.isVersionNewerThan("5.0-SNAPSHOT", "5.0"));
	}

	@Test
	public void newerThanSnapshot() {
		assertTrue(ComponentDescription.isVersionNewerThan("5.0", "5.0-SNAPSHOT"));
	}

	@Test
	public void snapshotNewer1() {
		assertTrue(ComponentDescription.isVersionNewerThan("5.1-SNAPSHOT", "5.0"));
	}

	@Test
	public void snapshotNewer2() {
		assertTrue(ComponentDescription.isVersionNewerThan("5.1-SNAPSHOT", "5.0-SNAPSHOT"));
	}

	@Test
	public void notNewerThanMyself() {
		assertFalse(ComponentDescription.isVersionNewerThan("5.1-SNAPSHOT", "5.1-SNAPSHOT"));
	}

}
