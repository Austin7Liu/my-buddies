package com.austin.module.meetup.support;

import org.springframework.stereotype.Component;

@Component
public class HaversineGeoDistanceCalculator implements GeoDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    @Override
    public double distanceMeters(double firstLatitude, double firstLongitude,
            double secondLatitude, double secondLongitude) {
        double latitudeDelta = Math.toRadians(secondLatitude - firstLatitude);
        double longitudeDelta = Math.toRadians(secondLongitude - firstLongitude);
        double firstLatitudeRadians = Math.toRadians(firstLatitude);
        double secondLatitudeRadians = Math.toRadians(secondLatitude);
        double value = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(firstLatitudeRadians) * Math.cos(secondLatitudeRadians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }
}
