package marytts.language.lb;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.modules.MinimalisticPosTagger;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

public class EvenMoreMinimalisticPosTagger extends MinimalisticPosTagger {

    /**
     * Constructor which can be directly called from init info in the config file. Different languages can call this code with
     * different settings.
     *
     * @param locale a locale string, e.g. "en"
     * @throws Exception Exception
     */
    public EvenMoreMinimalisticPosTagger(String locale) throws Exception {
        super(locale, "");
    }

    @Override
    public void startup() {
        logger = MaryUtils.getLogger(this.getClass());
        if (state != MODULE_OFFLINE)
            throw new RuntimeException("Module should be offline before startup!");
        logger.info(String.format("Module started (%s -> %s, locale %s)", getInputType(), getOutputType(), getLocale()));
        state = MODULE_RUNNING;
    }

    @Override
    public MaryData process(MaryData input) {
        Document document = input.getDocument();

        NodeIterator tokenIterator = MaryDomUtils.createNodeIterator(document, MaryXML.TOKEN);
        Element token;
        while ((token = (Element) tokenIterator.nextNode()) != null) {
            String text = token.getTextContent().trim();
            String pos;
            if (text.equals(","))
                pos = "$,";
            else if (text.matches("[.?!;:]"))
                pos = "$.";
            else if (text.matches("^[A-Z].*"))
                pos = "NN";
            else
                pos = "UNKN";
            token.setAttribute("pos", pos);
            logger.info("PARTOFSPEECH: " + pos);
        }

        MaryData output = new MaryData(getOutputType(), input.getLocale());
        output.setDocument(document);
        return output;
    }
}
