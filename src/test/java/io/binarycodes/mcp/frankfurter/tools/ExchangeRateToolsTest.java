package io.binarycodes.mcp.frankfurter.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.binarycodes.mcp.frankfurter.client.FrankfurterClient;
import io.binarycodes.mcp.frankfurter.client.Rate;
import io.binarycodes.mcp.frankfurter.client.RatesQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExchangeRateToolsTest {

    private final FrankfurterClient client = mock(FrankfurterClient.class);
    private final ExchangeRateTools tools = new ExchangeRateTools(client);

    @Test
    void convertsUsingTheFetchedRate() {
        Rate rate = new Rate(LocalDate.of(2026, 9, 11), "USD", "EUR", new BigDecimal("0.86267"));
        when(client.rate("USD", "EUR", null, null)).thenReturn(rate);

        Conversion conversion = tools.convertCurrency(new BigDecimal("100"), "usd", " eur ", null, null);

        assertThat(conversion.result()).isEqualByComparingTo("86.2670");
        assertThat(conversion.rate()).isEqualByComparingTo("0.86267");
        assertThat(conversion.date()).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(conversion.from()).isEqualTo("USD");
        assertThat(conversion.to()).isEqualTo("EUR");
    }

    @Test
    void normalisesCodeListsAndDates() {
        when(client.rates(any())).thenReturn(List.of());

        tools.getHistoricalRates("2024-01-02", "usd", "eur, gbp jpy", "ecb");

        verify(client).rates(RatesQuery.on(LocalDate.of(2024, 1, 2), "USD", List.of("EUR", "GBP", "JPY"), List.of("ECB")));
    }

    @Test
    void rejectsMalformedDates() {
        assertThatThrownBy(() -> tools.getRate("EUR", "USD", "yesterday", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YYYY-MM-DD");
    }

    @Test
    void timeSeriesRequiresQuotesAndValidGrouping() {
        assertThatThrownBy(() -> tools.getTimeSeries("2024-01-01", null, null, null, null, null))
                .hasMessageContaining("quotes is required");
        assertThatThrownBy(() -> tools.getTimeSeries("2024-01-01", null, "USD", null, "year", null))
                .hasMessageContaining("group must be");
    }

    @Test
    void timeSeriesPassesGroupAndOpenEndedRange() {
        when(client.rates(any())).thenReturn(List.of());

        tools.getTimeSeries("2024-01-01", null, "USD", null, "Month", null);

        verify(client).rates(RatesQuery.between(LocalDate.of(2024, 1, 1), null, null, List.of("USD"), "month", null));
    }
}
