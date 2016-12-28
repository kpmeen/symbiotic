Symbiotic - File Management
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
