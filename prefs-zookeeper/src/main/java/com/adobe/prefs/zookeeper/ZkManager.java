package com.adobe.prefs.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.prefs.zookeeper.ZkUtils.namespace;

/**
 * Class responsible with creating (and destroying on shutdown) a zookeeper client that
 * can (and <b>is recommended to be</b>) shared throughout the application.
 * <p>
 * This is because the CuratorFramework instance is supposed to only be instantiated
 * once per application and the preferences mechanism (service loader) requires this
 * instance to be available when the factory class is loaded.
 * <p>
 * The reason why this code is factored out of the {@link ZkPreferencesFactory}
 * is to provide this class as the single one in this package which is meant to be called directly by application code,
 * since everything else in this package is just SPI implementation for Preferences.
 */
public final class ZkManager {
    private ZkManager() {
    }

    private static final Logger logger = LoggerFactory.getLogger(ZkManager.class);

    private static volatile CuratorFramework curatorFramework;

    /**
     * Shares the Zookeeper client used by the Preferences integration with the application code.
     * However, this method returns a facade of the original curator, so that clients cannot shut it down
     * or otherwise modify it.
     *
     * @return a singleton curator framework instance
     */
    public static CuratorFramework curatorFramework() {
        if (curatorFramework != null) {
            return curatorFramework;
        } else synchronized (ZkManager.class) {
            if (curatorFramework == null) {
                final String quorum = System.getProperty("zk.quorum", "localhost");
                final int sessionTimeout = Integer.parseInt(
                        System.getProperty("zk.session.timeout", "30000"));
                final int connectionTimeout = Integer.parseInt(
                        System.getProperty("zk.connection.timeout", "15000"));
                final int initialDelay = Integer.parseInt(
                        System.getProperty("zk.retry.initialDelay", "10"));
                final int maxDelay = Integer.parseInt(
                        System.getProperty("zk.retry.maxDelay", "200"));
                final int maxCount = Integer.parseInt(
                        System.getProperty("zk.retry.maxCount", "10"));


                logger.info("Initializing the Zookeeper client for quorum: {}", quorum);

                final CuratorFramework curator = CuratorFrameworkFactory.newClient(quorum, sessionTimeout, connectionTimeout,
                        new BoundedExponentialBackoffRetry(initialDelay, maxDelay, maxCount));
                curator.start();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Shutting down the Zookeeper client...");
                    curator.close();
                }));

                curatorFramework = curator.usingNamespace(namespace("/"));

            }
            return curatorFramework;
        }

    }

    static CuratorFramework curatorFacade(final String rootPath) {
        return curatorFramework().usingNamespace(namespace(rootPath));
    }

}
