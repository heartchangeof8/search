package com.immobilier.searcher.model;

public class NearbyBusiness {
    private String name;
    private String type;
    private String distance;
    private String duration;
    private String mapsUrl;

    public NearbyBusiness(String name, String type, String distance, String duration, String mapsUrl) {
        this.name = name;
        this.type = type;
        this.distance = distance;
        this.duration = duration;
        this.mapsUrl = mapsUrl;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDistance() { return distance; }
    public String getDuration() { return duration; }
    public String getMapsUrl() { return mapsUrl; }

    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setDistance(String distance) { this.distance = distance; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setMapsUrl(String mapsUrl) { this.mapsUrl = mapsUrl; }

    @Override
    public String toString() {
        return "NearbyBusiness{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", distance='" + distance + '\'' +
                ", duration='" + duration + '\'' +
                ", mapsUrl='" + mapsUrl + '\'' +
                '}';
    }
}
