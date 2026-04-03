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
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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
}