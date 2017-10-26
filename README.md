under construction

- lib/cellDesignerParser-1.0.jar contains the CellDesigner parsing features factored out of BiNoM.
- lib/org.sbgn-with-dependencies.jar is built directly from the libsbgn sources.

To download the package, go to the [release page](https://github.com/royludo/cd2sbgnml/releases).

## Requirements

 - Java 8
 - JavaFX
 - Maven

## Install

`mvn clean`

`mvn install`

## Usage

You can use the cd2sbgnml.sh script to run a conversion from command line:
```bash
cd2sbgnml.sh <input file> <output file>
```

A small GUI is provided as the main class of the package. It can be launched by double clicking on the jar or by
directly calling the package with `java -jar`.