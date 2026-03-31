package io.github.grantchen2003.cdb.control.plane.replicas;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

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
        dynamo.putItem(PutItemRequest.builder()
                .tableName(REPLICAS_TABLE_NAME)
                .item(Map.of(
                        "id",               AttributeValue.fromS(replica.id()),
                        "userId",           AttributeValue.fromS(replica.userId()),
                        "chronicleName",    AttributeValue.fromS(replica.chronicleName()),
                        "type",             AttributeValue.fromS(replica.type().name()),
                        "ec2InstanceId",    AttributeValue.fromS(replica.ec2InstanceId()),
                        "createdAt",        AttributeValue.fromS(replica.createdAt().toString())
                ))
                .build());
    }

    public Optional<String> findUserIdById(String id) {
        final GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                .tableName(REPLICAS_TABLE_NAME)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .projectionExpression("userId")
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }

        return Optional.of(response.item().get("userId").s());
    }

    public void deleteById(String id) {
        dynamo.deleteItem(DeleteItemRequest.builder()
                .tableName(REPLICAS_TABLE_NAME)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .build());
    }
}
