package com.relay.api.campaign;

import com.relay.api.common.ApiException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CsvRecipientParser {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final List<String> REQUIRED = List.of("recipientId", "email", "phone");

    public record Row(String recipientRef, String email, String phone, String timezone) {
    }

    public void parse(InputStream input, Channel channel, Consumer<Row> sink) {
        try (Reader reader = readerWithoutBom(input);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .get()
                        .parse(reader)) {

            var headers = parser.getHeaderMap();
            for (String column : REQUIRED) {
                if (!headers.containsKey(column)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "CSV header must contain recipientId,email,phone.");
                }
            }

            long rows = 0;
            for (CSVRecord record : parser) {
                rows++;
                long line = rows + 1;
                String ref = value(record, "recipientId");
                String email = value(record, "email");
                String phone = value(record, "phone");
                String timezone = value(record, "timezone");

                if (ref.isEmpty()) {
                    throw rowError(line, "recipientId is required.");
                }
                if (channel == Channel.EMAIL && !EMAIL.matcher(email).matches()) {
                    throw rowError(line, "a valid email is required for EMAIL campaigns.");
                }
                if (channel == Channel.SMS && !PHONE.matcher(phone).matches()) {
                    throw rowError(line, "phone must be E.164, for example +60123456789.");
                }
                if (!timezone.isEmpty()) {
                    try {
                        ZoneId.of(timezone);
                    } catch (ZoneRulesException exception) {
                        throw rowError(line, "timezone is not a valid IANA zone.");
                    }
                }

                sink.accept(new Row(ref, email, phone, timezone));
            }

            if (rows == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CSV contains no recipient rows.");
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the CSV upload.");
        }
    }

    private static Reader readerWithoutBom(InputStream input) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        reader.mark(1);
        if (reader.read() != 0xFEFF) {
            reader.reset();
        }
        return reader;
    }

    private static String value(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            return "";
        }
        try {
            String value = record.get(column);
            return value == null ? "" : value.trim();
        } catch (IllegalArgumentException exception) {
            // A short row is allowed to omit trailing optional columns.
            return "";
        }
    }

    private static ApiException rowError(long line, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Row " + line + ": " + detail);
    }
}
