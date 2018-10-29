package com.adobe.prefs.zookeeper;

import org.apache.zookeeper.data.Stat;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class ZkPreferencesTest {

    @Test
    public void shouldMapNullStatsToNeitherContainerNorValue() {
        assertEquals(ZkPreferences.isContainerNode(null), false, "containerNode");
        assertEquals(ZkPreferences.isValueNode(null), false, "valueNode");
    }

    @Test(dataProvider = "stats")
    public void shouldMapStatsToValueContainerOrBoth(int cversion, int version, int dataLength,
                                boolean containerNode, boolean valueNode) {
        final Stat stat = stat(cversion, version, dataLength);
        assertEquals(ZkPreferences.isContainerNode(stat), containerNode, "containerNode");
        assertEquals(ZkPreferences.isValueNode(stat), valueNode, "valueNode");
    }

    @DataProvider
    public Object[][] stats() {
        return new Object[][] {
                {0, 0, 0, true, false},
                {0, 0, 1, false, true},
                {0, 1, 0, false, true},
                {0, 1, 1, false, true},
                {1, 0, 0, true, false},
                {1, 0, 1, true, true},
                {1, 1, 0, true, true},
                {1, 1, 1, true, true}
        };
    }

    Stat stat(int cversion, int version, int dataLength) {
        final Stat stat = Mockito.mock(Stat.class);
        when(stat.getCversion()).thenReturn(cversion);
        when(stat.getVersion()).thenReturn(version);
        when(stat.getDataLength()).thenReturn(dataLength);
        return stat;
    }

}
