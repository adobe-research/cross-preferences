package com.adobe.prefs.zookeeper;

import com.google.common.base.Throwables;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestZkServer extends ZooKeeperServerMain implements Runnable {
    static final int PORT = 12181;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final ServerConfig config;

    public TestZkServer() throws IOException, ConfigException {
        final Properties props = new Properties();
        props.setProperty("clientPort", String.valueOf(PORT));
        props.setProperty("dataDir", Files.createTempDirectory("zk").toString());
        final QuorumPeerConfig quorumConfig = new QuorumPeerConfig();
        quorumConfig.parseProperties(props);
        config = new ServerConfig();
        config.readFrom(quorumConfig);
    }

    @BeforeSuite(alwaysRun = true)
    public void startZookeeper() {
        logger.info("Starting the local zookeeper server...");
        System.setProperty("zk.quorum", "localhost:" + PORT);
        executor.execute(this);
    }

    @Override
    public void run() {
        try {
            runFromConfig(config);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @AfterSuite(alwaysRun = true)
    public void stopZookeeper() throws IOException {
        logger.info("Shutting down the local zookeeper...");
        shutdown();
    }
}
