package com.Abhinav.backend.features.authentication.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator {

    // Password must be at least 8 chars, include uppercase, lowercase, number, special char
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isValid(String password) {
        if (password == null) return false;
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }
}