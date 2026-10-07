package com.alamano.core.domain.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alamano.core.domain.professional.GeoPoint;
import org.junit.jupiter.api.Test;

class EtaCalculatorTest {
    private final EtaCalculator calculator = new EtaCalculator(25);

    @Test
    void estimatesAboutTwelveMinutesForFiveKilometersInBogota() {
        GeoPoint from = new GeoPoint(4.6486, -74.0628);
        GeoPoint to = new GeoPoint(4.6935, -74.0628);

        int eta = calculator.etaSeconds(from, to, "EN_ROUTE");

        assertEquals(720, eta, 36);
    }

    @Test
    void arrivedAndInProgressHaveZeroEta() {
        GeoPoint point = new GeoPoint(4.6486, -74.0628);
        assertEquals(0, calculator.etaSeconds(point, point, "ARRIVED"));
        assertEquals(0, calculator.etaSeconds(point, point, "IN_PROGRESS"));
    }

    @Test
    void missingDestinationHasUnknownEta() {
        assertNull(calculator.etaSeconds(new GeoPoint(4.6486, -74.0628), null, "EN_ROUTE"));
    }
}
