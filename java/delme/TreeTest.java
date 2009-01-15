package delme;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Locale;

import marytts.cart.CART;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.MaryCARTReader;
import marytts.cart.io.MaryCARTWriter;
import marytts.cart.io.WagonCARTReader;
import marytts.features.FeatureDefinition;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;

public class TreeTest {

    public static void main(String[] args) throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("lib/modules/en/us/lexicon/cmudict.lts.pfeats"), "UTF-8"));
        FeatureDefinition featureDef = new FeatureDefinition(br, false);
        br.close();

        CART st;
        
        br = new BufferedReader(new InputStreamReader(new FileInputStream("lib/modules/en/us/lexicon/cmudict.lts.wagontree.txt"), "UTF-8"));
        WagonCARTReader wagonReader = new WagonCARTReader(LeafType.StringAndFloatLeafNode);
        st = new CART(wagonReader.load(br, featureDef), featureDef);
        
        MaryCARTWriter cartWriter = new MaryCARTWriter();
        cartWriter.dumpMaryCART(st, "lib/modules/en/us/lexicon/cmudict.lts.tree.binary");
        PrintWriter pw = new PrintWriter("lib/modules/en/us/lexicon/cmudict.lts.tree.txt");
        cartWriter.toTextOut(st, pw);
        pw.close();

        MaryCARTReader cartReader = new MaryCARTReader();
        st = cartReader.load("lib/modules/en/us/lexicon/cmudict.lts.tree.binary");

        AllophoneSet usSampa = AllophoneSet.getAllophoneSet("lib/modules/en/us/lexicon/allophones.en_US.xml");

        TrainedLTS lts = new TrainedLTS(usSampa, st);
        String key = "funny";
        String result = lts.syllabify(lts.predictPronunciation(key));
        System.err.println("    "+key+" -> "+ result);

    }
}
