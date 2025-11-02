package com.immobilier.searcher.service;

import com.immobilier.searcher.model.TransitRoute;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Instant;
import java.util.*;

@Service
public class TransitService {

    private final WebClient webClient;

    @Value("${google.api.key}")
    private String apiKey;

    public TransitService(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create();
               //.proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
               //        .host("163.116.128.80")
               //        .port(8080));

        this.webClient = webClientBuilder
                .baseUrl("https://maps.googleapis.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public List<TransitRoute> getTransitRoutes(String originAddress) {
        String destination = "Châtelet – Les Halles, Paris";
        List<TransitRoute> allRoutes = new ArrayList<>();
        List<String> allowedTypes = List.of("SUBWAY", "HEAVY_RAIL", "TRAM");

        for (int offset = 0; offset <= 600; offset += 300) {
            long departureTime = Instant.now().getEpochSecond() + offset;

            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/directions/json")
                            .queryParam("origin", originAddress)
                            .queryParam("destination", destination)
                            .queryParam("mode", "transit")
                            .queryParam("alternatives", "true")
                            .queryParam("departure_time", departureTime)
                            .queryParam("transit_routing_preference", "less_walking")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode routeArray = response.path("routes");
            for (int r = 0; r < routeArray.size(); r++) {
                JsonNode leg = routeArray.get(r).path("legs").get(0);
                String totalDuration = leg.path("duration").path("text").asText();
                int totalDurationValue = leg.path("duration").path("value").asInt();

                JsonNode steps = leg.path("steps");
                String walkingDistance = "";
                String walkingDuration = "";
                String departureStation = "";
                List<String> transitLines = new ArrayList<>();

                for (JsonNode step : steps) {
                    String mode = step.path("travel_mode").asText();

                    if (mode.equals("WALKING") && walkingDistance.isEmpty()) {
                        walkingDistance = step.path("distance").path("text").asText();
                        walkingDuration = step.path("duration").path("text").asText();
                    }

                    if (mode.equals("TRANSIT")) {
                        JsonNode transit = step.path("transit_details");
                        String vehicleType = transit.path("line").path("vehicle").path("type").asText();
                        if (!allowedTypes.contains(vehicleType)) continue;

                        String line = transit.path("line").path("short_name").asText();
                        if (line == null || line.isBlank()) {
                            line = transit.path("line").path("name").asText();
                        }

                        String departure = transit.path("departure_stop").path("name").asText();
                        if (departureStation.isEmpty() && !departure.toLowerCase().contains("bus")) {
                            departureStation = departure;
                        }

                        String prefix = switch (vehicleType) {
                            case "SUBWAY" -> "M";
                            case "HEAVY_RAIL" -> "RER";
                            case "TRAM" -> "T";
                            default -> "";
                        };

                        transitLines.add(prefix + line);
                    }
                }
                String transitSteps = "🚶 " + walkingDuration + " → " + String.join(" → ", transitLines);
                if (!departureStation.isEmpty()) {
                    allRoutes.add(new TransitRoute(
                            walkingDistance,
                            walkingDuration,
                            departureStation,
                            transitSteps,
                            totalDuration,
                            totalDurationValue
                    ));
                }
            }
        }

        // 🔁 Supprimer les doublons
        Set<String> uniqueKeys = new HashSet<>();
        List<TransitRoute> filteredRoutes = new ArrayList<>();

        for (TransitRoute route : allRoutes) {
            String key = route.getDepartureStation() + "|" + route.getTransitSteps();
            if (!uniqueKeys.contains(key)) {
                uniqueKeys.add(key);
                filteredRoutes.add(route);
            }
        }

        // ⏱️ Trier par durée croissante
        filteredRoutes.sort(Comparator.comparingInt(TransitRoute::getTotalDurationValue));

        return filteredRoutes.subList(0, Math.min(3, filteredRoutes.size()));
    }
}
