package com.adobe.prefs.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class InMemoryPreferencesFactory implements PreferencesFactory {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryPreferencesFactory.class);

    private final Preferences systemRoot = new InMemoryPreferences();
    private final Preferences userRoot = new InMemoryPreferences();

    public InMemoryPreferencesFactory() {
        logger.info("Initialized");
    }

    @Override
    public Preferences systemRoot() {
        return systemRoot;
    }

    @Override
    public Preferences userRoot() {
        return userRoot;
    }

}
