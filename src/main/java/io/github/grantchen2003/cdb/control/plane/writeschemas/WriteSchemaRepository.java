package io.github.grantchen2003.cdb.control.plane.writeschemas;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

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
}