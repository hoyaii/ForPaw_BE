package com.hong.forapw.integration.geocoding.service;

import com.hong.forapw.integration.geocoding.model.GoogleMapDTO;
import com.hong.forapw.integration.geocoding.model.Coordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

import static com.hong.forapw.common.utils.UriUtils.buildGoogleGeocodingURI;

@Service
@RequiredArgsConstructor
public class GoogleGeocodingService implements GeocodingService {

    @Value("${google.map.geocoding.uri}")
    private String googleGeoCodingURI;

    @Value("${google.api.key}")
    private String googleAPIKey;

    private final WebClient webClient;

    @Override
    public Coordinates getCoordinates(String address) {
        URI geocodingUri = buildGoogleGeocodingURI(address, googleGeoCodingURI, googleAPIKey);
        GoogleMapDTO.MapDTO geocodingResponse = fetchGeocodingData(geocodingUri);

        return extractCoordinates(geocodingResponse);
    }

    private GoogleMapDTO.MapDTO fetchGeocodingData(URI uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GoogleMapDTO.MapDTO.class)
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("구글맵 API가 null을 반환하거나 잘못된 형식을 반환함."));
    }

    private Coordinates extractCoordinates(GoogleMapDTO.MapDTO geocodingResponse) {
        return geocodingResponse.results().stream()
                .findFirst()
                .map(result -> new Coordinates(result.geometry().location().lat(), result.geometry().location().lng()))
                .orElseThrow(() -> new RuntimeException("결과가 없습니다."));
    }
}