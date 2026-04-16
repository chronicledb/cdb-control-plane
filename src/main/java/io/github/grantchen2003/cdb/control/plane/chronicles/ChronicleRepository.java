package io.github.grantchen2003.cdb.control.plane.chronicles;

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
    private static final String CHRONICLES_TABLE_NAME = "chronicles";

    private final DynamoDbClient dynamo;

    public ChronicleRepository(DynamoDbClient dynamo) {
        this.dynamo = dynamo;
    }

    public void save(Chronicle chronicle) {
        try {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(CHRONICLES_TABLE_NAME)
                    .item(Map.of(
                            "id",            AttributeValue.fromS(chronicle.id()),
                            "userId",        AttributeValue.fromS(chronicle.userId()),
                            "name",          AttributeValue.fromS(chronicle.name()),
                            "writeSchemaId", AttributeValue.fromS(chronicle.writeSchemaId()),
                            "createdAt",     AttributeValue.fromS(chronicle.createdAt().toString())
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
                .tableName(CHRONICLES_TABLE_NAME)
                .key(Map.of(
                        "userId", AttributeValue.fromS(userId),
                        "name",   AttributeValue.fromS(name)
                ))
                .build());

        return response.hasItem();
    }
}
