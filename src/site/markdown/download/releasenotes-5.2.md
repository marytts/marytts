This is expected to be the last milestone release in the 5.x generation of MaryTTS.

New Features
============

Improvements
------------

* Support for Luxembourgish
* Completely rewritten text preprocessing module for English (removing reliance on FreeTTS)
* Better support for Java 8

Development news
----------------

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
* [#217](https://github.com/marytts/marytts/pull/217): incrementality changes

Fixed Issues/Bugs
-----------------
* [#452](https://github.com/marytts/marytts/issues/452): Disable assertions in user startup scripts
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
