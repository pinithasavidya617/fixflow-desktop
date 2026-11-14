package com.fixflow.util;

import java.util.regex.Pattern;


public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9+()\\s-]{7,20}$"
    );

    private ValidationUtil() {
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    ]
    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }


    public static boolean isValidOptionalEmail(String email) {
        if (!isNotEmpty(email)) {
            return true; // Optional field
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }


    public static String sanitize(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

