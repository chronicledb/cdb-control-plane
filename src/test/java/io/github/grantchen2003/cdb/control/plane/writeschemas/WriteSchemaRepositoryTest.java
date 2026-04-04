package io.github.grantchen2003.cdb.control.plane.writeschemas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WriteSchemaRepositoryTest {

    private static final String WRITE_SCHEMA_ID = "write-schema-123";
    private static final String USER_ID         = "user-123";
    private static final String CHRONICLE_NAME  = "my-chronicle";
    private static final String WRITE_SCHEMA_JSON = "{\"version\":\"2018-06-01\"}";

    @Mock
    private DynamoDbClient dynamo;

    private WriteSchemaRepository writeSchemaRepository;

    @BeforeEach
    void setUp() {
        writeSchemaRepository = new WriteSchemaRepository(dynamo);
    }

    @Test
    void save_putsCorrectItemToDynamo() {
        final Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        final WriteSchema writeSchema = new WriteSchema(WRITE_SCHEMA_ID, USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON, createdAt);
        final ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        writeSchemaRepository.save(writeSchema);

        verify(dynamo).putItem(captor.capture());
        final PutItemRequest request = captor.getValue();
        final Map<String, AttributeValue> item = request.item();

        assertThat(request.tableName()).isEqualTo("write_schemas");
        assertThat(item.get("id").s()).isEqualTo(WRITE_SCHEMA_ID);
        assertThat(item.get("userId").s()).isEqualTo(USER_ID);
        assertThat(item.get("chronicleName").s()).isEqualTo(CHRONICLE_NAME);
        assertThat(item.get("writeSchemaJson").s()).isEqualTo(WRITE_SCHEMA_JSON);
        assertThat(item.get("createdAt").s()).isEqualTo(createdAt.toString());
    }
}