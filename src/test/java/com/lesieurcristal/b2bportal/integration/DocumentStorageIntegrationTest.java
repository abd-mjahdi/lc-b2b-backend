package com.lesieurcristal.b2bportal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.erp.inbound.ErpInboundSyncJob;
import com.lesieurcristal.b2bportal.erp.outbox.ErpOutboundRetryJob;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.PasswordHasher;
import com.lesieurcristal.b2bportal.storage.ObjectStorage;
import com.lesieurcristal.b2bportal.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DocumentStorageIntegrationTest extends AbstractPostgresIT {

    private static final String CLIENT_A_EMAIL = "amine.alami@alamal-epicerie.ma";
    private static final String CLIENT_B_EMAIL = "khadija.bennani@superdiscount.ma";
    private static final String ADMIN_EMAIL = "rachad.khattabi@gmail.com";
    private static final String PASSWORD = "Password12";
    private static final String CLIENT_A_INVOICE = "900010001";
    private static final String CLIENT_B_INVOICE = "900010003";
    private static final byte[] TINY_PDF = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==");

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
    private ObjectStorage objectStorage;

    @BeforeEach
    void setKnownPasswords() {
        setPassword(CLIENT_A_EMAIL, PASSWORD);
        setPassword(CLIENT_B_EMAIL, PASSWORD);
        setPassword(ADMIN_EMAIL, PASSWORD);
    }

    @Test
    void invoicePdf_generateOnceThenReuseAndIsolateTenants() throws Exception {
        String tokenA = login(CLIENT_A_EMAIL);
        String tokenB = login(CLIENT_B_EMAIL);

        MvcResult first = mockMvc.perform(get("/api/invoices/{n}/file", CLIENT_A_INVOICE)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        byte[] pdf = first.getResponse().getContentAsByteArray();
        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(objectStorage.exists("invoices/CUST0001/" + CLIENT_A_INVOICE + ".pdf")).isTrue();

        mockMvc.perform(get("/api/invoices/{n}/file", CLIENT_A_INVOICE)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdf));

        mockMvc.perform(get("/api/invoices/{n}/file", CLIENT_B_INVOICE)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/invoices/{n}/file", CLIENT_A_INVOICE)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void disputeAttachment_uploadDownloadAndTenantIsolation() throws Exception {
        String tokenA = login(CLIENT_A_EMAIL);
        String tokenB = login(CLIENT_B_EMAIL);

        MockMultipartFile file = new MockMultipartFile(
                "file", "bl.pdf", "application/pdf", TINY_PDF);

        MvcResult created = mockMvc.perform(multipart("/api/invoices/{n}/dispute", CLIENT_A_INVOICE)
                        .file(file)
                        .param("reason", "QUANTITY_DISCREPANCY")
                        .param("description", "Cinq cartons manquants sur le bon de livraison")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAttachment").value(true))
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/invoices/disputes/mine/{id}/file", id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(TINY_PDF));

        mockMvc.perform(get("/api/invoices/disputes/mine/{id}/file", id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void claimAttachment_adminCanDownload() throws Exception {
        String tokenA = login(CLIENT_A_EMAIL);
        String adminToken = login(ADMIN_EMAIL);
        String lot = "IT-" + UUID.randomUUID().toString().substring(0, 8);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", TINY_PNG);

        MvcResult created = mockMvc.perform(multipart("/api/claims")
                        .file(file)
                        .param("lotNumber", lot)
                        .param("description", "Emballage endommagé constaté à la réception du lot")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAttachment").value(true))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        long id = body.get("id").asLong();

        mockMvc.perform(get("/api/admin/claims/{id}/file", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(TINY_PNG));
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
}
