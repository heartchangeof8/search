package com.immobilier.searcher.model;

import lombok.Getter;

public class TransitRoute {
    private String walkingDistance;
    private String walkingDuration;
    private String departureStation;
    private String transitSteps;
    private String totalDuration;
    private int totalDurationValue;

    public TransitRoute(String walkingDistance, String walkingDuration, String departureStation, String transitSteps, String totalDuration, int totalDurationValue) {
        this.walkingDistance = walkingDistance;
        this.walkingDuration = walkingDuration;
        this.departureStation = departureStation;
        this.transitSteps = transitSteps;
        this.totalDuration = totalDuration;
        this.totalDurationValue = totalDurationValue;
    }

    public String getWalkingDistance() {
        return walkingDistance;
    }

    public String getWalkingDuration() {
        return walkingDuration;
    }

    public String getDepartureStation() {
        return departureStation;
    }

    public String getTransitSteps() {
        return transitSteps;
    }

    public String getTotalDuration() {
        return totalDuration;
    }

    public int getTotalDurationValue() {
        return totalDurationValue;
    }
}
