package com.adobe.prefs.zookeeper;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import static com.adobe.prefs.zookeeper.ZkUtils.basename;
import static com.adobe.prefs.zookeeper.ZkUtils.bytes;
import static com.adobe.prefs.zookeeper.ZkUtils.string;
import static org.apache.curator.utils.ZKPaths.makePath;
import static org.apache.zookeeper.KeeperException.NotEmptyException;

/**
 * Preferences implementation backed by zookeeper.
 *
 * The main challenge is to maintain the clear distinction between <em>preferences</em>
 * (file-like entries or keys) and <em>children</em> (directory-like entries),
 * since the ZooKeeper nodes are designed to work as both and the Preferences API also allows the same name
 * to be used for a child and a preference key of the same node.
 *
 * The actual heuristic is encapsulated in the {@link #isContainerNode(Stat)} and {@link #isValueNode(Stat)} predicates
 *
 * However, both false positives and negatives are still possible.
 *
 */
class ZkPreferences extends AbstractPreferences implements PathChildrenCacheListener, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ZkPreferences.class);
    private static final String CHILD_MARKER = "_PrefX_0ca1b97f-1a85-4e28-a692-dd8242b15cbe";

    final CuratorFramework curator;

    private final boolean userNode;
    private final boolean encodedBinary;

    private final PathChildrenCache pcc;
    private final Set<NodeChangeListener> nodeChangeListeners = ConcurrentHashMap.newKeySet();
    private final Set<PreferenceChangeListener> preferenceChangeListeners = ConcurrentHashMap.newKeySet();


    /**
     * Creates a root node.
     * This is the only constructor visible from outside this class.
     * @param curator
     * @param encodedBinary
     */
    ZkPreferences(CuratorFramework curator, boolean encodedBinary, boolean userNode) {
        this(curator, null, "", encodedBinary, userNode);
    }

    /**
     * Creates a child node.
     * @param curator
     * @param parent
     * @param name
     * @param encodedBinary
     */
    private ZkPreferences(CuratorFramework curator, ZkPreferences parent, String name,
                          boolean encodedBinary, boolean userNode) {
        super(parent, name);
        this.curator = curator;
        this.userNode = userNode;
        this.encodedBinary = encodedBinary;
        pcc = new PathChildrenCache(curator, absolutePath(), true);
        newNode = true;
        if (parent != null) {
            logger.debug("Zookeeper preference node `{}` created as a child of {}", name, parent);
        }
        try {
            flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ZkPreferences parent() {
        return (ZkPreferences) super.parent();
    }

    /**
     * Decides whether a znode is to be treated as a child node.
     * A child node is treated as such when its `cversion` is greater than 0
     * (which means it is -- or used to be -- a container for other nodes).
     * When it has no children, we will <i>assume</i> it's meant to be a child node if it doesn't
     * (and never used to) contain any data.
     *
     * <p>Note that a znode can be both a preference key and a child node,
     * if both its value and its children have been modified</p>
     */
    static boolean isContainerNode(Stat stat) {
        if (stat == null) {
            return false;
        }
        return stat.getCversion() > 0 || (stat.getVersion() == 0 && stat.getDataLength() == 0);
    }

    /**
     * Decides whether a znode is to be treated as a preference key.
     * <p>It will return true if either of the following is true:<ul>
     *     <li>the node has a non-empty value</li>
     *     <li>the node has never had any children (cversion == 0)</li>
     * </ul></p>
     *
     * If the node has an empty value, but it has a both `cversion` and `version` values greater than 0,
     * it will only show as a child node and not a key (although it is obvious that it used to be a "key",
     * not only a "node").<br/>
     * This is because the zookeeper console client does not allow creating a node with an empty value,
     * so any node that was created with that client would also appear as a key for as long as that node exists.
     */
    static boolean isValueNode(Stat stat) {
        if (stat == null) {
            return false;
        }
        return stat.getDataLength() > 0 || stat.getVersion() > 0;
    }


    String path(String child) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(child), "Empty child");
        return makePath(absolutePath(), child);
    }

    @Override
    protected void putSpi(String key, String value) {
        logger.trace("Setting key `{}` in {}", key, this);
        putRawBytes(key, bytes(value));
    }

    private void putRawBytes(String key, byte[] bytes) {
        final String path = path(key);
        try {
            if (curator.checkExists().forPath(path) == null) {
                curator.create().forPath(path, bytes);
            } else {
                if (! Arrays.equals(bytes, curator.getData().forPath(path)) ) {
                    curator.setData().forPath(path, bytes);
                }
            }
        } catch (NoNodeException e) {
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        logger.trace("Setting key `{}` as byte array in {}", key, this);
        if (encodedBinary) {
            super.putByteArray(key, value);
        } else {
            putRawBytes(key, value);
        }
    }

    @Override
    protected String getSpi(String key) {
        logger.trace("Getting key `{}` in {}", key, this);
        return string(getRawBytes(key));
    }

    private byte[] getRawBytes(String key) {
        try {
            return curator.getData().forPath(path(key));
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public byte[] getByteArray(String key, byte[] def) {
        logger.trace("Getting key `{}` as byte array in {}", key, this);
        if (encodedBinary) {
            return super.getByteArray(key, def);
        } else {
            final byte[] value = getRawBytes(key);
            return value != null ? value : def;
        }
    }

    @Override
    protected void removeSpi(String key) {
        logger.trace("Removing preference key `{}` in {}", key, this);
        try {
            curator.delete().forPath(path(key));
        } catch (NotEmptyException e) {
            // fallback to setting a null value if the node is also a non-empty child node
            putSpi(key, null);
        } catch (NoNodeException e) {
            logger.debug("Failed to remove key `{}` from {} as it does not exist in zookeeper");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        logger.info("Removing preference node {}", this);
        try {
            if (curator.checkExists().forPath(absolutePath()) != null) {
                curator.delete().deletingChildrenIfNeeded().forPath(absolutePath());
                parent().triggerChildRemoved(this);
            }
        } catch (NoNodeException e) {
            logger.warn("Attempt to remove a child that does not exist: {}", this);
        } catch (Exception e) {
            throw new BackingStoreException(e);
        } finally {
            logger.info("Cleaning up node and preference listeners for {}", absolutePath());
            nodeChangeListeners.clear();
            preferenceChangeListeners.clear();
            stopWatching();
        }
    }

    @Override
    protected String[] keysSpi() {
        logger.trace("Getting preference keys of {}", this);
        return getChildren(ZkPreferences::isValueNode);
    }

    @Override
    protected String[] childrenNamesSpi() {
        logger.trace("Getting children of {}", this);
        return getChildren(ZkPreferences::isContainerNode);
    }


    /**
     * Lists only the child zookeeper nodes that comply to the provided filter (which should include
     * either "directories" or "files").
     */
    protected String[] getChildren(Predicate<Stat> filter) {
        try {
            final List<String> children = curator.getChildren().forPath(absolutePath());
            // list as keys only the child nodes with no children of their own
            for (Iterator<String> iter = children.iterator(); iter.hasNext(); ) {
                final String child = iter.next();
                final Stat stat = curator.checkExists().forPath(path(child));
                if (!filter.apply(stat)) {
                    iter.remove();
                }
            }
            return children.toArray(new String[children.size()]);
        } catch (Exception e) {
            return new String[0];
        }
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        logger.trace("Getting child `{}` of {}", name, this);
        return new ZkPreferences(curator, this, name, encodedBinary, userNode);
    }

    @Override
    protected void syncSpi() {
        // NOOP
    }

    private void startWatching() {
        logger.debug("Syncing preference node {}", this);
        try {
            pcc.start();
            pcc.getListenable().addListener(this);
            logger.info("Started zookeeper listener for {}", this);
        } catch (IllegalStateException benign) {
            logger.debug("The patch children cache seems to be started already: {}", benign.toString());
            try {
                pcc.rebuild();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to rebuild cache", e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start cache", e);
        }
    }

    private void stopWatching() {
        logger.info("Closing the Zookeeper listener for {}", this);
        try {
            pcc.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        logger.debug("Flushing preference node {}", this);
        try {
            final Stat pathStat = curator.checkExists().forPath(absolutePath());
            if (!isContainerNode(pathStat) && !CHILD_MARKER.equals(name())) {
                // force the `cversion` to increment by adding and removing a random key
                logger.debug("Creating a synthetic child `{}` to mark path as node by increasing the 'cversion': {}",
                        CHILD_MARKER, absolutePath());
                final String markerPath = path(CHILD_MARKER);
                curator.create().creatingParentsIfNeeded().forPath(markerPath, CHILD_MARKER.getBytes());
                curator.delete().forPath(markerPath);
            }
            assert isContainerNode(curator.checkExists().forPath(absolutePath())) :
                    "node not marked as child node: " + absolutePath();
            logger.info("Created zookeeper node for {}", this);
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    public boolean isUserNode() {
        return userNode;
    }

    @Override
    public void close() {
        synchronized (lock) {
            for (AbstractPreferences child : cachedChildren()) {
                try {
                    ((ZkPreferences) child).close();
                } catch (Exception e) {
                    logger.error("Failed to close child node: {}", child);
                }
            }
            logger.info("Cleaning up node and preference listeners for {}", absolutePath());
            nodeChangeListeners.clear();
            preferenceChangeListeners.clear();
            stopWatching();
        }
    }

    @Override
    public void childEvent(CuratorFramework curator, PathChildrenCacheEvent event) {
        try {
            final ChildData childData = event.getData();

            if (childData == null || childData.getStat() == null) {
                logger.debug("Unhandled zookeeper event: {}", event);
                return;
            }
            logger.debug("Zookeeper event received: {}", event);
            final String name = childData.getPath() != null ? basename(childData.getPath()) : null;

            if (CHILD_MARKER.equals(name)) {
                logger.trace("Ignoring events for child marker: {}", CHILD_MARKER);
                return;
            }

            final String value = string(childData.getData());
            switch (event.getType()) {
                case CHILD_REMOVED:
                    try {
                        if (isValueNode(childData.getStat())) {
                            triggerPreferenceDropped(name);
                        }
                    } catch (IllegalStateException e) {
                        logger.debug("Could not remove preference key `{}` from {}: {}", name, this, e.toString());
                    }
                    try {
                        if (nodeExists(name)) {
                            final Preferences child = node(name);
                            child.removeNode();    // remove node from local cache and notify listeners
                            triggerChildRemoved(child);
                        }
                    } catch (IllegalStateException e) {
                        logger.debug("Node `{}` already removed from {}: {}", name, this, e.toString());
                    }
                    break;
                case CHILD_ADDED:
                    if (isContainerNode(childData.getStat())) {
                        triggerChildAdded(name);
                    }
                    if (isValueNode(childData.getStat())) {
                        triggerPreferenceChange(name, value);
                    }
                    break;
                case CHILD_UPDATED:
                    triggerPreferenceChange(name, value);
                    break;
                default:
                    logger.debug("Ignoring zookeeper event: {}", event);
            }
        } catch (Exception e) {
            logger.error("Error handling event `" + event + "` in path: " + absolutePath(), e);
        }
    }


    private void alterListeners(Runnable action) {
        synchronized (lock) {
            final int beforeCount = preferenceChangeListeners.size() + nodeChangeListeners.size();
            action.run();
            final int afterCount = preferenceChangeListeners.size() + nodeChangeListeners.size();
            if (beforeCount == 0 && afterCount > 0) {
                startWatching();
            } else if (afterCount == 0 && beforeCount > 0) {
                stopWatching();
            }
        }
    }

    @Override
    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        alterListeners(() -> preferenceChangeListeners.add(pcl));
        logger.info("Preference change listener added: {}", pcl);
    }

    @Override
    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        alterListeners(() -> preferenceChangeListeners.remove(pcl));
        logger.info("Preference change listener removed: {}", pcl);
    }

    @Override
    public void addNodeChangeListener(NodeChangeListener ncl) {
        alterListeners(() -> nodeChangeListeners.add(ncl));
        logger.info("Node change listener added: {}", ncl);
    }

    @Override
    public void removeNodeChangeListener(NodeChangeListener ncl) {
        alterListeners(() -> nodeChangeListeners.remove(ncl));
        logger.info("Node change listener removed: {}", ncl);
    }

    private <L, E> void triggerEvent(Collection<L> listeners, BiConsumer<L, E> handler, E event) {
        listeners.parallelStream().forEach(listener -> {
            try {
                logger.trace("Notifying listener `{}` of event `{}` in: `{}`", listener, event, this);
                handler.accept(listener, event);
            } catch (Exception e) {
                logger.error("Could not notify listener `" + listener + "` of event `" + event
                        + "` in:  " + this, e);
            }
        });
    }

    private void triggerChildAdded(String childName) {
        logger.debug("Notifying {} node listeners that child `{}` was added under `{}`",
                nodeChangeListeners.size(), childName, this);
        triggerEvent(nodeChangeListeners, NodeChangeListener::childAdded, new NodeChangeEvent(this, node(childName)));
    }

    private void triggerChildRemoved(Preferences child) {
        logger.debug("Notifying {} node listeners that child `{}` was removed from `{}`",
                nodeChangeListeners.size(), child.name(), this);
        triggerEvent(nodeChangeListeners, NodeChangeListener::childRemoved, new NodeChangeEvent(this, child));
    }

    private void triggerPreferenceChange(String key, String value) {
        logger.debug("Notifying {} preference listeners that preference `{}` is now `{}` under `{}`",
                preferenceChangeListeners.size(), key, value, this);
        triggerEvent(preferenceChangeListeners, PreferenceChangeListener::preferenceChange, new PreferenceChangeEvent(this, key, value));
    }

    private void triggerPreferenceDropped(String key) {
        triggerPreferenceChange(key, null);
    }

}
