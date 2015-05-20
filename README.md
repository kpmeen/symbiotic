Symbiotic - Human Interaction System
=================================

For a more detailed view of the project please see the [wiki](https://gitlab.com/scalytica/symbiotic-server/wikis/home) pages.

## Open issues:

* See [gitlab issues](https://gitlab.com/scalytica/symbiotic-server/issues) list

## Development requirements

### Environment

* A computer (!) preferrably *nix based.
* JDK 1.7 or higher
* [Typesafe Activator 1.3.2](http://downloads.typesafe.com/typesafe-activator/1.3.2/typesafe-activator-1.3.2.zip)
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