package com.adobe.prefs.testing;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public abstract class PreferencesTestSupport {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long delay;
    final String namespace = UUID.randomUUID().toString();

    protected PreferencesTestSupport(long delay) {
        this.delay = delay;
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        scheduler.shutdown();
        for (Preferences prefs : Arrays.asList(Preferences.systemRoot(), Preferences.userRoot())) {
            if (prefs.nodeExists(namespace)) {
                prefs.node(namespace).removeNode();
            }
        }
    }

    protected NodeChangeListener nodeListener(Preferences prefs) {
        final NodeChangeListener listener = mock(NodeChangeListener.class);
        prefs.addNodeChangeListener(listener);
        return listener;
    }

    protected ScheduledFuture<?> schedule(Runnable task) {
        return scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    protected ScheduledFuture<?> expectNodeAdded(final NodeChangeListener listener,
                                                 final Preferences parent, final String childName, final int count) {
        final ArgumentCaptor<NodeChangeEvent> captor = ArgumentCaptor.forClass(NodeChangeEvent.class);

        final Runnable task = new Runnable() {
            @Override public void run() {
                verify(listener, times(count)).childAdded(captor.capture());
                final NodeChangeEvent event = captor.getValue();

                assertEquals(event.getParent(), parent);
                assertEquals(event.getChild().name(), childName);
            }
        };

        return schedule(task);
    }

    protected ScheduledFuture<?> expectNodeRemoved(final NodeChangeListener listener,
                                                   final Preferences parent, final String childName, final int count) {
        final ArgumentCaptor<NodeChangeEvent> captor = ArgumentCaptor.forClass(NodeChangeEvent.class);

        final Runnable task = new Runnable() {
            @Override public void run() {
                verify(listener, times(count)).childRemoved(captor.capture());
                final NodeChangeEvent event = captor.getValue();

                assertEquals(event.getParent(), parent);
                assertEquals(event.getChild().name(), childName);
            }
        };

        return schedule(task);

    }

    protected PreferenceChangeListener prefListener(Preferences prefs) {
        PreferenceChangeListener listener = mock(PreferenceChangeListener.class);
        prefs.addPreferenceChangeListener(listener);
        return listener;
    }

    protected ScheduledFuture<?> expectPrefChange(final PreferenceChangeListener listener, final Preferences prefs,
                                                  final String key, final String value, final int count) {
        final ArgumentCaptor<PreferenceChangeEvent> captor = ArgumentCaptor.forClass(PreferenceChangeEvent.class);

        final Runnable task = new Runnable() {
            @Override public void run() {
                verify(listener, times(count)).preferenceChange(captor.capture());

                PreferenceChangeEvent event = captor.getValue();
                assertSame(event.getNode(), prefs);
                assertEquals(event.getKey(), key);
                assertEquals(event.getNewValue(), value);
            }
        };

        return schedule(task);
    }

    @DataProvider(parallel = true)
    public Object[][] root() {
        return new Object[][] {
                { Preferences.systemRoot() },
                { Preferences.userRoot() }
        };
    }

    @DataProvider(parallel = true)
    public Object[][] chroot() {
        return new Object[][] {
                { Preferences.systemRoot().node(namespace) },
                { Preferences.userRoot().node(namespace) }
        };
    }

}
