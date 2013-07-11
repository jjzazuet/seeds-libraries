seeds-libraries
===============

> *Not the whole guava ;)*

Atomic jars for [guava-libraries 14.0](https://code.google.com/p/guava-libraries/wiki/Release14). 

> DISCLAIMER: No source files/package names were altered, just reshuffled into atomic `jar` files corresponding to each major library component. Also, no fruits were harmed in the making of these libraries ;).

## Why fork from Guava?

As discussed in issue [Guava Issue 605](https://code.google.com/p/guava-libraries/issues/detail?id=605), there are a lot of classes that in some cases, library and framework authors may not want to include with their binary product.

While it is also true that such authors may want to use tools such as [Proguard](http://proguard.sourceforge.net/) to minimize the size of their binary bundle, it implicitly requires another build time step in their process and also makes it more difficult to trace the cause of a bug back to its original source code during a debug session.

In the end, I believe this is only a matter of preference. And my preference is to use only what I need to. Not more, not less. 

The `seeds-libraries` project is an attempt to build and publish atomic versions of the Guava Libraries which are too valuable to leave out of any modern development project.

## Warning

As discussed in [Guava Issue 605](https://code.google.com/p/guava-libraries/issues/detail?id=605), mixing the seeds libraries with the original Guava libraries may lead to unexpected classpath/runtime errors. I have personally seen this in personal projects where for example, Gradle's DSLD support silently introduces a copy of the Guava libraries version 11 in the classpath of a project.

You have to be careful and aware of which set of classes will be used in your project (Guava's or seeds'). 

## Usage

Use [Gradle 1.6](http://gradle.org "Gradle 1.6") to compile the source code.

    gradle clean assemble

Upload to your local Maven repository.

    gradle install

The following artifacts will be available:

    seeds-base
    seeds-cache
    seeds-collect
    seeds-eventbus
    seeds-functional
    seeds-hash
    seeds-io
    seeds-math
    seeds-net
    seeds-primitives
    seeds-reflect
    seeds-util

Use an artifact like so:

    <dependency>
        <groupId>com.google.seeds</groupId>
        <artifactId>seeds-eventbus</artifactId>
        <version>14.0</version>
    </dependency>

Enjoy.

###Things to do.

With any luck, these could make it into Maven Central. If that's the case, here are some things left to do:

- Port the unit tests.
- Decouple some subcomponents which have only *one* class calling a method in another library component.
