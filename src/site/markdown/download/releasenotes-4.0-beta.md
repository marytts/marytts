MARY TTS 4.0-beta: Release Notes
================================

What's new
----------

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
* [creating an HMM-based voice](http://mary.opendfki.de/wiki/HMMVoiceCreationMary4.0) from the same recordings.

More details on individual issues addressed can be seen in the [list of tickets associated with this release](http://mary.opendfki.de/query?status=closed&amp;group=resolution&amp;milestone=4.0+beta).

Tested environments
-------------------

The MARY client and server code was tested on:

* Mac OS X (Intel) 10.5.8 with java 1.6.0_07
* Ubuntu Linux 8.10 and 9.04 with sun java 1.5, 1.6, and openjdk-6
* Windows XP and Vista

The web interface at http://localhost:59125 should work with any recent browser that supports AJAX.
We have obtained best results with:

* Firefox 3.5 with built-in audio support
* Firefox 3.0, Internet Explorer 6 and 7, Safari 4, with Quicktime plugin

Known issues
------------

* On Ubuntu Linux with sun java 1.5 and 1.6, we observed a problem with audio playback in the MARY client:
  the final section is cut off. With openjdk-6, it works ok.
* On Ubuntu Linux 8.10 with the builtin Firefox 3.0, the audio plugin doesn't work properly.
* Web interface in the browser Chrome suboptimal:
  Chrome does not seem to handle the HTML 5 `<audio>` tag correctly.
  It claims it can handle it, but then doesn't play audio.
  Click on "Save audio file" to get the raw audio data.

Bug reports
-----------

This is beta software.
It is not yet ready for production use.
You are likely to find problems, and we would appreciate if you tell us about it.

If you think you have identified a bug, proceed as follows:

1. Check in the [list of known bugs](http://mary.opendfki.de/query?group=status&amp;milestone=4.0) whether the bug has been reported already;
2. If you cannot find it, create a [new ticket](http://mary.opendfki.de/newticket) containing the following information:
    * operating system and java version used;
    * what to do to reproduce the error;
    * expected behaviour;
    * detailed log files, i.e. an excerpt of MARY TTS/log/server.log containing the error episode.
3. Discuss the problem on the [MARY user mailing list](http://www.dfki.de/mailman/listinfo/mary-users).

Contributions
-------------

People interested in adding support for a language can get in touch, e.g. via the [MARY developers mailing list](http://www.dfki.de/mailman/listinfo/mary-dev).
