package io.github.grantchen2003.cdb.control.plane.associations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssociationRepositoryTest {

    private static final String REPLICA_ID = "replica-123";
    private static final String VIEW_ID = "view-123";

    @Mock
    private DynamoDbClient dynamo;

    private AssociationRepository associationRepository;

    @BeforeEach
    void setUp() {
        associationRepository = new AssociationRepository(dynamo);
    }

    @Test
    void save_putsCorrectItemToDynamo() {
        final Association association = new Association(REPLICA_ID, VIEW_ID);
        final ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        associationRepository.save(association);

        verify(dynamo).putItem(captor.capture());
        final PutItemRequest request = captor.getValue();
        final Map<String, AttributeValue> item = request.item();

        assertThat(request.tableName()).isEqualTo("associations");
        assertThat(item.get("replicaId").s()).isEqualTo(REPLICA_ID);
        assertThat(item.get("viewId").s()).isEqualTo(VIEW_ID);
    }

    @Test
    void save_usesConditionExpression() {
        final Association association = new Association(REPLICA_ID, VIEW_ID);
        final ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        associationRepository.save(association);

        verify(dynamo).putItem(captor.capture());
        assertThat(captor.getValue().conditionExpression())
                .isEqualTo("attribute_not_exists(replicaId) AND attribute_not_exists(viewId)");
    }

    @Test
    void save_duplicateAssociation_throwsDuplicateAssociationException() {
        doThrow(ConditionalCheckFailedException.builder().build())
                .when(dynamo).putItem(any(PutItemRequest.class));

        assertThatThrownBy(() -> associationRepository.save(new Association(REPLICA_ID, VIEW_ID)))
                .isInstanceOf(DuplicateAssociationException.class)
                .hasMessageContaining(REPLICA_ID)
                .hasMessageContaining(VIEW_ID);
    }

    @Test
    void findByViewId_returnsMatchingAssociations() {
        final List<Map<String, AttributeValue>> items = List.of(
                Map.of(
                        "replicaId", AttributeValue.fromS(REPLICA_ID),
                        "viewId",    AttributeValue.fromS(VIEW_ID)
                )
        );

        when(dynamo.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(items).build());

        final List<Association> result = associationRepository.findByViewId(VIEW_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).replicaId()).isEqualTo(REPLICA_ID);
        assertThat(result.get(0).viewId()).isEqualTo(VIEW_ID);
    }

    @Test
    void findByViewId_queriesCorrectTableAndKey() {
        when(dynamo.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of()).build());

        final ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        associationRepository.findByViewId(VIEW_ID);

        verify(dynamo).query(captor.capture());
        final QueryRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("associations");
        assertThat(request.keyConditionExpression()).contains("viewId");
        assertThat(request.expressionAttributeValues().get(":viewId").s()).isEqualTo(VIEW_ID);
    }

    @Test
    void findByViewId_noAssociations_returnsEmptyList() {
        when(dynamo.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of()).build());

        assertThat(associationRepository.findByViewId(VIEW_ID)).isEmpty();
    }

    @Test
    void findByReplicaId_returnsMatchingAssociations() {
        final List<Map<String, AttributeValue>> items = List.of(
                Map.of(
                        "replicaId", AttributeValue.fromS(REPLICA_ID),
                        "viewId",    AttributeValue.fromS(VIEW_ID)
                )
        );
        when(dynamo.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(items).build());

        final List<Association> result = associationRepository.findByReplicaId(REPLICA_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).replicaId()).isEqualTo(REPLICA_ID);
        assertThat(result.get(0).viewId()).isEqualTo(VIEW_ID);
    }

    @Test
    void findByReplicaId_queriesCorrectTableIndexAndKey() {
        when(dynamo.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of()).build());

        final ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        associationRepository.findByReplicaId(REPLICA_ID);

        verify(dynamo).query(captor.capture());
        final QueryRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("associations");
        assertThat(request.indexName()).isEqualTo("replicaId-index");
        assertThat(request.keyConditionExpression()).contains("replicaId");
        assertThat(request.expressionAttributeValues().get(":replicaId").s()).isEqualTo(REPLICA_ID);
    }

    @Test
    void findByReplicaId_noAssociations_returnsEmptyList() {
        when(dynamo.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of()).build());

        assertThat(associationRepository.findByReplicaId(REPLICA_ID)).isEmpty();
    }

    @Test
    void delete_deletesCorrectItemFromDynamo() {
        final ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);

        associationRepository.delete(VIEW_ID, REPLICA_ID);

        verify(dynamo).deleteItem(captor.capture());
        final DeleteItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("associations");
        assertThat(request.key().get("viewId").s()).isEqualTo(VIEW_ID);
        assertThat(request.key().get("replicaId").s()).isEqualTo(REPLICA_ID);
    }
}