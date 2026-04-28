package io.github.grantchen2003.cdb.control.plane.views;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class ViewRepository {
    private static final String VIEWS_TABLE_NAME = "views";

    private final DynamoDbClient dynamo;

    public ViewRepository(DynamoDbClient dynamo) {
        this.dynamo = dynamo;
    }

    public void save(View view) {
        dynamo.putItem(PutItemRequest.builder()
                .tableName(VIEWS_TABLE_NAME)
                .item(Map.of(
                        "id",                    AttributeValue.fromS(view.id()),
                        "userId",                AttributeValue.fromS(view.userId()),
                        "chronicleNameViewName", AttributeValue.fromS(toCompositeKey(view.chronicleName(), view.viewName())),
                        "chronicleName",         AttributeValue.fromS(view.chronicleName()),
                        "viewName",              AttributeValue.fromS(view.viewName()),
                        "readSchemaId",          AttributeValue.fromS(view.readSchemaId()),
                        "createdAt",             AttributeValue.fromS(view.createdAt().toString())
                ))
                .build());
    }

    public boolean exists(String userId, String chronicleName, String viewName) {
        final GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                .tableName(VIEWS_TABLE_NAME)
                .key(Map.of(
                        "userId",                AttributeValue.fromS(userId),
                        "chronicleNameViewName", AttributeValue.fromS(toCompositeKey(chronicleName, viewName))
                ))
                .build());

        return response.hasItem();
    }

    public Optional<View> findById(String id) {
        final QueryResponse response = dynamo.query(QueryRequest.builder()
                .tableName(VIEWS_TABLE_NAME)
                .indexName("id-index")
                .keyConditionExpression("id = :id")
                .expressionAttributeValues(Map.of(
                        ":id", AttributeValue.fromS(id)
                ))
                .build());

        return response.items().stream()
                .findFirst()
                .map(item -> new View(
                        item.get("id").s(),
                        item.get("userId").s(),
                        item.get("chronicleName").s(),
                        item.get("viewName").s(),
                        item.get("readSchemaId").s(),
                        Instant.parse(item.get("createdAt").s())
                ));
    }

    public void deleteById(String id) {
        final QueryResponse response = dynamo.query(QueryRequest.builder()
                .tableName(VIEWS_TABLE_NAME)
                .indexName("id-index")
                .keyConditionExpression("id = :id")
                .expressionAttributeValues(Map.of(
                        ":id", AttributeValue.fromS(id)
                ))
                .build());

        response.items().forEach(item ->
                dynamo.deleteItem(DeleteItemRequest.builder()
                        .tableName(VIEWS_TABLE_NAME)
                        .key(Map.of(
                                "userId",                item.get("userId"),
                                "chronicleNameViewName", item.get("chronicleNameViewName")
                        ))
                        .build())
        );
    }

    private String toCompositeKey(String chronicleName, String viewName) {
        return chronicleName + "#" + viewName;
    }
}