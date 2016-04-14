Symbiotic - Document Management
=================================
[![Join the chat at https://gitter.im/scalytica/symbiotic](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalytica/symbiotic?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/7fa8070d0e5a4716a6d0b648716b83eb)](https://www.codacy.com) [![Codacy Badge](https://api.codacy.com/project/badge/coverage/7fa8070d0e5a4716a6d0b648716b83eb)](https://www.codacy.com)

For a more detailed view of the project please see the [wiki](https://github.com/kpmeen/symbiotic/wiki) pages.

## Open issues:

* See [issues](https://github.com/kpmeen/symbiotic/issues) list

## Development requirements

### Environment

* A computer (!) preferrably *nix based.
* JDK 1.8 or higher
* [Typesafe Activator](https://www.typesafe.com/activator/download) or [SBT](http://www.scala-sbt.org)
* MongoDB 3.2 or higher
* Latest version of Nginx or some other proxy that supports streaming uploads.

### Unpublished dependencies
This project has a dependency to a forked, unpublished, version of uPickle.
Please ensure you clone and `sbt publishLocal` the following repository: [KP uPickle](https://github.com/kpmeen/upickle)

### Building
Build using the regular activator commands.

Please read the [scalajs documentation](scalajs-lang) for details on how to build and run scalajs applications.
**To run activator (a.k.a. sbt console)**
 
```bash
activator 
```

All the following commands are available within the activator console 

```bash
# Compiling...
compile
test:compile

# Optionally leave test databases in place after test execution
# IMPORTANT: Tests may break if you do not clean the databases before a new test run.
set javaOptions += "-Ddb.preserve=true"

# Run all tests
test

# Run single test spec
testOnly the.package.MySpec

# Run single example in test spec
testOnly the.package.MySpec -- -ex "The text in the example between should and in" -

# Run play application
run

# Run play application with continous re-compile enable
~run

# Compile client
fastOptJS

# Continous compile of client 
~fastOptJS

```

For more details see the appropriate sbt/activator/play documentation.

### MongoDB
Here's a useful startup script for MongoDB. Ensure that the directory ```mongodb-files``` is present in the directory where the script lives before executing it.

```bash
#!/bin/bash

ulimit -n 1024
mongod --quiet --dbpath=mongodb-files --replSet rs0
```

### NOTES ABOUT TESTING
Ensure that a mongodb instance is running before executing the tests. Otherwise tests will fail miserably.

##### Social authentication
The social authentication config is located in the file `server/conf/silhouette.conf`. If you want to use any of these (e.g. Google), ensure you follow the instructions for the appropriate API on how to get the necessary clientId and secret. Do _not_ commit your keys to the source repository. Instead you should export them as environment variables. An example for google would be:
```bash
export GOOGLE_CLIENT_ID="theclientid"
export GOOGLE_CLIENT_SECRET="thesecret"
```

##### Testdata
To load some test data into the database, you can run the following command from `activator`:

```scala
test:runMain util.testdata.TestDataLoader
```

## Contributing
All contributions should be made as pull requests to the master branch.

--
Copyright (c) Knut Petter Meen, All rights reserved. [scalytica.net](http://scalytica.net)
