package com.relay.api.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.relay.api.common.ApiException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvRecipientParserTest {

    private final CsvRecipientParser parser = new CsvRecipientParser();

    @Test
    void parsesEmailAndOptionalTimezoneRows() {
        List<CsvRecipientParser.Row> rows = new ArrayList<>();

        parser.parse(csv("""
                recipientId,email,phone,timezone
                r1,ada@example.com,,Asia/Kuala_Lumpur
                r2,grace@example.com,+60123456789,
                """), Channel.EMAIL, rows::add);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).recipientRef()).isEqualTo("r1");
        assertThat(rows.get(0).timezone()).isEqualTo("Asia/Kuala_Lumpur");
        assertThat(rows.get(1).email()).isEqualTo("grace@example.com");
    }

    @Test
    void rejectsEmailCampaignRowsWithoutAValidEmail() {
        assertThatThrownBy(() -> parser.parse(
                csv("recipientId,email,phone\nr1,not-an-email,\n"), Channel.EMAIL, row -> {
                }))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Row 2")
                .hasMessageContaining("email");
    }

    @Test
    void rejectsSmsRowsWithoutAnE164Phone() {
        assertThatThrownBy(() -> parser.parse(
                csv("recipientId,email,phone\nr1,,0123456789\n"), Channel.SMS, row -> {
                }))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("E.164");
    }

    @Test
    void rejectsCsvWithoutRequiredHeaders() {
        assertThatThrownBy(() -> parser.parse(
                csv("id,mail\n1,a@example.com\n"), Channel.EMAIL, row -> {
                }))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("header");
    }

    private static ByteArrayInputStream csv(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
