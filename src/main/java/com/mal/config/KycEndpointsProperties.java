package com.mal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kyc.endpoints")
public class KycEndpointsProperties {

    private String address;
    private String document;
    private String biometric;
    private String sanctions;
}
