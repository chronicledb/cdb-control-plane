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
        item.put("chronicleName", AttributeValue.fromS(replica.chronicleName()));
        item.put("type",          AttributeValue.fromS(replica.type().name()));
        item.put("ec2InstanceId", AttributeValue.fromS(replica.ec2InstanceId()));
        item.put("status",        AttributeValue.fromS(replica.status().name()));
        item.put("createdAt",     AttributeValue.fromS(replica.createdAt().toString()));

        if (replica.publicIp() != null) {
            item.put("publicIp", AttributeValue.fromS(replica.publicIp()));
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

        final Map<String, AttributeValue> item = response.item();

        final Replica replica = mapToReplica(item);

        return Optional.of(replica);
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
                item.get("chronicleName").s(),
                ReplicaType.valueOf(item.get("type").s()),
                item.get("ec2InstanceId").s(),
                item.get("publicIp") != null ? item.get("publicIp").s() : null,
                ReplicaStatus.valueOf(item.get("status").s()),
                Instant.parse(item.get("createdAt").s())
        );
    }
}
