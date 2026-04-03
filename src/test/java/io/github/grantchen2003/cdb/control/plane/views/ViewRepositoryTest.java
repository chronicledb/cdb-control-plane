package io.github.grantchen2003.cdb.control.plane.views;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewRepositoryTest {

    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String VIEW_NAME      = "my-view";
    private static final String COMPOSITE_KEY  = CHRONICLE_NAME + "#" + VIEW_NAME;

    @Mock
    private DynamoDbClient dynamo;

    private ViewRepository viewRepository;

    @BeforeEach
    void setUp() {
        viewRepository = new ViewRepository(dynamo);
    }

    @Test
    void save_putsCorrectItemToDynamo() {
        final Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        final View view = new View(USER_ID, CHRONICLE_NAME, VIEW_NAME, createdAt);
        final ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        viewRepository.save(view);

        verify(dynamo).putItem(captor.capture());
        final PutItemRequest request = captor.getValue();
        final Map<String, AttributeValue> item = request.item();

        assertThat(request.tableName()).isEqualTo("views");
        assertThat(item.get("userId").s()).isEqualTo(USER_ID);
        assertThat(item.get("chronicleNameViewName").s()).isEqualTo(COMPOSITE_KEY);
        assertThat(item.get("chronicleName").s()).isEqualTo(CHRONICLE_NAME);
        assertThat(item.get("viewName").s()).isEqualTo(VIEW_NAME);
        assertThat(item.get("createdAt").s()).isEqualTo(createdAt.toString());
    }

    @Test
    void exists_itemFound_returnsTrue() {
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(
                GetItemResponse.builder()
                        .item(Map.of("userId", AttributeValue.fromS(USER_ID)))
                        .build()
        );

        assertThat(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).isTrue();
    }

    @Test
    void exists_itemNotFound_returnsFalse() {
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(
                GetItemResponse.builder().build()
        );

        assertThat(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).isFalse();
    }

    @Test
    void exists_usesCorrectKeyInRequest() {
        when(dynamo.getItem(any(GetItemRequest.class))).thenReturn(
                GetItemResponse.builder().build()
        );
        final ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);

        viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME);

        verify(dynamo).getItem(captor.capture());
        final GetItemRequest request = captor.getValue();
        final Map<String, AttributeValue> key = request.key();

        assertThat(request.tableName()).isEqualTo("views");
        assertThat(key.get("userId").s()).isEqualTo(USER_ID);
        assertThat(key.get("chronicleNameViewName").s()).isEqualTo(COMPOSITE_KEY);
    }
}