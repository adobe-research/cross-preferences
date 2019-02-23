package com.adobe.prefs.testing;

import org.testng.annotations.Test;

import java.util.concurrent.ScheduledFuture;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import static org.testng.Assert.*;

/**
 * Extension of the acceptance test that also validates that changes are propagated between
 * the preferences SPI and the actual backing store.
 */
public abstract class CrossPreferencesAcceptanceTest<F extends PreferencesFactory, P extends Preferences>
        extends PreferencesAcceptanceTest<F, P> {

    @SuppressWarnings("unused")
    protected CrossPreferencesAcceptanceTest(final String factoryClassName, final String preferencesClassName, final long delay) throws Exception {
        super(factoryClassName, preferencesClassName, delay);
    }

    @SuppressWarnings("unused")
    protected CrossPreferencesAcceptanceTest(final Class<F> factoryClass, final Class<P> preferencesClass, final long delay) {
        super(factoryClass, preferencesClass, delay);
    }

    protected CrossPreferencesAcceptanceTest(final long delay) {
        super(delay);
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "backing-store"}, dependsOnGroups = {"hierarchy", "kv"})
    public void shouldSyncNodeChanges(Preferences prefs) throws Exception {
        assertFalse(prefs.nodeExists("x"));
        assertFalse(childExistsInBackingStore(prefs, "x"));

        final Preferences x = prefs.node("x");
//        prefs.flush();
        assertTrue(childExistsInBackingStore(prefs, "x"));

        final NodeChangeListener listener = nodeListener(x);
        createInBackingStore(x, "y");
        assertTrue(childExistsInBackingStore(x, "y"));

        final ScheduledFuture<?> nodeAdded = expectNodeAdded(listener, x, "y", 1);
//        prefs.sync();
        assertTrue(x.nodeExists("y"));
        nodeAdded.get();

        assertTrue(x.nodeExists("y"));
        assertTrue(childExistsInBackingStore(x, "y"));
        removeChildFromBackingStore(x, "y");
        assertFalse(childExistsInBackingStore(x, "y"));

        final ScheduledFuture<?> nodeRemoved = expectNodeRemoved(listener, x, "y", 1);
//        prefs.sync();
        nodeRemoved.get();
        assertFalse(x.nodeExists("y"));

        x.removeNode();
//        prefs.flush();
        assertFalse(childExistsInBackingStore(prefs, "x"));
    }

    @Test(dataProvider = "chroot", groups = {"prefs", "backing-store"}, dependsOnGroups = {"hierarchy", "kv"})
    public void shouldSyncKeyValueUpdates(Preferences prefs) throws Exception {
        final Preferences s = prefs.node("sync");

        final PreferenceChangeListener listener = prefListener(s);
        s.putInt("a", 1);
        final ScheduledFuture<?> set = expectPrefChange(listener, s, "a", "1", 1);
//        s.flush();
        assertEquals(getFromBackingStore(s, "a"), "1");
        set.get();

        putInBackingStore(s, "a", "2");
        final ScheduledFuture<?> change = expectPrefChange(listener, s, "a", "2", 2);
//        s.sync();
        assertEquals(s.getInt("a", 1), 2);
        change.get();

        removeKeyFromBackingStore(s, "a");
        final ScheduledFuture<?> remove = expectPrefChange(listener, s, "a", null, 3);
//        s.sync();
        assertEquals(s.getInt("a", 0), 0);
        remove.get();

    }


    protected abstract void putInBackingStore(Preferences prefs, String key, String value) throws Exception;

    protected abstract String getFromBackingStore(Preferences prefs, String key) throws Exception;

    protected abstract void createInBackingStore(Preferences prefs, String childName) throws Exception;

    protected abstract void removeKeyFromBackingStore(Preferences prefs, String key) throws Exception;

    protected abstract void removeChildFromBackingStore(Preferences prefs, String childName) throws Exception;

    protected abstract boolean childExistsInBackingStore(Preferences prefs, String childName) throws Exception;
}
