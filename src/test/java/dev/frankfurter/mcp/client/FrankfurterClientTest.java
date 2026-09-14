package dev.frankfurter.mcp.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(FrankfurterClient.class)
@Import(FrankfurterClientConfig.class)
@EnableConfigurationProperties(FrankfurterProperties.class)
class FrankfurterClientTest {

    private static final String BASE = "https://api.frankfurter.dev/v2";

    @Autowired
    FrankfurterClient client;

    @Autowired
    MockRestServiceServer server;

    @Test
    void ratesBuildsQueryFromNonNullFieldsOnly() {
        server.expect(requestTo(BASE + "/rates?base=USD&quotes=EUR,GBP&from=2024-01-01&group=month"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"date":"2024-01-01","base":"USD","quote":"EUR","rate":0.91668},
                         {"date":"2024-01-01","base":"USD","quote":"GBP","rate":0.78}]
                        """, MediaType.APPLICATION_JSON));

        List<Rate> rates = client.rates(RatesQuery.between(LocalDate.of(2024, 1, 1), null, "USD", List.of("EUR", "GBP"), "month", null));

        assertThat(rates).hasSize(2);
        assertThat(rates.getFirst()).isEqualTo(new Rate(LocalDate.of(2024, 1, 1), "USD", "EUR", new BigDecimal("0.91668")));
    }

    @Test
    void rateUsesPathVariablesAndOptionalDate() {
        server.expect(requestTo(BASE + "/rate/EUR/USD?date=2020-05-05"))
                .andRespond(withSuccess("""
                        {"date":"2020-05-05","base":"EUR","quote":"USD","rate":1.0888}
                        """, MediaType.APPLICATION_JSON));

        Rate rate = client.rate("EUR", "USD", LocalDate.of(2020, 5, 5), null);

        assertThat(rate.rate()).isEqualByComparingTo("1.0888");
    }

    @Test
    void currencyMapsSnakeCaseFields() {
        server.expect(requestTo(BASE + "/currency/EUR"))
                .andRespond(withSuccess("""
                        {"iso_code":"EUR","iso_numeric":"978","name":"Euro","symbol":"€",
                         "start_date":"1999-01-04","end_date":"2026-09-14","providers":["ECB","FRED"]}
                        """, MediaType.APPLICATION_JSON));

        Currency eur = client.currency("EUR");

        assertThat(eur.isoCode()).isEqualTo("EUR");
        assertThat(eur.startDate()).isEqualTo(LocalDate.of(1999, 1, 4));
        assertThat(eur.providers()).containsExactly("ECB", "FRED");
    }

    @Test
    void legacyCurrenciesUseScopeAll() {
        server.expect(requestTo(BASE + "/currencies?scope=all"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.currencies(true)).isEmpty();
    }

    @Test
    void apiErrorsSurfaceTheirMessage() {
        server.expect(requestTo(BASE + "/rate/EUR/XXX"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":422,\"message\":\"invalid currency: XXX\"}"));

        assertThatThrownBy(() -> client.rate("EUR", "XXX", null, null))
                .isInstanceOf(FrankfurterException.class)
                .hasMessage("invalid currency: XXX")
                .extracting("status").isEqualTo(422);
    }
}
