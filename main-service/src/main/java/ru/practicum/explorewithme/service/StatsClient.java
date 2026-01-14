package ru.practicum.explorewithme.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.explorewithme.dto.EndpointHit;
import ru.practicum.explorewithme.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatsClient {

    private final WebClient webClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(@Value("${stats-server.url:http://localhost:9090}") String statsServerUrl) {
        this.webClient = WebClient.create(statsServerUrl);
    }

    public void hit(EndpointHit endpointHit) {
        webClient.post()
                .uri("/hit")
                .bodyValue(endpointHit)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/stats")
                            .queryParam("start", start.format(FORMATTER))
                            .queryParam("end", end.format(FORMATTER))
                            .queryParam("unique", unique);

                    if (uris != null && !uris.isEmpty()) {
                        builder.queryParam("uris", uris.toArray());
                    }

                    return builder.build();
                })
                .retrieve()
                .bodyToFlux(ViewStats.class)
                .collectList()
                .block();
    }
}