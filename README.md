seeds-libraries
===============

> *Not the whole guava ;)*

Atomic jars for [guava-libraries 16.0.1](https://code.google.com/p/guava-libraries/wiki/Release16). 

> DISCLAIMER: With the exception of package naming, no source files were altered, just reshuffled into atomic source trees corresponding to each major library component. Also, no fruits were harmed in the making of these libraries ;).

## Why fork from Guava?

As discussed in issue [Guava Issue 605](https://code.google.com/p/guava-libraries/issues/detail?id=605), there are a lot of classes that in some cases, library and framework authors may not want to include with their binary product.

While it is also true that such authors may want to use tools such as [Proguard](http://proguard.sourceforge.net/) to minimize the size of their binary bundle, it implicitly requires another build time step in their process and also makes it more difficult to trace the cause of a bug back to its original source code during a debug session.

In the end, I believe this is only a matter of preference. And my preference is to use only what I need to. Not more, not less. 

The `seeds-libraries` project is an attempt to build and publish atomic versions of the Guava Libraries which are too valuable to leave out of any modern development project.

## Warning

As discussed in [Guava Issue 605](https://code.google.com/p/guava-libraries/issues/detail?id=605), mixing the seeds libraries with the original Guava libraries may lead to unexpected classpath/runtime errors. I have personally seen this in personal projects where for example, Gradle's DSLD support silently introduces a copy of the Guava libraries version 11 in the classpath of a project.

Therefore, I have decided to introduce an intentional package break and leave all the classes under the main package `net.tribe7` in order to minimize the amount of classpath conflicts. You have to be careful and aware of which set of classes will be used in your project (Guava's or seeds').

## Usage

Release 16.0.1 is now on Maven Central. Use an artifact like so:

    <dependency>
        <groupId>net.tribe7.seeds</groupId>
        <artifactId>seeds-eventbus</artifactId>
        <version>16.0.1</version>
    </dependency>

The following artifacts are available.

	seeds-base
	seeds-cache
	seeds-collect
	seeds-escape
	seeds-eventbus
	seeds-functional
	seeds-hash
	seeds-html
	seeds-io
	seeds-math
	seeds-net
	seeds-primitives
	seeds-reflect
	seeds-strings
	seeds-util
	seeds-xml


## Compiling from source.

Use [Gradle 1.6](http://gradle.org "Gradle 1.6") to compile the source code.

    gradle clean assemble

You can then upload the artifacts to your local Maven repository.

    gradle install

Enjoy.
