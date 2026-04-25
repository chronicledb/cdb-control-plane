package io.github.grantchen2003.cdb.control.plane.associations;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.List;
import java.util.Map;

@Repository
public class AssociationRepository {
    private static final String ASSOCIATIONS_TABLE_NAME = "associations";
    private final DynamoDbClient dynamo;

    public AssociationRepository(DynamoDbClient dynamo) {
        this.dynamo = dynamo;
    }

    public void save(Association association) {
        try {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(ASSOCIATIONS_TABLE_NAME)
                    .item(Map.of(
                            "replicaId", AttributeValue.fromS(association.replicaId()),
                            "viewId", AttributeValue.fromS(association.viewId())
                    ))
                    .conditionExpression("attribute_not_exists(replicaId) AND attribute_not_exists(viewId)")
                    .build());
        } catch (ConditionalCheckFailedException e) {
            throw new DuplicateAssociationException(association.replicaId(), association.viewId());
        }
    }

    public List<Association> findByViewId(String viewId) {
        return dynamo.query(QueryRequest.builder()
                        .tableName(ASSOCIATIONS_TABLE_NAME)
                        .keyConditionExpression("viewId = :viewId")
                        .expressionAttributeValues(Map.of(":viewId", AttributeValue.fromS(viewId)))
                        .build())
                .items()
                .stream()
                .map(item -> new Association(item.get("replicaId").s(), item.get("viewId").s()))
                .toList();
    }
}
