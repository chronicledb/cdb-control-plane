package io.github.grantchen2003.cdb.control.plane.replicas;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

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
}
