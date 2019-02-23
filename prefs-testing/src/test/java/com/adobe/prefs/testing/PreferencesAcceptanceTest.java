package com.adobe.prefs.testing;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import static java.util.Arrays.asList;
import static org.testng.Assert.*;

/**
 * Acceptance test suite for Preferences implementations.
 */
public abstract class PreferencesAcceptanceTest<F extends PreferencesFactory, P extends Preferences> extends PreferencesTestSupport {

    private final Class<F> factoryClass;
    private final Class<P> preferencesClass;

    /**
     * This constructor is here only to make it easy to test platform implementations
     * bundled in the JDK (like file-system, MacOSX or Windows), as those classes are usually
     * package-private.
     */
    @SuppressWarnings("unused")
    protected PreferencesAcceptanceTest(String factoryClassName, String preferencesClassName, long delay) throws Exception {
        this((Class<F>) Class.forName(factoryClassName),
                (Class<P>) Class.<Class<Preferences>>forName(preferencesClassName),delay);
    }

    protected PreferencesAcceptanceTest(Class<F> factoryClass,
                                        Class<P> preferencesClass,
                                        long delay) {
        super(delay);
        System.setProperty(PreferencesFactory.class.getName(), factoryClass.getName());
        this.factoryClass = factoryClass;
        this.preferencesClass = preferencesClass;
    }

    protected PreferencesAcceptanceTest(final long delay) {
        super(delay);
        final Type[] typeArgs = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
        factoryClass = (Class<F>) typeArgs[0];
        preferencesClass = (Class<P>) typeArgs[1];
    }

    @BeforeClass
    public void checkFactoryInitialization() throws Exception {
        Field factory = Preferences.class.getDeclaredField("factory");
        factory.setAccessible(true);
        assertEquals(factory.get(null).getClass(), factoryClass);
        factory.setAccessible(false);
    }

    @Test(groups = "factory", timeOut = 500L)
    public void shouldReturnTheSameRoot() {
        assertSame(Preferences.systemRoot(), Preferences.systemRoot());
        // this is not explicitly required by the javadoc, but it's "implied"
        // that the same instance should be returned if the "user context" hasn't changed.
        assertSame(Preferences.userRoot(), Preferences.userRoot());
    }

    @Test(groups = "factory", timeOut = 500L)
    public void userAndSystemRootsShouldBeDifferent() {
        assertNotEquals(Preferences.systemRoot(), Preferences.userRoot());
    }

    @Test(dataProvider = "root", groups = "factory", timeOut = 500L)
    public void shouldCreatePreferencesOfExpectedType(Preferences root) {
        assertEquals(root.getClass(), preferencesClass);
    }

    @Test(dataProvider = "root", groups = "prefs", dependsOnGroups = "factory", timeOut = 500L)
    public void rootAbsolutePathMustBeSlash(Preferences root) {
        assertEquals(root.absolutePath(), "/");
    }

    @Test(groups = "prefs", dependsOnGroups = "factory", timeOut = 500L)
    public void rootNodesShouldBeFlagged() {
        assertFalse(Preferences.systemRoot().isUserNode(), "systemRoot flagged as userNode");
        assertTrue(Preferences.userRoot().isUserNode(), "userRoot not flagged as userNode");
    }

