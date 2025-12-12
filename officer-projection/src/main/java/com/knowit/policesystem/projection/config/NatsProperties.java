package com.knowit.policesystem.projection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projection.nats")
public class NatsProperties {

    private boolean enabled = true;
    private String url = "nats://localhost:4222";
    private String stream = "officer-events";
    private String consumer = "projection-service";
    private String subjectPrefix = "";
    private boolean queryEnabled = true;
    private String querySubjectPrefix = "query";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }

    public boolean isQueryEnabled() {
        return queryEnabled;
    }

    public void setQueryEnabled(boolean queryEnabled) {
        this.queryEnabled = queryEnabled;
    }

    public String getQuerySubjectPrefix() {
        return querySubjectPrefix;
    }

    public void setQuerySubjectPrefix(String querySubjectPrefix) {
        this.querySubjectPrefix = querySubjectPrefix;
    }
}
