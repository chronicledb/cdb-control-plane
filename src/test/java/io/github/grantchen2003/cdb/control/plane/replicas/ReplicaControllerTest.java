package io.github.grantchen2003.cdb.control.plane.replicas;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReplicaController.class)
class ReplicaControllerTest {
    private static final String API_KEY       = "test-api-key";
    private static final String userId        = "3e30e447-ecd4-48b0-b592-207cd16b0609";
    private static final String chronicleName = "my-chronicle";
    private static final String replicaType   = "REDIS";
    private static final Replica replica      = new Replica(
            "replica-id",
            userId,
            chronicleName,
            ReplicaType.REDIS,
            "i-0abc123def456",
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChronicleService chronicleService;

    @MockitoBean
    private ReplicaService replicaService;

    @MockitoBean
    private UserService userService;

    @Test
    void createReplica_success() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(userId));
        when(chronicleService.existsByUserIdAndName(userId, chronicleName)).thenReturn(true);
        when(replicaService.createReplica(userId, chronicleName, ReplicaType.REDIS)).thenReturn(replica);

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(chronicleName, replicaType)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(replica.id()))
                .andExpect(jsonPath("$.userId").value(replica.userId()))
                .andExpect(jsonPath("$.chronicleName").value(replica.chronicleName()))
                .andExpect(jsonPath("$.type").value(replicaType))
                .andExpect(jsonPath("$.createdAt").value(replica.createdAt().toString()));
    }

    @Test
    void createReplica_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(chronicleName, replicaType)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReplica_chronicleNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(userId));
        when(chronicleService.existsByUserIdAndName(userId, chronicleName)).thenReturn(false);

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(chronicleName, replicaType)
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Chronicle not found"));
    }

    @Test
    void createReplica_invalidReplicaType_returnsBadRequest() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(userId));
        when(chronicleService.existsByUserIdAndName(userId, chronicleName)).thenReturn(true);

        mockMvc.perform(post("/replicas")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReplicaController.CreateReplicaRequest(chronicleName, "INVALID_TYPE")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid replica type: INVALID_TYPE"));
    }
}