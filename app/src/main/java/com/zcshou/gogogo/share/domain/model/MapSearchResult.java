package com.acooldog.toolbox.share.domain.model;

public final class MapSearchResult {
    private final String name;
    private final String address;
    private final double latitude;
    private final double longitude;

    public MapSearchResult(String name, String address, double latitude, double longitude) {
        this.name = name == null ? "" : name.trim();
        this.address = address == null ? "" : address.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
