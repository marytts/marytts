package marytts.tools.install;

import org.testng.Assert;
import org.testng.annotations.*;

public class ComponentDescriptionTest {

	@Test
	public void versionNewer() {
		Assert.assertTrue(ComponentDescription.isVersionNewerThan("0.2", "0.1"));
	}

	@Test
	public void snapshotNotNewer() {
		Assert.assertFalse(ComponentDescription.isVersionNewerThan("5.0-SNAPSHOT", "5.0"));
	}

	@Test
	public void newerThanSnapshot() {
		Assert.assertTrue(ComponentDescription.isVersionNewerThan("5.0", "5.0-SNAPSHOT"));
	}

	@Test
	public void snapshotNewer1() {
		Assert.assertTrue(ComponentDescription.isVersionNewerThan("5.1-SNAPSHOT", "5.0"));
	}

	@Test
	public void snapshotNewer2() {
		Assert.assertTrue(ComponentDescription.isVersionNewerThan("5.1-SNAPSHOT", "5.0-SNAPSHOT"));
	}

	@Test
	public void notNewerThanMyself() {
		Assert.assertFalse(ComponentDescription.isVersionNewerThan("5.1-SNAPSHOT", "5.1-SNAPSHOT"));
	}

}
