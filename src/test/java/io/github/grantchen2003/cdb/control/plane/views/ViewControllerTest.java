package io.github.grantchen2003.cdb.control.plane.views;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
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
    private static final View   VIEW           = new View(VIEW_ID, USER_ID, CHRONICLE_NAME, VIEW_NAME, Instant.parse("2024-01-01T00:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        when(viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(VIEW);

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(VIEW.userId()))
                .andExpect(jsonPath("$.chronicleName").value(VIEW.chronicleName()))
                .andExpect(jsonPath("$.viewName").value(VIEW.viewName()));
    }

    @Test
    void createView_invalidApiKey_returnsUnauthorized() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createView_chronicleNotFound_returnsNotFound() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenThrow(new ChronicleNotFoundException());

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME)
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Chronicle not found"));
    }

    @Test
    void createView_duplicateViewName_returnsConflict() throws Exception {
        when(userService.findUserIdByRawApiKey(API_KEY)).thenReturn(Optional.of(USER_ID));
        when(viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenThrow(new DuplicateViewException());

        mockMvc.perform(post("/views")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ViewController.CreateViewRequest(CHRONICLE_NAME, VIEW_NAME)
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("View already exists"));
    }
}