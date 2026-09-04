package com.amlve.grading.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperty {

    private String apiKey;
    private String baseUrl;
    private String fileBaseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getFileBaseUrl() {
        return fileBaseUrl;
    }

    public void setFileBaseUrl(String fileBaseUrl) {
        this.fileBaseUrl = fileBaseUrl;
    }
}
