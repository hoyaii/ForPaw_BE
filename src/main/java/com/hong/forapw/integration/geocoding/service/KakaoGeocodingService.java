package com.hong.forapw.integration.geocoding.service;

import com.hong.forapw.integration.geocoding.model.KakaoMapDTO;
import com.hong.forapw.integration.geocoding.model.Coordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

import static com.hong.forapw.common.utils.UriUtils.buildKakaoGeocodingURI;

@Service
@RequiredArgsConstructor
public class KakaoGeocodingService implements GeocodingService {

    @Value("${kakao.key}")
    private String kakaoAPIKey;

    @Value("${kakao.map.geocoding.uri}")
    private String kakaoGeoCodingURI;

    private final WebClient webClient;

    @Override
    public Coordinates getCoordinates(String address) {
        URI geocodingUri = buildKakaoGeocodingURI(address, kakaoGeoCodingURI);
        KakaoMapDTO.MapDTO geocodingResponse = fetchGeocodingData(geocodingUri);

        return extractCoordinates(geocodingResponse);
    }

    private KakaoMapDTO.MapDTO fetchGeocodingData(URI uri) {
        return webClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + kakaoAPIKey)
                .retrieve()
                .bodyToMono(KakaoMapDTO.MapDTO.class)
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("카카오맵 API가 null을 반환하거나 잘못된 형식을 반환함."));
    }

    private Coordinates extractCoordinates(KakaoMapDTO.MapDTO kakaoMapDTO) {
        return kakaoMapDTO.documents().stream()
                .findFirst()
                .map(documentDTO -> new Coordinates(Double.parseDouble(documentDTO.y()), Double.parseDouble(documentDTO.x())))
                .orElseThrow(() -> new RuntimeException("결과가 없습니다."));
    }
}
