package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.ShelterResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Animal.Animal;
import com.hong.ForPaw.domain.RegionCode;
import com.hong.ForPaw.controller.DTO.ShelterDTO;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.*;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.Animal.FavoriteAnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final AnimalRepository animalRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final ObjectMapper mapper;
    private final WebClient webClient;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.shelter.uri}")
    private String baseUrl;

    @Transactional
    @Scheduled(cron = "0 0 6 * * MON") // 매주 월요일 새벽 6시에 실행
    public void loadShelterData() {
        List<RegionCode> regionCodeList = regionCodeRepository.findAll();

        Flux.fromIterable(regionCodeList)
                .delayElements(Duration.ofMillis(50))
                .flatMap(regionCode -> {
                    Integer uprCd = regionCode.getUprCd();
                    Integer orgCd = regionCode.getOrgCd();

                    URI uri = buildShelterURI(baseUrl, serviceKey, uprCd, orgCd);

                    return webClient.get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class)
                            .retry(3)
                            .flatMapMany(response -> processShelterData(response, regionCode));
                })
                .collectList()
                .subscribe(shelterRepository::saveAll);
    }
    @Transactional
    public ShelterResponse.FindShelterListDTO findShelterList(Double lat, Double lng){
        // 가장 가까운 순으로 보호소 가져옴
        List<Shelter> shelterPage = shelterRepository.findNearestShelters(lat, lng);

        List<ShelterResponse.ShelterDTO> shelterDTOS = shelterPage.stream()
                .map(shelter -> new ShelterResponse.ShelterDTO(shelter.getId(), shelter.getName(), shelter.getLatitude(), shelter.getLongitude()))
                .collect(Collectors.toList());

        return new ShelterResponse.FindShelterListDTO(shelterDTOS);
    }

    @Transactional
    public ShelterResponse.FindShelterByIdDTO findShelterById(Long shelterId, Long userId, Integer page, String sort){
        // 보호소가 존재하지 않으면 에러
        Shelter shelter = shelterRepository.findById(shelterId).orElseThrow(
                () -> new CustomException(ExceptionCode.SHELTER_NOT_FOUND)
        );

        Pageable pageable = createPageable(page, 5, sort);
        Page<Animal> animalPage = animalRepository.findByShelterId(shelterId, pageable);

        // 사용자가 '좋아요' 표시한 Animal의 ID 목록
        // 만약 로그인 되어 있지 않다면, 빈 리스트로 처리한다.
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findLikedAnimalIdsByUserId(userId) : new ArrayList<>();

        List<ShelterResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> {
                    Long inquiryNum = redisService.getDataInLong("inquiryNum", animal.getId().toString());
                    Long likeNum = redisService.getDataInLong("animalLikeNum", animal.getId().toString());

                    return new ShelterResponse.AnimalDTO(
                        animal.getId(),
                        animal.getName(),
                        animal.getAge(),
                        animal.getGender(),
                        animal.getSpecialMark(),
                        animal.getRegion(),
                        inquiryNum,
                        likeNum,
                        likedAnimalIds.contains(animal.getId()),
                        animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new ShelterResponse.FindShelterByIdDTO(shelter.getCareAddr(), shelter.getCareTel(), animalDTOS);
    }

    private Flux<Shelter> processShelterData(String response, RegionCode regionCode){
        try {
            ShelterDTO json = mapper.readValue(response, ShelterDTO.class);
            List<ShelterDTO.itemDTO> itemDTOS = Optional.ofNullable(json.response().body().items())
                    .map(ShelterDTO.ItemsDTO::item)
                    .orElse(Collections.emptyList());

            return Flux.fromIterable(itemDTOS)
                    .map(itemDTO -> createShelter(regionCode, itemDTO.careRegNo(), itemDTO.careNm()));
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private Shelter createShelter(RegionCode regionCode, Long careRegNo, String careNm){
        return Shelter.builder()
                .regionCode(regionCode)
                .id(careRegNo)
                .name(careNm)
                .build();
    }

    private URI buildShelterURI(String baseUrl, String serviceKey, Integer uprCd, Integer orgCd) {
        String url = baseUrl + "?serviceKey=" + serviceKey + "&upr_cd=" + uprCd + "&org_cd=" + orgCd + "&_type=json";
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Pageable createPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }
}
