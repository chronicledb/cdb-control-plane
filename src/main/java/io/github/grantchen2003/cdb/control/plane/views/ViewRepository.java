package io.github.grantchen2003.cdb.control.plane.views;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

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
                        "viewId",                AttributeValue.fromS(view.viewId()),
                        "userId",                AttributeValue.fromS(view.userId()),
                        "chronicleNameViewName", AttributeValue.fromS(toCompositeKey(view.chronicleName(), view.viewName())),
                        "chronicleName",         AttributeValue.fromS(view.chronicleName()),
                        "viewName",              AttributeValue.fromS(view.viewName()),
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

    private String toCompositeKey(String chronicleName, String viewName) {
        return chronicleName + "#" + viewName;
    }
}