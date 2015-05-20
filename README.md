Cross Preferences
=================

## Overview

Java Preferences SPI implementations that enable using _distributed configuration stores_
via a simple (and standard) API.

As a bonus, this project also contains a simple REST console for preferences management,
no matter what preferences backing store is used.

### Introduction

The goal is to allow Java (or any JVM language) developers to make use of highly available,
distributed configuration stores without the need to integrate with them directly.

Out of the box, the Java Preferences API has platform-dependent backing stores that make use of registries
(on Windows machines), file system (on Unix machines) and so on.

In highly available cluster applications, the shared configuration eventually gets stored in dedicated
distributed stores, like the open-source [Zookeeper](http://zookeeper.apache.org/),
[etcd](https://coreos.com/using-coreos/etcd/) or [Consul](https://consul.io/);
but such an integration is usually pretty intrusive for the application code
and everyone going that route ends up writing their own abstraction library.

However, what all these systems have in common is that they provide a tree-structure key/value
store with the ability for clients to register as listeners for "directories" and/or "keys".
As this functionality is already fully covered by the Preferences API, it seems natural to integrate these
solutions as preferences backing stores in order to make this functionality easily accessible from Java.

Of course, each of these distributed config stores comes with a lot of extra features (often, more important
than the configuration management itself): leader election, service discovery and so on.
But such use cases are deliberately left outside the scope of this project.

### Project structure

The project contains modules that integrate each of the modern config stores mentioned above with the Preferences API.

A REST console (prefs-admin) is also included in order to provide a uniform admin interface for any such config store.

There is also a shared module for testing which includes utilities for both SPI and API users.

Each of the modules comes with its own README file covering all the details.

## Usage

Each distributed storage will be implemented in its own module and (under normal circumstances) only one module will
be linked by any application.

In order to make use of it, one will only need to have it present in the JVM classpath (and provide any configuration
settings via system properties), since the modules integrate with the preferences API via the `ServiceLoader` mechanism.

However, if more than one prefs jar is present in the classpath for some reason, the actual implementation to be used
can be decided by setting the `java.util.prefs.PreferencesFactory` system property.


The application is supposed to only work with standard preferences with no awareness of the underlying store:

    // Returns a preferences node with the path ${userRoot}/com/adobe/mypackage
    Preferences prefs = Preferences.userNodeForPackage(MyClass.class);
    prefs.getInt("ttl", 100); // get the preference value for "ttl" or use 100 as default
    prefs.addPreferenceChangeListener(this); // notifications for preference value changes
    prefs.addNodeChangeListener(this); // notifications for children lifecycle changes

## Limitations

Although it has a brilliant author, the Java Preferences API is (1) pretty old and (2) not really meant to be used
in a multi-JVM environment.

One limitation that needs to be highlighted is the fact that you can only use *one* implementation per JVM
if you want to stick to the standard usage patterns.

This doesn't seem to be a showstopper, since it's not that common that different parts of an application would
make use of different configuration stores. However, several aspects require special attention for this reason:
 - The preferences lifecycle starts with the first usage ("lazy initialization", on the first load of the class
 `java.util.Preferences`), so any required setup (like programatically setting system properties) must be performed
  before that. Also, except for node removal, cleanup operations can only be registered as shutdown hooks.
 - In "plug-in" environments, the service loader will not be able to locate directives in child class loaders;
  setting the factory as a system property will still work (due to a hack in `java.util.prefs.Preferences`. However,
  the safest way would be to make the preferences SPI jar available in the classpath of the main application (web
  container or whatever).
