package marytts.phonetic;

import marytts.phonetic.converter.Arpabet;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class IPATest
{

    @Test
    public void testCase() throws Exception {
	System.out.println(AlphabetFactory.listAvailableAlphabets());
	Assert.assertNotNull(AlphabetFactory.getAlphabet("arpabet"));
	Assert.assertNotNull(AlphabetFactory.getAlphabet("Arpabet"));
    }

    @Test
    public void listArpabetCategories() throws Exception
    {
	Arpabet al = new Arpabet();
	for (String label: al.listLabels()) {
	    System.out.println("label = " + label);
	    String ip = al.getCorrespondingIPA(label);
	    for (int c=0; c<ip.length(); c++) {
		System.out.println("\t- " + ip.charAt(c));

		for (String prop: IPA.ipa_cat_map.get(ip.charAt(c)))
		    System.out.println("\t\t- " + prop);
	    }

	}
	// if (true)
	//     throw new Exception("oups");
    }
}


/* IPATest.java ends here */
