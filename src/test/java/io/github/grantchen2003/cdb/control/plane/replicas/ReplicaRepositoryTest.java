package io.github.grantchen2003.cdb.control.plane.replicas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplicaRepositoryTest {
    private static final Replica replica = new Replica(
            "replica-id",
            "user-id",
            "my-chronicle",
            ReplicaType.REDIS,
            "i-0abc123def456",
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Mock
    private DynamoDbClient dynamo;

    @InjectMocks
    private ReplicaRepository replicaRepository;

    @Test
    void save_putsCorrectItemToDynamo() {
        replicaRepository.save(replica);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamo).putItem(captor.capture());

        PutItemRequest request = captor.getValue();
        Map<String, AttributeValue> item = request.item();

        assertThat(request.tableName()).isEqualTo("replicas");
        assertThat(item.get("id").s()).isEqualTo(replica.id());
        assertThat(item.get("userId").s()).isEqualTo(replica.userId());
        assertThat(item.get("chronicleName").s()).isEqualTo(replica.chronicleName());
        assertThat(item.get("type").s()).isEqualTo(replica.type().name());
        assertThat(item.get("ec2InstanceId").s()).isEqualTo(replica.ec2InstanceId());
        assertThat(item.get("createdAt").s()).isEqualTo(replica.createdAt().toString());
    }

    @Test
    void findUserIdById_found_returnsUserId() {
        final GetItemResponse response = GetItemResponse.builder()
                .item(Map.of("userId", AttributeValue.fromS(replica.userId())))
                .build();
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(response);

        final Optional<String> result = replicaRepository.findUserIdById(replica.id());

        assertThat(result).contains(replica.userId());
    }

    @Test
    void findUserIdById_notFound_returnsEmpty() {
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        final Optional<String> result = replicaRepository.findUserIdById(replica.id());

        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_deletesCorrectItem() {
        replicaRepository.deleteById(replica.id());

        final ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(dynamo).deleteItem(captor.capture());

        final DeleteItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("replicas");
        assertThat(request.key().get("id").s()).isEqualTo(replica.id());
    }
}