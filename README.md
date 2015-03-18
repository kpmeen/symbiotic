Copr8 - Human Interaction System
=================================

For a more detailed view of the project please see the [wiki](https://github.com/kpmeen/copr8/wiki) pages.


## Development requirements

* A computer (!) preferrably *nix based.
* JDK 1.7 or higher
* [Typesafe Activator 1.3.2](http://downloads.typesafe.com/typesafe-activator/1.3.2/typesafe-activator-1.3.2.zip)
* MongoDB 2.6.8 or higher

## Building
Build using the regular activator commands.

**To run activator (a.k.a. sbt console)**
 
```
activator 
```

All the following commands are available within the activator console 

```
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


## Contributing
TBD...

--
Copyright (c) Knut Petter Meen, All rights reserved. [scalytica.net](http://scalytica.net)
