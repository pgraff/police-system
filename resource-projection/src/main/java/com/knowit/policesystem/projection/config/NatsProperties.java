package com.knowit.policesystem.projection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nats")
public class NatsProperties {

    private boolean enabled = true;
    private boolean queryEnabled = true;
    private String url = "nats://localhost:4222";
    private String subjectPrefix = "";
    private String querySubjectPrefix = "query";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isQueryEnabled() {
        return queryEnabled;
    }

    public void setQueryEnabled(boolean queryEnabled) {
        this.queryEnabled = queryEnabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }

    public String getQuerySubjectPrefix() {
        return querySubjectPrefix;
    }

    public void setQuerySubjectPrefix(String querySubjectPrefix) {
        this.querySubjectPrefix = querySubjectPrefix;
    }
}
