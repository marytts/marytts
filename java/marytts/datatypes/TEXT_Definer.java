/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.datatypes;


/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class TEXT_Definer extends MaryDataType {
    static {
        define("TEXT", null, true, false, PLAIN_TEXT, null, null,
                         null);

    }
}
