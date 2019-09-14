package marytts.io.serializer.label;

// List
import java.util.ArrayList;

import java.io.IOException;


import marytts.data.utils.IntegerPair;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.item.linguistic.Word;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Syllable;

/* Assert/test */
import org.testng.Assert;
import org.testng.annotations.*;

/**
 *
 *
 * @author <a href="mailto:slemaguer@tcd.ie">Sébastien Le Maguer</a>
 */
public class DefaultHTSLabelSerializerTest
{

    @Test
    public void testLoadingLabelWithoutDuration() throws Exception {
        DefaultHTSLabelSerializer ser = new DefaultHTSLabelSerializer();
    }
}
