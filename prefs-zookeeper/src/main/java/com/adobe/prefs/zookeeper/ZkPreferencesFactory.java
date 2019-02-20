package com.adobe.prefs.zookeeper;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.PreferencesFactory;

/**
 * PreferencesFactory implementation that returns preferences stored in zookeeper.
 * This class will be instantiated by the service loader when the Preferences are first used.
 */
public class ZkPreferencesFactory implements PreferencesFactory {
    private static final Logger logger = LoggerFactory.getLogger(ZkPreferencesFactory.class);

    private final Supplier<ZkPreferences> userRootSupplier;
    private final Supplier<ZkPreferences> systemRootSupplier;

    public ZkPreferencesFactory() {
        this(systemRootPath(), userRootPath());
    }

    protected ZkPreferencesFactory(String systemRootPath, String userRootPath) {
        final boolean encodedBinary = Boolean.parseBoolean(System.getProperty("prefs.zk.binary.base64_encoded", "true"));
        logger.info("Zookeeper prefs factory initialized with system root: {} and user root: {}",
                systemRootPath, userRootPath);
        userRootSupplier = Suppliers.memoize(prefsSupplier(userRootPath, encodedBinary, true));
        systemRootSupplier = Suppliers.memoize(prefsSupplier(systemRootPath, encodedBinary, false));
    }

    private Supplier<ZkPreferences> prefsSupplier(String path, final boolean encodedBinary, final boolean userNode) {
        return Suppliers.compose(curator -> new ZkPreferences(curator, encodedBinary, userNode),
                ZkManager.curatorFacadeSupplier(path));
    }

    @Override
    public ZkPreferences systemRoot() {
        return systemRootSupplier.get();
    }

    @Override
    public ZkPreferences userRoot() {
        return userRootSupplier.get();
    }

    static String systemRootPath() {
        return System.getProperty("java.util.prefs.systemRoot", "/prefs/sys");
    }

    static String userRootPath() {
        return System.getProperty("java.util.prefs.userRoot",
                "/prefs/usr/" + System.getProperty("user.name"));
    }
}
