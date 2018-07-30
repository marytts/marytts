package marytts.language.en

import marytts.LocalMaryInterface
import marytts.datatypes.MaryDataType
import marytts.features.FeatureRegistry
import marytts.util.FeatureUtils
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

class MaryInterfaceEnIT {

    LocalMaryInterface mary

    @BeforeTest
    void setUp() {
        mary = new LocalMaryInterface()
    }

    @Test
    void convertTextToAcoustparams() {
        mary.outputType = MaryDataType.ACOUSTPARAMS.name()
        assert mary.generateXML('Hello world')
    }

    @Test
    void convertTextToTargetfeatures() {
        mary.outputType = MaryDataType.TARGETFEATURES
        assert mary.generateText('Hello world')
    }

    @Test
    void convertTextToPhonemes() {
        mary.outputType = MaryDataType.PHONEMES.name()
        assert mary.generateXML('Applejuice')
    }

    @Test
    void canSelectTargetfeatures() {
        mary.outputType = MaryDataType.TARGETFEATURES.name()
        def featureNames = 'phone stressed'
        mary.outputTypeParams = featureNames
        def tf = mary.generateText('Hello world')
        def expected = FeatureRegistry.getTargetFeatureComputer(mary.locale, featureNames).featureDefinition
        def actual = FeatureUtils.readFeatureDefinition(tf)
        assert actual == expected
    }
}
