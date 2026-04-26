package io.github.grantchen2003.cdb.control.plane.chronicles;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChronicleController.class)
class ChronicleControllerTest {

    private static final String CHRONICLE_ID     = "chronicle-123";
    private static final String USER_ID          = "user-123";
    private static final String CHRONICLE_NAME   = "my-table";
    private static final String RAW_API_KEY      = "valid-api-key";
    private static final String WRITE_SCHEMA_ID  = "schema-123";
    private static final String WRITE_SCHEMA_JSON = "{\"types\":{},\"tables\":{}}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChronicleService chronicleService;

    @MockitoBean
    private UserService userService;

    // -----------------------------------------------------------------------
    // POST /chronicles/{chronicleName} — happy path
    // -----------------------------------------------------------------------

    @Test
    void createChronicle_returns201() throws Exception {
        stubAuth(true);
        stubChronicleService();

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isCreated());
    }

    @Test
    void createChronicle_responseContainsAllFields() throws Exception {
        stubAuth(true);
        stubChronicleService();

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(jsonPath("$.id", equalTo(CHRONICLE_ID)))
                .andExpect(jsonPath("$.userId", equalTo(USER_ID)))
                .andExpect(jsonPath("$.name", equalTo(CHRONICLE_NAME)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void createChronicle_responseContentTypeIsJson() throws Exception {
        stubAuth(true);
        stubChronicleService();

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void createChronicle_callsServiceWithCorrectArguments() throws Exception {
        stubAuth(true);
        stubChronicleService();

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                .header("X-Api-Key", RAW_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()));

        verify(chronicleService, times(1)).createChronicle(eq(USER_ID), eq(CHRONICLE_NAME), eq(WRITE_SCHEMA_JSON));
    }

    // -----------------------------------------------------------------------
    // POST /chronicles/{chronicleName} — auth failures
    // -----------------------------------------------------------------------

    @Test
    void createChronicle_returns401WhenApiKeyIsUnknown() throws Exception {
        stubAuth(false);

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", "unknown-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createChronicle_returns400WhenApiKeyHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createChronicle_doesNotCallChronicleServiceWhenUnauthorized() throws Exception {
        stubAuth(false);

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                .header("X-Api-Key", "unknown-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()));

        verify(chronicleService, never()).createChronicle(anyString(), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // POST /chronicles/{chronicleName} — bad request (invalid write schema)
    // -----------------------------------------------------------------------

    @Test
    void createChronicle_returns400WhenWriteSchemaIsInvalid() throws Exception {
        stubAuth(true);
        when(chronicleService.createChronicle(eq(USER_ID), eq(CHRONICLE_NAME), eq(WRITE_SCHEMA_JSON)))
                .thenThrow(new InvalidWriteSchemaException("missing required field 'types'"));

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    void createChronicle_returns400WhenWriteSchemaIsMissing() throws Exception {
        stubAuth(true);

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /chronicles/{chronicleName} — conflict (name already exists for this userId)
    // -----------------------------------------------------------------------

    @Test
    void createChronicle_returns409WhenNameAlreadyExists() throws Exception {
        stubAuth(true);
        when(chronicleService.createChronicle(eq(USER_ID), eq(CHRONICLE_NAME), eq(WRITE_SCHEMA_JSON)))
                .thenThrow(new DuplicateChronicleException(USER_ID, CHRONICLE_NAME));

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void createChronicle_conflictResponseContainsErrorMessage() throws Exception {
        stubAuth(true);
        when(chronicleService.createChronicle(eq(USER_ID), eq(CHRONICLE_NAME), eq(WRITE_SCHEMA_JSON)))
                .thenThrow(new DuplicateChronicleException(USER_ID, CHRONICLE_NAME));

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    void createChronicle_sameNameUnderDifferentUserIdsDoesNotConflict() throws Exception {
        stubAuth(true);
        when(chronicleService.createChronicle(eq(USER_ID), eq(CHRONICLE_NAME), eq(WRITE_SCHEMA_JSON)))
                .thenReturn(new Chronicle(CHRONICLE_ID, USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_ID, Instant.now()));

        mockMvc.perform(post("/chronicles/{chronicleName}", CHRONICLE_NAME)
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isCreated());
    }

    // -----------------------------------------------------------------------
    // POST /chronicles/{chronicleName} — bad requests (invalid name)
    // -----------------------------------------------------------------------

    @Test
    void createChronicle_returns400WhenNameIsBlank() throws Exception {
        stubAuth(true);

        mockMvc.perform(post("/chronicles/{chronicleName}", "   ")
                        .header("X-Api-Key", RAW_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest());

        verify(chronicleService, never()).createChronicle(anyString(), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // Wrong HTTP methods
    // -----------------------------------------------------------------------

    @Test
    void getChronicles_returns405() throws Exception {
        mockMvc.perform(get("/chronicles/{chronicleName}", CHRONICLE_NAME))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void deleteChronicles_returns405() throws Exception {
        mockMvc.perform(delete("/chronicles/{chronicleName}", CHRONICLE_NAME))
                .andExpect(status().isMethodNotAllowed());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void stubAuth(boolean found) {
        when(userService.findUserIdByRawApiKey(anyString()))
                .thenReturn(found ? Optional.of(USER_ID) : Optional.empty());
    }

    private void stubChronicleService() {
        when(chronicleService.createChronicle(eq(USER_ID), eq(CHRONICLE_NAME), eq(WRITE_SCHEMA_JSON)))
                .thenReturn(new Chronicle(CHRONICLE_ID, USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_ID, Instant.now()));
    }

    private String requestBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of("writeSchemaJson", WRITE_SCHEMA_JSON));
    }
}