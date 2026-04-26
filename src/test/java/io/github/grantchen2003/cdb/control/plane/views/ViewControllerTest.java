package io.github.grantchen2003.cdb.control.plane.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.associations.AssociationService;
import io.github.grantchen2003.cdb.control.plane.associations.ForbiddenAssociationException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViewController.class)
class ViewControllerTest {

    private static final String API_KEY        = "test-api-key";
    private static final String VIEW_ID        = "view-123";
    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String VIEW_NAME      = "my-view";
    private static final String READ_SCHEMA_ID = "read-schema-123";
    private static final String READ_SCHEMA_JSON = "{}";
    private static final View VIEW = new View(
            VIEW_ID, USER_ID,
            CHRONICLE_NAME,
            VIEW_NAME,
            READ_SCHEMA_ID,
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssociationService associationService;

    @MockitoBean
    private ChronicleService chronicleService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ViewService viewService;

    @Test
    void createView_success() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(true);
        when(viewService.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(false);
        when(viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)).thenReturn(VIEW);

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(VIEW.userId()))
                .andExpect(jsonPath("$.chronicleName").value(VIEW.chronicleName()))
                .andExpect(jsonPath("$.viewName").value(VIEW.viewName()))
                .andExpect(jsonPath("$.readSchemaId").value(VIEW.readSchemaId()));
    }

    @Test
    void createView_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createView_chronicleNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)).thenThrow(new ChronicleNotFoundException());

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Chronicle not found"));
    }

    @Test
    void createView_duplicateViewName_returnsConflict() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)).thenThrow(new DuplicateViewException());

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("View already exists"));
    }

    @Test
    void getViewReplicaEndpoints_success() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.getRunningReplicaEndpoints(USER_ID, VIEW_ID))
                .thenReturn(List.of("203.0.113.10:5432", "203.0.113.11:5432"));

        mockMvc.perform(get("/views/{viewId}/replicas", VIEW_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints").isArray())
                .andExpect(jsonPath("$.endpoints[0]").value("203.0.113.10:5432"))
                .andExpect(jsonPath("$.endpoints[1]").value("203.0.113.11:5432"));
    }

    @Test
    void getViewReplicaEndpoints_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(get("/views/{viewId}/replicas", VIEW_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getViewReplicaEndpoints_forbidden_returnsForbidden() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.getRunningReplicaEndpoints(USER_ID, VIEW_ID))
                .thenThrow(new ForbiddenAssociationException());

        mockMvc.perform(get("/views/{viewId}/replicas", VIEW_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isForbidden());
    }

    @Test
    void getViewReplicaEndpoints_noRunningReplicas_returnsEmptyList() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.getRunningReplicaEndpoints(USER_ID, VIEW_ID)).thenReturn(List.of());

        mockMvc.perform(get("/views/{viewId}/replicas", VIEW_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints").isEmpty());
    }
}