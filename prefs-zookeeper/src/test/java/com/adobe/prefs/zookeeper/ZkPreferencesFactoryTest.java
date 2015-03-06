package com.adobe.prefs.zookeeper;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ZkPreferencesFactoryTest {

    @Test
    public void testDefaultSettings() throws Exception {
        ZkPreferencesFactory factory = new ZkPreferencesFactory();
        assertEquals(factory.systemRoot().curator.getNamespace(), "prefs/sys");
        assertEquals(factory.userRoot().curator.getNamespace(), "prefs/usr/" + System.getProperty("user.name"));
    }

    @Test(dependsOnMethods = "testDefaultSettings")
    public void testCustomSettings() throws Exception {
        System.setProperty("java.util.prefs.systemRoot", "/sys/root");
        System.setProperty("java.util.prefs.userRoot", "/usr/root");
        ZkPreferencesFactory factory = new ZkPreferencesFactory();
        assertEquals(factory.systemRoot().curator.getNamespace(), "sys/root");
        assertEquals(factory.userRoot().curator.getNamespace(), "usr/root");
    }

}