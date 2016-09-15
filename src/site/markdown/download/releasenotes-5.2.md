This is expected to be the last milestone release in the 5.x generation of MaryTTS.

New Features
============

Improvements
------------

* Support for Luxembourgish
* Completely rewritten text preprocessing module for English (removing reliance on FreeTTS)
* Better support for Java 8
* Some migration towards building with Gradle

Voicebuilding
-------------

* All unit-selection voices have been rebuilt using the new [Gradle plugin](https://github.com/marytts/gradle-marytts-voicebuilding-plugin)
* All HSMM voices are now hosted on Bintray and can be resolved as dependencies
* Voices with open data now have open-source voicebuilding repositories hosted on GitHub, which include downloadable distribution packages

Development news
----------------

* [#533](https://github.com/marytts/marytts/pull/533): support system preferredmodule
* [#525](https://github.com/marytts/marytts/pull/525): Update dependencies and maven plugins
* [#523](https://github.com/marytts/marytts/pull/523): solving quotes phonetisation problem
* [#506](https://github.com/marytts/marytts/pull/506): Duplicate subexpressions in `WeightedCodebookMfccMapper.java`
* [#499](https://github.com/marytts/marytts/pull/499): Upgrade Apache Commons Collections to v3.2.2
* [#493](https://github.com/marytts/marytts/pull/493): Drop assembly plugin
* [#425](https://github.com/marytts/marytts/pull/425): various fixes for C++ client source code
* [#399](https://github.com/marytts/marytts/pull/399): Component installer License download re-code following [#395](https://github.com/marytts/marytts/issues/395)
* [#393](https://github.com/marytts/marytts/issues/393): `package.html` files converted into `package-info.java` files
* [#362](https://github.com/marytts/marytts/issues/362): add support for Groovy
* [#353](https://github.com/marytts/marytts/issues/353): Swap inlined third-party code with dependencies
* [#351](https://github.com/marytts/marytts/issues/351): update of maven plugins used for the website and build plugins
* [#333](https://github.com/marytts/marytts/issues/333): remove subscription/post links to archived mary-dev mailing list
* [#330](https://github.com/marytts/marytts/issues/330): show port number when starting MaryTTS server
* [#320](https://github.com/marytts/marytts/issues/320): move outdated example code from runtime assembly into doc directory
* [#309](https://github.com/marytts/marytts/issues/309): try to process tokens if they contain word characters, even when they are tagged as punctuation
* [#228](https://github.com/marytts/marytts/issues/228): fix on drop FreeTTS dependencies
* [#227](https://github.com/marytts/marytts/pull/227): Enhanced OutputStreams for Mary Client
* [#217](https://github.com/marytts/marytts/pull/217): incrementality changes

Fixed Issues/Bugs
-----------------

* [#593](https://github.com/marytts/marytts/pull/593): Don't split up multiple punctuation marks in tokenization
* [#570](https://github.com/marytts/marytts/issues/570): Praat TextGrid output is invalid with boundaries (times are not monotonic)
* [#564](https://github.com/marytts/marytts/pull/564): add missing TOKENS examples
* [#555](https://github.com/marytts/marytts/issues/555): HMMModel generates malformed XML duration attributes
* [#531](https://github.com/marytts/marytts/issues/531): java.awt.HeadlessException in (Half)PhoneLabelFeatureAligner
* [#516](https://github.com/marytts/marytts/issues/516): Single words conduct to have a wrong POS which leads to a crash of the target feature module
* [#515](https://github.com/marytts/marytts/pull/515): preprocessing contraction and double quotes correction
* [#503](https://github.com/marytts/marytts/issues/503): `halfphoneUnitFeatureDefinition_ac.txt` does not have any continuous features even though `halfphoneFeatures_ac.mry` does
* [#480](https://github.com/marytts/marytts/issues/480): IBAN code &rarr; stacktrace
* [#469](https://github.com/marytts/marytts/issues/469): APML is broken
* [#468](https://github.com/marytts/marytts/issues/468): SABLE is broken
* [#467](https://github.com/marytts/marytts/issues/467): SIMPLEPHONEMES is broken
* [#465](https://github.com/marytts/marytts/issues/465): enable acoustic features by default
* [#460](https://github.com/marytts/marytts/issues/460): Tokens mistakenly POS-tagged as punctuation cause wrong boundary insertion
* [#458](https://github.com/marytts/marytts/issues/458): VoiceCompiler generates invalid package name from db.voicename property
* [#452](https://github.com/marytts/marytts/issues/452): Disable assertions in user startup scripts
* [#448](https://github.com/marytts/marytts/issues/448): unit selection: final boundary durations synthesized 50% shorter than requested
* [#428](https://github.com/marytts/marytts/issues/428): error in marytts cart DecisionNode
* [#421](https://github.com/marytts/marytts/pull/421): Force English locale for parsing date when English language is used
* [#409](https://github.com/marytts/marytts/pull/409): ensure that ICU4J's resource is read with the correct encoding, regardless of environment
* [#398](https://github.com/marytts/marytts/pull/398): Use https URLs whenever possible
* [#395](https://github.com/marytts/marytts/issues/395): component installer hangs if licenses cannot be downloaded
* [#375](https://github.com/marytts/marytts/issues/375): add Groovy script to generate component descriptor XML and fix POM template
* [#369](https://github.com/marytts/marytts/issues/369): handle exceptions on missing or malformed userdict entries
* [#365](https://github.com/marytts/marytts/issues/365): upgrade groovy-maven (formerly gmaven) plugin to solve noClassDefFoundError when running MaryTTS server
* [#359](https://github.com/marytts/marytts/issues/359): don't append an /6/ to the previous syllable if that syllable is not adjacent
* [#354](https://github.com/marytts/marytts/issues/354): move custom jtok resources into jtok-user
* [#352](https://github.com/marytts/marytts/issues/352): javadoc fails with Java 8
* [#342](https://github.com/marytts/marytts/issues/342): workaround for NullPointerException in syllables that violate sonority constraints
* [#341](https://github.com/marytts/marytts/issues/341): temporarily handle digit suffix stress notation from legacy LTS CARTs until these are rebuilt
* [#322](https://github.com/marytts/marytts/issues/322): drop transitional punctuation POS tag logic
* [#314](https://github.com/marytts/marytts/issues/314): not processing null results from phonemise methods
* [#237](https://github.com/marytts/marytts/issues/237): fix for incorrect linear interpolation in MathUtils.interpolateNonZeroValues
* [#213](https://github.com/marytts/marytts/issues/213): fix for rate adjustment 
* [#206](https://github.com/marytts/marytts/issues/206): fix for LTSTrainerTest failure on unexpected file.encoding
* [#204](https://github.com/marytts/marytts/issues/204): fix for Locale null breaking MaryServer
* [#202](https://github.com/marytts/marytts/issues/202): URISyntaxException avoids WikipediaMarkupCleanerTest failing if workspace contains space
* [#198](https://github.com/marytts/marytts/issues/198): fix for closing fileOutputStream after audio save
* [#185](https://github.com/marytts/marytts/issues/185): fix for EnvironmentTest failure on Java 8
