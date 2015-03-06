package com.adobe.prefs.zookeeper;

import com.adobe.prefs.testing.CrossPreferencesAcceptanceTest;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.prefs.Preferences;

@Test
public class ZkAcceptanceTest extends CrossPreferencesAcceptanceTest<ZkPreferencesFactory, ZkPreferences> {

    private String systemRoot;
    private String userRoot;
    private CuratorFramework curator;
    
    public ZkAcceptanceTest() {
        super(100);
    }

    @BeforeTest
    public void init() {
        systemRoot = ZkPreferencesFactory.systemRootPath();
        userRoot = ZkPreferencesFactory.userRootPath();
        curator = ZkManager.curatorFramework();
    }

    @Override
    protected void putInBackingStore(Preferences prefs, String key, String value) throws Exception {
        final String path = path(prefs, key);
        try {
            curator.create().creatingParentsIfNeeded().forPath(path, value.getBytes("UTF-8"));
        } catch (Exception e) {
            curator.setData().forPath(path, value.getBytes("UTF-8"));
        }
    }

    @Override
    protected String getFromBackingStore(Preferences prefs, String key) throws Exception{
        try {
            final byte[] data = curator.getData().forPath(path(prefs, key));
            return data != null ? new String(data, "UTF-8"): null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void createInBackingStore(Preferences prefs, String childName) throws Exception {
        curator.newNamespaceAwareEnsurePath(path(prefs, childName, "k")).ensure(curator.getZookeeperClient());
        curator.delete().forPath(path(prefs, childName, "k"));
    }

    @Override
    protected void removeKeyFromBackingStore(Preferences prefs, String key) throws Exception {
        curator.delete().forPath(path(prefs, key));
    }

    @Override
    protected void removeChildFromBackingStore(Preferences prefs, String childName) throws Exception {
        curator.delete().deletingChildrenIfNeeded().forPath(path(prefs, childName));
    }

    @Override
    protected boolean childExistsInBackingStore(Preferences prefs, String childName) throws Exception {
        final Stat stat = curator.checkExists().forPath(path(prefs, childName));
        return ZkPreferences.childFilter.apply(stat);
    }

    String namespace(Preferences prefs) {
        return prefs.isUserNode() ? userRoot : systemRoot;
    }

    String path(Preferences prefs, String... names) {
        return ZKPaths.makePath(namespace(prefs), prefs.absolutePath(), names);
    }

}
