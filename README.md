[![CI](https://github.com/marytts/marytts/actions/workflows/main.yml/badge.svg)](https://github.com/marytts/marytts/actions/workflows/main.yml)

# MaryTTS

This is the source code repository for the multilingual open-source MARY text-to-speech platform (MaryTTS).
MaryTTS is a client-server system written in pure Java, so it runs on many platforms.

**For a downloadable package ready for use, see [the releases page](https://github.com/marytts/marytts/releases).**

Older documentation can also be found at https://github.com/marytts/marytts-wiki, http://mary.dfki.de and https://mary.opendfki.de.

This README is part of the the MaryTTS source code repository.
It contains information about compiling and developing the MaryTTS sources.

The code comes under the Lesser General Public License LGPL version 3 -- see LICENSE.md for details.


## Running MaryTTS

Run `./gradlew run`  (or `gradlew.bat run` on Windows) to start a MaryTTS server.
Then access it at http://localhost:59125 using your web browser.

If you want to start a MaryTTS on a different address and port, you can use the following options:

```sh
./gradlew run -Dsocket.port=5920 -Dsocket.addr=0.0.0.0 --info
```
where 5920 is the new port and 0.0.0.0 the new address. In case of the address being 0.0.0.0, all the interfaces will be listened.

By using the option `--info`, you set the logger of `gradle` *AND* MaryTTS at the level INFO. By using `--debug`, you set the level to DEBUG.

It is also possible to set the MaryTTS logger level to `INFO` or `DEBUG` by defining the system variable `log4j.logger.marytts`.

## Downloading and installing voices

Run `./gradlew runInstallerGui` to start an installer GUI to download and install more voices.
A running MaryTTS server needs to be restarted before the new voices can be used.


## Building MaryTTS

Run `./gradlew build`.
This will compile and test all modules, and create the output for each under `build/`.

Note that previously, MaryTTS v5.x was built with Maven. Please refer to the [**5.x branch**](https://github.com/marytts/marytts/tree/5.x).


## Packaging MaryTTS

Run `./gradlew distZip` or `./gradlew distTar` to build a distribution package under `build/distributions`.
You can also "install" an unpacked distribution directly into `build/install` by running `./gradlew installDist`.

The distribution contains all the files required to run a standalone MaryTTS server instance, or to download and install more voices.
The scripts to run the server or installer GUI can be found inside the distribution in the `bin/` directory.


##  Using MaryTTS in your own Java projects

The easiest way to use MaryTTS in your own Java projects is to declare a dependency on a relevant MaryTTS artifact, such as the default US English HSMM voice:

### Maven

Add to your `pom.xml`:
```xml
<repositories>
  <repository>
    <url>https://mlt.jfrog.io/artifactory/mlt-mvn-releases-local</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>de.dfki.mary</groupId>
    <artifactId>voice-cmu-slt-hsmm</artifactId>
    <version>5.2.1</version>
    <exclusions>
      <exclusion>
        <groupId>com.twmacinta</groupId>
        <artifactId>fast-md5</artifactId>
      </exclusion>
      <exclusion>
         <groupId>gov.nist.math</groupId>
         <artifactId>Jampack</artifactId>
      </exclusion>
    </exclusions>
  </dependency>
</dependencies>
```

### Gradle

Add to your `build.gradle`:
```groovy
repositories {
   mavenCentral()

   exclusiveContent {
      forRepository {
         maven {
            url 'https://mlt.jfrog.io/artifactory/mlt-mvn-releases-local'
         }
      }
      filter {
         includeGroup 'de.dfki.lt.jtok'
      }
   }
}

dependencies {
   implementation group: 'de.dfki.mary', name: 'voice-cmu-slt-hsmm', version: '5.2.1', {
      exclude group: 'com.twmacinta', module: 'fast-md5'
      exclude group: 'gov.nist.math', module: 'Jampack'
   }
}
```


## Synthesizing speech

Text to wav basic examples are proposed in this repository
- Maven: https://github.com/marytts/marytts-txt2wav/tree/maven
- Gradle: https://github.com/marytts/marytts-txt2wav/tree/gradle


## Using MaryTTS for other programming languages

If you want to use MaryTTS for other programming languages (like python for example), you need to achieve 3 steps

1. compiling marytts
2. starting the server
3. query synthesis on the server


### Synthesize speech using the server

Synthesizing speech, using the server, is pretty easy.
You need to generate proper HTTP queries and deal with the associated HTTP responses.
Examples are proposed :
- python 3: https://github.com/marytts/marytts-txt2wav/tree/python
- shell: https://github.com/marytts/marytts-txt2wav/tree/sh

## Extra documentation

### Server as service (Linux specific)

An example of how to define marytts server as service is proposed [here](./src/main/dist/misc/marytts.server).

### User dictionaries

You can extend the dictionaries by adding a user dictionary. The documentation of how to do it is [here](./src/main/dist/user-dictionaries/README.md).

## Contributing

The recommended workflow for making contributions to the MaryTTS source code is to follow the GitHub model:

1. fork the MaryTTS repository into your own profile on GitHub, by navigating to https://github.com/marytts/marytts and clicking "fork" (of course you need a GitHub account);

2. use the `git clone`, `commit`, and `push` commands to make modifications on your own marytts repository;
   in this process, make sure to `git pull upstream master` regularly to stay in sync with latest developments on the master repo;

3. when you think a reusable contribution is ready, open a "pull request" on GitHub to allow for easy merging into the master repository.

Have a look at the [GitHub documentation](http://help.github.com/) for further details.


### IDE configuration

Wiki pages are available to help you to configure your IDE to develop MaryTTS.
The following IDEs have been tested and documented:

- IntelliJ IDEA
- Eclipse: https://github.com/marytts/marytts/wiki/Eclipse
