# README


[![Build](https://github.com/mcgivrer/Platform2D/actions/workflows/build.yml/badge.svg)](https://github.com/mcgivrer/Platform2D/actions/workflows/build.yml) [![CodeQL](https://github.com/mcgivrer/Platform2D/actions/workflows/codeql.yml/badge.svg)](https://github.com/mcgivrer/Platform2D/actions/workflows/codeql.yml) [![Deploy GitHub Pages](https://github.com/mcgivrer/Platform2D/actions/workflows/jekyll-gh-pages.yml/badge.svg)](https://github.com/mcgivrer/Platform2D/actions/workflows/jekyll-gh-pages.yml)


## Context

This Platform2D class is an optimized mono class 2D platform game framework bringing all the required basic features to
implement a basic java game.

## Build

This project is free of any build tools but `java` and `javac`! :)
A simple `build.sh` script is used to build java code, create a jar and an auto-runnable linux compatible script.

```bash
build.sh a
```

> [!TIP]
> You can use the `build.sh h` to get help on this script arguments.
> The produced build delivers a javadoc jar and sources._

## Execute

you can execute the produced jar with the following line :

```bash
build.sh r
```

or you can execute it from the java command:

```bash
java -jar target/platform2d-1.0.0.jar
```

or, if you are a linux based guy, or a Windows constrained developer, but using git-bash:

```bash
target/build/platform2d-1.0.0.run
```

![Screen captuire from the 1.0.0 release](src/main/docs/illustrations/platform2d-1.0.0.png "Platform2D release 1.0.0")

## Contribute

If you want to bring your own knowledge, you can fork this repository and create some pull request proposals to add new
features or refactor things ;)

Keep in mind that this code required to be simple, compact and less than 3000 lines :)

## License

This code is licensed through the well-known MIT license.

That's all folk!

McG.
