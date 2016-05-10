[![Build Status](https://travis-ci.org/marytts/marytts.svg?branch=5.1.x)](https://travis-ci.org/marytts/marytts)

# MaryTTS

This is the source code repository for the multilingual open-source MARY text-to-speech platform (MaryTTS).
MaryTTS is a client-server system written in pure Java, so it runs on many platforms.

**For a downloadable package ready for use, see [the releases page](https://github.com/marytts/marytts/releases).**

**For documentation on using MaryTTS from various angles, see [the wiki](https://github.com/marytts/marytts/wiki).**

Older documentation can also be found at http://mary.dfki.de and https://mary.opendfki.de.

This README is part of the the MaryTTS source code repository.
It contains information about compiling and developing the MaryTTS sources.

The code comes under the Lesser General Public License LGPL version 3 -- see LICENSE.md for details.


##  Using MaryTTS in your own Java projects

### Adding the MaryTTS to your dependencies

The easiest way to use MaryTTS in your own Java projects is to declare a dependency on a relevant MaryTTS artifact:

- in the `pom.xml` for Maven:
```xml
<repositories>
  <repository>
    <url>https://jcenter.bintray.com</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>de.dfki.mary</groupId>
    <artifactId>voice-cmu-slt-hsmm</artifactId>
    <version>5.2</version>
  </dependency>
</dependencies>
```
- in the `build.gradle` for Gradle
```groovy
repositories {
  jcenter()
}

dependencies {
  compile 'de.dfki.mary:marytts:5.2'
}
```


### Synthesizing speech

Text to wav basic examples are proposed in this repository
- Maven: https://github.com/marytts/marytts-txt2wav/tree/maven
- Gradle: https://github.com/marytts/marytts-txt2wav/tree/gradle


## Using MaryTTS for other programming languages

If you want to use MaryTTS for other programming languages (like python for example), you need to achieve 3 steps

1. compiling marytts
2. starting the server
3. query synthesis on the server


### Compiling MaryTTS on the command line

MaryTTS v5.x builds with Maven 3.0.x.
If it is not installed on your system, see
http://maven.apache.org/download.html or install it using your favorite package manager.

Compiling the MARY system itself can be done using

    mvn install

in the top-level folder.

This will compile the system, run all unit and integration tests, package the system to the extent possible, and install it in your local maven repository.


### Running the freshly built MaryTTS server

After a successful compile, you should find a ready-to-run unpacked installation of the MaryTTS server system in `target/marytts-<VERSION>`.
Run the server as

	target/marytts-<VERSION>/bin/marytts-server

Then connect to it with your browser at http://localhost:59125 or using the `marytts-client` in the same folder.

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

The recommended workflow for making contributions to the MaryTTS source code is to follow the GitHub model:

1. fork the MaryTTS repository into your own profile on GitHub, by navigating to https://github.com/marytts/marytts and clicking "fork" (of course you need a GitHub account);

2. use the `git clone`, `commit`, and `push` commands to make modifications on your own marytts repository;
   in this process, make sure to `git pull upstream master` regularly to stay in sync with latest developments on the master repo;

3. when you think a reusable contribution is ready, open a "pull request" on GitHub to allow for easy merging into the master repository.

Have a look at the [GitHub documentation](http://help.github.com/) for further details.


### IDE configuration

Wiki pages are available to help you to configure your IDE to develop MaryTTS.
The following IDEs have been tested and documented:

- Eclipse: https://github.com/marytts/marytts/wiki/Eclipse
