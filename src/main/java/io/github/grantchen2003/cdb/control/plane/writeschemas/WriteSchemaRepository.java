package io.github.grantchen2003.cdb.control.plane.writeschemas;

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
public class WriteSchemaRepository {
    private static final String WRITE_SCHEMAS_TABLE_NAME = "write_schemas";

    private final DynamoDbClient dynamo;

    public WriteSchemaRepository(DynamoDbClient dynamo) {
        this.dynamo = dynamo;
    }

    public void save(WriteSchema writeSchema) {
        dynamo.putItem(PutItemRequest.builder()
                .tableName(WRITE_SCHEMAS_TABLE_NAME)
                .item(Map.of(
                        "id",              AttributeValue.fromS(writeSchema.id()),
                        "userId",          AttributeValue.fromS(writeSchema.userId()),
                        "chronicleName",   AttributeValue.fromS(writeSchema.chronicleName()),
                        "writeSchemaJson", AttributeValue.fromS(writeSchema.writeSchemaJson()),
                        "createdAt",       AttributeValue.fromS(writeSchema.createdAt().toString())
                ))
                .build());
    }

    public Optional<WriteSchema> findByUserIdAndChronicleName(String userId, String chronicleName) {
        final QueryResponse response = dynamo.query(QueryRequest.builder()
                .tableName(WRITE_SCHEMAS_TABLE_NAME)
                .indexName("userId-chronicleName-index")
                .keyConditionExpression("userId = :userId AND chronicleName = :chronicleName")
                .expressionAttributeValues(Map.of(
                        ":userId",        AttributeValue.fromS(userId),
                        ":chronicleName", AttributeValue.fromS(chronicleName)
                ))
                .build());

        return response.items().stream()
                .findFirst()
                .map(item -> new WriteSchema(
                        item.get("id").s(),
                        item.get("userId").s(),
                        item.get("chronicleName").s(),
                        item.get("writeSchemaJson").s(),
                        Instant.parse(item.get("createdAt").s())
                ));
    }
}