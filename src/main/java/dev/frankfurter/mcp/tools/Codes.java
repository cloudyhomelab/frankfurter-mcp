package dev.frankfurter.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/** Normalises free-form agent input (mixed case, stray spaces) into what the API expects. */
final class Codes {

    private static final Pattern SEPARATORS = Pattern.compile("[,\\s]+");

    private Codes() {
    }

    static String one(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase();
    }

    static List<String> many(String codes) {
        if (codes == null || codes.isBlank()) {
            return null;
        }
        return SEPARATORS.splitAsStream(codes.trim()).map(String::toUpperCase).toList();
    }

    static LocalDate date(String date, String field) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        }
        catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must be an ISO date (YYYY-MM-DD), got: " + date);
        }
    }
}
