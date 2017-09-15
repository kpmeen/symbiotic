Symbiotic - File Management
=================================
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Join the chat at https://gitter.im/scalytica/symbiotic](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kpmeen/symbiotic)
[![build status](https://gitlab.com/kpmeen/symbiotic/badges/master/build.svg)](https://gitlab.com/kpmeen/symbiotic/commits/master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/83d503edeba943829ed81bdde1c67c2c)](https://www.codacy.com/app/kp/symbiotic?utm_source=github.com&utm_medium=referral&utm_content=kpmeen/symbiotic&utm_campaign=Badge_Grade)
[![Download](https://api.bintray.com/packages/kpmeen/maven/symbiotic-core/images/download.svg) ](https://bintray.com/kpmeen/maven/symbiotic-core/_latestVersion)

The project is split into several distinct modules.

* `symbiotic-shared` - Shared code and traits required by all libraries.
* `symbiotic-core` - Provides the core document mangement .functionality.
* `symbiotic-fs` - Provides peristent storage of files to the filesystem
* `symbiotic-json` - Provides JSON conversions for the data types in the `symbiotic-shared` lib.
* `symbiotic-mongodb` - Provides persistent storage of files and metadata using MongoDB (with GridFS)
* `symbiotic-postgres` - Provides persistent storage of metadata using postgres, and files using the `symbiotic-fs` library.
* `symbiotic-play` - Provides Play! Framework JSON formatters and a Guice module to use in a Play! Framework application.
* `symbotic-testkit` - Test specs to verify different persistence implementations, etc.
* `symbiotic-server` - Play! Framework reference implementation. 
* `symbiotic-client` - Sample client application in Scala-JS.

### Sample implementation
To see an example of how the symbiotic core libraries can be used, please refer to the modules `symbiotic-client` and `symbiotic-server`. Here you'll find an implementation of a simple document management system using the available features in Symbiotic.

The client started off as an experiment in using [scala-js](http://www.scala-js.org) with a large'ish codebase. To which the conclusion is a big thumbs up.

## Development requirements

### Environment

* A computer (!) preferrably *nix based.
* JDK 1.8 or higher
* [SBT](http://www.scala-sbt.org)
* Docker
* Latest version of Nginx or some other proxy that supports streaming uploads.

### Starting Databases

The project provides a convenience script to bootstrap the necessary databases. For this to work, please ensure docker is installed on the machine. Then run the following script:

```
./backends.sh start
```

To stop the database containers:

```
./backends.sh stop
```

To clean the databases (in case a fresh start is desired):

```
./backends.sh clean
```

### NOTES ABOUT TESTING
Ensure that the database instances are running before executing the tests. Otherwise tests will fail miserably.

##### Social authentication
The social authentication config is located in the file `symbiotic-server/conf/silhouette.conf`. If you want to use any of these
(e.g. Google), ensure you follow the instructions for the appropriate API on how to get the necessary clientId and secret.
Do _not_ commit your keys to the source repository. Instead you should export them as environment variables. An example
for google would be:

```bash
export GOOGLE_CLIENT_ID="theclientid"
export GOOGLE_CLIENT_SECRET="thesecret"
```

##### Testdata
To load some test data into the database, you can run the following command from `sbt`:

```scala
test:runMain util.testdata.TestDataLoader
```

## Contributing
All contributions should be made as pull requests to the master branch.

--
Copyright (c) Knut Petter Meen, All rights reserved. [scalytica.net](http://scalytica.net)
