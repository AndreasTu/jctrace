package de.turban.deadlock.tracer.runtime.serdata;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

public class DataDeserializerTest {

    @Test
    public void testDbFromFile() {

        DataDeserializer deSer = new DataDeserializer();
        List<ISerializableData> list = deSer.readData(Paths.get("src/test/resources/Deadlock.db"));
        assertThat(list.size(), equalTo(375));
    }
}
