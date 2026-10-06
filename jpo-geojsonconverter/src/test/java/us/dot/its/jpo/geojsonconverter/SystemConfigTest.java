package us.dot.its.jpo.geojsonconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SystemConfigTest {

    @Test
    public void testDoConfig() {

        SystemConfig testSystemConfig = new SystemConfig(14, "testSchemaName");

        testSystemConfig.doConfig();
    }

    @Test
    public void testSettersAndGetters() {

        String testSchemaName = "testSchemaName12356";
        int testThreadCount = 5;

        SystemConfig testSystemConfig = new SystemConfig(123, "originalSchemaName");

        testSystemConfig.setSchemaName(testSchemaName);
        testSystemConfig.setThreadCount(testThreadCount);

        assertEquals(testSchemaName, testSystemConfig.getSchemaName(), "Incorrect schemaName");
        assertEquals(testThreadCount, testSystemConfig.getThreadCount(), "Incorrect threadCount");
    }
}
