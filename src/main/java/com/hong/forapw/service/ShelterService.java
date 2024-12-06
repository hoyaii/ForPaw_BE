package com.hong.forapw.service;

import com.hong.forapw.controller.dto.*;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.JsonParser;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.animal.AnimalType;
import com.hong.forapw.domain.District;
import com.hong.forapw.domain.Province;
import com.hong.forapw.domain.RegionCode;
import com.hong.forapw.domain.Shelter;
import com.hong.forapw.repository.animal.AnimalRepository;
import com.hong.forapw.repository.animal.FavoriteAnimalRepository;
import com.hong.forapw.repository.RegionCodeRepository;
import com.hong.forapw.repository.ShelterRepository;
import com.hong.forapw.service.geocoding.Coordinates;
import com.hong.forapw.service.geocoding.GeocodingService;
import com.hong.forapw.service.geocoding.GoogleGeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.core.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;
import static com.hong.forapw.core.utils.PaginationUtils.isLastPage;
import static com.hong.forapw.core.utils.UriUtils.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final AnimalRepository animalRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final WebClient webClient;
    private final JsonParser jsonParser;
    private final GoogleGeocodingService googleService;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.shelter.uri}")
    private String baseUrl;

    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animalLikeNum";
    private static final Long ANIMAL_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달

    @Transactional
    @Scheduled(cron = "0 0 6 * * MON")
    public void updateNewShelters() {
        List<Long> savedShelterIds = shelterRepository.findAllIds(); // 이미 저장되어 있는 보호소는 배제
        List<RegionCode> regionCodes = regionCodeRepository.findAll();

        Flux.fromIterable(regionCodes)
                .delayElements(Duration.ofMillis(50))
                .flatMap(regionCode -> fetchShelterDataFromApi(regionCode, savedShelterIds))
                .collectList()
                .doOnNext(shelterRepository::saveAll)
                .doOnError(error -> log.error("보호소 데이터 패치 실패: {}", error.getMessage()))
                .subscribe();
    }

    @Transactional
    public void updateShelterData(List<AnimalJsonResponse> animalJsonResponses) {
        for (AnimalJsonResponse response : animalJsonResponses) {
            Shelter shelter = response.shelter();
            String animalJsonData = response.animalJson();
            updateShelterByAnimalData(animalJsonData, shelter);
        }

        updateShelterAddressByGoogle();
    }

    @Transactional
    public ShelterResponse.FindShelterListDTO findActiveShelterList() {
        List<Shelter> shelters = shelterRepository.findAllWithAnimalAndLatitude();

        List<ShelterResponse.ShelterDTO> shelterDTOS = shelters.stream()
                .map(shelter -> new ShelterResponse.ShelterDTO(
                        shelter.getId(),
                        shelter.getName(),
                        shelter.getLatitude(),
                        shelter.getLongitude(),
                        shelter.getCareAddr(),
                        shelter.getCareTel()))
                .collect(Collectors.toList());

        return new ShelterResponse.FindShelterListDTO(shelterDTOS);
    }

    @Transactional(readOnly = true)
    public ShelterResponse.FindShelterInfoByIdDTO findShelterInfoById(Long shelterId) {
        Shelter shelter = shelterRepository.findById(shelterId).orElseThrow(
                () -> new CustomException(ExceptionCode.SHELTER_NOT_FOUND)
        );

        return new ShelterResponse.FindShelterInfoByIdDTO(
                shelter.getId(),
                shelter.getName(),
                shelter.getLatitude(),
                shelter.getLongitude(),
                shelter.getCareAddr(),
                shelter.getCareTel(),
                shelter.getAnimalCnt()
        );
    }

    @Transactional(readOnly = true)
    public ShelterResponse.FindShelterAnimalsByIdDTO findAnimalsByShelter(Long shelterId, Long userId, String type, Pageable pageable) {
        List<Long> userLikedAnimalIds = findUserLikedAnimalIds(userId);

        Page<Animal> animalPage = animalRepository.findByShelterIdAndType(AnimalType.fromString(type), shelterId, pageable);
        List<ShelterResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> createAnimalDTO(animal, userLikedAnimalIds))
                .collect(Collectors.toList());

        return new ShelterResponse.FindShelterAnimalsByIdDTO(animalDTOS, isLastPage(animalPage));
    }

    @Transactional(readOnly = true)
    public ShelterResponse.FindShelterListWithAddr findShelterListWithAddress() {
        List<Shelter> shelters = shelterRepository.findAll();

        Map<Province, Map<District, List<Shelter>>> groupedShelters = groupSheltersByProvinceAndDistrict(shelters);
        Map<String, List<ShelterResponse.DistrictDTO>> responseMap = createShlelterResponseMap(groupedShelters);

        return new ShelterResponse.FindShelterListWithAddr(responseMap);
    }

    private void updateShelterByAnimalData(String animalJsonData, Shelter shelter) {
        jsonParser.parse(animalJsonData, AnimalDTO.class).ifPresent(
                animalDTO -> updateShelterWithAnimalData(animalDTO, shelter)
        );
    }

    private void updateShelterWithAnimalData(AnimalDTO animalData, Shelter shelter) {
        findFirstAnimalItem(animalData)
                .ifPresent(firstAnimalItem -> shelterRepository.updateShelterInfo(
                        firstAnimalItem.careTel(), firstAnimalItem.careAddr(), countActiveAnimals(animalData), shelter.getId())
                );
    }

    private Optional<AnimalDTO.ItemDTO> findFirstAnimalItem(AnimalDTO animalDTO) {
        return Optional.ofNullable(animalDTO)
                .map(AnimalDTO::response)
                .map(AnimalDTO.ResponseDTO::body)
                .map(AnimalDTO.BodyDTO::items)
                .map(AnimalDTO.ItemsDTO::item)
                .filter(items -> !items.isEmpty())
                .map(items -> items.get(0));
    }

    private long countActiveAnimals(AnimalDTO animalDTO) {
        LocalDate currentDate = LocalDate.now().minusDays(1);
        return animalDTO.response().body().items().item().stream()
                .filter(animal -> isAnimalNoticeNotExpired(animal, currentDate))
                .count();
    }

    private boolean isAnimalNoticeNotExpired(AnimalDTO.ItemDTO animal, LocalDate currentDate) {
        return LocalDate.parse(animal.noticeEdt(), YEAR_HOUR_DAY_FORMAT).isAfter(currentDate);
    }

    private void updateShelterAddressByGoogle() {
        updateShelterAddress(googleService);
    }

    private void updateShelterAddress(GeocodingService geocodingService) {
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);
        for (Shelter shelter : shelters) {
            try {
                Coordinates coordinates = geocodingService.getCoordinates(shelter.getCareAddr());
                shelterRepository.updateAddressInfo(coordinates.lat(), coordinates.lng(), shelter.getId());
            } catch (Exception e) {
                log.warn("보호소의 위/경도 업데이트 실패 ,shelter {}: {}", shelter.getId(), e.getMessage());
            }
        }
    }

    private Flux<Shelter> fetchShelterDataFromApi(RegionCode regionCode, List<Long> savedShelterIds) {
        try {
            URI uri = buildShelterOpenApiURI(baseUrl, serviceKey, regionCode.getUprCd(), regionCode.getOrgCd());
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retry(3)
                    .flatMapMany(response -> convertResponseToNewShelter(response, regionCode, savedShelterIds))
                    .onErrorResume(e -> Flux.empty());
        } catch (Exception e) {
            log.warn("보소 데이터 패치를 위한 URI가 유효하지 않음, regionCode{}: {}", regionCode, e.getMessage());
            return Flux.empty();
        }
    }

    private Flux<Shelter> convertResponseToNewShelter(String response, RegionCode regionCode, List<Long> savedShelterIds) {
        return Mono.fromCallable(() -> parseJsonToItemDTO(response))
                .flatMapMany(Flux::fromIterable)
                .filter(itemDTO -> isNewShelter(itemDTO, savedShelterIds))
                .map(itemDTO -> createShelter(itemDTO, regionCode))
                .onErrorResume(e -> {
                    log.warn("보호소 데이터를 패치해오는 과정 중 파싱에서 에러 발생, regionCode {}: {}", regionCode, e.getMessage());
                    return Flux.empty();
                });
    }

    private List<ShelterDTO.itemDTO> parseJsonToItemDTO(String response) {
        return jsonParser.parse(response, ShelterDTO.class)
                .map(this::extractShelterItemDTOS)
                .orElse(Collections.emptyList());
    }

    private List<ShelterDTO.itemDTO> extractShelterItemDTOS(ShelterDTO shelterDTO) {
        return Optional.ofNullable(shelterDTO.response())
                .map(ShelterDTO.ResponseDTO::body)
                .map(ShelterDTO.BodyDTO::items)
                .map(ShelterDTO.ItemsDTO::item)
                .orElse(Collections.emptyList());
    }

    private boolean isNewShelter(ShelterDTO.itemDTO itemDTO, List<Long> existShelterIds) {
        return !existShelterIds.contains(itemDTO.careRegNo());
    }

    private Shelter createShelter(ShelterDTO.itemDTO itemDTO, RegionCode regionCode) {
        return Shelter.builder()
                .regionCode(regionCode)
                .id(itemDTO.careRegNo())
                .name(itemDTO.careNm())
                .build();
    }

    private Long getCachedLikeNum(Long key) {
        Long likeNum = redisService.getValueInLongWithNull(ShelterService.ANIMAL_LIKE_NUM_KEY_PREFIX, key.toString());

        if (likeNum == null) {
            likeNum = animalRepository.countLikesByAnimalId(key);
            redisService.storeValue(ShelterService.ANIMAL_LIKE_NUM_KEY_PREFIX, key.toString(), likeNum.toString(), ANIMAL_EXP);
        }

        return likeNum;
    }

    /**
     * Shelter 리스트를 Province와 District 기준으로 그룹화
     */
    private Map<Province, Map<District, List<Shelter>>> groupSheltersByProvinceAndDistrict(List<Shelter> shelters) {
        return shelters.stream()
                .collect(Collectors.groupingBy(
                        shelter -> shelter.getRegionCode().getUprName(),
                        Collectors.groupingBy(
                                shelter -> shelter.getRegionCode().getOrgName()
                        )
                ));
    }

    /**
     * Province-District-Shelter 구조의 맵을 응답 데이터 구조(Map<String, List<DistrictDTO>>)로 변환
     */
    private Map<String, List<ShelterResponse.DistrictDTO>> createShlelterResponseMap(
            Map<Province, Map<District, List<Shelter>>> groupedShelters) {
        return groupedShelters.entrySet().stream()
                .collect(Collectors.toMap(
                        provinceEntry -> provinceEntry.getKey().name(),
                        provinceEntry -> convertToDistrictDTOList(provinceEntry.getValue())
                ));
    }

    private List<ShelterResponse.DistrictDTO> convertToDistrictDTOList(Map<District, List<Shelter>> districtShelterMap) {
        return districtShelterMap.entrySet().stream()
                .map(districtEntry -> {
                    Map<String, List<String>> districtMap = Map.of(
                            districtEntry.getKey().name(),
                            districtEntry.getValue().stream()
                                    .map(Shelter::getName)
                                    .toList()
                    );
                    return new ShelterResponse.DistrictDTO(districtMap);
                })
                .collect(Collectors.toList());
    }

    private List<Long> findUserLikedAnimalIds(Long userId) {
        // 로그인하지 않은 경우 빈 리스트 반환
        return (userId != null) ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : Collections.emptyList();
    }

    private ShelterResponse.AnimalDTO createAnimalDTO(Animal animal, List<Long> userLikedAnimalIds) {
        return new ShelterResponse.AnimalDTO(
                animal.getId(),
                animal.getName(),
                animal.getAge(),
                animal.getGender(),
                animal.getSpecialMark(),
                animal.getKind(),
                animal.getWeight(),
                animal.getNeuter(),
                animal.getProcessState(),
                animal.getRegion(),
                animal.getInquiryNum(),
                getCachedLikeNum(animal.getId()),
                userLikedAnimalIds.contains(animal.getId()),
                animal.getProfileURL());
    }
}