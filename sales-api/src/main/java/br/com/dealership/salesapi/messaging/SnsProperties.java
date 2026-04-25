package br.com.dealership.salesapi.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sns")
public record SnsProperties(String topicArn) {
}
