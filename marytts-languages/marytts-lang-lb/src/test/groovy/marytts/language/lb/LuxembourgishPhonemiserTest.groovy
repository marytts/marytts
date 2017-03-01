package marytts.language.lb

import groovy.xml.*
import org.custommonkey.xmlunit.*
import org.testng.Assert
import org.testng.annotations.*

import marytts.datatypes.MaryData
import marytts.util.dom.DomUtils

/**
 * @author ingmar
 */
class LuxembourgishPhonemiserTest {

    def phonemiser

    @BeforeSuite
    void setUp() {
        phonemiser = new LuxembourgishPhonemiser()
    }
}
