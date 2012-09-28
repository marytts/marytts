MARY TTS README
===============

This is the source code repository for the multilingual open-source MARY text-to-speech platform (MARY TTS).
MARY TTS is a client-server system written in pure Java, so it runs on many platforms.

**For a downloadable package ready for use, see [the download page](https://github.com/marytts/marytts/downloads).** 

**For documentation on using MARY TTS from various angles, see [the wiki](https://github.com/marytts/marytts/wiki).**
Older documentation can also be found at http://mary.dfki.de and http://mary.opendfki.de .


This README is part of the the MARY TTS source code repository. It contains information about compiling
and developing the MARY TTS sources.

The code comes under the Lesser General Public License LGPL version 3 -- see LICENSE.txt for details. 


Working on MARY TTS code
------------------------

The recommended workflow for making contributions to the MARY TTS source code is to follow the github model:

1. fork the MARY TTS repository into your own space on github, by navigating to https://github.com/marytts/marytts
   and clicking "fork" (of course you need a github account);
   
2. use the git clone, commit, and push commands to make modifications on your own marytts repository; in this process,
   make sure to `git pull upstream master` repeatedly to stay in sync with latest developments on the master repo;

3. when you think a reusable contribution is ready, issue a "pull request" on github to allow for easy merging into
   the master repository.
   
Have a good look at the [github documentation](http://help.github.com/) if any of the words here seem unfamiliar.


Compiling MARY TTS on the command line
--------------------------------------

MARY TTS builds with Maven 3.0.x. If it is not installed
on your system, you can get it from here: http://maven.apache.org/download.html

Compiling the MARY system itself can be done using

    mvn install

in the top-level folder. This will compile the system, run all unit and integration tests, package the system to the extent possible, and install it in your local maven repository.


Running the freshly built MARY TTS server
-----------------------------------------

After a successful compile, you should find a ready-to-run unpacked install of the MARY TTS server system in `target/marytts-<VERSION>`. Run the server as 

	target/marytts-<VERSION>/bin/marytts-server.sh
	
Then connect to it with your browser at http://localhost:59125 or using the marytts-client.sh in the same folder.

The runtime system is also available as deployable packages:

    target/marytts-<VERSION>.zip
    target/marytts-<VERSION>.tar.gz

Installation is easy: Unpack anywhere, and run the scripts in the `bin/` folder.


Using the new languages support and voice building tools
--------------------------------------------------------

Compiling the MARY TTS system creates a folder containing the MARY TTS build tools, in 

    target/marytts-builder-<VERSION>/

Shell scripts for the available tools are provided in the `bin/` subfolder.  


Using MARY TTS in your own Java projects
----------------------------------------

The easiest way to get all dependencies right is to use Maven. The example projects below `user-examples` should be sufficient to get you started.

The `pom.xml` file in each example project copies the required jar files and sets the classpath of the project jar file such that it can be simply started as follows:

    java -jar user-examples/example-embedded/target/example-embedded-<VERSION>.jar


Developing MARY TTS in Eclipse
------------------------------

The easiest and therefore recommended way to edit MARY TTS source files is using [Eclipse IDE for Java developers](http://eclipse.org).
We have tested with Eclipse Indigo, feel free to experiment with other versions.

Two relevant Eclipse plugins which come pre-bundled with Eclipse Indigo for Java developers are [M2E Maven Eclipse Integration](http://eclipse.org/m2e/)
and [Egit Eclipse Git Source code management](http://eclipse.org/egit/).

M2E can be used to import the Maven projects into an Eclipse workspace as follows.

1. Start with an empty workspace, from the "File" menu select "Import..." and open the "Maven" menu item.

2. Import the maven projects as eclipse projects:

    a. If you have previously cloned the git repo, select "Existing Maven Projects", choose the root `marytts` directory as root, and select the subprojects you want to import.

    b. If you have not yet cloned the git repo, select "Checkout Maven projects from SCM", choose SCM method `git`
       (you may have to follow the link "install SCM connectors from the m2e marketplace" and install `m2e-egit`),
       and enter the git repository location as download link (e.g., `git@github.com:marytts/marytts.git` to directly clone the master repo, but see "Working on MARY TTS code" above for good practice).

3. To make the Eclipse projects aware of the version control system, select all of them, right-click and select "Team"->"Share projects".
   In the popup window, select "Git", click Next, then check the checkbox "Use or create repository in parent folder of project", and click "Finish".

This should get you up and running.

You are of course free to edit the source code using other tools. Just be aware that MARY TTS requires Java 6, and source file encoding must be set to UTF-8.