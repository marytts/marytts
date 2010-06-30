package marytts.modules.acoustic;

import java.util.List;

import marytts.unitselection.select.Target;

import org.w3c.dom.Element;

/**
 * Model which currently predicts only a flat 400 ms duration for each boundary Element
 * <p>
 * Could be replaced by a PauseTree or something else, but that would require a CARTModel instead of this.
 * 
 * @author steiner
 * 
 */
public class BoundaryModel extends Model {
    public BoundaryModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, targetElementListName, null);
    }

    @Override
    public void applyTo(List<Element> elements) {
        for (Element element : elements) {
            if (!element.hasAttribute(targetAttributeName)) {
                element.setAttribute(targetAttributeName, "400");
            }
        }
    }

    /**
     * For boundaries, this does nothing;
     */
    @Override
    protected float evaluate(Target target) {
        return Float.NaN;
    }

    /**
     * For boundaries, this does nothing;
     */
    @Override
    public void loadDataFile() {
        return;
    }
}
