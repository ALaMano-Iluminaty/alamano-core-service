package com.alamano.core.domain.professional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SearchAreaTest {

    @Test
    void rejectsInvalidRadii() {
        GeoPoint bogota = new GeoPoint(4.6486, -74.0628);

        assertThrows(InvalidSearchAreaException.class, () -> new SearchArea(bogota, 0));
        assertThrows(InvalidSearchAreaException.class, () -> new SearchArea(bogota, -1));
        assertThrows(InvalidSearchAreaException.class, () -> new SearchArea(bogota, 20.01));
    }

    @Test
    void rejectsCoordinatesOutsideGeographicRanges() {
        assertThrows(InvalidSearchAreaException.class, () -> new GeoPoint(90.01, 0));
        assertThrows(InvalidSearchAreaException.class, () -> new GeoPoint(-90.01, 0));
        assertThrows(InvalidSearchAreaException.class, () -> new GeoPoint(0, 180.01));
        assertThrows(InvalidSearchAreaException.class, () -> new GeoPoint(0, -180.01));
    }

    @Test
    void boundingBoxContainsCenterAndHasExpectedLatitudeDelta() {
        GeoPoint center = new GeoPoint(4.6486, -74.0628);
        BoundingBox box = new SearchArea(center, 5).boundingBox();

        assertTrue(box.minLatitude() <= center.latitude());
        assertTrue(box.maxLatitude() >= center.latitude());
        assertTrue(box.minLongitude() <= center.longitude());
        assertTrue(box.maxLongitude() >= center.longitude());
        assertEquals(0.045, center.latitude() - box.minLatitude(), 0.0001);
        assertEquals(0.045, box.maxLatitude() - center.latitude(), 0.0001);
    }
}