    @Test(dataProvider = "root", priority = -1, groups = {"prefs", "hierarchy"},
            dependsOnGroups = "factory", timeOut = 5000L)
    public void shouldIsolateRootNodes(Preferences root) throws Exception {
        assertFalse(root.nodeExists(namespace)); // the second invocation fails if the two root nodes are the same
        final Preferences prefs = root.node(namespace);
        assertTrue(root.nodeExists(namespace));
        assertSame(root.node('/' + namespace), prefs);
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "hierarchy"}, dependsOnGroups = "factory", timeOut = 5000L)
    public void shouldCreateChildren(Preferences prefs) throws Exception {
        final String childName = "child";

        assertFalse(prefs.nodeExists(childName));
        final Preferences child = prefs.node(childName);
        assertTrue(prefs.nodeExists(childName));

        assertSame(prefs.node(childName), child, "not found by name");
        assertSame(prefs.node(child.absolutePath()), child, "not found by path using parent");
        assertSame(child.node(child.absolutePath()), child, "not found by path using itself");
        assertEquals(child.name(), childName, "wrong child name");
        assertEquals(child.absolutePath(), prefs.absolutePath() + '/' + childName, "wrong child path");
        assertSame(child.parent(), prefs, "wrong child parent");
        assertEquals(child.isUserNode(), prefs.isUserNode(), "wrong userNode flag");
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "hierarchy"}, dependsOnGroups = "factory", timeOut = 5000L)
    public void shouldCreateHierarchy(Preferences prefs) throws Exception {
        final Preferences a1 = prefs.node("a1");
        final Preferences a2 = a1.node("a2");
        final Preferences a3 = a2.node("a3");

        assertSame(prefs.node("a1/a2/a3"), a3);
        assertSame(a1.parent(), prefs, "wrong parent");
        assertSame(a2.parent(), a1, "wrong parent");
        assertSame(a3.parent(), a2, "wrong parent");

        final Preferences b3 = prefs.node("b1/b2/b3");
        assertSame(b3.parent(), prefs.node("b1/b2"));
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "hierarchy"}, priority = 1,
            dependsOnMethods = "shouldCreateHierarchy", expectedExceptions = IllegalStateException.class,
            timeOut = 5000L)
    public void shouldRemoveChildren(Preferences prefs) throws Exception {
        assertTrue(prefs.nodeExists("a1/a2/a3"), "precondition failed");
        prefs.node("a1/a2/a3").removeNode();
        assertFalse(prefs.nodeExists("a1/a2/a3"), "node not removed");
        assertTrue(prefs.nodeExists("a1/a2"), "parent node removed by accident");

        final Preferences a1 = prefs.node("a1");
        a1.removeNode();
        assertFalse(prefs.nodeExists("a1/a2"), "descendant not removed");
        assertFalse(prefs.nodeExists("a1"), "node with descendants not removed");

        a1.get("k", "v");
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "kv"}, dependsOnGroups = "factory", timeOut = 5000L)
    public void shouldStoreKeyValues(Preferences prefs) {
        final Preferences container = prefs.node("container");
        container.put("k", "v");
        assertEquals(container.get("k", ""), "v");
        container.putInt("k", 3);
        assertEquals(container.get("k", ""), "3");
        assertEquals(container.getInt("k", 0), 3);
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "kv"},
            dependsOnMethods = "shouldStoreKeyValues", timeOut = 5000L)
    public void shouldRemoveKeys(Preferences prefs) throws Exception {
        final Preferences container = prefs.node("container");
        assertTrue(asList(container.keys()).contains("k"));

        container.remove("k");
        assertFalse(asList(container.keys()).contains("k"));
        assertEquals(container.get("k", null), null);
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "hierarchy", "kv"},
            dependsOnGroups = "factory", timeOut = 5000L)
    public void shouldIsolateKeysAndChildren(Preferences prefs) throws Exception {
        final Preferences mixed = prefs.node("mixed");
        final String keyOnly = "keyOnly";
        final String childOnly = "childOnly";

        mixed.putByteArray(keyOnly, new byte[] {11, 12});

        assertTrue(asList(mixed.keys()).contains(keyOnly));
        assertNotNull(mixed.get(keyOnly, null));

        assertFalse(mixed.nodeExists(keyOnly));
        assertFalse(asList(mixed.childrenNames()).contains(keyOnly));

        mixed.node(childOnly);

        assertTrue(mixed.nodeExists(childOnly));
        assertTrue(asList(mixed.childrenNames()).contains(childOnly));

        assertFalse(asList(mixed.keys()).contains(childOnly));
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "hierarchy", "kv"},
            dependsOnMethods = "shouldIsolateKeysAndChildren", timeOut = 5000L)
    public void shouldAllowSameNameForBoth(Preferences prefs) throws Exception {
        final Preferences mixed = prefs.node("mixed");
        final String both = "bothKeyAndChild";

        mixed.put(both, "value");
        assertTrue(asList(mixed.keys()).contains(both));


        final Preferences child = mixed.node(both);
        assertNotNull(child);
        assertTrue(mixed.nodeExists(both));

        assertTrue(asList(mixed.keys()).contains(both));
        assertNotNull(mixed.get(both, null));
        assertTrue(asList(mixed.childrenNames()).contains(both));

    }

    @Test(dataProvider = "chroot", groups = {"prefs", "hierarchy", "notifications"},
            dependsOnGroups = "factory", timeOut = 10000L)
    public void shouldTriggerNodeAdded(Preferences prefs) throws Exception {
        final Preferences watched = prefs.node("watched");
        final NodeChangeListener listener = nodeListener(watched);
        final Preferences n1 = watched.node("n1");
        expectNodeAdded(listener, watched, n1.name(), 1).get();
        watched.node("n1"); // not a new node, so the invocation count should stay the same
        prefs.node("n0");   // shouldn't trigger anything for our listener, as it's a parent event
        expectNodeAdded(listener, watched, n1.name(), 1).get();

        final Preferences n2 = watched.node("n2");
        expectNodeAdded(listener, watched, n2.name(), 2).get();

        watched.node("n3/nested");
        expectNodeAdded(listener, watched, "n3", 3).get();
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "hierarchy", "notifications"},
            dependsOnMethods = "shouldTriggerNodeAdded", timeOut = 10000L)
    public void shouldTriggerNodeRemoved(Preferences prefs) throws Exception {
        final Preferences watched = prefs.node("watched");
        final NodeChangeListener listener = nodeListener(watched);

        watched.node("n1").removeNode();
        expectNodeRemoved(listener, watched, "n1", 1).get();

        watched.node("n2").removeNode();
        expectNodeRemoved(listener, watched, "n2", 2).get();

        watched.node("n3/nested").removeNode();
        expectNodeRemoved(listener, watched, "n2", 2).get();

        watched.node("n3").removeNode();
        expectNodeRemoved(listener, watched, "n3", 3).get();
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "kv", "notifications"},
            dependsOnGroups = "factory", timeOut = 5000L)
    public void shouldTriggerPreferenceChange(Preferences prefs) throws Exception {
        final Preferences watched = prefs.node("watchedContainer");
        final PreferenceChangeListener listener = prefListener(watched);
        watched.putFloat("key", 1.2F);
        expectPrefChange(listener, watched, "key", "1.2", 1).get();
        watched.remove("key");
        expectPrefChange(listener, watched, "key", null, 2).get();
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "concurrency"},
            dependsOnGroups = {"hierarchy", "kv"}, invocationCount = 15, threadPoolSize = 6, timeOut = 30000L)
    public void concurrencyTest(Preferences prefs) throws Exception {

        final Preferences child = prefs.node(UUID.randomUUID().toString());
        final String key = UUID.randomUUID().toString();

        final NodeChangeListener nodeListener = nodeListener(child);
        final PreferenceChangeListener prefListener = prefListener(child);

        prefs.putInt(key, 12);
        final Preferences grandson = child.node("grandson");
        child.putBoolean(key, true);
        ScheduledFuture<?> nodeAdded = expectNodeAdded(nodeListener, child, grandson.name(), 1);
        ScheduledFuture<?> prefSet = expectPrefChange(prefListener, child, key, "true", 1);
        assertEquals(prefs.getInt(key, 0), 12);
        assertTrue(child.getBoolean(key, false));

        grandson.removeNode();
        ScheduledFuture<?> nodeRemoved = expectNodeRemoved(nodeListener, child, grandson.name(), 1);

        prefSet.get();

        child.remove(key);
        child.removeNode();

        assertEquals(prefs.getInt(key, 0), 12);

        nodeAdded.get();
        nodeRemoved.get();
        expectPrefChange(prefListener, child, key, null, 2).get();

    }

}
