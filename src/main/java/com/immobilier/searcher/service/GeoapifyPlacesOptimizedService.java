package com.immobilier.searcher.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.immobilier.searcher.constants.CityLinks;
import com.immobilier.searcher.model.NearbyBusiness;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.util.*;

@Service
public class GeoapifyPlacesOptimizedService {

    private final WebClient webClient;

    @Value("${geoapify.api.key}")
    private String apiKey;

    public GeoapifyPlacesOptimizedService(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create();
               // .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
               //         .host("163.116.128.80")
               //         .port(8080));

        this.webClient = builder
                .baseUrl("https://api.geoapify.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public List<NearbyBusiness> getNearbyBusinesses(String address, int maxDurationMinutes) {
        double[] coords = geocodeAddress(address);
        if (coords == null) return List.of();
        return getNearbyBusinesses(coords[0], coords[1], maxDurationMinutes);
    }

    private double[] geocodeAddress(String address) {
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/geocode/search")
                        .queryParam("text", address)
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Geoapify geocode error: " + ex.getMessage());
                    return Mono.empty();
                })
                .block();

        if (response == null || !response.has("features") || response.path("features").isEmpty()) return null;

        try  {
            CityLinks.choosenCity =  CityLinks.cities.get(response.path("query").path("parsed").path("city").asText());
        } catch (Exception ex) {
            System.out.println(ex);
        }

        JsonNode location = response.path("features").get(0).path("geometry").path("coordinates");
        if (location.isMissingNode()) return null;

        double lon = location.get(0).asDouble();
        double lat = location.get(1).asDouble();
        return new double[]{lat, lon};
    }

    public List<NearbyBusiness> getNearbyBusinesses(double lat, double lon, int maxDurationMinutes) {
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("bakery", "commercial.food_and_drink.bakery");
        categoryMap.put("butcher", "commercial.food_and_drink.butcher");
        categoryMap.put("fast_food", "catering.fast_food");
        categoryMap.put("restaurant", "catering.restaurant");
        categoryMap.put("shopping_mall", "commercial.shopping_mall");
        categoryMap.put("gym", "sport.fitness.fitness_centre");
        categoryMap.put("supermarket", "commercial.supermarket");
        categoryMap.put("school", "education.school");
        categoryMap.put("mosque", "tourism.sights.place_of_worship.mosque");
        categoryMap.put("transport metro", "public_transport.subway");
        categoryMap.put("transport Rer", "public_transport.train");
        categoryMap.put("transport tram", "public_transport.tram");
        //categoryMap.put("train_station", "railway.train");
//        categoryMap.put("bus", "public_transport.bus");

        String allCategories = String.join(",", categoryMap.values());
        String filter = String.format(Locale.US, "circle:%.6f,%.6f,1000", lon, lat);
        String bias = String.format(Locale.US, "proximity:%.6f,%.6f", lon, lat);

        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/places")
                        .queryParam("categories", allCategories)
                        .queryParam("filter", filter)
                        .queryParam("bias", bias)
                        .queryParam("limit", "80")
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Geoapify places error: " + ex.getMessage());
                    return Mono.empty();
                })
                .block();

        if (response == null || !response.has("features") || response.path("features").isEmpty()) {
            System.out.println("No results from Geoapify");
            return List.of();
        }

        JsonNode features = response.path("features");
        List<NearbyBusiness> results = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (JsonNode place : features) {
            JsonNode geometry = place.path("geometry").path("coordinates");
            JsonNode props = place.path("properties");
            JsonNode categoriesNode = props.path("categories");

            if (geometry.isMissingNode() || categoriesNode.isMissingNode() || !categoriesNode.isArray()) continue;

            double placeLon = geometry.get(0).asDouble();
            double placeLat = geometry.get(1).asDouble();
            double distance = haversine(lat, lon, placeLat, placeLon);
            double estimatedMinutes = (distance / 5.0) * 60;

            if (estimatedMinutes > maxDurationMinutes) continue;

            String rawName = props.path("name").asText("N/A");

            String type = "unknown";
            for (JsonNode cat : categoriesNode) {
                String raw = cat.asText();
                for (Map.Entry<String, String> entry : categoryMap.entrySet()) {
                    if (raw.equals(entry.getValue())) {
                        type = entry.getKey();
                        break;
                    }
                }
                if (!type.equals("unknown")) break;
            }

            String fullName = rawName;

            if (seenNames.contains(fullName)) continue;
            seenNames.add(fullName);

            String distanceStr = String.format(Locale.US, "%.1f km", distance);
            String durationStr = String.format(Locale.US, "%.0f mins", estimatedMinutes);
            String mapsUrl = String.format(Locale.US,
                    "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f", placeLat, placeLon);

            results.add(new NearbyBusiness(fullName, type, distanceStr, durationStr, mapsUrl));
        }

        return results;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
