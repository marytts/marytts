package marytts.data.item.utils;

import marytts.data.item.Item;

/**
 *  Util item which contains only a string value. The actual semantic of the label is ported in the
 *  sequence name. It is primarly use to interact with other tools without having to parse the
 *  results.
 *
 *  @author <a href="mailto:slemaguer@tcd.ie">Sébastien Le Maguer</a>
 */
public class StringItem extends Item
{
    /** The value of the item */
    private String value;

    /**
     *  The constructor which takes the value as a parameter
     *
     *  @param value the value
     */
    public StringItem(String value) {
        super();
        this.value = value;
    }

    /**
     *  Get the value of the item
     *
     *  @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     *  Set the value of the item
     *
     *  @param value the new value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     *  Override the default toString method
     *
     *  @return getValue()
     */
    @Override
    public String toString() {
        return getValue();
    }

    /**
     *  Equallity check
     *
     *  @param obj the object to compare
     *  @return true if obj is a StringItem object and if his value is the same than the one from
     *  the current object;
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof StringItem)) {
            return false;
        }

        return getValue().equals(((StringItem) obj).getValue());
    }
}
