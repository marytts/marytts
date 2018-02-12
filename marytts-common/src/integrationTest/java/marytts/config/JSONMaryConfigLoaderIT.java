package marytts.config;

import marytts.exceptions.MaryConfigurationException;

import marytts.data.item.phonology.Phoneme;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.*;

public class JSONMaryConfigLoaderIT {
    @Test
    public void testDefaultConfigurationLoading() throws MaryConfigurationException {
	JSONMaryConfigLoader loader = new JSONMaryConfigLoader();
	Assert.assertNotNull(MaryConfigurationFactory.getDefaultConfiguration());
    }


    @Test
    public void testLoadingWithValue() throws MaryConfigurationException {
	JSONMaryConfigLoader loader = new JSONMaryConfigLoader();
	MaryConfiguration m = loader.loadConfiguration(this.getClass().getResourceAsStream("ok_value.json"));

	Phoneme ph = new Phoneme("test");
	m.applyConfiguration(ph);
	System.out.println(m.toString());
	System.out.println(ph.getStress());
	Assert.assertTrue(ph.getStress().equals("ok"));
    }


    @Test
    public void testLoadingWithObject() throws MaryConfigurationException {
	JSONMaryConfigLoader loader = new JSONMaryConfigLoader();
	MaryConfiguration m = loader.loadConfiguration(this.getClass().getResourceAsStream("ok_object.json"));

	Phoneme ph = new Phoneme("test");
	m.applyConfiguration(ph);
	System.out.println(m.toString());
	System.out.println(ph.getStress());
	Assert.assertTrue(ph.getStress().equals("ok"));
    }
}
