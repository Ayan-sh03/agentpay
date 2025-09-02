package com.payment.payment.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${opa.url}")
    private String opaUrl;

    @Bean
    public WebClient opaWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
            .responseTimeout(Duration.ofSeconds(3))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(3))
                    .addHandlerLast(new WriteTimeoutHandler(3))
            );

        return WebClient.builder()
            .baseUrl(opaUrl)
            .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
            .build();
    }
}
