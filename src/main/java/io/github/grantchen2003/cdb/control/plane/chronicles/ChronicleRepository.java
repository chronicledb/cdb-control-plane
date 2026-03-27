package io.github.grantchen2003.cdb.control.plane.chronicles;

import io.github.grantchen2003.cdb.control.plane.config.DynamoDbTableConfig;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

@Repository
public class ChronicleRepository {

    private final DynamoDbClient dynamo;
    private final String tableName;

    public ChronicleRepository(DynamoDbClient dynamo, DynamoDbTableConfig tableConfig) {
        this.dynamo = dynamo;
        this.tableName = tableConfig.getTable("chronicles");
    }

    public void save(Chronicle chronicle) {
        try {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            "id",        AttributeValue.fromS(chronicle.id()),
                            "userId",    AttributeValue.fromS(chronicle.userId()),
                            "name",      AttributeValue.fromS(chronicle.name()),
                            "createdAt", AttributeValue.fromS(chronicle.createdAt().toString())
                    ))
                    .conditionExpression("attribute_not_exists(userId) AND attribute_not_exists(#n)")
                    .expressionAttributeNames(Map.of("#n", "name"))
                    .build());
        } catch (ConditionalCheckFailedException e) {
            throw new DuplicateChronicleException(chronicle.userId(), chronicle.name());
        }
    }

    public boolean existsByUserIdAndName(String userId, String name) {
        final GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "userId", AttributeValue.fromS(userId),
                        "name",   AttributeValue.fromS(name)
                ))
                .build());

        return response.hasItem();
    }
}
