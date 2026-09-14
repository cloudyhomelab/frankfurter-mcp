package dev.frankfurter.mcp.tools;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Conversion(BigDecimal amount, String from, String to, LocalDate date, BigDecimal rate, BigDecimal result) {
}
