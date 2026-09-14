package dev.frankfurter.mcp.client;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
class FrankfurterClientConfig {

    @Bean
    RestClient frankfurterRestClient(RestClient.Builder builder, FrankfurterProperties props, ObjectMapper mapper) {
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeader("User-Agent", "frankfurter-mcp")
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new FrankfurterException(response.getStatusCode().value(),
                            errorMessage(response.getBody().readAllBytes(), mapper));
                })
                .build();
    }

    private static String errorMessage(byte[] body, ObjectMapper mapper) {
        try {
            Object message = mapper.readValue(body, Map.class).get("message");
            if (message != null) {
                return message.toString();
            }
        }
        catch (RuntimeException ignored) {
            // not the API's JSON error envelope; fall through to the raw body
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
