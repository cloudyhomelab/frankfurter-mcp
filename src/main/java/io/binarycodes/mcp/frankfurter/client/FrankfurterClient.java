package io.binarycodes.mcp.frankfurter.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Thin typed wrapper over the Frankfurter v2 REST API. */
@Component
public class FrankfurterClient {

    private static final ParameterizedTypeReference<List<Rate>> RATES = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Currency>> CURRENCIES = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Provider>> PROVIDERS = new ParameterizedTypeReference<>() { };

    private final RestClient http;

    public FrankfurterClient(RestClient frankfurterRestClient) {
        this.http = frankfurterRestClient;
    }

    public List<Rate> rates(RatesQuery query) {
        return http.get()
                .uri(uri -> query.apply(uri.path("/rates")).build())
                .retrieve()
                .body(RATES);
    }

    public Rate rate(String base, String quote, LocalDate date, List<String> providers) {
        return http.get()
                .uri(uri -> {
                    uri.path("/rate/{base}/{quote}");
                    if (date != null) {
                        uri.queryParam("date", date.toString());
                    }
                    if (providers != null && !providers.isEmpty()) {
                        uri.queryParam("providers", String.join(",", providers));
                    }
                    return uri.build(base, quote);
                })
                .retrieve()
                .body(Rate.class);
    }

    public List<Currency> currencies(boolean includeLegacy) {
        return http.get()
                .uri(uri -> {
                    uri.path("/currencies");
                    if (includeLegacy) {
                        uri.queryParam("scope", "all");
                    }
                    return uri.build();
                })
                .retrieve()
                .body(CURRENCIES);
    }

    public Currency currency(String code) {
        return http.get().uri("/currency/{code}", code).retrieve().body(Currency.class);
    }

    public List<Provider> providers() {
        return http.get().uri("/providers").retrieve().body(PROVIDERS);
    }

    public Provider provider(String key) {
        return http.get().uri("/providers/{key}", key).retrieve().body(Provider.class);
    }
}
