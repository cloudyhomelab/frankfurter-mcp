package dev.frankfurter.mcp.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.util.UriBuilder;

/**
 * Parameters for {@code /rates}. Null fields are omitted from the request so the API applies its defaults
 * (base EUR, all quotes, latest date).
 */
public record RatesQuery(
        String base,
        List<String> quotes,
        LocalDate date,
        LocalDate from,
        LocalDate to,
        String group,
        List<String> providers) {

    public static RatesQuery latest(String base, List<String> quotes, List<String> providers) {
        return new RatesQuery(base, quotes, null, null, null, null, providers);
    }

    public static RatesQuery on(LocalDate date, String base, List<String> quotes, List<String> providers) {
        return new RatesQuery(base, quotes, date, null, null, null, providers);
    }

    public static RatesQuery between(LocalDate from, LocalDate to, String base, List<String> quotes, String group,
            List<String> providers) {
        return new RatesQuery(base, quotes, null, from, to, group, providers);
    }

    UriBuilder apply(UriBuilder uri) {
        param(uri, "base", base);
        param(uri, "quotes", join(quotes));
        param(uri, "date", date);
        param(uri, "from", from);
        param(uri, "to", to);
        param(uri, "group", group);
        param(uri, "providers", join(providers));
        return uri;
    }

    private static void param(UriBuilder uri, String name, Object value) {
        if (value != null) {
            uri.queryParam(name, value.toString());
        }
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }
}
