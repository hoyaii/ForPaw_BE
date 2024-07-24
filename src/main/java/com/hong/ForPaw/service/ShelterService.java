package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.ShelterResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Animal.Animal;
import com.hong.ForPaw.domain.Animal.AnimalType;
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
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;
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

    private static final String SORT_BY_CREATED_DATE = "createdDate";

    private static final Map<String, AnimalType> ANIMAL_TYPE_MAP = Map.of(
            "dog", AnimalType.DOG,
            "cat", AnimalType.CAT,
            "other", AnimalType.OTHER
    );

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
                    URI uri = buildShelterURI(baseUrl, serviceKey, uprCd, orgCd);

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
    public ShelterResponse.FindShelterListDTO findActiveShelterList(){
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
    public ShelterResponse.FindShelterInfoByIdDTO findShelterInfoById(Long shelterId){
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
    public ShelterResponse.FindShelterAnimalsByIdDTO findShelterAnimalListById(Long shelterId, Long userId, Integer page, String sort){
        AnimalType animalType = converStringToAnimalType(sort);
        Pageable pageable = createPageable(page, 5, SORT_BY_CREATED_DATE);

        Page<Animal> animalPage = animalRepository.findByShelterIdAndType(animalType, shelterId, pageable);
        boolean isLastPage = !animalPage.hasNext();

        // 사용자가 '좋아요' 표시한 Animal의 ID 목록, 만약 로그인 되어 있지 않다면, 빈 리스트로 처리한다.
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findLikedAnimalIdsByUserId(userId) : new ArrayList<>();

        List<ShelterResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> {
                    Long likeNum = redisService.getDataInLong("animalLikeNum", animal.getId().toString());

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

        return new ShelterResponse.FindShelterAnimalsByIdDTO(animalDTOS, isLastPage);
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

    private AnimalType converStringToAnimalType(String sort) {
        if(sort.equals("meetDate")) {
            return null;
        }

        return Optional.ofNullable(ANIMAL_TYPE_MAP.get(sort))
                .orElseThrow(() -> new CustomException(ExceptionCode.BAD_APPROACH));
    }
}
