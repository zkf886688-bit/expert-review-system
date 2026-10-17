package com.review.system.util;

public final class MaskUtil {

    private MaskUtil() {
    }

    public static String maskIdCard(String value) {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
    }

    public static String maskBankAccount(String value) {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    public static String maskPhone(String value) {
        if (value == null || value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
