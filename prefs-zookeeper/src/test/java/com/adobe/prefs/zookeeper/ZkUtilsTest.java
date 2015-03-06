package com.adobe.prefs.zookeeper;

import org.testng.annotations.Test;

import static com.adobe.prefs.zookeeper.ZkUtils.*;
import static org.testng.Assert.assertEquals;

public class ZkUtilsTest {

    @Test
    public void testBytes() throws Exception {
        assertEquals(bytes(null), null);
        assertEquals(bytes("abc"), new byte[] {'a', 'b', 'c'});
    }

    @Test
    public void testString() throws Exception {
        assertEquals(string(null), null);
        assertEquals(string(new byte[] {'a', 'b', 'c'}), "abc");
    }

    @Test
    public void testBasename() throws Exception {
        assertEquals(basename("single"), "single");
        assertEquals(basename("/full/path"), "path");
    }

    @Test
    public void testNamespace() throws Exception {
        assertEquals(namespace("/name/space/"), "name/space");
        assertEquals(namespace("/name/space"), "name/space");
        assertEquals(namespace("namespace"), "namespace");
        assertEquals(namespace("//namespace//"), "namespace");

    }
}