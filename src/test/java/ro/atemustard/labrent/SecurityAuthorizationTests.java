package ro.atemustard.labrent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SecurityAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCannotAccessAdminRequestQueue() throws Exception {
        mockMvc.perform(get("/api/rental-requests/all")
                        .header("Authorization", bearerToken("ion.popescu", "parola123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminRequestQueue() throws Exception {
        mockMvc.perform(get("/api/rental-requests/all")
                        .header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotCreateEquipment() throws Exception {
        String body = """
                {
                  "name": "Unauthorized Equipment",
                  "description": "Should be rejected",
                  "category": "Test",
                  "totalQuantity": 1
                }
                """;

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", bearerToken("ion.popescu", "parola123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    private String bearerToken(String username, String password) throws Exception {
        String body = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return "Bearer " + json.get("token").asText();
    }
}
