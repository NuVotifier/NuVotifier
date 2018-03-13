This document outlines various developer resources concerning NuVotifier.

# Contributing

NuVotifier is an open source GNU GPLv3 licensed project. We accept contributions
through pull requests, and will make sure to credit you for your gracious
contribution.

# Building NuVotifier

NuVotifier can be built by running the following: `./gradlew build` or
`./gradlew.bat build` on Windows. The resultant universal jar is built and
written to `universal/build/libs/nuvotifier-{version}.jar`.

The build directories can be cleaned instead using the `./gradlew clean` (and
`./gradlew.bat build`) command.

# NuVotifier as a plugin dependency

ParallelBlock now maintains an independent Maven repository for free use by the
community. Builds are automatically published by a CI operated by ParallelBlock.

## Maven

You can have your project depend on NuVotifier as a dependency through the
following code snippets:

```
<project>
...
    <repositories>
        <repository>
            <id>parallelblock-public</id>
            <name>ParallelBlock Public Repository</name>
            <url>https://repo.parallelblock.com/repository/maven-public/</url>
        </repository>
        ...
    </repositories>
...
    <dependencies>
        <dependency>
            <groupId>com.vexsoftware</groupId>
            <artifactId>nuvotifier-universal</artifactId>
            <version>...</version>
            <scope>provided</scope>
        </dependency>
        ...
    </dependencies>
...
</project>
```

## Gradle

You can include NuVotifier into your gradle project using the following lines:

```
...
repositories {
    maven {
        url 'https://repo.parallelblock.com/repository/maven-public/'
    }
    ...
}
...
dependencies {
    compileOnly "com.vexsoftware:nuvotifier-universal:verison"
    ...
}
...
```

# Implementing NuVotifier v2 protocol in a server list

NuVotifier provides production ready reference implementations for interacting
with NuVotifier servers. These libraries are available below:

- [NodeJS](https://github.com/NuVotifier/votifier2-js)
- [PHP](https://github.com/NuVotifier/votifier2-php)
- [Golang](https://github.com/minecrafter/go-votifier)
