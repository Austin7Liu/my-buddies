package com.austin.module.meetup.support;

public interface GeoDistanceCalculator {

    double distanceMeters(double firstLatitude, double firstLongitude,
            double secondLatitude, double secondLongitude);
}
