package marytts.language.de;

import java.util.Locale;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.util.dom.DomUtils;

import org.w3c.dom.Document;

import org.testng.Assert;
import org.testng.annotations.*;

public class MaryInterfaceDeIT {

	@Test
	public void canSetLocale() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		Locale loc = Locale.GERMAN;
		Assert.assertTrue(!loc.equals(mary.getLocale()));
		mary.setLocale(loc);
		Assert.assertEquals(loc, mary.getLocale());
	}

	@Test
	public void canProcessTokensToAllophones() throws Exception {
		// setup
		MaryInterface mary = new LocalMaryInterface();
		mary.setInputType(MaryDataType.TOKENS.name());
		mary.setOutputType(MaryDataType.ALLOPHONES.name());
		mary.setLocale(Locale.GERMAN);
		String example = MaryDataType.getExampleText(MaryDataType.TOKENS, mary.getLocale());
		Assert.assertNotNull(example);
		Document tokens = DomUtils.parseDocument(example);
		// exercise
		Document allos = mary.generateXML(tokens);
		// verify
		Assert.assertNotNull(allos);
	}
}
