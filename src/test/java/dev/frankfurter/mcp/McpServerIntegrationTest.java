package dev.frankfurter.mcp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import dev.frankfurter.mcp.client.FrankfurterClient;
import dev.frankfurter.mcp.client.FrankfurterException;
import dev.frankfurter.mcp.client.Rate;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Drives the running server through a real MCP client over Streamable HTTP; the upstream API is mocked. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {

    @LocalServerPort
    int port;

    @MockitoBean
    FrankfurterClient frankfurter;

    McpSyncClient mcp;

    @BeforeEach
    void connect() {
        mcp = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).build()).build();
        mcp.initialize();
    }

    @AfterEach
    void disconnect() {
        mcp.closeGracefully();
    }

    @Test
    void exposesAllToolsAsReadOnly() {
        List<Tool> tools = mcp.listTools().tools();

        assertThat(tools).extracting(Tool::name).containsExactlyInAnyOrder(
                "get_latest_rates", "get_historical_rates", "get_time_series", "get_rate", "convert_currency",
                "list_currencies", "get_currency", "list_providers", "get_provider");
        assertThat(tools).allSatisfy(tool -> assertThat(tool.annotations().readOnlyHint()).isTrue());
    }

    @Test
    void callsConvertCurrencyEndToEnd() {
        when(frankfurter.rate("USD", "EUR", LocalDate.of(2026, 9, 11), null))
                .thenReturn(new Rate(LocalDate.of(2026, 9, 11), "USD", "EUR", new BigDecimal("0.86267")));

        CallToolResult result = mcp.callTool(CallToolRequest.builder("convert_currency")
                .arguments(Map.of("amount", 100, "from", "usd", "to", "eur", "date", "2026-09-11"))
                .build());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(text(result)).contains("\"result\":86.2670").contains("\"rate\":0.86267");
    }

    @Test
    void upstreamErrorsBecomeToolErrorsNotProtocolFailures() {
        when(frankfurter.rate(any(), any(), any(), any())).thenThrow(new FrankfurterException(422, "invalid currency: XXX"));

        CallToolResult result = mcp.callTool(CallToolRequest.builder("get_rate")
                .arguments(Map.of("base", "EUR", "quote", "XXX"))
                .build());

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("invalid currency: XXX");
    }

    private static String text(CallToolResult result) {
        return result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(c -> ((TextContent) c).text())
                .reduce("", String::concat);
    }
}
