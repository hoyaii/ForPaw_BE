package com.hong.forapw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.core.utils.UriUtils.*;

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
    private final JsonParser jsonParser;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.shelter.uri}")
    private String baseUrl;

    @Value("${kakao.key}")
    private String kakaoAPIKey;

    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animalLikeNum";
    private static final Long ANIMAL_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달

    @Transactional
    @Scheduled(cron = "0 0 6 * * MON") // 매주 월요일 새벽 6시에 실행
    public void updateShelterData() {
        List<Long> existShelterIds = shelterRepository.findAllIds();
        List<RegionCode> regionCodes = regionCodeRepository.findAll();

        Flux.fromIterable(regionCodes)
                .delayElements(Duration.ofMillis(50))
                .flatMap(regionCode -> {
                    Integer uprCd = regionCode.getUprCd();
                    Integer orgCd = regionCode.getOrgCd();
                    URI uri = createShelterOpenApiURI(baseUrl, serviceKey, uprCd, orgCd);

                    return webClient.get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class)
                            .retry(3)
                            .flatMapMany(response -> Mono.fromCallable(() -> convertResponseToShelter(response, regionCode, existShelterIds))
                                    .flatMapMany(Flux::fromIterable)
                                    .onErrorResume(e -> Flux.empty())); // 에러 발생 시, 빈 Flux 반환 (JSON 파싱 에러)
                })
                .collectList()
                .subscribe(shelterRepository::saveAll);
    }

    @Transactional
    public void updateShelterData(List<Tuple2<Shelter, String>> animalJsonResponses) {
        for (Tuple2<Shelter, String> tuple : animalJsonResponses) {
            Shelter shelter = tuple.getT1();
            String animalJsonData = tuple.getT2();

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
        // 보호소가 존재하지 않으면 에러
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
    public ShelterResponse.FindShelterAnimalsByIdDTO findShelterAnimalListById(Long shelterId, Long userId, String type, Pageable pageable) {
        Page<Animal> animalPage = animalRepository.findByShelterIdAndType(AnimalType.fromString(type), shelterId, pageable);

        // 사용자가 '좋아요' 표시한 Animal의 ID 목록, 만약 로그인 되어 있지 않다면, 빈 리스트로 처리한다.
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();

        List<ShelterResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> {
                    Long likeNum = getCachedLikeNum(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId());
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
                            likeNum,
                            likedAnimalIds.contains(animal.getId()),
                            animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new ShelterResponse.FindShelterAnimalsByIdDTO(animalDTOS, !animalPage.hasNext());
    }

    @Transactional(readOnly = true)
    public ShelterResponse.FindShelterListWithAddr findShelterListWithAddr() {
        List<Shelter> shelters = shelterRepository.findAll();

        // Province와 District로 보호소를 그룹화
        Map<Province, Map<District, List<Shelter>>> groupedShelters = shelters.stream()
                .collect(Collectors.groupingBy(
                        shelter -> shelter.getRegionCode().getUprName(), // Province로 그룹화
                        Collectors.groupingBy(
                                shelter -> shelter.getRegionCode().getOrgName() // District로 그룹화
                        )
                ));

        // 그룹화된 데이터를 응답 형식으로 변환
        Map<String, List<ShelterResponse.DistrictDTO>> responseMap = groupedShelters.entrySet().stream()
                .collect(Collectors.toMap(
                        provinceEntry -> provinceEntry.getKey().name(), // Province 이름을 키로
                        provinceEntry -> provinceEntry.getValue().entrySet().stream()
                                .map(districtEntry -> {
                                    Map<String, List<String>> subDistrict = Map.of(
                                            districtEntry.getKey().name(),
                                            districtEntry.getValue().stream()
                                                    .map(Shelter::getName) // 해당 District 내의 보호소 이름 목록을 값으로
                                                    .collect(Collectors.toList())
                                    );
                                    return new ShelterResponse.DistrictDTO(subDistrict);
                                })
                                .collect(Collectors.toList())
                ));

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
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate currentDate = LocalDate.now().minusDays(1);

        return animalDTO.response().body().items().item().stream()
                .filter(animal -> LocalDate.parse(animal.noticeEdt(), dateFormatter).isAfter(currentDate))
                .count();
    }

    private void updateShelterAddressByGoogle() {
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);

        for (Shelter shelter : shelters) {
            URI uri = createGoogleGeocodingURI(shelter.getCareAddr());
            GoogleMapDTO.MapDTO mapDTO = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(GoogleMapDTO.MapDTO.class)
                    .block();

            if (mapDTO != null && !mapDTO.results().isEmpty()) {
                GoogleMapDTO.ResultDTO resultDTO = mapDTO.results().get(0);
                shelterRepository.updateAddressInfo(
                        resultDTO.geometry().location().lat(),
                        resultDTO.geometry().location().lng(),
                        shelter.getId()
                );
            }
        }
    }

    @Transactional
    public void updateShelterAddressByKakao() {
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);

        Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(50))
                .flatMap(shelter -> {
                    URI uri = createKakaoGeocodingURI(shelter.getCareAddr());
                    return webClient.get()
                            .uri(uri)
                            .header("Authorization", "KakaoAK " + kakaoAPIKey)
                            .retrieve()
                            .bodyToMono(KakaoMapDTO.MapDTO.class)
                            .flatMap(mapDTO -> Mono.justOrEmpty(mapDTO.documents().stream().findFirst()))
                            .doOnNext(document -> {
                                shelterRepository.updateAddressInfo(Double.valueOf(document.y()), Double.valueOf(document.x()), shelter.getId());
                            });
                })
                .subscribe();
    }

    private List<Shelter> convertResponseToShelter(String response, RegionCode regionCode, List<Long> existShelterIds) throws IOException {
        // JSON 파싱 에러 던질 수 있음
        ShelterDTO json = mapper.readValue(response, ShelterDTO.class);

        List<ShelterDTO.itemDTO> itemDTOS = Optional.ofNullable(json.response().body().items())
                .map(ShelterDTO.ItemsDTO::item)
                .orElse(Collections.emptyList());

        return itemDTOS.stream()
                .filter(itemDTO -> !existShelterIds.contains(itemDTO.careRegNo())) // 이미 저장되어 있는 보호소는 업데이트 하지 않는다
                .map(itemDTO -> Shelter.builder()
                        .regionCode(regionCode)
                        .id(itemDTO.careRegNo())
                        .name(itemDTO.careNm())
                        .build())
                .collect(Collectors.toList());
    }

    private Long getCachedLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = animalRepository.countLikesByAnimalId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), ANIMAL_EXP);
        }

        return likeNum;
    }
}
