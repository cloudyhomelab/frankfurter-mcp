package dev.frankfurter.mcp.client;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Currency(
        @JsonProperty("iso_code") String isoCode,
        @JsonProperty("iso_numeric") String isoNumeric,
        String name,
        String symbol,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        List<String> providers) {
}
