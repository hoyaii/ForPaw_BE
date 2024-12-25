package com.hong.forapw.integration.geocoding.service;

import com.hong.forapw.integration.geocoding.model.Coordinates;

public interface GeocodingService {
    Coordinates getCoordinates(String address);
}
