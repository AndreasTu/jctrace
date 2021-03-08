package de.turban.deadlock.tracer.runtime.serdata;


import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DataDeserializerTest {

    @Test
    public void testDbFromFile() {

        DataDeserializer deSer = new DataDeserializer();
        List<ISerializableData> list = deSer.readData(Paths.get("src/test/resources/Deadlock.db"));
        assertThat(list.size()).isEqualTo(375);
    }
}
