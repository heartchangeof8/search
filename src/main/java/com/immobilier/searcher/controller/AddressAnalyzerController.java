package com.immobilier.searcher.controller;

import com.immobilier.searcher.constants.CityLinks;
import com.immobilier.searcher.model.NearbyBusiness;
import com.immobilier.searcher.model.TransitRoute;
import com.immobilier.searcher.service.GeoapifyPlacesOptimizedService;
import com.immobilier.searcher.service.TransitService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AddressAnalyzerController {

    private final GeoapifyPlacesOptimizedService placesService;
    private final TransitService transitService;

    public AddressAnalyzerController(GeoapifyPlacesOptimizedService placesService, TransitService transitService) {
        this.placesService = placesService;
        this.transitService = transitService;
    }

    @GetMapping("/analyze")
    public Map<String, Object> analyzeAddress(@RequestParam(required = false) String address,
                                              @RequestParam(defaultValue = "10") int maxDuration) {
        List<NearbyBusiness> businesses;
        List<TransitRoute> transitRoutes;

        if (address != null && !address.isBlank()) {
            businesses = placesService.getNearbyBusinesses(address, maxDuration);
            //transitRoutes = transitService.getTransitRoutes(address);
        } else {
            throw new IllegalArgumentException("Either address must be provided.");
        }

        return Map.of(
                "nearbyBusinesses", businesses
                //"transitRoutes", transitRoutes
                //,"villeIdealeLink", CityLinks.choosenCity
        );
    }


    @GetMapping("/transit-route")
    public List<TransitRoute> getTransit(@RequestParam String address) {
        return transitService.getTransitRoutes(address);
    }
}
