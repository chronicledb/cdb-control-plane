package io.github.grantchen2003.cdb.control.plane.replicas;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReplicaRepository {
    private static final String REPLICAS_TABLE_NAME = "replicas";
    private final DynamoDbClient dynamo;

    public ReplicaRepository(DynamoDbClient dynamo) {
        this.dynamo = dynamo;
    }

    public void save(Replica replica) {
        final Map<String, AttributeValue> item = new HashMap<>();
        item.put("id",            AttributeValue.fromS(replica.id()));
        item.put("userId",        AttributeValue.fromS(replica.userId()));
        item.put("chronicleId",   AttributeValue.fromS(replica.chronicleId()));
        item.put("chronicleName", AttributeValue.fromS(replica.chronicleName()));
        item.put("type",          AttributeValue.fromS(replica.type().name()));
        item.put("status",        AttributeValue.fromS(replica.status().name()));
        item.put("createdAt",     AttributeValue.fromS(replica.createdAt().toString()));

        if (replica.applierInstanceId() != null) {
            item.put("applierInstanceId", AttributeValue.fromS(replica.applierInstanceId()));
        }

        if (replica.storageEngineInstanceId() != null) {
            item.put("storageEngineInstanceId", AttributeValue.fromS(replica.storageEngineInstanceId()));
        }

        if (replica.txManagerInstanceId() != null) {
            item.put("txManagerInstanceId", AttributeValue.fromS(replica.txManagerInstanceId()));
        }

        if (replica.txManagerPublicIp() != null) {
            item.put("txManagerPublicIp", AttributeValue.fromS(replica.txManagerPublicIp()));
        }

        dynamo.putItem(PutItemRequest.builder()
                .tableName(REPLICAS_TABLE_NAME)
                .item(item)
                .build());
    }

    public Optional<Replica> findById(String id) {
        final GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                .tableName(REPLICAS_TABLE_NAME)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }

        return Optional.of(mapToReplica(response.item()));
    }

    public List<Replica> findByStatus(ReplicaStatus status) {
        final QueryRequest request = QueryRequest.builder()
                .tableName(REPLICAS_TABLE_NAME)
                .indexName("replicas-by-status")
                .keyConditionExpression("#s = :status")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(":status", AttributeValue.fromS(status.name())))
                .build();

        return dynamo.query(request).items().stream()
                .map(this::mapToReplica)
                .toList();
    }

    public void deleteById(String id) {
        dynamo.deleteItem(DeleteItemRequest.builder()
                .tableName(REPLICAS_TABLE_NAME)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .build());
    }

    private Replica mapToReplica(Map<String, AttributeValue> item) {
        return new Replica(
                item.get("id").s(),
                item.get("userId").s(),
                item.get("chronicleId").s(),
                item.get("chronicleName").s(),
                ReplicaType.valueOf(item.get("type").s()),
                item.containsKey("applierInstanceId")       ? item.get("applierInstanceId").s()       : null,
                item.containsKey("storageEngineInstanceId") ? item.get("storageEngineInstanceId").s() : null,
                item.containsKey("txManagerInstanceId")     ? item.get("txManagerInstanceId").s()     : null,
                item.containsKey("txManagerPublicIp")       ? item.get("txManagerPublicIp").s()       : null,
                ReplicaStatus.valueOf(item.get("status").s()),
                Instant.parse(item.get("createdAt").s())
        );
    }
}