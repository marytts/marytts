package marytts;

import org.testng.Assert;
import org.testng.annotations.Test;

public class VersionTest {

    @Test
    public void testVersion() {
        String expected = System.getProperty("version");
        Assert.assertNotNull(expected);
        String actual = Version.specificationVersion();
        Assert.assertEquals(actual, expected);
    }
}
