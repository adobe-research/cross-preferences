package com.adobe.prefs.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ZkManagerTest {

    @Test
    public void shouldExposeAValidCurator() throws Exception {
        final CuratorFramework curator = ZkManager.curatorFramework();
        assertEquals(curator.getState(), CuratorFrameworkState.STARTED);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldNotAllowClosingTheFramework() {
        ZkManager.curatorFramework().close();
    }

}
