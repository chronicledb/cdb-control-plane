package io.github.grantchen2003.cdb.control.plane.replicas;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReplicaController.class)
class ReplicaControllerTest {

    private static final String API_KEY        = "test-api-key";
    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String REPLICA_TYPE   = "REDIS";
    private static final String REPLICA_ID     = "replica-id";
    private static final Replica REPLICA = new Replica(
            REPLICA_ID, USER_ID, CHRONICLE_NAME, ReplicaType.REDIS,
            "i-applier-123", "i-storage-123", "i-txmanager-123",
            "203.0.113.10", ReplicaStatus.PROVISIONING, Instant.parse("2024-01-01T00:00:00Z")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReplicaService replicaService;

    @MockitoBean
    private UserService userService;

    @Test
    void createReplica_success() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.createReplica(USER_ID, CHRONICLE_NAME, REPLICA_TYPE)).thenReturn(REPLICA);

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(CHRONICLE_NAME, REPLICA_TYPE)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REPLICA.id()))
                .andExpect(jsonPath("$.userId").value(REPLICA.userId()))
                .andExpect(jsonPath("$.chronicleName").value(REPLICA.chronicleName()))
                .andExpect(jsonPath("$.type").value(REPLICA_TYPE))
                .andExpect(jsonPath("$.status").value(REPLICA.status().name()))
                .andExpect(jsonPath("$.createdAt").value(REPLICA.createdAt().toString()))
                .andExpect(jsonPath("$.publicIp").value(REPLICA.txManagerPublicIp()));
    }

    @Test
    void createReplica_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(CHRONICLE_NAME, REPLICA_TYPE)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReplica_chronicleNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.createReplica(USER_ID, CHRONICLE_NAME, REPLICA_TYPE))
                .thenThrow(new ChronicleNotFoundException());

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(CHRONICLE_NAME, REPLICA_TYPE)
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Chronicle not found"));
    }

    @Test
    void createReplica_invalidReplicaType_returnsBadRequest() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.createReplica(USER_ID, CHRONICLE_NAME, "INVALID_TYPE"))
                .thenThrow(new InvalidReplicaTypeException("INVALID_TYPE"));

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(CHRONICLE_NAME, "INVALID_TYPE")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid replica type: INVALID_TYPE"));
    }

    @Test
    void deleteReplica_success() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(REPLICA));

        mockMvc.perform(delete("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReplica_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReplica_replicaNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReplica_replicaOwnedByOtherUser_returnsForbidden() throws Exception {
        final Replica otherUsersReplica = new Replica(
                REPLICA_ID, "other-user-id", CHRONICLE_NAME, ReplicaType.REDIS,
                "i-applier-123", "i-storage-123", "i-txmanager-123",
                "203.0.113.10", ReplicaStatus.PROVISIONING, Instant.parse("2024-01-01T00:00:00Z")
        );
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(otherUsersReplica));

        mockMvc.perform(delete("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReplica_success() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(REPLICA));

        mockMvc.perform(get("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REPLICA.id()))
                .andExpect(jsonPath("$.userId").value(REPLICA.userId()))
                .andExpect(jsonPath("$.chronicleName").value(REPLICA.chronicleName()))
                .andExpect(jsonPath("$.type").value(REPLICA_TYPE))
                .andExpect(jsonPath("$.status").value(REPLICA.status().name()))
                .andExpect(jsonPath("$.createdAt").value(REPLICA.createdAt().toString()))
                .andExpect(jsonPath("$.publicIp").value(REPLICA.txManagerPublicIp()));
    }

    @Test
    void getReplica_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(get("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReplica_replicaNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReplica_replicaOwnedByOtherUser_returnsForbidden() throws Exception {
        final Replica otherUsersReplica = new Replica(
                REPLICA_ID, "other-user-id", CHRONICLE_NAME, ReplicaType.REDIS,
                "i-applier-123", "i-storage-123", "i-txmanager-123",
                "203.0.113.10", ReplicaStatus.PROVISIONING, Instant.parse("2024-01-01T00:00:00Z")
        );
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(otherUsersReplica));

        mockMvc.perform(get("/replicas/{replicaId}", REPLICA_ID)
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isForbidden());
    }
}