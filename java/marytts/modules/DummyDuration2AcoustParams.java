package marytts.modules;

import java.util.Locale;

import marytts.datatypes.MaryDataType;
import marytts.util.MaryUtils;


public class DummyDuration2AcoustParams extends InternalModule
{
    public DummyDuration2AcoustParams()
    {
        this((Locale)null);
    }
    
    /**
     * Constructor to be called  with instantiated objects.
     * @param locale
     */
    public DummyDuration2AcoustParams(String locale)
    {
        super("DummyAllophones2AcoustParams",
                MaryDataType.DURATIONS,
                MaryDataType.ACOUSTPARAMS,
                MaryUtils.string2locale(locale));
    }
    
    /**
     * Constructor to be called  with instantiated objects.
     * @param locale
     */
    public DummyDuration2AcoustParams(Locale locale)
    {
        super("DummyAllophones2AcoustParams",
                MaryDataType.DURATIONS,
                MaryDataType.ACOUSTPARAMS,
                locale);
    }
}

