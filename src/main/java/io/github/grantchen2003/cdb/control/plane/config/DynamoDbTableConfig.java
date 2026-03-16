package io.github.grantchen2003.cdb.control.plane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "aws.dynamodb")
public class DynamoDbTableConfig {
    private Map<String, String> tables;

    public void setTables(Map<String, String> tables) {
        this.tables = tables;
    }

    public String getTable(String name) {
        String table = tables.get(name);
        if (table == null) throw new IllegalArgumentException("No table configured for: " + name);
        return table;
    }
}