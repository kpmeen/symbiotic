# Document Management

**NOTE: This package should be extractable to a separate lib eventually.**

The gist of this package is to contain the implementation of a simple Document Management solution. In essence it is much like the workflow package in that it is not system specific. But could be used as a generic document management library. For which you would obviously expose your own services and UI.

* Making it depend on the Play framework is OK.
* Making it depend on some other types in the model is OK (these can later be extracted too).
* It _will_ depend on MongoDB!!
* And it _will_ (eventually) depend on ElasticSearch!
