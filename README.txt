----------------------------------------
Mary:             Modular
                  Architecture for
                  Research on
          speech sYnthesis

     Release 3.5.0, December 2007
----------------------------------------
README
======

This is Mary, the Modular Architecture for Research on speech sYnthesis,
developed by DFKI and Institute of Phonetics, University of the Saarland.

For copyright information, see the file "MARY software user agreement.txt"
in this directory, as well as other user agreements that may have been
installed depending on the components selected during installation.


Overview
========
* New in this release
* Introduction
* Installing and running the Mary server
  - Installation under Linux/Solaris
  - Installation under Windows
  - Running server and client
* Important configuration settings
* Starting only selected components
* Building new voices
* How to use Mary in other applications: Demo socket client
* Known limitations
* Bug reports


New in this release
===================

This release provides substantial new features while maintaining the API
compatible to previous versions.

New features include:
* installer slimmed down to just over 30 MB, including some voices;
* separate voice installer tool, allowing you to download voices comfortably
  and with an integrity verification prior to installation;
* new synthesis technology: Marcela Charfuelan ported the excellent HMM-based
  synthesis code from the HTS project (http://hts.sp.nitech.ac.jp/) to Java --
  several HMM-based voices already ship with MARY;
* audio effects: Oytun Turk implemented a range of audio effects that can be
  applied to the MARY voices, and can be controlled through the MARYGUIClient;
* voice creation toolkit: Sathish Chandra Pammi and Anna Hunecke have
  cleaned up and documented the tool we use for building new synthesis voices;
* voice recording tool "Redstart": Mat Wilson has programmed a very nice GUI 
  for recording new voices;
* OGG Vorbis support: under Linux, the MARY server can now generate audio in
  OGG Vorbis format.

Introduction
============

This is the text-to-speech synthesis system MARY (Modular Architecture for
Research on speech sYnthesis). Apart from being open-source and multi-platform,
its main feature is its flexibility allowing the access to intermediate
processing results in XML format.

For a detailed description of the Mary system, see the article (included as
doc/papers/schroeder_trouvain2003.pdf):

M. Schroeder & J. Trouvain (2003). The German Text-to-Speech Synthesis
System MARY: A Tool for Research, Development and Teaching.
International Journal on Speech Technology, 6, pp. 365-377..



Installing and Running the Mary server
======================================

Supported platforms:

* Windows (tested on Windows 98, NT 4.0, 2000, XP, and Vista)
* Linux (32 and 64 bit versions)
* Solaris
* MacOS X


Pre-conditions:

* Java 1.5 (or higher)


Running the server and the client
---------------------------------

To run the system, you need to start TWO programs:

1. The MARY server
   The server is the core of the system, doing the actual work.
   A shortcut for starting it should have been created for you on your desktop
   when you installed the system.
   You can also find the program at MARY_BASE/bin/maryserver
   
   The MARY server will verify the integrity of the installation prior
   to startup. If it detects a misconfiguration, it will report it
   and refuse to start. It may also propose to download a missing component
   from the internet.
   When you see a message like the following, you can proceed to step 2:
   "Mary server 3.x.x starting... started in 3.51 s"
   
2. The MARY client
   Start the MARY client to convert text into speech.
   A shortcut for starting it should have been created for you on your desktop
   when you installed the system.
   You can also find the program at MARY_BASE/bin/maryclient

   The MARY client will fail if no MARY server could be found. Make sure your
   server was correctly started prior to starting the client!
   
   Of course, the MARY system can be used in various client-server setups,
   e.g. for web applications. See MARY_BASE/examples/client
   for a number of implementations in various programming languages.

* And proceed as described in the Schroeder&Trouvain(2003) paper.

* If you want to see what the server does, watch the server log file:

    tail -f $MARY_BASE/log/server.log (linux)
    type %MARY_BASE%\log\server.log (windows)



Troubleshooting
---------------

* If you get "poweronselftest failed" messages and you are on a slow
  machine, try increasing the timeout value, e.g.
    maryserver "-Dmodules.timeout=60000"
  to give modules 60 seconds instead of 10.


Important configuration settings
================================

The mary config files contain a large number of configuration settings. These
files are located under MARY_BASE/conf.

The most essential configuration settings, found in MARY_BASE/conf/marybase.config,
are listed in the following.

modules.poweronselftest
    Perform a power-on self test for each module. This slows down the
    server startup, but gives you extra security that the system is
    properly installed. Per default, it is enabled for server use. If you
    are confident that the server is running well, and want it to start
    faster, you can set this to false. Note that this setting has no
    influence on runtime performance.

modules.timeout
    The duration after which a module is considered to have timed out.
    If you get repeated "Timeout occurred" messages in the logfile and
    you know that you are on a slow machine or under heavy load, try
    increasing this value.

log.tofile
   Determine the destination of log messages. Per default, log messages
   are written to a log file. For easier inspection, you may choose to
   have log messages written to the screen by setting this property to false.

log.level
    The verbosity of logging output. The default setting INFO will write
    a reasonable amount of log messages documenting the current state of
    processing. It should suffice for most cases. If you trust the server
    and want to know only about serious problems, use WARN. If you want to
    debug the server and want extra verbose output, set it to DEBUG. Note
    that the DEBUG setting will slow down the server.

log.filename
    The filename of the log file. Per default, this is
    MARY_BASE/log/server.log.

server
    Whether to run as a socket server. Enabled per default. If set to false,
    process one file of input, and exit. A possible command line would be
    java -Dserver=false -Dmary.base=$MARY_BASE de.dfki.lt.mary.Mary myfile.txt > myfile.wav

socket.port
    The socket port on which the server will listen for client connections.
    Per default, this is set to 59125. You can run several servers on the
    same machine by giving them different socket.ports to listen at.


All of these settings can also be given on the command line, where they must
be preceded with -D. Settings given on the command line override settings in
the maryrc file. For example,
    maryserver -Dmodules.poweronselftest=false -Dlog.level=DEBUG
will start the mary server without a power-on self test, but with debug log
output, even if the settings in the maryrc file are different.


Starting only selected components
=================================

By default, the server will use all the .config files under MARY_BASE/conf
to determine how to start up. If you wish to permanently disable a certain
component, simply delete the corresponding .config file.

If you want to disable a certain component temporarily, you can pass the
following system property to the server: "-Dignore.<component>.config".
This will start the server as if the file MARY_BASE/conf/<component>.config
was not present.

For example, in order not to load the German system,
you would need to ignore german.config and all German voice configs, e.g.:

Linux/Solaris/Mac: maryserver -Dignore.german.config -Dignore.mbrola-de3.config -Dignore.mbrola-de7.config
Windows: maryserver.bat "-Dignore.german.config" "-Dignore.mbrola-de3.config" "-Dignore.mbrola-de7.config"


Building new voices
===================

The tool "Voice Import Tool" is very powerful and aimed to be easy to use.
Detailed documentation can be found on the project wiki page:
http://mary.opendfki.de/wiki/VoiceImportToolsTutorial


How to use Mary in other applications: Demo socket client
=========================================================

Implementations of the MARY client in various programming languages, as well as
an example of how to use the Java MARY client from a simple Java program, can
be found in MARY TTS/examples.


Known limitations
=================

Under Windows Vista, installation is not possible in the program folder c:\Program Files
because of stricter security settings. Installation in a different folder should work.

Under Solaris or MacOS X, MP3 encoding does not work, as there is no native library
for mp3 encoding.


Bug reports
===========

The system was tested before the release, but clearly you can never test enough.
If any problems should occur, please submit bug reports via the mary-users
mailing list -- register at http://dfki.de/mailman/listinfo/mary-users


IMPORTANT: Be sure to include the following information in any bug report:

* Detailed error message, including a server.log file produced with a
  log.level=DEBUG setting (see Important configuration settings above);
* If possible, a small test input file allowing us to reproduce the problem.
