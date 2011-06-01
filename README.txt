MARY TTS README
---------------

Compile
-------

MARY TTS builds with Maven 3.0.x. If it is not installed
on your system, you can get it from here: http://maven.apache.org/download.html


TEMPORARY WORKAROUND
For the moment, some dependencies must be manually installed before you can build the system. This step should become obsolete in the future.
To install the temporary stuff, run
	./installDependencies.sh
which will take a while.

In order to install just individual dependencies, you can also run this as:
    ./installDependencies.sh dep1 dep2
where dep1 and dep2 must be the names of dependencies as defined inside installDependencies.sh.
END TEMPORARY WORKAROUND


Compiling the MARY system itself can be done using

    mvn install

in the top-level folder. This will compile the system, run all unit and integration tests, package the system to the extent possible, and install it in your local maven repository.


Run
---

After a successful compile, you should find a ready-to-run unpacked install of the MARY TTS server system in target/marytts-<VERSION>. Run the server as 

	target/marytts-<VERSION>/bin/maryserver.sh
	
Then connect to it with your browser at http://localhost:59125
