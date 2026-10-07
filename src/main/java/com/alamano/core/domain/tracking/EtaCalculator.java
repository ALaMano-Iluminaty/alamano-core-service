package com.alamano.core.domain.tracking;

import com.alamano.core.domain.professional.GeoPoint;

public final class EtaCalculator {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private final double averageSpeedKmh;

    public EtaCalculator(double averageSpeedKmh) {
        if (!Double.isFinite(averageSpeedKmh) || averageSpeedKmh <= 0) {
            throw new IllegalArgumentException("La velocidad promedio debe ser positiva.");
        }
        this.averageSpeedKmh = averageSpeedKmh;
    }

    public Integer etaSeconds(GeoPoint from, GeoPoint to, String serviceStatus) {
        if ("ARRIVED".equals(serviceStatus) || "IN_PROGRESS".equals(serviceStatus)) return 0;
        if (to == null) return null;
        if (!"RESERVED".equals(serviceStatus) && !"EN_ROUTE".equals(serviceStatus)) return null;
        double distanceKm = distanceKm(from, to);
        return (int) Math.ceil(distanceKm / averageSpeedKmh * 3600.0);
    }

    private static double distanceKm(GeoPoint from, GeoPoint to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(to.longitude() - from.longitude());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
