Symbiotic Web
======================

## Development requirements

### Environment

* A computer (!) preferrably *nix based.
* JDK 1.8 or higher
* [Typesafe Activator](https://www.typesafe.com/activator/download) (or plain SBT)
* MongoDB 3.0.x
* Latest version of Tengine (Nginx will work fine, but doesn't support streaming uploads as easily)

### Unpublished dependencies
This project has a dependency to a forked, unpublished, version of uPickle.
Please ensure you clone and `activator publishLocal` the following repository: [KP uPickle](https://github.com/kpmeen/upickle)

### Building

Please read the [scalajs documentation](scalajs-lang) for details on how to build and run scalajs applications.
