package com.alamano.core.domain.professional;

public record NearbyProfessional(String professionalId, GeoPoint location, double distanceKm) {}
