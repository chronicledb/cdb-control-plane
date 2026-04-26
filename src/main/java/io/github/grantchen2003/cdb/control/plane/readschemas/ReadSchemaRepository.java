package io.github.grantchen2003.cdb.control.plane.readschemas;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

@Repository
public class ReadSchemaRepository {
    private static final String READ_SCHEMAS_TABLE_NAME = "read_schemas";

    private final DynamoDbClient dynamo;

    public ReadSchemaRepository(DynamoDbClient dynamo) {
        this.dynamo = dynamo;
    }

    public void save(ReadSchema readSchema) {
        dynamo.putItem(PutItemRequest.builder()
                .tableName(READ_SCHEMAS_TABLE_NAME)
                .item(Map.of(
                        "id",              AttributeValue.fromS(readSchema.id()),
                        "userId",          AttributeValue.fromS(readSchema.userId()),
                        "chronicleName",   AttributeValue.fromS(readSchema.chronicleName()),
                        "viewName",        AttributeValue.fromS(readSchema.viewName()),
                        "readSchemaJson",  AttributeValue.fromS(readSchema.readSchemaJson()),
                        "createdAt",       AttributeValue.fromS(readSchema.createdAt().toString())
                ))
                .build());
    }
}
