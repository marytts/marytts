package marytts.language.de;

import java.util.Locale;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;

import marytts.exceptions.SynthesisException;
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
}
