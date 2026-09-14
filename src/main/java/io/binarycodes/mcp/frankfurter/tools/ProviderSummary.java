package io.binarycodes.mcp.frankfurter.tools;

import io.binarycodes.mcp.frankfurter.client.Provider;

public record ProviderSummary(String key, String name, String countryCode, String rateType, String pivotCurrency,
        String publishCadence, int currencyCount) {

    static ProviderSummary of(Provider p) {
        return new ProviderSummary(p.key(), p.name(), p.countryCode(), p.rateType(), p.pivotCurrency(),
                p.publishCadence(), p.currencies() == null ? 0 : p.currencies().size());
    }
}
