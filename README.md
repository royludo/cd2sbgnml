under construction

lib/org.sbgn-with-dependencies.jar is built directly from the libsbgn sources.

To download the package, go to the [release page](https://github.com/royludo/cd2sbgnml/releases).

## Requirements

 - Java 8 (with JavaFX if you want to use the GUI)
 - Maven (tested with Maven 3.5)

## Install

After cloning the repository and getting into its directory:

`mvn clean`

`mvn install`

This will output 2 jars in the `target/` directory:
 - cd2sbgnml-{version}.jar: the bare compiled project, with no dependency included. Can be used directly in
 other Java projects.
 - cd2sbgnml-{version}-app.jar: the full project with all dependencies included. Scripts and GUI provided here can be used.

## Usage

After install, you can use the 2 bash scripts to run a conversion from command line (from the project's root directory):
```bash
cd2sbgnml.sh <input file> <output file>

sbgnml2cd.sh <input file> <output file>
```

A small GUI is also provided as the main class of the package. It can be launched by double clicking on the jar or by
directly calling the package with `java -jar`. Be sure to have JavaFX working in your Java distribution.

With the scripts, all log messages will go to System.out. With the GUI, everything will be written in
the selected log file.