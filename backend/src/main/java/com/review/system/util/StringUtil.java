package com.review.system.util;

public final class StringUtil {

    private StringUtil() {
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
