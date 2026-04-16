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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.List;
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
            "chronicle-123",
            "my-chronicle",
            ReplicaType.REDIS,
            "i-applier-123",
            "i-storage-123",
            "i-txmanager-123",
            "203.0.113.10",
            ReplicaStatus.PROVISIONING,
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Mock
    private DynamoDbClient dynamo;

    @InjectMocks
    private ReplicaRepository replicaRepository;

    @Test
    void save_putsCorrectItemToDynamo() {
        replicaRepository.save(replica);

        final ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamo).putItem(captor.capture());

        final PutItemRequest request = captor.getValue();
        final Map<String, AttributeValue> item = request.item();

        assertThat(request.tableName()).isEqualTo("replicas");
        assertThat(item.get("id").s()).isEqualTo(replica.id());
        assertThat(item.get("userId").s()).isEqualTo(replica.userId());
        assertThat(item.get("chronicleId").s()).isEqualTo(replica.chronicleId());
        assertThat(item.get("chronicleName").s()).isEqualTo(replica.chronicleName());
        assertThat(item.get("type").s()).isEqualTo(replica.type().name());
        assertThat(item.get("applierInstanceId").s()).isEqualTo(replica.applierInstanceId());
        assertThat(item.get("storageEngineInstanceId").s()).isEqualTo(replica.storageEngineInstanceId());
        assertThat(item.get("txManagerInstanceId").s()).isEqualTo(replica.txManagerInstanceId());
        assertThat(item.get("txManagerPublicIp").s()).isEqualTo(replica.txManagerPublicIp());
        assertThat(item.get("status").s()).isEqualTo(replica.status().name());
        assertThat(item.get("createdAt").s()).isEqualTo(replica.createdAt().toString());
    }

    @Test
    void save_doesNotIncludeTxManagerPublicIp_whenNull() {
        final Replica replicaWithNullIp = replica.withTxManagerPublicIp(null);

        replicaRepository.save(replicaWithNullIp);

        final ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamo).putItem(captor.capture());
        assertThat(captor.getValue().item()).doesNotContainKey("txManagerPublicIp");
    }

    @Test
    void findById_found_returnsReplica() {
        final GetItemResponse response = GetItemResponse.builder()
                .item(Map.ofEntries(
                        Map.entry("id",                      AttributeValue.fromS(replica.id())),
                        Map.entry("userId",                  AttributeValue.fromS(replica.userId())),
                        Map.entry("chronicleId",             AttributeValue.fromS(replica.chronicleId())),
                        Map.entry("chronicleName",           AttributeValue.fromS(replica.chronicleName())),
                        Map.entry("type",                    AttributeValue.fromS(replica.type().name())),
                        Map.entry("applierInstanceId",       AttributeValue.fromS(replica.applierInstanceId())),
                        Map.entry("storageEngineInstanceId", AttributeValue.fromS(replica.storageEngineInstanceId())),
                        Map.entry("txManagerInstanceId",     AttributeValue.fromS(replica.txManagerInstanceId())),
                        Map.entry("txManagerPublicIp",       AttributeValue.fromS(replica.txManagerPublicIp())),
                        Map.entry("status",                  AttributeValue.fromS(replica.status().name())),
                        Map.entry("createdAt",               AttributeValue.fromS(replica.createdAt().toString()))
                ))
                .build();
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(response);

        final Optional<Replica> result = replicaRepository.findById(replica.id());

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(replica.id());
        assertThat(result.get().userId()).isEqualTo(replica.userId());
        assertThat(result.get().chronicleId()).isEqualTo(replica.chronicleId());
        assertThat(result.get().chronicleName()).isEqualTo(replica.chronicleName());
        assertThat(result.get().type()).isEqualTo(replica.type());
        assertThat(result.get().applierInstanceId()).isEqualTo(replica.applierInstanceId());
        assertThat(result.get().storageEngineInstanceId()).isEqualTo(replica.storageEngineInstanceId());
        assertThat(result.get().txManagerInstanceId()).isEqualTo(replica.txManagerInstanceId());
        assertThat(result.get().txManagerPublicIp()).isEqualTo(replica.txManagerPublicIp());
        assertThat(result.get().status()).isEqualTo(replica.status());
        assertThat(result.get().createdAt()).isEqualTo(replica.createdAt());
    }

    @Test
    void findById_found_handlesNullTxManagerPublicIp() {
        final GetItemResponse response = GetItemResponse.builder()
                .item(Map.of(
                        "id",                      AttributeValue.fromS(replica.id()),
                        "userId",                  AttributeValue.fromS(replica.userId()),
                        "chronicleId",             AttributeValue.fromS(replica.chronicleId()),
                        "chronicleName",           AttributeValue.fromS(replica.chronicleName()),
                        "type",                    AttributeValue.fromS(replica.type().name()),
                        "applierInstanceId",       AttributeValue.fromS(replica.applierInstanceId()),
                        "storageEngineInstanceId", AttributeValue.fromS(replica.storageEngineInstanceId()),
                        "txManagerInstanceId",     AttributeValue.fromS(replica.txManagerInstanceId()),
                        "status",                  AttributeValue.fromS(replica.status().name()),
                        "createdAt",               AttributeValue.fromS(replica.createdAt().toString())
                ))
                .build();
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(response);

        final Optional<Replica> result = replicaRepository.findById(replica.id());

        assertThat(result).isPresent();
        assertThat(result.get().txManagerPublicIp()).isNull();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        final Optional<Replica> result = replicaRepository.findById(replica.id());

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatus_returnsMatchingReplicas() {
        final QueryResponse response = QueryResponse.builder()
                .items(List.of(Map.ofEntries(
                        Map.entry("id",                      AttributeValue.fromS(replica.id())),
                        Map.entry("userId",                  AttributeValue.fromS(replica.userId())),
                        Map.entry("chronicleId",             AttributeValue.fromS(replica.chronicleId())),
                        Map.entry("chronicleName",           AttributeValue.fromS(replica.chronicleName())),
                        Map.entry("type",                    AttributeValue.fromS(replica.type().name())),
                        Map.entry("applierInstanceId",       AttributeValue.fromS(replica.applierInstanceId())),
                        Map.entry("storageEngineInstanceId", AttributeValue.fromS(replica.storageEngineInstanceId())),
                        Map.entry("txManagerInstanceId",     AttributeValue.fromS(replica.txManagerInstanceId())),
                        Map.entry("txManagerPublicIp",       AttributeValue.fromS(replica.txManagerPublicIp())),
                        Map.entry("status",                  AttributeValue.fromS(replica.status().name())),
                        Map.entry("createdAt",               AttributeValue.fromS(replica.createdAt().toString()))
                )))
                .build();
        when(dynamo.query(any(QueryRequest.class))).thenReturn(response);

        final List<Replica> result = replicaRepository.findByStatus(ReplicaStatus.PROVISIONING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(replica.id());
        assertThat(result.get(0).status()).isEqualTo(ReplicaStatus.PROVISIONING);
    }

    @Test
    void findByStatus_returnsEmptyList_whenNoMatches() {
        when(dynamo.query(any(QueryRequest.class))).thenReturn(QueryResponse.builder().build());

        final List<Replica> result = replicaRepository.findByStatus(ReplicaStatus.PROVISIONING);

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