MaryTTS
=====================

[Unreleased]
------------

### Improvements

* Build with Gradle v6.3
* Improve publishing

### Fixed Issues/Bugs

* Migrate CI/CD from Travis to GitHub Actions
* Add compability with Java 11 and 12
* Lock down Java compatibility to 1.8
* Reduce non-API dependency leakage (particularly `groovy-all`)

See [all changes since v5.2]

[v5.2] (2016-09-15)
-------------------
This is expected to be the last milestone release in the 5.x generation of MaryTTS.

### Improvements

* Support for Luxembourgish
* Completely rewritten text preprocessing module for English (removing reliance on FreeTTS)
* Better support for Java 8
* Some migration towards building with Gradle

#### Voicebuilding


* All unit-selection voices have been rebuilt using the new [Gradle plugin](https://github.com/marytts/gradle-marytts-voicebuilding-plugin)
* All HSMM voices are now hosted on Bintray and can be resolved as dependencies
* Voices with open data now have open-source voicebuilding repositories hosted on GitHub, which include downloadable distribution packages

### Development news

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

### Fixed Issues/Bugs

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

See [all changes since v5.1.2]

[v5.1.2] (2015-01-23)
-------------------

Another milestone release with several improvements and fixes.

### Improvements

* For French, numbers in the input text were silently ignored; this has been [improved](https://github.com/marytts/marytts/issues/269) using a *spellout formatter* from [ICU4J](http://icu-project.org/). This fix can (and will soon) also be applied to other languages that are missing a text Preprocess module. 
* A new rule-based Syllabfier class has been implemented.
* All releases are now [hosted on GitHub](https://github.com/marytts/marytts/releases) again; references to the Downloads on Bitbucket have been updated accordingly.

### Development news

* Targets are cached so these do not get recreated, and can be re-used later.
* Words are now added to wordlist in batches for new language components.
* A toString() helper method was added to MaryData class to help with debugging. 

### Fixed Issues/Bugs

* [#268](https://github.com/marytts/marytts/issues/268): getAllophone() no longer silently returns `null` on invalid input.
* [#267](https://github.com/marytts/marytts/issues/267): `REALISED_DURATIONS` and `REALISED_ACOUSTPARAMS` are no longer (mistakenly) available as input types.
* [#266](https://github.com/marytts/marytts/issues/266): Syllabification no longer ignores any provided stress diacritics.
* [#258](https://github.com/marytts/marytts/issues/258): Byte overflow in TargetFeatureComputer -- issue with limit of array.
* [#218](https://github.com/marytts/marytts/issues/218): a voice would not be built correctly following the **groupId** change in v.5.1.1.
* [#154](https://github.com/marytts/marytts/issues/154): trailing whitespace in config files could prevent correct parsing.

See [all changes since v5.1.1]

[v5.1.1] (2014-10-05)
-------------------

A milestone release that stabilizes changes in build and hosting infrastructure and fixes several issues.

### Language updates

* Telugu and Turkish: two voices were added which had not been rebuilt since v4.0.0.
* added `TOKENS` example text for Italian, Turkish, British English, Telugu, and Russian.
* Voice components are sorted by locale and name in `download/marytts-components.xml`.

### Documentation updates

* Added to the documentation section on the mary site is a page on [MaryTTS's history](http://mary.dfki.de/documentation/history.html).
* Also added a point on MP3 support to the [FAQ](http://mary.dfki.de/faq.html).
* Minor tweaks to navigation menu, earlier version release notes, download links, etc.

### Development news

##### New Infrastructure:

* Continuous Integration is now provided by [Travis CI](https://travis-ci.org/marytts/marytts).
* The web server running at http://mary.dfki.de has been replaced with a new machine, which hosts the latest stable website content and runs the latest stable build as an online demo.
* The latest release artifacts are hosted at [Bintray](https://bintray.com/marytts) and indexed in [jCenter](https://bintray.com/bintray/jcenter).
* The latest development (SNAPSHOT) artifacts are hosted at [OJO](https://oss.jfrog.org/).
* A website built from the latest development (SNAPSHOT) version is hosted at http://marytts.github.io/.

##### Build environment:

* All supported languages have been moved into a container module `marytts-languages`.
* Several maven plugins were updated to the latest versions.
* Building MaryTTS now requires Maven 3.0.2 or later.

### Fixed Issues/Bugs

* added missing voice resources that had been present in v5.0 before hosting had switched servers, as well as fixing some missing license files.
* [#207](https://github.com/marytts/marytts/issues/207): Deploying to Bintray/jCenter required changing the **groupId** from `marytts` to `de.dfki.mary`.
* [#206](https://github.com/marytts/marytts/issues/206): LTSTrainerTest would fail if UTF-8 encoding was not specified
* [#204](https://github.com/marytts/marytts/issues/204): A locale set to `null` no longer breaks the Mary server   
* [#202](https://github.com/marytts/marytts/issues/202): WikipediaMarkupCleanerTest failed if workspace contains space
* [#185](https://github.com/marytts/marytts/issues/185): EnviromentTest no longer fails with Java 8

See [all changes since v5.1]

[v5.1] (2014-07-16)
-------------------

Following significant restructuring introduced in v5.0, this is a milestone release to stabilize numerous new features and fixes over the past two years.

Apart from numerous fixed issues, there are several notable improvements in this version.

### French language support

Thanks to Sathish Pammi's work at ENST and UPMC in Paris, there is support for French TTS.
This supersedes an [earlier, experimental implementation](http://mary.opendfki.de/browser/branches/fr-branch), which relied on external, native resources with an incompatible license.

Moreover, thanks to the great work and generosity of the speakers, there are a number of French voices built from three open-source corpora:

* https://github.com/marytts/enst-camille-data
* https://github.com/marytts/upmc-jessica-data
* https://github.com/marytts/upmc-pierre-data

### Improved Italian language support

Thanks to Fabio Tesser and Giulio Paci at ISTC in Padova, numerous improvements were made to Italian TTS.

This also includes the resurrection of the HTK Labeler used for voicebuilding as an alternative to the EHMM Labeler, which may produce better results under certain conditions.

### Documentation

Although much of the documentation has yet to be updated, the [MaryTTS website](http://mary.dfki.de) itself is now built directly from the MaryTTS source code (using the [Maven Site Plugin](http://maven.apache.org/plugins/maven-site-plugin/)).
This unifies the fossilized legacy content with new documentation, including the GitHub-hosted [wiki](https://github.com/marytts/marytts/wiki), and makes it easy to manage.

### Easier integration

Finally, thanks to cloud hosting at [Bintray](https://bintray.com/marytts), it is now possible to integrate MaryTTS directly into other applications without the requirement to install it first locally, simply by adding the following repository block to your `pom.xml`:

    <repository>
	  <id>bintray</id>
	  <url>https://dl.bintray.com/marytts/marytts</url>
    </repository>

### Development news

There has been some fluctuation in the core development team since the release of v5.0;
departures and other responsibilities have resulted in less activity than we would have liked.

Fortunately, we do finally have several new projects for MaryTTS, and several exciting features waiting the wings, ready to be rebased on a stable version (this one), including major overhauls of the voicebuilding toolchain and the new language support, and a new web-based installer.
Expect these to land in the near future.

See [all changes since v5.0]

[v5.0] (2012-06-14)
-------------------

MARY TTS 5.0 is the first release from the thoroughly restructured code base.

MARY TTS 5.0 is better supported by automated tests than any previous version of MARY TTS, but it may well be that in practical use some hiccups will appear that have not surfaced in testing.
Therefore cautious users are advised to treat this as a beta release.

### Simpler installation

Installing MARY TTS is now performed by simply unpacking the `zip` archive at the target location.
No clicking through installer pages anymore.
In particular, it is now trivial to install MARY TTS on a server without a GUI connection.

The component installer, `bin/marytts-component-installer.sh`, still uses a GUI;
see issue [#43](https://github.com/marytts/marytts/issues/43) for a workaround.

### Simplified use of MARY TTS in your own projects

It is now possible to use MARY TTS, with HMM-based voices at least, simply by placing the right files into the classpath.
For example, to use US English voice `cmu-slt-hsmm` in your own code, add the following jar files to your classpath:

    marytts-server-5.0-jar-with-dependencies.jar
    marytts-lang-en-5.0.jar
    voice-cmu-slt-hsmm-5.0.jar

Instead of `marytts-server-5.0-jar-with-dependencies.jar` you can also include the individual dependencies, which can be automated using maven;
the source code on github includes examples for doing this in the `user-examples` folder.

### New MaryInterface API

Using MARY TTS programmatically gets a lot simpler with MARY TTS 5.0 through the new MaryInterface API.
The same API can be used to access the TTS components running within the same java process or as a separate client-server setup.
For details, see [MaryInterface](https://github.com/marytts/marytts/wiki/MaryInterface).

### Emotion Markup Language support

MARY TTS 5.0 includes an implementation of W3C's [Emotion Markup Language](http://www.w3.org/TR/emotionml/) as a means of requesting expressive synthetic speech.
The result of course depends on the expressive capabilities of the selected synthesis voice;
try out the EMOTIONML example with the German `dfki-pavoque-styles` voice on the [demo page](http://mary.dfki.de:59125/).

### Modularized code base

The MARY TTS source code has been modularized for version 5.0.

The modular structure also makes it easier to see which components belong to a given language:
sub-projects such as `marytts-lang-de`, `marytts-lang-en` etc. contain all code and data that is language-specific.
The updated [New Language Support](https://github.com/marytts/marytts/wiki/New-Language-Support) documentation describes how to create a new language sub-project and integrate it into the system.

### Distributed hosting of installable voices

The maintenance of the list of installable voices has been decentralized:
if you build a voice and wish to make it available to others, you can host it somewhere on the net, e.g. on Google Drive, Dropbox or similar.
For details, see [Publishing a MARY TTS Voice](https://github.com/marytts/marytts/wiki/Publishing-a-MARY-TTS-Voice).

### New structures for improved code quality

MARY TTS 5.0 mostly results from the aim to establish sound software engineering practices in MARY TTS development.
Aspects of this include:

* the build environment was changed to maven, allowing us to write proper unit and integration tests for the code, and run them at every build;
* the code base was moved to github, allowing us to work as a distributed team, to develop features in whichever constellations arise, and to merge them into a stable master repository when ready;
* a Continuous Integration service, kindly provided by [BuildHive](https://buildhive.cloudbees.com/job/marytts/job/marytts/), automatically checks whether the code, and even the latest GitHub pull requests, build OK including all tests passing.

See [all changes since v4.3.1]

[v4.3.1] (2011-11-30)
-------------------

This is a minor release, whose main purpose it is to enable Italian TTS (see below).
It also includes small but useful improvements enabling the use of MARY TTS with screen readers on Linux (see below).

### New language: Italian

Thanks to the great work by Fabio Tesser and co-workers at ISTC Padova, Italy, we can now make available support for Italian in MARY TTS.
To install the Italian voice, first download and install MARY TTS from the link above, then run the MARY Component installer, click "Update" to get the list of available languages and voices, and select the Italian Language and the istc-lucia-hsmm voice.

### Improvements

Bill Cox has kindly provided a patch resulting in substantial reduction of time-to-audio for HMM-based voices.
The key idea is to use the vocoder in streaming mode rather than produce all audio before sending it onwards.
Since only the socket server, but not the http server, supports streaming audio, you need to use the socket server or a custom integration mechanism to benefit from this improvement.

Critically, however, this has improved the responsiveness of MARY TTS sufficiently to allow using MARY TTS with screen readers on Linux!

### New voices

Bill also provided two male US English voices built from Arctic data sets, cmu-rms-hsmm and cmu-bdl-hsmm.
According to him, specifically the rms voice is quite intelligible at high speed (with the speedup done in a post-processing step currently).

See [all changes since v4.3.0]

[v4.3.0] (2010-12-20)
-------------------

This is a feature release, adding new features while maintaining compatibility with existing 4.x voices.

This release marks the final results of work on MARY TTS in the [PAVOQUE project](http://mary.dfki.de/pavoque/index.html), in which we experimented with different technologies for adding expressivity to unit selection synthesis.
The release makes available those project results that may be of interest to a wider audience.

### New features for expressive unit selection synthesis

* selecting style from a multi-style database using a symbolic style feature;
* imposing target prosody using FD-PSOLA signal modification.

Style can be selected using RAWMARYXML's `<prosody style="...">` markup (see new expressive voice, below).

Prosody modification is available for all unit selection voices, including older ones;
to activate it, click the checkbox "Apply prosody modification" in the [web interface](http://mary.dfki.de:59125/).
This feature should be considered experimental, and the quality depends on many factors, including the accuracy of the pitchmarks used for building the unit selection voice.
While this feature is likely to lead to reduced quality, it enables research on expressive prosody with unit selection voices.

For more information on the MaryXML `<prosody>` markup which can now be applied to all types of MARY voices, see [ProsodySpecificationSupport](http://mary.opendfki.de/wiki/ProsodySpecificationSupport).

### New expressive voice

* we release the multi-style expressive German voice 'dfki-pavoque-styles' (660 MB) built from the full PAVOQUE corpus;
  see [Steiner et al. (2010)](http://www.dfki.de/web/research/publications?pubid=4877) for a description of this corpus.
  The different styles can be requested using RAWMARYXML `<prosody style="A_STYLE">...</prosody>`, where `A_STYLE` is one of `happy`, `angry`, `sad`, and `poker`.

### New language: Russian

* Nickolay Shmyrev has kindly made available language support for Russian, as well as the Russian male unit selection voice voxforge-ru-nsh.
Thanks Nickolay!

### Bugfixes

* This release also includes a number of bugfixes, see https://mary.opendfki.de/trac/query?milestone=4.3&group=status&order=priority

See [all changes since v4.2.0]

[v4.2.0] (2010-12-08)
-------------------

This is a feature release, adding new features while maintaining compatibility with existing 4.x voices.

This release marks the final results of work on MARY TTS in the [SEMAINE project](https://semaine-db.eu/), where our main focus for TTS has been on building expressive British English voices with listener vocalization capabilities.

### New features for synthesis of expressive vocalizations

* Improved support for expressive vocalizations in the British English unit selection voices dfki-poppy, dfki-prudence, dfki-spike and dfki-obadiah.
  Signal modification is now used to combine intonation contours and segmental forms, as described in [Pammi et al. (2010)](http://www.dfki.de/lt/publication_show.php?id=4886).
* Added vocalization support to the HMM-based versions of these voices:
  dfki-poppy-hsmm, dfki-prudence-hsmm, dfki-spike-hsmm and dfki-obadiah-hsmm.

For details on the new MaryXML `<vocalization>` tag and usage examples, see [VocalizationSynthesis](http://mary.opendfki.de/wiki/VocalizationSynthesis).

### New voice

* For German, we created bits1-hsmm, a female HMM-based voice built from recordings provided by [BITS](http://www.phonetik.uni-muenchen.de/Forschung/BITS/index.html).

### Bugfixes

* This release also includes a number of bugfixes, see https://mary.opendfki.de/trac/query?milestone=4.2&group=status&order=priority

See [all changes since v4.1.1]

[v4.1.1] (2010-09-09)
-------------------

This is a bugfix release.

### Bugfixes

* Fixed a critical bug for Windows users who got an "OutOfMemoryError" when trying to start the server with unit selection voices
    * [#314 Memory mapping causes OutOfMemoryError if -Xmx is set too high](http://mary.opendfki.de/ticket/314)
* Other small bug fixes -- see https://mary.opendfki.de/trac/query?milestone=4.1.1&group=status&order=priority

### New voices

* Added Telugu HMM-based voice "cmu-nk-hsmm".

### More info

For more background information, please also refer to the [Release notes of MARY 4.1.0](http://mary.dfki.de/download/releasenotes-4.1.0.html).

See [all changes since v4.1.0]

[v4.1.0] (2010-09-01)
-------------------

This is a feature release, adding new features while maintaining compatibility with existing 4.0 voices.

### New features for expressive TTS

* **Prosody control for HMM-based voices.** Using the SSML [`<prosody>` tag](http://www.w3.org/TR/speech-synthesis11/#edef_prosody), it is now possible to control the intonation generated for HMM-based voices from markup.
  Of particular interest is the "contour" attribute, which allows you to change the shape of the intonation curve.
  For details and examples, see ProsodySpecificationSupport.
* **Expressive vocalizations** in certain unit selection voices.
  MaryXML now supports a new `<vocalization>` tag, with which you can request the generation of non-verbal or para-verbal expressions as they are often produced by the listener in a conversation, such as "yeah", "m-hm", laughter, sigh, etc.
  For details and examples, see [VocalizationSynthesis](http://mary.opendfki.de/wiki/VocalizationSynthesis).

### New voices

* For British English, we release HMM-based versions of the four voices Poppy, Spike, Obadiah and Prudence.
* Updated versions of many of the previously existing voices, with improved quality.

### Bugfixes and other improvements

* Faster startup times.
  MARY 4.1.0 starts nearly twice as fast and needs about 30% less physical memory compared to MARY 4.0.0, due to the use of memory mapping for unit selection voices.
* Quality of German and English components improved.
  A number of bugs were fixed which had degraded the quality of the synthesis results in MARY 4.0.0:
    * [#297 German words spelled out when they shouldn't](http://mary.opendfki.de/ticket/297)
    * [#273 Several problems with abbreviations in German](http://mary.opendfki.de/ticket/273)
    * [#308 German compound analysis over-active](http://mary.opendfki.de/ticket/308)
    * [#287 Abbreviations in German trigger major boundary insertion](http://mary.opendfki.de/ticket/287)
    * [#304 English support for apostrophies is broken](http://mary.opendfki.de/ticket/304)
* Multiple bugs were also fixed under the surface.
  For a full list, see https://mary.opendfki.de/trac/query?milestone=4.x&group=status&order=priority
  
### More info

For more background information, please also refer to the [Release notes of MARY 4.0.0](http://mary.dfki.de/download/releasenotes-4.0.0.html).

See [all changes since v4.0.0]

[v4.0.0] (2009-12-18)
-------------------

### What's new

This is the first stable release of the new 4.0 code.
MARY 4.0 is a major cleanup over previous versions of MARY TTS.

### Changes since MARY 3.6

* 100% Pure Java.
  All native libraries have been removed from the system.
  MARY should now run on any platform that has Java 1.5 or newer.
* Fully open source.
  All code is now open source under the [LGPL](http://www.gnu.org/licenses/lgpl-3.0-standalone.html), including German TTS.
  Voices are distributed under Creative Commons or BSD licenses.
* Many languages and voices. In addition to several high-quality German and US English voices, this release adds four expressive British English voices built for the [SEMAINE project](http://www.semaine-project.eu/), a Turkish and a Telugu voice.
  Also, we have made it easy to add more languages and voices in the future.
* New component installer.
  New languages and voices can be installed using a new component installer tool.
  If new languages and/or voices are made available for download, they can be installed without having to re-install the full system.

MARY now comes with a toolkit for people who want to add support for new languages or build their own voices.
There is detailed documentation for

* [creating initial support for a new language](http://mary.opendfki.de/wiki/NewLanguageSupport);
* [recording and building a unit selection voice](http://mary.opendfki.de/wiki/VoiceImportToolsTutorial); and
* [creating an HMM-based voice](https://mary.opendfki.de/trac/wiki/HMMVoiceCreation) from the same recordings.

### Changes since MARY 4.0 beta

* New British English expressive voices.
  We are releasing the voices of four expressive characters built for the [SEMAINE project](https://semaine-db.eu/).
* Improved Installer.
  You can now install downloaded components without requiring an internet connection at install time.
  The Installer handles updates of language and voice components correctly.
* Improved German pronounciation.
  Several thousand words were added to the pronounciation lexicon, bringing the total number of German transcriptions to over 26,000.
  Some bugs were fixed in the transcription of unknown words, making the synthesis of German more reliable.
* Added MBROLA voices.
  There are still many people who use MARY with MBROLA diphone voices because of the control over prosody that they provide.
  Therefore we have added MBROLA voices to this release.
  They can be installed like the other voices through the MARY component installer.
* Voice creation tools were simplified.
  The handling of external programs needed, in particular, for the creation of HMM-based voices was simplified.
  We provide a script now that you can use to find or, if necessary, download and compile third-party software required for training HMM-based voices.

More details on individual issues addressed can be seen in the [list of tickets associated with this release](https://mary.opendfki.de/trac/query?milestone=4.0&group=status&order=priority) and in the [list of tickets associated with the 4.0 beta release](https://mary.opendfki.de/trac/query?milestone=4.0+beta&group=status&order=priority).

### Tested environments

The MARY client and server code was tested on:

* Mac OS X (Intel) 10.5.8 with java 1.6.0_07
* Ubuntu Linux 8.10 and 9.04 with sun java 1.5, 1.6, and openjdk-6
* Windows XP and Vista

The web interface at http://localhost:59125 should work with any recent browser that supports AJAX.
We have obtained best results with:

* Firefox 3.5 with built-in audio support
* Firefox 3.0, Internet Explorer 6 and 7, Safari 4, with Quicktime plugin

### Known issues

* On Ubuntu Linux with sun java 1.5 and 1.6, we observed a problem with audio playback in the MARY client:
  the final section is cut off.
  With openjdk-6, it works ok.
* On Ubuntu Linux 8.10 with the builtin Firefox 3.0, the audio plugin doesn't work properly.
* Web interface in the browser Chrome suboptimal:
  Chrome does not seem to handle the HTML 5 `<audio>` tag correctly.
  It claims it can handle it, but then doesn't play audio.
  Click on "Save audio file" to get the raw audio data.

### Bug reports

Thanks to user feedback, we have fixed a number of bugs found in the beta release.
We are therefore confident that the system is ready for production use.
Nevertheless it is likely that new bugs will appear in new circumstances.

If you think you have identified a bug, proceed as follows:

1. Check in the [list of known bugs](https://mary.opendfki.de/trac/query?milestone=4.0&group=status&order=priority) whether the bug has been reported already;
2. If you cannot find it, prepare your request to the mailing list.
   Try to find out exactly:
    * what to do to reproduce the error;
    * expected behaviour;
3. Then send an informative email as described in the [FAQ](http://mary.opendfki.de/wiki/FrequentlyAskedQuestions#bugreport).

### Contributions

People interested in adding support for a language can get in touch, e.g. via the [MARY developers mailing list](http://www.dfki.de/mailman/listinfo/mary-dev).

See [all changes since v4.0-beta]

[v4.0-beta] (2009-09-24)
-------------------

### What's new

This is a first beta release of the new 4.0 code.
MARY 4.0 is a major cleanup over previous versions of MARY TTS.

* 100% Pure Java.
  All native libraries have been removed from the system.
  MARY should now run on any platform that has Java 1.5 or newer.
* Fully open source.
  All code is now open source under the [LGPL](http://www.gnu.org/licenses/lgpl-3.0-standalone.html), including German TTS.
  Voices are distributed under Creative Commons or BSD licenses.
* New languages.
  We have added Turkish and Telugu synthesis for now, and have made it easy to add more languages in the future.

MARY now comes with a toolkit for people who want to add support for new languages or build their own voices. There is detailed documentation for

* [creating initial support for a new language](http://mary.opendfki.de/wiki/NewLanguageSupport);
* [recording and building a unit selection voice](http://mary.opendfki.de/wiki/VoiceImportToolsTutorial); and
* [creating an HMM-based voice](https://mary.opendfki.de/trac/wiki/HMMVoiceCreation) from the same recordings.

More details on individual issues addressed can be seen in the [list of tickets associated with this release](https://mary.opendfki.de/trac/query?milestone=4.0+beta&group=status&order=priority).

### Tested environments

The MARY client and server code was tested on:

* Mac OS X (Intel) 10.5.8 with java 1.6.0_07
* Ubuntu Linux 8.10 and 9.04 with sun java 1.5, 1.6, and openjdk-6
* Windows XP and Vista

The web interface at http://localhost:59125 should work with any recent browser that supports AJAX.
We have obtained best results with:

* Firefox 3.5 with built-in audio support
* Firefox 3.0, Internet Explorer 6 and 7, Safari 4, with Quicktime plugin

### Known issues

* On Ubuntu Linux with sun java 1.5 and 1.6, we observed a problem with audio playback in the MARY client:
  the final section is cut off. With openjdk-6, it works ok.
* On Ubuntu Linux 8.10 with the builtin Firefox 3.0, the audio plugin doesn't work properly.
* Web interface in the browser Chrome suboptimal:
  Chrome does not seem to handle the HTML 5 `<audio>` tag correctly.
  It claims it can handle it, but then doesn't play audio.
  Click on "Save audio file" to get the raw audio data.

### Bug reports

This is beta software.
It is not yet ready for production use.
You are likely to find problems, and we would appreciate if you tell us about it.

If you think you have identified a bug, proceed as follows:

1. Check in the [list of known bugs](https://mary.opendfki.de/trac/query?milestone=4.0&group=status&order=priority) whether the bug has been reported already;
2. If you cannot find it, create a [new ticket](http://mary.opendfki.de/newticket) containing the following information:
    * operating system and java version used;
    * what to do to reproduce the error;
    * expected behaviour;
    * detailed log files, i.e. an excerpt of MARY TTS/log/server.log containing the error episode.
3. Discuss the problem on the [MARY user mailing list](http://www.dfki.de/mailman/listinfo/mary-users).

### Contributions

People interested in adding support for a language can get in touch, e.g. via the [MARY developers mailing list](http://www.dfki.de/mailman/listinfo/mary-dev).

See [all changes since v3.6.0]

[v3.6.0] (2008-05-21)
-------------------

This is a minor feature release, providing one important new feature and several bugfixes over previous MARY 3.5.0.

### New feature:

* New tool for creating your own HMM-based voices for MARY (see tutorial at http://mary.opendfki.de/wiki/HMMVoiceCreation)
  Feedback and questions about building voices in MARY are welcome via the mary-users mailing list (http://www.dfki.de/mailman/listinfo/mary-users).

### Bugfixes:

* Important for many will be that the "self-healing" capabilities of the MARY installation are now working again:
  if you install an English system but a German voice, the system detects a misconfiguration and offers to download and install the missing components, and will start up normally after that.
* the full list of bugs fixed can be found on http://mary.opendfki.de/query?milestone=3.6

See [all changes since v3.5.0]

[v3.5.0] (2007-12-07)
-------------------

This release provides substantial new features while maintaining the API compatible to previous versions.

### New features include:

* installer slimmed down to just over 30 MB, including some voices;
* separate voice installer tool, allowing you to download voices comfortably  and with an integrity verification prior to installation;
* new synthesis technology:
  Marcela Charfuelan ported the excellent HMM-based synthesis code from the HTS project (http://hts.sp.nitech.ac.jp/) to Java - several HMM-based voices already ship with MARY;
* audio effects:
  Oytun Turk implemented a range of audio effects that can be applied to the MARY voices, and can be controlled through the MARYGUIClient;
* voice creation toolkit:
  Sathish Chandra Pammi and Anna Hunecke have cleaned up and [documented the tool](http://mary.opendfki.de/wiki/VoiceImportToolsTutorial) we use for building new synthesis voices;
* voice recording tool "Redstart":
  Mat Wilson has programmed a very nice GUI for recording new voices;
* OGG Vorbis support:
  under Linux, the MARY server can now generate audio in OGG Vorbis format.

See [all changes since v3.1.0]

[v3.1.0] (2007-08-17)
-------------------

Ten months after the last stable release, a major milestone release is finally here:
MARY 3.1.0.

### Its main features are:

* state of the art unit selection (English and German);
* support for two more platforms:
  64 bit Linux and Mac OS X on Intel platforms;
* a voice creation toolkit (work in progress, see http://mary.opendfki.de/browser/tags/3.1.0/lib/modules/import/README for preliminary documentation if you want to try it out).

Thanks to those who have helped test the beta versions!
All the problems that we have become aware of should be fixed in this release.
For a reasonably complete list of issues addressed in this release, see http://mary.opendfki.de/milestone/3.1.0

Should you come across additional bugs, please post them to the mary-users mailing list (http://www.dfki.de/mailman/listinfo/mary-users).

### Some background info:

The unit selection code released here has performed better-than-average in this year's Blizzard Challenge (http://www.festvox.org/blizzard/), showing that the system can be considered state-of-the-art.
For details, see:
http://festvox.org/blizzard/bc2007/blizzard_2007/full_papers/blz3_007.pdf

The German voices have been created from the BITS corpora - for details, see:
http://www.dfki.de/dfkibib/publications/docs/schroeder_hunecke2007.pdf

See also the [full list of bugs fixed](https://mary.opendfki.de/trac/query?group=status&milestone=3.1.0)

See [all changes since v3.1.beta2]

[v3.1-beta2] (2007-07-15)
-------------------

### Highlights:

* Four German unit selection voices created from recordings in the BITS project;
* added acoustic models, which should also improve the English unit selection voices;
* added support for 64-bit linux and Intel Mac architectures.

### On our to-do list are still many things, including:

* a well-documented and easy-to-use voice creation toolkit;
* smaller voices by using suitable speech coding for the databases.
* For more details, see the development page: https://mary.opendfki.de/trac/query?group=status&milestone=3.1.0

### Known issues with this release:

* diphone voices are basically broken. Don't install this version if you want to use diphone voices.
* only very limited testing on various platforms. Please report any errors you may find!

See [all changes since v3.1-beta1]

[v3.1-beta1] (2006-12-13)
-------------------

First beta release of the new unit selection code.
Try the new slt-arctic, bdl-arctic and jmk-arctic voices!

For some details of what has been done and what still needs to be done for the stable release, see the [full list of issues](https://mary.opendfki.de/trac/query?milestone=3.1.0&group=status&order=priority) on the development portal.

See [all changes since v3.0.3]

[v3.0.3] (2006-10-27)
-------------------

Third bugfix release.
Most relevant changes:

* fixed standalone mode for running MARY ([ticket 80](http://mary.opendfki.de/ticket/80))
* improved control over prosody using ToBI tags (tickets [59](http://mary.opendfki.de/ticket/59), [60](http://mary.opendfki.de/ticket/60))
* Several minor improvements of German synthesis (tickets [44](http://mary.opendfki.de/ticket/44), [49](http://mary.opendfki.de/ticket/49), [57](http://mary.opendfki.de/ticket/57), [78](http://mary.opendfki.de/ticket/78))
* MBROLA to AUDIO conversion fixed (tickets [54](http://mary.opendfki.de/ticket/54), [55](http://mary.opendfki.de/ticket/55))

An update is recommended only if you encountered one of these bugs.

See also the [full list of bugs fixed](https://mary.opendfki.de/trac/query?group=status&milestone=3.0.3)

See [all changes since v3.0.2]

[v3.0.2] (2006-07-04)
-------------------

Second bugfix release.
A number of bugs have been fixed, including:

* WAV audio can now (really) be saved with proper audio headers ([ticket 38](http://mary.opendfki.de/ticket/38))
* several bugs related to failing validation of intermediate processing results (due to an outdated MaryXML Schema) were fixed (tickets [40](http://mary.opendfki.de/ticket/40), [41](http://mary.opendfki.de/ticket/41), [43](http://mary.opendfki.de/ticket/43))
* Synthesizing APML was improved (tickets [51](http://mary.opendfki.de/ticket/51), [52](http://mary.opendfki.de/ticket/52))
* Pronunciation for German compounds was improved ([ticket 36](http://mary.opendfki.de/ticket/36))

An update is recommended if you encountered one of these bugs.

See also the [full list of bugs fixed](https://mary.opendfki.de/trac/query?group=status&milestone=3.0.2)

See [all changes since v3.0.1]

[v3.0.1] (2006-03-07)
-------------------

First bugfix release.
A number of bugs have been fixed which have occurred after the initial release.
Most relevant:

* Audio can now be saved with proper audio headers ([ticket 32](http://mary.opendfki.de/ticket/32))
* Mary GUI client is now accessible via the keyboard ([ticket 3](http://mary.opendfki.de/ticket/3))

An update is recommended.

See also the [full list of bugs fixed](https://mary.opendfki.de/trac/query?group=status&milestone=3.0.1)

See [all changes since v3.0.0]


[v3.0.0] (2006-02-14)
-------------------

This is the first open-source release.
We have put in a lot of effort to test and debug it, but of course the system may have some teething problems.
Please help us improve the system by submitting [bug reports](http://mary.opendfki.de/newticket) on the [OpenMary development page](http://mary.opendfki.de/).

[Unreleased]: https://github.com/marytts/marytts/tree/master
[all changes since v5.2]: https://github.com/marytts/marytts/compare/v5.2...HEAD
[v5.2]: https://github.com/marytts/marytts/releases/tag/v5.2
[all changes since v5.1.2]: https://github.com/marytts/marytts/compare/v5.1.2...v5.2
[v5.1.2]: https://github.com/marytts/marytts/releases/tag/v5.1.2
[all changes since v5.1.1]: https://github.com/marytts/marytts/compare/v5.1.1...v5.1.2
[v5.1.1]: https://github.com/marytts/marytts/releases/tag/v5.1.1
[all changes since v5.1]: https://github.com/marytts/marytts/compare/v5.1...v5.1.1
[v5.1]: https://github.com/marytts/marytts/releases/tag/v5.1
[all changes since v5.0]: https://github.com/marytts/marytts/compare/v5.0...v5.1
[v5.0]: https://github.com/marytts/marytts/releases/tag/v5.0
[all changes since v4.3.1]: https://github.com/marytts/marytts/compare/v4.3.1...v5.0
[v4.3.1]: https://github.com/marytts/marytts/releases/tag/v4.3.1
[all changes since v4.3.0]: https://github.com/marytts/marytts/compare/v4.3.0...v4.3.1
[v4.3.0]: https://github.com/marytts/marytts/releases/tag/v4.3.0
[all changes since v4.2.0]: https://github.com/marytts/marytts/compare/v4.2.0...v4.3.0
[v4.2.0]: https://github.com/marytts/marytts/releases/tag/v4.2.0
[all changes since v4.1.1]: https://github.com/marytts/marytts/compare/v4.1.1...v4.2.0
[v4.1.1]: https://github.com/marytts/marytts/releases/tag/v4.1.1
[all changes since v4.1.0]: https://github.com/marytts/marytts/compare/v4.1.0...v4.1.1
[v4.1.0]: https://github.com/marytts/marytts/releases/tag/v4.1.0
[all changes since v4.0.0]: https://github.com/marytts/marytts/compare/v4.0.0...v4.1.0
[v4.0.0]: https://github.com/marytts/marytts/releases/tag/v4.0.0
[all changes since v4.0-beta]: https://github.com/marytts/marytts/compare/v4.0beta...v4.0.0
[v4.0-beta]: https://github.com/marytts/marytts/releases/tag/v4.0beta
[all changes since v3.6.0]: https://github.com/marytts/marytts/compare/v3.6.0...v4.0beta
[v3.6.0]: https://github.com/marytts/marytts/releases/tag/v3.6.0
[all changes since v3.5.0]: https://github.com/marytts/marytts/compare/v3.5.0...v3.6.0
[v3.5.0]: https://github.com/marytts/marytts/releases/tag/v3.5.0
[all changes since v3.1.0]: https://github.com/marytts/marytts/compare/v3.1.0...v3.5.0
[v3.1.0]: https://github.com/marytts/marytts/releases/tag/v3.1.0
[all changes since v3.1.beta2]: https://github.com/marytts/marytts/compare/v3.1beta2...v3.1.0
[v3.1-beta2]: https://github.com/marytts/marytts/releases/tag/v3.1beta2
[all changes since v3.1-beta1]: https://github.com/marytts/marytts/compare/v3.1beta1...v3.1beta2
[v3.1-beta1]: https://github.com/marytts/marytts/releases/tag/v3.1beta1
[all changes since v3.0.3]: https://github.com/marytts/marytts/compare/v3.0.3...v3.1beta1
[v3.0.3]: https://github.com/marytts/marytts/releases/tag/v3.0.3
[all changes since v3.0.2]: https://github.com/marytts/marytts/compare/v3.0.2...v3.0.3
[v3.0.2]: https://github.com/marytts/marytts/releases/tag/v3.0.2
[all changes since v3.0.1]: https://github.com/marytts/marytts/compare/v3.0.1...v3.0.2
[v3.0.1]: https://github.com/marytts/marytts/releases/tag/v3.0.1
[all changes since v3.0.0]: https://github.com/marytts/marytts/compare/v3.0.0...v3.0.1
[v3.0.0]: https://github.com/marytts/marytts/releases/tag/v3.0.0
[MaryTTS]: https://github.com/marytts/marytts