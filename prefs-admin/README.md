Preferences Admin Console
=========================

A REST console for preferences management. This is just an HTTP interface for the Java Preferences,
so it can work with any configured backing store.

It does include all the other modules, so it only needs to be configured with the desired implementation
and the required system properties for that implementation and it's good to go:

    java -D java.util.preferences.PreferencesFactory=${DESIRED_IMPLEMENTATION} -DrequiredProp=desiredValue \
        -jar prefs-admin-${VERSION}.jar

It will by default listen on the `8910` (eight-nine-ten) port, but this can be changed via the optional port
parameter.

    java -jar prefs-admin-${VERSION}.jar $PORT

The API produces [HAL](http://stateless.co/hal_specification.html) payloads for both XML and JSON.
Moreover, it can produce XHTML for manual management. Even though the XHTML output can be used as
a web interface, this is not meant to be a real UI.

## API Specification

The API exposes two roots (`sys` and `usr`) that map to the preferences `systemRoot` and `userRoot`.
Nodes are referenced by an URL path ending with a `/`. Paths that do not end with a `/` will be considered keys.

The supported operations are:

- PUT: create a node or set a key to a value
- GET: get the children of a node or get the value of some key
- DELETE: remove a node or remove a key
