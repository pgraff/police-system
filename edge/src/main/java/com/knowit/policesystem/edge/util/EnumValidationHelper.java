package com.knowit.policesystem.edge.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for extracting enum values for validation error messages.
 */
public class EnumValidationHelper {

    /**
     * Extracts valid enum values from an enum class.
     *
     * @param enumClass the enum class
     * @return list of valid enum value names
     */
    public static List<String> getValidEnumValues(Class<?> enumClass) {
        if (enumClass != null && enumClass.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) enumClass;
            return Arrays.stream(enumType.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Creates a map of field names to their valid enum values.
     * Useful for populating validValues in error responses.
     *
     * @param fieldName the field name
     * @param enumClass the enum class
     * @return map with field name as key and list of valid values as value
     */
    public static Map<String, List<String>> createValidValuesMap(String fieldName, Class<?> enumClass) {
        List<String> validValues = getValidEnumValues(enumClass);
        if (!validValues.isEmpty()) {
            return Map.of(fieldName, validValues);
        }
        return Map.of();
    }
}
