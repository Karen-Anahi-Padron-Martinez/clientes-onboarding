package com.kapm.onboarding_clientes.config;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


@Configuration
@RequiredArgsConstructor
public class PuntoRedConfig {

    private final PuntoRedProperties properties;
    @Bean
    public ClientHttpRequestFactory puntoRedClientHttpRequestFactory() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);

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
    @Bean(name = "puntoRedRestClient")
    public RestClient puntoRedRestClient(ClientHttpRequestFactory puntoRedClientHttpRequestFactory) {
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(puntoRedClientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }

        return builder.build();
    }

    @Bean
    public org.springframework.web.servlet.config.annotation.WebMvcConfigurer jsonOnlyWebMvcConfigurer() {
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
                converters.removeIf(converter -> 
                    converter instanceof org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter);
            }
        };
    }
}
