Symbiotic - File Management
=================================
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Join the chat at https://gitter.im/scalytica/symbiotic](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kpmeen/symbiotic?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Shippable](https://api.shippable.com/projects/5673eefd1895ca4474669840/badge?branch=master
)](https://app.shippable.com/projects/5673eefd1895ca4474669840/status)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/7fa8070d0e5a4716a6d0b648716b83eb)](https://www.codacy.com) [![Codacy Badge](https://api.codacy.com/project/badge/coverage/7fa8070d0e5a4716a6d0b648716b83eb)](https://www.codacy.com)

This project is still under development and lacks some features. But as it stands, Symbiotic is fully functional if what you need is a per user file management system. Immmediate features on the plan includes implementing support for organising users into groups, and sharing files between users.

The _main_ part of the project can be found in the [server](https://github.com/kpmeen/symbiotic/tree/master/server) folder. If what you're looking for is a backend to handle files and documents for your own UI/client, then that's where to look. The functionality is exposed in a JSON based REST API.

The [client](https://github.com/kpmeen/symbiotic/tree/master/client) part of the project started off as an experiment in using [scala-js](http://www.scala-js.org) with a large'ish codebase. The conclusion to that experiment is a big thumbs up. The client can be used as a reference for how the API's are used. But it shouldn't be considered to be _the_ client. It is highly likely that the implementation will change a lot. Partly due to lessons learned the first time around using scala-js, but also due to planned features.

## Development requirements

### Environment

* A computer (!) preferrably *nix based.
* JDK 1.8 or higher
* [SBT](http://www.scala-sbt.org) or [Typesafe Activator](https://www.typesafe.com/activator/download)
* MongoDB 3.2 or higher
* Latest version of Nginx or some other proxy that supports streaming uploads.

### Unpublished dependencies
This project has a dependency to a forked, unpublished, version of uPickle.
Please ensure you clone and `sbt publishLocal` the following repository: [KP uPickle](https://github.com/kpmeen/upickle)

### MongoDB

#### Using Docker
In the root project directory (which is also where this file is located), there is a script called `docker-mongo.sh`.
To use this you will need to have docker installed on you machine. The script will pull down the latest MongoDB image,
and start up a container called `symbiotic-mongo`. The container will expose the MongoDB default port to the host
system. The script will also create a directory `.mongodb-files` in the project root directory to be used as a mounted
volume in docker.

The first time you start the image, you will need to initialise the replica-set.

```bash
docker exec -it symbiotic-mongo mongo
```
Which will bring you into the mongo shell. Now initialise the replica set:

```bash
rs.initiate()
```

And to verify the replica set is initialised correctly:

```bash
rs.status()
```

If the replica set is _not_ enabled (may take a second or two), please refer to the MongoDB documentation for
trouble-shooting.

#### Locally installed MongoDB instance
Here's a useful startup script for MongoDB. Ensure that the directory ```mongodb-files``` is present in the directory
where the script lives before executing it.

```bash
#!/bin/bash

ulimit -n 1024
mongod --quiet --dbpath=mongodb-files --replSet rs0
```

### NOTES ABOUT TESTING
Ensure that a mongodb instance is running before executing the tests. Otherwise tests will fail miserably.

##### Social authentication
The social authentication config is located in the file `server/conf/silhouette.conf`. If you want to use any of these
(e.g. Google), ensure you follow the instructions for the appropriate API on how to get the necessary clientId and secret.
Do _not_ commit your keys to the source repository. Instead you should export them as environment variables. An example
for google would be:

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
