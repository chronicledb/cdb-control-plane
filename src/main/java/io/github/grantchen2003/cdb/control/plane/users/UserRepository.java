package io.github.grantchen2003.cdb.control.plane.users;

import io.github.grantchen2003.cdb.control.plane.config.DynamoDbTableConfig;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final DynamoDbClient dynamo;
    private final String tableName;
    private final String hashedApiKeyGsi;

    public UserRepository(DynamoDbClient dynamo, DynamoDbTableConfig tableConfig) {
        this.dynamo = dynamo;
        this.tableName = tableConfig.getTable("users");
        this.hashedApiKeyGsi = tableConfig.getGsi("users-by-hashed-api-key");
    }

    public void save(User user) {
        dynamo.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "id",        AttributeValue.fromS(user.id()),
                        "hashedApiKey",     AttributeValue.fromS(user.hashedApiKey()),
                        "createdAt", AttributeValue.fromS(user.createdAt().toString())
                ))
                .build());
    }

    public Optional<User> findByHashedApiKey(String hashedApiKey) {
        final QueryResponse response = dynamo.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(hashedApiKeyGsi)
                .keyConditionExpression("hashedApiKey = :hak")
                .expressionAttributeValues(Map.of(":hak", AttributeValue.fromS(hashedApiKey)))
                .limit(1)
                .build());

        return response.items().stream()
                .findFirst()
                .map(item -> new User(
                        item.get("id").s(),
                        item.get("hashedApiKey").s(),
                        Instant.parse(item.get("createdAt").s())
                ));
    }
}