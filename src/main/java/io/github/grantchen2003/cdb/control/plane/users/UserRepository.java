package io.github.grantchen2003.cdb.control.plane.users;

import io.github.grantchen2003.cdb.control.plane.config.DynamoDbTableConfig;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final DynamoDbClient dynamo;
    private final String tableName;

    public UserRepository(DynamoDbClient dynamo, DynamoDbTableConfig tableConfig) {
        this.dynamo = dynamo;
        this.tableName = tableConfig.getTable("users");
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

    public Optional<User> findById(String id) {
        final GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .build());

        if (!response.hasItem()) return Optional.empty();

        final Map<String, AttributeValue> item = response.item();
        return Optional.of(new User(
                item.get("id").s(),
                item.get("hashedApiKey").s(),
                Instant.parse(item.get("createdAt").s())
        ));
    }
}