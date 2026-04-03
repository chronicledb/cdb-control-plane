package io.github.grantchen2003.cdb.control.plane.associations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.replicas.Replica;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaService;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaStatus;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import io.github.grantchen2003.cdb.control.plane.views.View;
import io.github.grantchen2003.cdb.control.plane.views.ViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssociationController.class)
class AssociationControllerTest {

    private static final String API_KEY = "test-api-key";
    private static final String USER_ID = "user-123";
    private static final String REPLICA_ID = "replica-123";
    private static final String VIEW_ID = "view-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final View VIEW = new View(VIEW_ID, USER_ID, CHRONICLE_NAME, "my-view", Instant.parse("2024-01-01T00:00:00Z"));
    private static final Replica REPLICA = new Replica(
            REPLICA_ID,
            USER_ID,
            CHRONICLE_NAME,
            ReplicaType.REDIS,
            "i-applier-123",
            "i-storage-123",
            "i-txmanager-123",
            "203.0.113.10",
            ReplicaStatus.PROVISIONING,
            Instant.parse("2024-01-01T00:00:00Z")
    );
    private static final Association ASSOCIATION = new Association(REPLICA_ID, VIEW_ID);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssociationService associationService;

    @MockitoBean
    private ReplicaService replicaService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ViewService viewService;

    @Test
    void createAssociation_success() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(REPLICA));
        when(associationService.createAssociation(REPLICA_ID, VIEW_ID)).thenReturn(ASSOCIATION);

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replicaId").value(REPLICA_ID))
                .andExpect(jsonPath("$.viewId").value(VIEW_ID));
    }

    @Test
    void createAssociation_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAssociation_viewNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("View not found"));
    }

    @Test
    void createAssociation_viewOwnedByOtherUser_returnsForbidden() throws Exception {
        final View otherUsersView = new View(VIEW_ID, "other-user", CHRONICLE_NAME, "my-view", Instant.parse("2024-01-01T00:00:00Z"));
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(otherUsersView));

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAssociation_replicaNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Replica not found"));
    }

    @Test
    void createAssociation_replicaOwnedByOtherUser_returnsForbidden() throws Exception {
        final Replica otherUsersReplica = new Replica(
                REPLICA_ID,
                "other-user-id",
                CHRONICLE_NAME,
                ReplicaType.REDIS,
                "i-applier-123",
                "i-storage-123",
                "i-txmanager-123",
                "203.0.113.10",
                ReplicaStatus.PROVISIONING,
                Instant.parse("2024-01-01T00:00:00Z")
        );
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(otherUsersReplica));

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAssociation_replicaAndViewInDifferentChronicles_returnsUnprocessableEntity() throws Exception {
        final Replica replicaInOtherChronicle = new Replica(
                REPLICA_ID,
                USER_ID,
                "other-chronicle",
                ReplicaType.REDIS,
                "i-applier-123",
                "i-storage-123",
                "i-txmanager-123",
                "203.0.113.10",
                ReplicaStatus.PROVISIONING,
                Instant.parse("2024-01-01T00:00:00Z")
        );

        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(replicaInOtherChronicle));

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Replica and view must belong to the same chronicle"));
    }

    @Test
    void createAssociation_duplicateAssociation_returnsConflict() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(REPLICA));
        when(associationService.createAssociation(REPLICA_ID, VIEW_ID))
                .thenThrow(new DuplicateAssociationException(REPLICA_ID, VIEW_ID));

        mockMvc.perform(post("/associations")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssociationController.CreateAssociationRequest(VIEW_ID, REPLICA_ID)
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Association between replica " + REPLICA_ID + " and view " + VIEW_ID + " already exists"
                ));
    }
}