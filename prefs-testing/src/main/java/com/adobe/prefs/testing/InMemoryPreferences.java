package com.adobe.prefs.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * Preferences implementation that uses a map to store its keys and values.
 * <p>
 * Since there's no backing store, this implementation can always "start from scratch",
 * so it can rely on the {@link java.util.prefs.AbstractPreferences#kidCache superclass children cache}
 * for node management. Therefore all the tree-related methods have dummy implementations.
 * </p>
 * Also, there's no need for access synchronization here, since it's all taken care of by the
 * {@link java.util.prefs.AbstractPreferences superclass}.
 */
class InMemoryPreferences extends AbstractPreferences {

    private final Map<String, String> prefs = new HashMap<>();

    protected InMemoryPreferences() {
        this(null, "");
    }

    protected InMemoryPreferences(AbstractPreferences parent, String name) {
        super(parent, name);
        newNode = true;
    }

    @Override
    protected void putSpi(String key, String value) {
        prefs.put(key, value);
    }

    @Override
    protected String getSpi(String key) {
        return prefs.get(key);
    }

    @Override
    protected void removeSpi(String key) {
        prefs.remove(key);
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return prefs.keySet().toArray(new String[prefs.size()]);
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        return new InMemoryPreferences(this, name);
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return new String[0];   // none other than the ones already cached by the parent class
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        // it's ok, parent will remove from its cache
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        // wait, what?...
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        // where?
    }
}
