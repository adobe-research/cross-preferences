package com.adobe.prefs.zookeeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.PreferencesFactory;

/**
 * PreferencesFactory implementation that returns preferences stored in zookeeper.
 * This class will be instantiated by the service loader when the Preferences are first used.
 */
public class ZkPreferencesFactory implements PreferencesFactory {
    private static final Logger logger = LoggerFactory.getLogger(ZkPreferencesFactory.class);

    private final ZkPreferences userRoot;
    private final ZkPreferences systemRoot;

    public ZkPreferencesFactory() {
        this(systemRootPath(), userRootPath());
    }

    protected ZkPreferencesFactory(String systemRootPath, String userRootPath) {
        final boolean encodedBinary = Boolean.parseBoolean(System.getProperty("prefs.zk.binary.base64_encoded", "true"));
        logger.info("Zookeeper prefs factory initialized with system root: {} and user root: {}",
                systemRootPath, userRootPath);
        userRoot = new ZkPreferences(ZkManager.curatorFacade(userRootPath), encodedBinary, true);
        systemRoot = new ZkPreferences(ZkManager.curatorFacade(systemRootPath), encodedBinary, false);
    }

    @Override
    public ZkPreferences systemRoot() {
        return systemRoot;
    }

    @Override
    public ZkPreferences userRoot() {
        return userRoot;
    }

    static String systemRootPath() {
        return System.getProperty("java.util.prefs.systemRoot", "/prefs/sys");
    }

    static String userRootPath() {
        return System.getProperty("java.util.prefs.userRoot",
                "/prefs/usr/" + System.getProperty("user.name"));
    }
}
