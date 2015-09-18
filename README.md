[![License](http://img.shields.io/:license-mit-blue.svg)](http://doge.mit-license.org)

Symbiotic
========

For a more detailed view of the project please see the [wiki](https://github.com/kpmeen/symbiotic-server/wiki) pages.

## Open issues:

* See [issues](https://github.com/kpmeen/symbiotic-server/issues) list

## Development requirements

### Environment

* A computer (!) preferrably *nix based.
* JDK 1.7 or higher
* [Typesafe Activator](https://www.typesafe.com/activator/download)
* MongoDB 3.0.x

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

**NOTE ABOUT TESTING**: If there is no running MongoDB, the tests relying on MongoDB will boot up an embedded instance to use. This slows down test execution a little bit. So if you want fast tests...run a standalone MongoDB instance.
At the moment there is an issue with the MongoRunner that boots up a mongod when using v 3.x.x. So be safe and rund standalone!

### Contributing
TBD...

--
Copyright (c) Knut Petter Meen, All rights reserved. [scalytica.net](http://scalytica.net)
