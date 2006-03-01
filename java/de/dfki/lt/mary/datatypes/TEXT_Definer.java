/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package de.dfki.lt.mary.datatypes;

import de.dfki.lt.mary.MaryDataType;

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
