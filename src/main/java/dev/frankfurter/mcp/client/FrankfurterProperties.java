package dev.frankfurter.mcp.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param baseUrl root of the Frankfurter v2 API; point at a self-hosted instance if needed
 */
@ConfigurationProperties("frankfurter")
public record FrankfurterProperties(@DefaultValue("https://api.frankfurter.dev/v2") String baseUrl) {
}
