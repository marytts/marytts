[![Build Status](https://travis-ci.org/marytts/marytts.svg?branch=5.1.x)](https://travis-ci.org/marytts/marytts)

# MaryTTS README

This is the source code repository for the multilingual open-source MARY text-to-speech platform (MaryTTS).
MaryTTS is a client-server system written in pure Java, so it runs on many platforms.

**For a downloadable package ready for use, see [the releases page](https://github.com/marytts/marytts/releases).**

**For documentation on using MaryTTS from various angles, see [the wiki](https://github.com/marytts/marytts/wiki).**

Older documentation can also be found at https://mary.dfki.de and https://mary.opendfki.de .


This README is part of the the MaryTTS source code repository.
It contains information about compiling and developing the MaryTTS sources.

The code comes under the Lesser General Public License LGPL version 3 -- see [LICENSE.md](LICENSE.md) for details.


##  Using MaryTTS in your own Java projects

### Adding the MaryTTS to your dependency
The easiest way to use MaryTTS in your own java projects is to define a dependency to MaryTTS:

- in the pom.xml for maven:
```xml
<dependency>
  <groupId>de.dfki.mary</groupId>
  <artifactId>marytts</artifactId>
  <version>5.2</version>
  <type>pom</type>
</dependency>
```
- in gradle
```groovy
compile 'de.dfki.mary:marytts:5.2'
```

### Synthesizing speech

Text to wav basic examples are proposed in this repository
- maven: https://github.com/marytts/marytts-txt2wav/tree/maven
- gradle: https://github.com/marytts/marytts-txt2wav/tree/gradle

## Using MaryTTS for other programming languages

If you want to use MaryTTS for other programming languages (like python for example), you need to achieve 3 steps
1. compiling marytts
2. starting the server
3. query synthesis on the server


### Compiling MaryTTS on the command line

MaryTTS builds with Maven 3.0.x.
If it is not installed on your system, you can get it from here:
http://maven.apache.org/download.html or install it using your favorite package manager.

Compiling the MARY system itself can be done using

    mvn install

in the top-level folder.

This will compile the system, run all unit and integration tests, package the system to the extent possible, and install it in your local maven repository.


### Running the freshly built MaryTTS server


After a successful compile, you should find a ready-to-run unpacked install of the MaryTTS server system in `target/marytts-<VERSION>`.
Run the server as

	target/marytts-<VERSION>/bin/marytts-server.sh

Then connect to it with your browser at http://localhost:59125 or using the marytts-client.sh in the same folder.

The runtime system is also available as deployable packages:

    target/marytts-<VERSION>.zip

Installation is easy:
unpack anywhere, and run the scripts in the `bin/` folder.

### Synthesize speech using the server

Synthesizing speech, using the server, is pretty easy.
You need to generate proper HTTP queries and deal with the associated HTTP responses.
Examples are proposed :
- python 3: https://github.com/marytts/marytts-txt2wav/tree/python
- shell: https://github.com/marytts/marytts-txt2wav/tree/sh

## Developing MaryTTS

### Working on MaryTTS code

The recommended workflow for making contributions to the MaryTTS source code is to follow the github model:

1. fork the MaryTTS repository into your own space on github, by navigating to  https://github.com/marytts/marytts and clicking "fork" (of course you need a github account);

2. use the git clone, commit, and push commands to make modifications on your own marytts repository;
   in this process, make sure to `git pull upstream master` repeatedly to stay in sync with latest developments on the master repo;

3. when you think a reusable contribution is ready, issue a "pull request" on github to allow for easy merging into the master repository.

Have a good look at the [github documentation](http://help.github.com/) if any of the words here seem unfamiliar.

### IDE configuration

Wiki pages are available to help you to configure your IDE to develop MaryTTS.
The following IDE have been tested and documented:
- Eclipse : https://github.com/marytts/marytts/wiki/Eclipse
