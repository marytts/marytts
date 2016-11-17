# MaryTTS

This is the multilingual open-source MARY text-to-speech platform (MaryTTS).
MaryTTS is a client-server system written in pure Java, so it runs on any platform that has a Java runtime installed.

Please refer to http://mary.dfki.de and https://github.com/marytts/marytts for further details.

This README is part of the the MaryTTS distribution.

The code comes under the Lesser General Public License LGPL version 3 -- see LICENSE.md for details.


## Running MaryTTS

Run `bin/marytts-server`  (or `bin\marytts-server.bat` on Windows) to start a MaryTTS server.
Then access it at http://localhost:59125 using your web browser.


## Downloading and installing voices

Run `bin/marytts-component-installer` (or `bin\marytts-component-installer.bat` on Windows) to start an installer GUI to download and install more voices.
A running MaryTTS server needs to be restarted before the new voices can be used.
