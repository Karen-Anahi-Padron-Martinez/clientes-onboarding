package com.kapm.onboarding_clientes.config;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


@Configuration
@RequiredArgsConstructor
public class BrevoConfig {

    private final BrevoProperties properties;

    @Bean(name = "brevoClientHttpRequestFactory")
    public ClientHttpRequestFactory brevoClientHttpRequestFactory() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(10);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(properties.getReadTimeout()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Bean(name = "brevoRestClient")
    public RestClient brevoRestClient(@Qualifier("brevoClientHttpRequestFactory") ClientHttpRequestFactory requestFactory) {
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl().trim());
        }

        return builder.build();
    }
}
