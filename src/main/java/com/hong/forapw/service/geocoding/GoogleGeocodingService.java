package com.hong.forapw.service.geocoding;

import com.hong.forapw.controller.dto.GoogleMapDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

import static com.hong.forapw.core.utils.UriUtils.buildGoogleGeocodingURI;

@Component
@RequiredArgsConstructor
public class GoogleGeocodingService implements GeocodingService {

    private final WebClient webClient;

    @Override
    public Coordinates getCoordinates(String address) {
        URI geocodingUri = buildGoogleGeocodingURI(address);
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