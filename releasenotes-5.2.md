This is expected to be the last milestone release in the 5.x generation of MaryTTS.

New Features
============

Improvements
------------

* Support for Luxembourgish

Development news
----------------

* lorem ipsum

Fixed Issues/Bugs
------------------
* [375](https://github.com/marytts/marytts/issues/375): add Groovy script to generate component descriptor XML and fix POM template
* [#369](https://github.com/marytts/marytts/issues/369): handle exceptions on missing or malformed userdict entries
* [#365](https://github.com/marytts/marytts/issues/365): upgrade groovy-maven (formerly gmaven) plugin to solve noClassDefFoundError when running MaryTTS server
* [#362](https://github.com/marytts/marytts/issues/362): add support for Groovy
* [#359](https://github.com/marytts/marytts/issues/359): don't append an /6/ to the previous syllable if that syllable is not adjacent
* [#354](https://github.com/marytts/marytts/issues/354): move custom jtok resources into jtok-user
* [#353](https://github.com/marytts/marytts/issues/353): replace inline code from Jama with external dependency to fix swapped inlined code
* [#351](https://github.com/marytts/marytts/issues/351): update of maven plugins used for the website and build plugins
* [#342](https://github.com/marytts/marytts/issues/342): workaround for NullPointerException in syllables that violate sonority constraints
* [#341](https://github.com/marytts/marytts/issues/341): temporarily handle digit suffix stress notation from legacy LTS CARTs until these are rebuilt
* [#333](https://github.com/marytts/marytts/issues/333): remove subscription/post links to archived mary-dev mailing list
* [#330](https://github.com/marytts/marytts/issues/330): show port number when starting MaryTTS server
* [#322](https://github.com/marytts/marytts/issues/322): drop transitional punctuation POS tag logic
* [#320](https://github.com/marytts/marytts/issues/320): move outdated example code from runtime assembly into doc directory
* [#314](https://github.com/marytts/marytts/issues/314): not processing null results from phonemise methods
* [#309](https://github.com/marytts/marytts/issues/309): try to process tokens if they contain word characters, even when they are tagged as punctuation
* [#237](https://github.com/marytts/marytts/issues/237): fix for incorrect linear interpolation in MathUtils.interpolateNonZeroValues
* [#213](https://github.com/marytts/marytts/issues/213): fix for rate adjustment 

