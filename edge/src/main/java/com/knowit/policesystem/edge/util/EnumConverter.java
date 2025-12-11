package com.knowit.policesystem.edge.util;

/**
 * Utility class for converting enums to strings.
 * Provides standardized conversion logic, including special case handling for status enums.
 */
public final class EnumConverter {

    private EnumConverter() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a status enum to its string representation.
     * Handles special case: InProgress -> "In-Progress" (with hyphen).
     * Other enum values are converted using their name() method.
     *
     * @param status the status enum to convert
     * @param <T> the enum type
     * @return the string representation, or null if status is null
     */
    public static <T extends Enum<T>> String convertStatusToString(T status) {
        if (status == null) {
            return null;
        }

        // Special case: InProgress -> "In-Progress"
        if (status.name().equals("InProgress")) {
            return "In-Progress";
        }

        return status.name();
    }

    /**
     * Converts any enum to its string representation using the name() method.
     * This is a generic conversion without special case handling.
     *
     * @param enumValue the enum value to convert
     * @param <T> the enum type
     * @return the string representation, or null if enumValue is null
     */
    public static <T extends Enum<T>> String convertEnumToString(T enumValue) {
        if (enumValue == null) {
            return null;
        }
        return enumValue.name();
    }
}
