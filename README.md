[![Build Status](https://travis-ci.org/royludo/cd2sbgnml.svg?branch=master)](https://travis-ci.org/royludo/cd2sbgnml)

## Project

The aim is to provide the most accurate translation possible between
[CellDesigner](http://www.celldesigner.org/)
and [SBGN-ML](https://sbgn.github.io/sbgn/) file formats.
Translation in both direction is possible. This project should ultimately be integrated into
[SBFC](https://www.ebi.ac.uk/biomodels/tools/converters/).

Compatible formats:
 - Sbml Level 2 Version 4, CellDesigner modelVersion 4.0 (output of CellDesigner 4.4)
 - SBGN-ML 0.2 (PD)

To download the full app directly, go to the [release page](https://github.com/royludo/cd2sbgnml/releases).

More information can be found in the [Wiki](https://github.com/royludo/cd2sbgnml/wiki).

Javadoc is available [here](https://royludo.github.io/cd2sbgnml).

All known issues and limitations of the translator are listed in the [issues](https://github.com/royludo/cd2sbgnml/issues)
and on this [wiki page](https://github.com/royludo/cd2sbgnml/wiki/Limitations).

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

Javadoc can be generated with:

`mvn javadoc:javadoc`

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

## Contributions and issues

If you have any suggestions or want to report a bug, don't hesitate to create an [issue](https://github.com/royludo/cd2sbgnml/issues).
Pull requests and all forms of contribution will be warmly welcomed.

## Useful links

 - More details about the [SBGN-ML format](https://github.com/sbgn/sbgn/wiki/SBGN_ML)
 - The [CellDesigner format](http://www.celldesigner.org/documents/CellDesigner4ExtensionTagSpecificationE.pdf) (outdated for 4.4)
 - [SBFC doc](http://sbfc.sourceforge.net/mediawiki/index.php/Main_Page)
 - a super useful webservice for [SBGN rendering](http://sysbioapps.dyndns.org/Home/Services)

## Acknowledgements

This work was initially done at [SysBio group](http://sysbio.curie.fr/) in [Institut Curie](https://curie.fr/)
under the supervision of Andrei Zinovyev, in collaboration with Alexander Mazein from [EISBM](http://www.eisbm.org/)
and the Disease Maps community.

Thanks to Frank T. Bergmann for his SBGN rendering tool which made debugging a whole lot easier.

Thanks to Nicolas, Laetitia, Henri, Choumouss and Julien for the everyday office mate support.

And thanks to Olga Ivanova for this translation rule sheet that, in the end, definitely had a real part in this project.

This work has received funding from the European Union Horizon 2020 research and
innovation programme under grant agreement No 668858.
