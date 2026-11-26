package com.relay.api.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "relay.worker.poll-ms=60000")
class CampaignApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DeliveryWorker worker;

    @BeforeEach
    void cleanCampaigns() {
        jdbc.update("DELETE FROM campaigns");
    }

    @Test
    void createsListsAndReadsACampaign() throws Exception {
        String id = create("northstar", "Bearer demo-northstar-local", "idem-1");

        mvc.perform(get("/campaigns").header("Authorization", "Bearer demo-northstar-local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].totalRecipients").value(2));

        mvc.perform(get("/campaigns/{id}", id).header("Authorization", "Bearer demo-northstar-local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.template").value("Hello {{recipientId}}"));
    }

    @Test
    void isolatesCampaignsByTenant() throws Exception {
        String id = create("northstar", "Bearer demo-northstar-local", "idem-2");

        mvc.perform(get("/campaigns/{id}", id).header("Authorization", "Bearer demo-meridian-local"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/campaigns").header("Authorization", "Bearer demo-meridian-local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void returnsTheExistingCampaignForARepeatedIdempotencyKey() throws Exception {
        String first = create("northstar", "Bearer demo-northstar-local", "idem-3");
        String second = create("northstar", "Bearer demo-northstar-local", "idem-3");

        assertThat(second).isEqualTo(first);
        Integer campaigns = jdbc.queryForObject("SELECT COUNT(*) FROM campaigns", Integer.class);
        assertThat(campaigns).isEqualTo(1);
    }

    @Test
    void rejectsInvalidCsvRows() throws Exception {
        mvc.perform(multipart("/campaigns")
                        .file(csv("r1,not-an-email,\n"))
                        .param("tenantId", "northstar")
                        .param("name", "Bad CSV")
                        .param("channel", "EMAIL")
                        .param("template", "Hello")
                        .param("scheduledAt", "now")
                        .header("Authorization", "Bearer demo-northstar-local"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Row 2: a valid email is required for EMAIL campaigns."));
    }

    @Test
    void workerSendsJobsAndFinishesTheCampaign() throws Exception {
        String id = create("northstar", "Bearer demo-northstar-local", "idem-4");

        for (int attempt = 0; attempt < 20; attempt++) {
            worker.tick();
            Thread.sleep(250);
            String status = jdbc.queryForObject("SELECT status FROM campaigns WHERE id = ?", String.class, id);
            if (!status.equals("SCHEDULED") && !status.equals("RUNNING")) {
                break;
            }
        }

        String status = jdbc.queryForObject("SELECT status FROM campaigns WHERE id = ?", String.class, id);
        assertThat(status).isIn("COMPLETED", "FAILED");
        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_jobs WHERE campaign_id = ? AND status IN ('PENDING', 'PROCESSING')",
                Integer.class, id);
        assertThat(pending).isZero();
    }

    private String create(String tenantId, String authorization, String idempotencyKey) throws Exception {
        String body = mvc.perform(multipart("/campaigns")
                        .file(csv("r1,ada@example.com,,Asia/Kuala_Lumpur\nr2,grace@example.com,+60123456789,\n"))
                        .param("tenantId", tenantId)
                        .param("name", "Statement ready")
                        .param("channel", "EMAIL")
                        .param("template", "Hello {{recipientId}}")
                        .param("scheduledAt", "now")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.totalRecipients").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    private static MockMultipartFile csv(String rows) {
        return new MockMultipartFile(
                "csv", "recipients.csv", "text/csv",
                ("recipientId,email,phone,timezone\n" + rows).getBytes(StandardCharsets.UTF_8));
    }
}
