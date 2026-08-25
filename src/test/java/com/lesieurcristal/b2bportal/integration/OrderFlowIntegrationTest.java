package com.lesieurcristal.b2bportal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lesieurcristal.b2bportal.entity.app.ErpOutbox;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.erp.inbound.ErpInboundSyncJob;
import com.lesieurcristal.b2bportal.erp.outbox.ErpOutboundRetryJob;
import com.lesieurcristal.b2bportal.erp.outbox.ErpOutboxService;
import com.lesieurcristal.b2bportal.repository.ErpOutboxRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.PasswordHasher;
import com.lesieurcristal.b2bportal.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderFlowIntegrationTest extends AbstractPostgresIT {

    private static final String CLIENT_A_EMAIL = "amine.alami@alamal-epicerie.ma";
    private static final String CLIENT_B_EMAIL = "khadija.bennani@superdiscount.ma";
    private static final String PASSWORD = "Password12";
    private static final String CLIENT_B_ORDER = "4500010003";

    @MockitoBean
    private ErpInboundSyncJob inboundSyncJob;

    @MockitoBean
    private ErpOutboundRetryJob outboundRetryJob;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private ErpOutboxRepository erpOutboxRepository;

    @BeforeEach
    void setKnownPasswords() {
        setPassword(CLIENT_A_EMAIL, PASSWORD);
        setPassword(CLIENT_B_EMAIL, PASSWORD);
    }

    @Test
    void login_rejectsUnknownPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPass9"}
                                """.formatted(CLIENT_A_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitOrder_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload("IT-NO-AUTH")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitOrder_persistsOrderAndOutboxThenEnforcesTenantIsolation() throws Exception {
        String tokenA = login(CLIENT_A_EMAIL);
        String tokenB = login(CLIENT_B_EMAIL);
        String customerRef = "IT-" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload(customerRef)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.orderGroupId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("confirmed"))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String orderNumber = body.get("orderNumber").asText();
        String orderGroupId = body.get("orderGroupId").asText();

        List<ErpOutbox> outbox = erpOutboxRepository.findByDirectionAndEventTypeOrderByIdAsc(
                ErpOutboxService.DIRECTION_OUTBOUND, ErpOutboxService.TYPE_SUBMIT_ORDER);
        assertThat(outbox)
                .anySatisfy(row -> {
                    assertThat(row.getAggregateId()).isEqualTo(orderGroupId);
                    assertThat(row.getStatus()).isEqualTo("SENT");
                    assertThat(row.getPayload()).contains(customerRef);
                });

        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));

        mockMvc.perform(get("/api/orders/{orderNumber}", CLIENT_B_ORDER)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private void setPassword(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Seed user missing: " + email));
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        userRepository.save(user);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private static String orderPayload(String customerRef) {
        return """
                {
                  "customerOrderReference": "%s",
                  "shipToCity": "Casablanca",
                  "shipToCountry": "Maroc",
                  "requestedDeliveryDate": "2026-09-01",
                  "transportMethod": "ROUTE",
                  "orderLines": [
                    { "productCode": "HSO-001", "quantity": 2, "salesUnit": "CAR" }
                  ]
                }
                """.formatted(customerRef);
    }
}
