package io.github.grantchen2003.cdb.control.plane.users;

import io.github.grantchen2003.cdb.control.plane.config.DynamoDbTableConfig;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

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
                        "email",     AttributeValue.fromS(user.email()),
                        "createdAt", AttributeValue.fromN(String.valueOf(user.createdAt()))
                ))
                .build());
    }

    public Optional<User> findByEmail(String email) {
        final QueryResponse response = dynamo.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName("email-index")
                .keyConditionExpression("email = :email")
                .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(email)))
                .build());

        return response.items().stream()
                .findFirst()
                .map(item -> new User(
                        item.get("id").s(),
                        item.get("email").s(),
                        Long.parseLong(item.get("createdAt").n())
                ));
    }
}