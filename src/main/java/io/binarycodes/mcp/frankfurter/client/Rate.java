package io.binarycodes.mcp.frankfurter.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Rate(LocalDate date, String base, String quote, BigDecimal rate) {
}
