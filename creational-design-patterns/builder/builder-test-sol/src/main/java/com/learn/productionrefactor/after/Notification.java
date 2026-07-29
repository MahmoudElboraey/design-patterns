package com.learn.productionrefactor.after;


import java.util.List;
import java.util.Map;

public class Notification {
    private Channel channel;          // "email" | "sms" | "push"
    private String recipient;
    private String subject;
    private String body;
    private List<String> ccList;
    private Map<String, String> headers;
    private int retries;
    private boolean urgent;


    private Notification(Builder builder) {
        this.channel = builder.channel;
        this.recipient = builder.recipient;
        this.subject = builder.subject;
        this.body = builder.body;
        this.ccList = List.copyOf(builder.ccList);
        this.headers = Map.copyOf(builder.headers);
        this.retries = builder.retries;
        this.urgent = builder.urgent;

    }


    public static Builder builder (){
         return new Builder();
    }

    public static final class Builder {
        private Channel channel;          // "email" | "sms" | "push"
        private String recipient;
        private String subject;
        private String body;
        private List<String> ccList;
        private Map<String, String> headers;
        private int retries;
        private boolean urgent;


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
            this.ccList = List.copyOf(ccList);
            return this;
        }
        public Builder headers(Map<String, String> headers) {
            this.headers = Map.copyOf(headers);
            return this;
        }
        public Builder retries(int retries) {
            this.retries = retries;
            return this;
        }
        public Builder urgent(boolean urgent) {
            this.urgent = urgent;
            return this;
        }



        public Notification build(){
            return new Notification(this);
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
