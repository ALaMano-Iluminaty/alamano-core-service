package com.alamano.core.domain.professional;

import java.util.Objects;

public record SearchArea(GeoPoint center, double radiusKm) {
    public static final double DEFAULT_RADIUS_KM = 5.0;
    public static final double MAX_RADIUS_KM = 20.0;
    private static final double KM_PER_LATITUDE_DEGREE = 111.0;
    private static final double MIN_POLE_COSINE = 1.0e-6;

    public SearchArea {
        if (center == null) {
            throw new InvalidSearchAreaException("El centro de búsqueda es obligatorio.");
        }
        if (!Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > MAX_RADIUS_KM) {
            throw new InvalidSearchAreaException("El radio debe ser mayor que 0 y no superar 20 km.");
        }
    }

    public BoundingBox boundingBox() {
        double deltaLat = radiusKm / KM_PER_LATITUDE_DEGREE;
        double cosine = Math.cos(Math.toRadians(center.latitude()));
        double deltaLng = Math.abs(cosine) < MIN_POLE_COSINE
                ? 180.0
                : radiusKm / (KM_PER_LATITUDE_DEGREE * Math.abs(cosine));
        return new BoundingBox(
                Math.max(-90, center.latitude() - deltaLat),
                Math.min(90, center.latitude() + deltaLat),
                Math.max(-180, center.longitude() - deltaLng),
                Math.min(180, center.longitude() + deltaLng));
    }
}
