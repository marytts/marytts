Title: Download

# The MARY Text-to-Speech System: Download

To install and run MARY TTS, you will need a recent [Java Runtime](http://www.java.com/getjava) installed (Java 1.6 or newer).

To install, download [MARY TTS 5.0 zip](https://github.com/downloads/marytts/marytts/marytts-5.0.zip) or [MARY TTS 5.0 tar.gz](https://github.com/downloads/marytts/marytts/marytts-5.0.tar.gz)

[MARY TTS 5.0 Release Notes (June 2012)](https://github.com/marytts/marytts/wiki/MARY-TTS-5.0)

## MARY TTS 5.0+ Releases

... are available from http://github.com/marytts/marytts.

## Older MARY Releases

| Version | Release Date | Java Version | Download | Size | |
|---|:---:|:---:|---|---:|---|
| [4.3.1](https://github.com/marytts/marytts/releases/tag/v4.3.1) | 2011-11-30 | 1.6+ | [Standalone installer](http://mary.dfki.de/download/4.3.1/openmary-standalone-install-4.3.1.jar) | 41.2 MB | [Release notes](releasenotes-4.3.1.html) |
| [4.3.0](https://github.com/marytts/marytts/releases/tag/v4.3.0) | 2010-12-20 | 1.5+ | [Standalone installer](http://mary.dfki.de/download/4.3.0/openmary-standalone-install-4.3.0.jar) | 41 MB   | [Release notes](releasenotes-4.3.0.html) |
| [4.2.0](https://github.com/marytts/marytts/releases/tag/v4.2.0) | 2010-12-08 | 1.5+ | [Standalone installer](http://mary.dfki.de/download/4.2.0/openmary-standalone-install-4.2.0.jar) | 41 MB   | [Release notes](releasenotes-4.2.0.html) |
| [4.1.1](https://github.com/marytts/marytts/releases/tag/v4.1.1) | 2010-09-09 | 1.5+ | [Standalone installer](http://mary.dfki.de/download/4.1.1/openmary-standalone-install-4.1.1.jar) | 33.9 MB | [Release notes](releasenotes-4.1.1.html) |
| [4.1.0](https://github.com/marytts/marytts/releases/tag/v4.1.0) | 2010-09-01 | 1.5+ | [Standalone installer](http://mary.dfki.de/download/4.1.0/openmary-standalone-install-4.1.0.jar) | 33.8 MB | [Release notes](releasenotes-4.1.0.html) |
| [4.0.0](https://github.com/marytts/marytts/releases/tag/v4.0.0) | 2009-12-18 | 1.5+ | [Standalone installer](http://mary.dfki.de/download/4.0.0/openmary-standalone-install-4.0.0.jar) | 31.4 MB | [Release notes](releasenotes-4.0.0.html) |
| [4.0-beta](https://github.com/marytts/marytts/releases/tag/v4.0beta) | 2009-09-24 | 1.5+ | | [Release notes](releasenotes-4.0-beta.html) |

## Pre-4.0 releases

These releases are mixed open source / research license, and mostly contain native code.
This makes them more difficult to reuse and port to new platforms.
Support for these releases terminates with the publication of 4.0beta.

Older comments:

DFKI has released the core MARY system as open source, including English and Tibetan synthesis and various voices, including free unit selection voices based on the CMU Arctic databases.

In addition, German synthesis is released under a [DFKI research license](http://mary.dfki.de/download/DFKI%20MARY%20software%20user%20agreement.html).
The system can also use [MBROLA](http://tcts.fpms.ac.be/synthesis/mbrola.html) voices, under the [MBROLA license](http://mary.dfki.de/download/Mbrola%20software%20user%20agreement.html).

The interactive, web-based installer which you can download below will guide you through the installation process.
It will let you decide on the install location on your file system and will let you select the components you wish to install.
You will need to accept the licenses appropriate for the selected components before these will actually be downloaded and installed.

The system should run on MS Windows (2000 and XP, maybe others), PC-Linux, Sun Solaris, and Mac OS X.

| Version | Release Date | Java Version | Download | Size | |
|---|:---:|:---:|---|---:|---|
| [3.6.0](https://github.com/marytts/marytts/releases/tag/v3.6.0) | 2008-05-21 | 1.5+ | [Web installer](http://mary.dfki.de/download/mary-install-3.6.0.jar)<br/>[Standalone installer](http://mary.dfki.de/download/mary-standalone-install-3.6.0.jar) | 704 KB<br/>32.2 MB | [Release notes](releasenotes-3.6.0.html) |
| [3.5.0](https://github.com/marytts/marytts/releases/tag/v3.5.0) | 2007-12-07 | 1.5+ | | | [Release notes](releasenotes-3.5.0.html) |
| [3.1.0](https://github.com/marytts/marytts/releases/tag/v3.1.0) | 2007-08-17 | 1.4+ | | | [Release notes](releasenotes-3.1.0.html) |

### 3.1beta2

[mary-install-3.1beta2.jar](http://mary.dfki.de/download/mary-install-3.1beta2.jar) (web-based installer, 450kB)

**Release notes:**
Another beta release of the new unit selection code.
We have added acoustic targets, so slt-arctic, bdl-arctic and jmk-arctic voices should again sound better than in 3.1beta1.
Also, we have four German unit selection voices, using recordings from the BITS project!
Quality is not optimal yet, but we're getting there.

Audio data is still stored as uncompressed PCM data, which makes the full install bigger then 2 GB - therefore, unfortunately there is no standalone-installer for this release.
We hope to be able to offer smaller voices in the future.

**Known issue:**
The diphone voices in this release are pretty much broken.
If you want to use diphone voices, your best bet is probably the 3.0.3 stable release.

For some details of what has been done and what still needs to be done for the stable 3.1 release, see the [full list of issues](http://mary.opendfki.de/milestone/3.1.0) on the development portal.

### 3.1beta1

[mary-install-3.1beta1.jar](http://mary.dfki.de/download/mary-install-3.1beta1.jar) (web-based installer, 450kB) or use [mary-standalone-install-3.1beta1.jar](http://mary.dfki.de/download/mary-standalone-install-3.1beta1.jar) (standalone installer, ~500MB) if the web-based installer causes problems

**Release notes:**
First beta release of the new unit selection code.
Try the new slt-arctic, bdl-arctic and jmk-arctic voices!

For some details of what has been done and what still needs to be done for the stable release, see the [full list of issues](http://mary.opendfki.de/milestone/3.1.0) on the development portal.

### 3.0.3

[mary-install-3.0.3.jar](http://mary.dfki.de/download/mary-install-3.0.3.jar) (web-based installer, 450kB) or use [mary-standalone-install-3.0.3.jar](http://mary.dfki.de/download/mary-standalone-install-3.0.3.jar) (standalone installer, 420MB) if the web-based installer causes problems

**Release notes:**
Third bugfix release.
Most relevant changes:

* fixed standalone mode for running MARY ([ticket 80](http://mary.opendfki.de/ticket/80))
* improved control over prosody using ToBI tags (tickets [59](http://mary.opendfki.de/ticket/59), [60](http://mary.opendfki.de/ticket/60))
* Several minor improvements of German synthesis (tickets [44](http://mary.opendfki.de/ticket/44), [49](http://mary.opendfki.de/ticket/49), [57](http://mary.opendfki.de/ticket/57), [78](http://mary.opendfki.de/ticket/78))
* MBROLA to AUDIO conversion fixed (tickets [54](http://mary.opendfki.de/ticket/54), [55](http://mary.opendfki.de/ticket/55))
* An update is recommended only if you encountered one of these bugs.

See also the [full list of bugs fixed](http://mary.opendfki.de/query?milestone=3.0.3)

### 3.0.2

[mary-install-3.0.2.jar](http://mary.dfki.de/download/mary-install-3.0.2.jar) (web-based installer, 450kB) or use [mary-standalone-install-3.0.2.jar](http://mary.dfki.de/download/mary-standalone-install-3.0.2.jar) (standalone installer, 420MB) if the web-based installer causes problems

**Release notes:**
Second bugfix release.
A number of bugs have been fixed, including:

* WAV audio can now (really) be saved with proper audio headers ([ticket 38](http://mary.opendfki.de/ticket/38))
* several bugs related to failing validation of intermediate processing results (due to an outdated MaryXML Schema) were fixed (tickets [40](http://mary.opendfki.de/ticket/40), [41](http://mary.opendfki.de/ticket/41), [43](http://mary.opendfki.de/ticket/43))
* Synthesizing APML was improved (tickets [51](http://mary.opendfki.de/ticket/51), [52](http://mary.opendfki.de/ticket/52))
* Pronunciation for German compounds was improved ([ticket 36](http://mary.opendfki.de/ticket/36)) 
* An update is recommended if you encountered one of these bugs.

See also the [full list of bugs fixed](http://mary.opendfki.de/query?status=closed&amp;milestone=3.0.2&amp;order=priority)

### 3.0.1

[mary-install-3.0.1.jar](http://mary.dfki.de/download/mary-install-3.0.1.jar) (web-based installer, 450kB) or use [mary-standalone-install-3.0.1.jar](http://mary.dfki.de/download/mary-standalone-install-3.0.1.jar) (standalone installer, 420MB) if the web-based installer causes problems

**Relase notes:**
First bugfix release.
A number of bugs have been fixed which have occurred after the initial release.
Most relevant:

* Audio can now be saved with proper audio headers ([ticket 32](http://mary.opendfki.de/ticket/32))
* Mary GUI client is now accessible via the keyboard ([ticket 3](http://mary.opendfki.de/ticket/3))

An update is recommended.

See also the [full list of bugs fixed](http://mary.opendfki.de/query?status=closed&amp;milestone=3.0.1&amp;order=priority)

### 3.0.0

[mary-install-3.0.0.jar](http://mary.dfki.de/download/mary-install-3.0.0.jar) (web-based installer, 450kB) or use [mary-standalone-install-3.0.0.jar](http://mary.dfki.de/download/mary-standalone-install-3.0.0.jar) (standalone installer, 420MB) if the web-based installer causes problems

**Note:**
This is the first open-source release.
We have put in a lot of effort to test and debug it, but of course the system may have some teething problems.
Please help us improve the system by submitting [bug reports](http://mary.opendfki.de/newticket) on the [OpenMary development page](http://mary.opendfki.de/).

--------

## Related downloads

### Plone Speech Synthesis Tool

This Plone product endows your Plone portal with speech output capabilities.
It is a client which connects to a MARY TTS server, so you will need to install the MARY TTS system to use the Plone Speech Synthesis Tool.

Installation:
1. Install MARY TTS and run the server
2. Unpack SpeechSynthesisTool in your portal's Products/ folder;
   restart zope;
   install product using the portal_quickinstaller;
   and configure the location of your MARY TTS server under "site setup"->"SpeechSynthesisTool Setup".

Download [SpeechSynthesisTool-0.1.zip](http://mary.dfki.de/download/SpeechSynthesisTool-0.1.zip) or get from SVN:

    svn checkout https://mary.opendfki.de/repos/trunk/examples/client/SpeechSynthesisTool SpeechSynthesisTool
