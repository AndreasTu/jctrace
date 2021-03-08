package de.turban.deadlock.tracer.runtime.serdata;

import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DataSerializerDeserializerTest {

    @Test
    public void testSimpleCreateSerDeserModify() {

        DataSerializer ser = new DataSerializer();
        DataDeserializer deSer = new DataDeserializer();

        List<LockCacheEntrySer> l = new ArrayList<>();
        l.add(new LockCacheEntrySer(1, "Test", 1, 1));

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        ser.serializeData(os, l);

        List<ILockCacheEntry> in = deSer(deSer, os);
        assertThat(in.size(), equalTo(1));
        ILockCacheEntry entry = in.get(0);
        assertThat(entry.getId(), equalTo(1));
        assertThat(entry.getLockedCount(), equalTo(1L));

        l.clear();
        l.add(new LockCacheEntrySer(1, "Test", 10, 3));
        ser.serializeData(os, l);

        in = deSer(deSer, os);
        assertThat(in.size(), equalTo(1));

        entry = in.get(0);
        assertThat(entry.getId(), equalTo(1));
        assertThat(entry.getLockedCount(), equalTo(10L));

    }

    @Test
    public void testSerDeserModifyTwoElements() {

        DataSerializer ser = new DataSerializer();
        DataDeserializer deSer = new DataDeserializer();

        List<LockCacheEntrySer> l = new ArrayList<>();
        l.add(new LockCacheEntrySer(1, "Test", 1, 1));

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        ser.serializeData(os, l);

        List<ILockCacheEntry> in = deSer(deSer, os);
        assertThat(in.size(), equalTo(1));
        ILockCacheEntry entry = in.get(0);
        assertThat(entry.getId(), equalTo(1));
        assertThat(entry.getLockedCount(), equalTo(1L));

        l.clear();
        l.add(new LockCacheEntrySer(2, "Test2", 10, 3));
        ser.serializeData(os, l);

        in = deSer(deSer, os);
        assertThat(in.size(), equalTo(2));

        entry = in.get(0);
        assertThat(entry.getId(), equalTo(1));
        assertThat(entry.getLockedCount(), equalTo(1L));

        entry = in.get(1);
        assertThat(entry.getId(), equalTo(2));
        assertThat(entry.getLockedCount(), equalTo(10L));

    }

    private List<ILockCacheEntry> deSer(DataDeserializer deSer, ByteArrayOutputStream os) {
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        DataResolverCreator resolver = new DataResolverCreator();
        IDeadlockDataResolver data = resolver.resolveData(deSer.readData(is));

        List<ILockCacheEntry> in = data.getLockCache().getLockEntries();
        return in;
    }
}
