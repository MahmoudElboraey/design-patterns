package com.learn.productionrefactor.before;

import java.util.List;
import java.util.Map;

public class NotificationConfig {
    public String channel;          // "email" | "sms" | "push"
    public String recipient;
    public String subject;
    public String body;
    public List<String> ccList;
    public Map<String, String> headers;
    public int retries;
    public boolean urgent;

    public NotificationConfig(String channel, String recipient) {
        this.channel = channel;
        this.recipient = recipient;
    }

    public NotificationConfig(String channel, String recipient, String subject, String body) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    public NotificationConfig(String channel, String recipient, String subject,
                              String body, List<String> ccList, Map<String, String> headers,
                              int retries, boolean urgent) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.ccList = ccList;
        this.headers = headers;
        this.retries = retries;
        this.urgent = urgent;
    }

}