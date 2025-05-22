package br.com.hyteck.school_control.utils;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility class for common formatting operations.
 */
public final class FormatUtils { // Mark class as final if it only contains static methods

    private static final Locale BRAZIL_LOCALE = Locale.of("pt", "BR");

    /**
     * Date formatter for dd/MM/yyyy pattern using Brazilian Portuguese locale.
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", BRAZIL_LOCALE);

    /**
     * Currency formatter for Brazilian Portuguese locale (e.g., R$ 1.234,56).
     */
    public static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FormatUtils() {
        throw new IllegalStateException("Utility class");
    }
}
