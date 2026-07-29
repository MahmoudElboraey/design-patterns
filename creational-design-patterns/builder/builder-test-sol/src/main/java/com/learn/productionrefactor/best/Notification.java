package com.learn.productionrefactor.best;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Notification {

    private final Channel channel;
    private final String recipient;
    private final String subject;
    private final String body;
    private final List<String> ccList;
    private final Map<String, String> headers;
    private final int retries;
    private final boolean urgent;

    private Notification(Builder b) {
        this.channel = b.channel;
        this.recipient = b.recipient;
        this.subject = b.subject;
        this.body = b.body;
        this.ccList = b.ccList == null ? List.of() : List.copyOf(b.ccList);
        this.headers = b.headers == null ? Map.of() : Map.copyOf(b.headers);
        this.retries = b.retries;
        this.urgent = b.urgent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Channel channel;
        private String recipient;
        private String subject;
        private String body;
        private List<String> ccList;
        private Map<String, String> headers;
        private int retries = 0;
        private boolean urgent = false;

        private Builder() {
        }

        public Builder channel(Channel channel) {
            this.channel = channel;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder ccList(List<String> ccList) {
            this.ccList = ccList == null ? null : List.copyOf(ccList);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers == null ? null : Map.copyOf(headers);
            return this;
        }

        public Builder retries(int retries) {
            if (retries < 0) {
                throw new IllegalArgumentException("retries must be >= 0, was " + retries);
            }
            this.retries = retries;
            return this;
        }

        public Builder urgent(boolean urgent) {
            this.urgent = urgent;
            return this;
        }

        public Notification build() {
            validate();
            return new Notification(this);
        }

        private void validate() {
            Objects.requireNonNull(channel, "channel is required");
            if (recipient == null || recipient.isBlank()) {
                throw new IllegalArgumentException("recipient is required");
            }
            if (subject != null && !channel.supportsSubject()) {
                throw new IllegalArgumentException("subject is not supported for channel " + channel);
            }
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public List<String> getCcList() {
        return ccList;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getRetries() {
        return retries;
    }

    public boolean isUrgent() {
        return urgent;
    }
}
