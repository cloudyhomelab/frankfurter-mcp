package io.binarycodes.mcp.frankfurter.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import io.binarycodes.mcp.frankfurter.client.Currency;
import io.binarycodes.mcp.frankfurter.client.FrankfurterClient;
import io.binarycodes.mcp.frankfurter.client.Provider;
import io.binarycodes.mcp.frankfurter.client.Rate;
import io.binarycodes.mcp.frankfurter.client.RatesQuery;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateTools {

    private static final String BASE_DESC = "Base currency ISO 4217 code, e.g. USD. Defaults to EUR.";
    private static final String QUOTES_DESC = "Comma-separated quote currency codes to return, e.g. \"USD,GBP,JPY\". Omit for all currencies.";
    private static final String PROVIDERS_DESC = "Comma-separated provider keys (e.g. \"ECB\") to use instead of the default blended rate. Use list_providers to discover keys.";
    private static final String DATE_DESC = "Date as YYYY-MM-DD.";

    private final FrankfurterClient client;

    public ExchangeRateTools(FrankfurterClient client) {
        this.client = client;
    }

    @McpTool(name = "get_latest_rates",
            description = "Get the latest daily mid-market exchange rates for a base currency. Rates are blended across ~98 central banks unless providers is set.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public List<Rate> getLatestRates(
            @McpToolParam(description = BASE_DESC, required = false) String base,
            @McpToolParam(description = QUOTES_DESC, required = false) String quotes,
            @McpToolParam(description = PROVIDERS_DESC, required = false) String providers) {
        return client.rates(RatesQuery.latest(Codes.one(base), Codes.many(quotes), Codes.many(providers)));
    }

    @McpTool(name = "get_historical_rates",
            description = "Get exchange rates for a base currency on a specific past date. Data goes back to 1948 for some currencies, 1999 for the euro.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public List<Rate> getHistoricalRates(
            @McpToolParam(description = DATE_DESC) String date,
            @McpToolParam(description = BASE_DESC, required = false) String base,
            @McpToolParam(description = QUOTES_DESC, required = false) String quotes,
            @McpToolParam(description = PROVIDERS_DESC, required = false) String providers) {
        return client.rates(RatesQuery.on(Codes.date(date, "date"), Codes.one(base), Codes.many(quotes), Codes.many(providers)));
    }

    @McpTool(name = "get_time_series",
            description = "Get exchange rates for a date range, one row per date and quote currency. Use group to downsample long ranges to weekly or monthly points.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public List<Rate> getTimeSeries(
            @McpToolParam(description = "Start date as YYYY-MM-DD.") String from,
            @McpToolParam(description = "End date as YYYY-MM-DD. Defaults to today.", required = false) String to,
            @McpToolParam(description = "Comma-separated quote currency codes, e.g. \"USD,GBP\". Required to keep responses small.") String quotes,
            @McpToolParam(description = BASE_DESC, required = false) String base,
            @McpToolParam(description = "Downsample to \"week\" or \"month\". Omit for daily rates.", required = false) String group,
            @McpToolParam(description = PROVIDERS_DESC, required = false) String providers) {
        List<String> quoteList = Codes.many(quotes);
        if (quoteList == null) {
            throw new IllegalArgumentException("quotes is required for a time series, e.g. \"USD\" or \"USD,GBP\"");
        }
        String groupBy = group == null || group.isBlank() ? null : group.trim().toLowerCase();
        if (groupBy != null && !groupBy.equals("week") && !groupBy.equals("month")) {
            throw new IllegalArgumentException("group must be \"week\" or \"month\", got: " + group);
        }
        return client.rates(RatesQuery.between(Codes.date(from, "from"), Codes.date(to, "to"), Codes.one(base), quoteList,
                groupBy, Codes.many(providers)));
    }

    @McpTool(name = "get_rate",
            description = "Get the exchange rate for a single currency pair, latest or on a given date.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Rate getRate(
            @McpToolParam(description = "Base currency code, e.g. EUR.") String base,
            @McpToolParam(description = "Quote currency code, e.g. USD.") String quote,
            @McpToolParam(description = DATE_DESC + " Omit for the latest rate.", required = false) String date,
            @McpToolParam(description = PROVIDERS_DESC, required = false) String providers) {
        return client.rate(Codes.one(base), Codes.one(quote), Codes.date(date, "date"), Codes.many(providers));
    }

    @McpTool(name = "convert_currency",
            description = "Convert an amount from one currency to another using the latest rate or the rate on a given date. Returns the rate used and the result rounded to 4 decimals.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Conversion convertCurrency(
            @McpToolParam(description = "Amount to convert, e.g. 100 or 19.99.") BigDecimal amount,
            @McpToolParam(description = "Currency code of the amount, e.g. USD.") String from,
            @McpToolParam(description = "Currency code to convert into, e.g. EUR.") String to,
            @McpToolParam(description = DATE_DESC + " Omit for the latest rate.", required = false) String date,
            @McpToolParam(description = PROVIDERS_DESC, required = false) String providers) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        Rate rate = client.rate(Codes.one(from), Codes.one(to), Codes.date(date, "date"), Codes.many(providers));
        return convert(amount, rate);
    }

    static Conversion convert(BigDecimal amount, Rate rate) {
        BigDecimal result = amount.multiply(rate.rate()).setScale(4, RoundingMode.HALF_EVEN);
        return new Conversion(amount, rate.base(), rate.quote(), rate.date(), rate.rate(), result);
    }

    @McpTool(name = "list_currencies",
            description = "List supported currencies with ISO code, name, symbol and the date range for which rates exist.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public List<Currency> listCurrencies(
            @McpToolParam(description = "Set true to include legacy currencies that no longer exist (e.g. DEM, FRF).", required = false) Boolean includeLegacy) {
        return client.currencies(Boolean.TRUE.equals(includeLegacy));
    }

    @McpTool(name = "get_currency",
            description = "Get details for one currency, including which providers publish rates for it.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Currency getCurrency(@McpToolParam(description = "Currency ISO 4217 code, e.g. CHF.") String code) {
        return client.currency(Codes.one(code));
    }

    @McpTool(name = "list_providers",
            description = "List the central banks and official sources whose rates the API blends, with their provider keys.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public List<ProviderSummary> listProviders() {
        return client.providers().stream().map(ProviderSummary::of).toList();
    }

    @McpTool(name = "get_provider",
            description = "Get full details for one provider, including every currency it publishes.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Provider getProvider(@McpToolParam(description = "Provider key, e.g. ECB.") String key) {
        return client.provider(Codes.one(key));
    }
}
