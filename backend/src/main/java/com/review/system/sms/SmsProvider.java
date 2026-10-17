package com.review.system.sms;

public interface SmsProvider {

    String name();

    boolean isConfigured();

    SmsSendResult send(String phone, String content);
}
