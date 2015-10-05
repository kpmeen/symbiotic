Symbiotic - Document Management System
=================================

For a more detailed view of the project please see the [wiki](https://github.com/kpmeen/symbiotic-server/wiki) pages.

## Open issues:

* See [issues](https://github.com/kpmeen/symbiotic-server/issues) list

## Development requirements

### Environment

* A computer (!) preferrably *nix based.
* JDK 1.7 or higher
* [Typesafe Activator](https://www.typesafe.com/activator/download)
* MongoDB 3.0.x
* Latest version of Tengine (Nginx will work fine, but doesn't support streaming uploads as easily)

### Building
Build using the regular activator commands.

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

To load some test data into the database, you can run the following command from `activator`:

```scala
test:runMain util.testdata.TestDataLoader
```

### Contributing
TBD...

--
Copyright (c) Knut Petter Meen, All rights reserved. [scalytica.net](http://scalytica.net)
