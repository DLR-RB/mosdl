MOSDL Compiler - Mission Operations Service Description Language
================================================================

MOSDL is a language that can be used to specify [CCSDS MO services](https://ccsdsmo.github.io/). It is similar to the *interface definition languages* of other remote procedure call systems (RPC) like [Google gRPC](https://grpc.io/) or [Apache Thrift](https://thrift.apache.org/).

The standardized service description format of CCSDS MO services is an XML format described by an [XML Schema](https://sanaregistry.org/r/moschemas). However, these XML documents can be difficult to edit, iterate and reason about. MOSDL provides an alternative to the CCSDS MO XML format. MOSDL documents can be compiled to standards-compliant XML documents using the compiler contained in this project. It is also possible to transform CCSDS MO XML documents to MOSDL documents.

MOSDL has been developed at the German Space Operations Center GSOC, which is part of [DLR, the German Aerospace Center](https://www.dlr.de/).

__WARNING__: This project is not released yet! The code should compile and be usable, but artifacts have not been uploaded to *Maven Central*. A release of a non-snapshot version is expected soon (depending on the upload process to *Maven Central*).

Current version: __${project.version}__ (also see [change log](CHANGELOG.md))


Table of contents
-----------------

- [Example](#example)
- [MOSDL language](#mosdl-language)
- [Usage](#usage)
    - [Stand-alone usage](#stand-alone-usage)
    - [Usage as plugin in a Maven-based build process](#usage-as-plugin-in-a-maven-based-build-process)
    - [Usage as a Java library](#usage-as-a-java-library)
        - [Load a specification](#load-a-specification)
        - [Write a specification](#write-a-specification)
        - [Glueing together loading and writing of a specification](#glueing-together-loading-and-writing-of-a-specification)
- [Source code and compilation](#source-code-and-compilation)
    - [Source code](#source-code)
    - [Compilation](#compilation)
- [Contributing](#contributing)
- [Maintainer](#maintainer)
- [Copyright, license and third party software](#copyright-license-and-third-party-software)
    - [Copyright](#copyright)
    - [License](#license)
	- [Third party software in binary distributions](#third-party-software-in-binary-distributions)


Example
-------

A very simple MOSDL document looks like this:

```
area TestArea [123];

service HelloWorldService {
    request myOperation(hello: String)
        -> (world: String);
}
```

The snippet above describes a service (`HelloWorldService`) offering one request-response operation (`myOperation`) that takes a single String parameter as request (`hello`) and returns a single String parameter as response (`world`).

The MOSDL compiler can be used to generate an CCSDS MO XML document, which can then be used by third-party software (e.g. [ESA CCSDS MO Stub Generator](https://github.com/esa/CCSDS_MO_StubGenerator/)) to generate documentation, API code for service consumers and providers, and possible other representations.


MOSDL language
--------------

The MOSDL language is documented in [MOSDL.md](MOSDL.md). Syntax highlighting is provided for [Notepad++](https://notepad-plus-plus.org/) users: Just load [`npp_mosdl.xml`](npp_mosdl.xml) in Notepad++ using its user-defined language functionality.


Usage
-----

This project can be used in three different ways:

1. as a command-line interface (CLI) program for transforming MOSDL to CCSDS MO XML files and vice versa,
2. as a plugin for a [Maven](https://maven.apache.org/)-based build process in order to transform MOSDL to/from CCSDS MO XML,
3. as a Java library for reading and writing MOSDL and CCSDS MO XML files.

You need Java 8 JDK (e.g. [AdoptOpenJDK](https://adoptopenjdk.net/)) if you use this project as Maven plugin or Java library. The stand-alone program should also run fine with a Java 8 JRE. Currently, Java versions other than Java 8 are not supported.


### Stand-alone usage

Releases of the MOSDL standalone CLI program are avaiable on the [GitHub Releases page](https://github.com/DLR-RB/MOSDL/releases).

Unzip the standalone command-line interface distribution package and invoke the compiler from the command line like:

```
java -jar mosdl.jar --help
```

This will print detailed usage instruction which are repeated here for convenience:

```
Compiler for MOSDL - Mission Operations Service Description Language for CCSDS MO Services (version ${project.version})

Usage: <service-descr> [<target-dir>] [-x|--xml] [-m|--mosdl] [-sv|--skip-validation] [-t|--doc-type <mosdl-doc-type>] [-h|--help]

<service-descr>
        MO service description file (CCSDS MO XML or MOSDL service description language) or directory containing files in MOSDL service description language.
        (required)
<target-dir>
        Target directory for generated files.
        Default: .
-x, --xml
        If given MO XML service description file will be generated.
-m, --mosdl
        If given MOSDL service description file will be generated.
-sv, --skip-validation
        If given try to recover from MOSDL files with errors and do not validate XML input and output files against the service schema. Useful for slightly malformed files.
-t, --doc-type <mosdl-doc-type>
        Type of documentation to create for MOSDL files (BULK: operation documentation will be generated in bulk; INLINE: operation documentation will be put in-line; SUPPRESS: all documentation is stripped).
        Possible values: BULK, INLINE, SUPPRESS
        Default: BULK
-h, --help
        Print detailed usage instructions for this program.
```


### Usage as plugin in a Maven-based build process

The Maven plugin is available on *Maven Central*. Download the [JAR file](https://search.maven.org/remote_content?g=${project.groupId}&a=${project.artifactId}&v=${project.version}) or add a build plugin dependency to your Maven `pom.xml` file:

```xml
<plugin>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}</artifactId>
    <version>${project.version}</version>
    <configuration>
        <serviceSpecs>
            <serviceSpec>src/main/mosdl</serviceSpec>
        </serviceSpecs>
        <xml>true</xml>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Reference all your service description XML/MOSDL files or directories in the `<serviceSpecs>` tag (multiple `<serviceSpec>` tags are allowed here). It is advisable to put your service specifications in the `src/main/mosdl` directory. The generated files will be put in the `generated-sources/mosdl` subfolder of your target folder.

The following configuration options are available:

* `<serviceSpecs>`: Required. List the service specification files you wish to generate code for here. You can specify a service description XML file here, a single MOSDL file or a directory containing multiple MOSDL files that together form a single service specification.
* `<xml>`: Optional. Set to `true` if you want to generate the service description XML files.
* `<mosdl>`: Optional. Set to `true` if you want to generate the service description MOSDL files.
* `<skipValidation>`: Optional. Set to `true` if you want to skip validation of XML input and output files against the service schema.
* `<mosdlDocType>`: Optional, one of `BULK` (default), `INLINE` or `SUPPRESS`. Use`BULK` to create bulk operation documentation using tags for messages, parameters and errors. Use `INLINE` if you want to attach documentation directly to these elements instead of using special tags. Set to `SUPPRESS` if you want to strip documentation when creating MOSDL files. Has no effect if no MOSDL files are created.


### Usage as a Java library

The Java library is available on *Maven Central*. Download the [latest JAR file](https://search.maven.org/remote_content?g=${groupId}&a=${project.artifactId}&v=LATEST) or add a dependency to your Maven `pom.xml` file:

```xml
<dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}</artifactId>
    <version>${project.version}</version>
</dependency>
```

Use cases for this library are loading specifications, writing specifications and glueing loading and writing together. This document shall just give a hint where to start. Please refer to the code documentation for detailed instructions on how to use the classes provided in this project.


#### Load a specification

Two `SpecLoader` implementations are shipped that allow you to load a specification from MOSDL (`MosdlSpecLoader`) or XML (`XmlSpecLoader`).

Example:

```java
SpecLoader specLoader = new MosdlSpecLoader(false);
specLoader.setInput(inputFileOrDirectory);
if (specLoader.isLoadable()) {
    SpecificationType spec = loader.load();
}
```


#### Write a specification

Two `Generator` implementations are shipped that allow you to write a specification to MOSDL (`MosdlGenerator`) or XML (`XmlGenerator`).

Example:

```java
Generator generator = new MosdlGenerator(false);
generator.generate(spec, targetDirectory);
```


#### Glueing together loading and writing of a specification

An abstract `Runner` class is provided with an `execute()` method that loads a set of `SpecLoader`s, tries to load a specification with one of them, loads a set of `Generator`s and writes out the loaded specification using all of them. Methods `createSpecLoaders()` and `createGenerators()` need to be implemented by a subclass. A concrete subclass `MosdlRunner` is provided that creates all of the above mentioned specification loaders and generators. If you want to include your own specification loader or generator you can subclass `Runner` (or `MosdlRunner`) yourself.


Source code and compilation
---------------------------

### Source code

The source code of this project is available at GitHub: https://github.com/DLR-RB/MOSDL

Use [git](https://git-scm.com/) to clone the source repository:

```
git clone https://github.com/DLR-RB/MOSDL.git
```


### Compilation

The only requirements for compiling the project yourself are a Java 8 JDK (e.g. [AdoptOpenJDK](https://adoptopenjdk.net/)) and [Maven 3](https://maven.apache.org/). Currently, Java versions other than Java 8 are not supported.

This project uses a standard Maven-based build process. You can compile the source yourself by simply issuing 

```
mvn install
```

in the project base directory. Required dependencies need to be resolved by Maven, e.g. by getting them from [Maven Central](https://search.maven.org/). You will find the compiled packages in the `target` directory as well as your local Maven repository.


Contributing
------------

Contributions to this project are welcome. You may open issues, fix or expand documentation, provide new functionality or create more and better tests. If you have a minor contribution you can open a pull request. For any major contribution please open an issue first or discuss with the repository owner. Please also note that you need to fill out and sign a [contributor license agreement](DLR%20Individual%20Contributor%20License%20Agreement.pdf).


Maintainer
----------

The current maintainer of MOSDL is [Stefan Gärtner (@S-Gaertner)](https://github.com/S-Gaertner).


Copyright, license and third party software
-------------------------------------------

### Copyright

Mission Operations Service Description Language and Compiler (MOSDL)

Copyright 2019 DLR - Deutsches Zentrum für Luft- und Raumfahrt e.V.

This product was developed at DLR - GSOC (German Space Operations Center at the German Aerospace Center DLR, https://www.dlr.de/).


### License

MOSDL is licensed under the [Apache License, Version 2.0](LICENSE.txt).


### Third party software in binary distributions

The command-line interface binary distribution of MOSDL comes bundled with a number of third party software components. Please refer to [THIRD-PARTY.txt](THIRD-PARTY.txt) for notices, licenses and additional terms and conditions of these components.
