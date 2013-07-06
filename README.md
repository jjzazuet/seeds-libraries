seeds-libraries
===============

> *Not the whole guava ;)*

Atomic jars for [guava-libraries 14.0](https://code.google.com/p/guava-libraries/wiki/Release14). 

No source files/package names were altered, just reshuffled into atomic `jar` files corresponding to each major library component.

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
