package com.review.system.sms;

public final class SmsPhoneUtil {

    private SmsPhoneUtil() {
    }

    public static String normalizeChinaMobile(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("86") && digits.length() == 13) {
            digits = digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            return digits;
        }
        return null;
    }

    public static String toE164China(String phone) {
        String mobile = normalizeChinaMobile(phone);
        return mobile == null ? null : "+86" + mobile;
    }

    public static String truncate(String content, int maxLen) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLen) {
            return content;
        }
        return content.substring(0, maxLen - 3) + "...";
    }
}
