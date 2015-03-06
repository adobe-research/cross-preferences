Preferences Testing
=====================

This module serves two purposes, as it's `/src/main` and `/src/test` directories address different target audiences:

* Application testing support (`/src/main`):
    * a convenient in-memory preferences implementation suitable for development and unit testing
* SPI implementation testing support (`/src/test`):
    * a base class for preferences tests containing utilities for namespacing and event listening
    * more importantly, a **preferences acceptance test suite** to be *included as a reference test* in every prefs implementation module

Application developers that make use of `java.util.prefs` in their code should therefore only be interested
in the main artifact produced by this module. The `tests` artifact is only useful in the scope of this project itself.
