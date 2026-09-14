package dev.frankfurter.mcp.client;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Provider(
        String key,
        String name,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("rate_type") String rateType,
        @JsonProperty("pivot_currency") String pivotCurrency,
        @JsonProperty("data_url") String dataUrl,
        @JsonProperty("terms_url") String termsUrl,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("publish_cadence") String publishCadence,
        String frequency,
        List<String> currencies) {
}
