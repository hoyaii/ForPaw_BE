package com.hong.forapw.service;

import com.hong.forapw.controller.dto.*;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.JsonParser;
import com.hong.forapw.domain.animal.AnimalType;
import com.hong.forapw.domain.animal.FavoriteAnimal;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.Shelter;
import com.hong.forapw.repository.animal.AnimalRepository;
import com.hong.forapw.repository.animal.FavoriteAnimalRepository;
import com.hong.forapw.repository.ShelterRepository;
import com.hong.forapw.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hong.forapw.core.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;
import static com.hong.forapw.core.utils.PaginationUtils.isLastPage;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final UserRepository userRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final ShelterService shelterService;
    private final EntityManager entityManager;
    private final WebClient webClient;
    private final JsonParser jsonParser;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.animal.uri}")
    private String animalURI;

    @Value("${animal.names}")
    private String[] animalNames;

    @Value("${kakao.key}")
    private String kakaoAPIKey;

    @Value("${kakao.map.geocoding.uri}")
    private String kakaoGeoCodingURI;

    @Value("${recommend.uri}")
    private String animalRecommendURI;

    @Value("${animal.update.uri}")
    private String updateAnimalIntroduceURI;

    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animalLikeNum";
    private static final String ANIMAL_SEARCH_KEY_PREFIX = "animalSearch";
    public static final Long ANIMAL_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달
    private static final Map<String, AnimalType> ANIMAL_TYPE_MAP = Map.of("dog", AnimalType.DOG, "cat", AnimalType.CAT, "other", AnimalType.OTHER);

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void updateAnimalData() {
        List<Long> existingAnimalIds = animalRepository.findAllIds();
        List<Shelter> shelters = shelterRepository.findAllWithRegionCode();

        List<Tuple2<Shelter, String>> animalJsonResponses = Optional.ofNullable(fetchShelterJsonResponses(shelters).collectList().block())
                .orElse(Collections.emptyList());

        saveNewAnimalData(animalJsonResponses, existingAnimalIds);

        shelterService.updateShelterData(animalJsonResponses);
        postProcessAfterAnimalUpdate();
    }

    @Transactional
    public void updateShelterAddressByKakao() {
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);

        Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(50))
                .flatMap(shelter -> {
                    URI uri = buildKakaoGeocodingURI(shelter.getCareAddr());
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

    @Transactional(readOnly = true)
    public AnimalResponse.FindAnimalListDTO findAnimalList(String type, Long userId, Pageable pageable) {
        AnimalType animalType = converStringToAnimalType(type);
        Page<Animal> animalPage = animalRepository.findByAnimalType(animalType, pageable);
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();

        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> createAnimalDTO(animal, likedAnimalIds))
                .collect(Collectors.toList());

        return new AnimalResponse.FindAnimalListDTO(animalDTOS, isLastPage(animalPage));
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindRecommendedAnimalList findRecommendedAnimalList(Long userId) {
        List<Long> recommendedAnimalIds = findRecommendedAnimalIds(userId);
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();

        List<AnimalResponse.AnimalDTO> animalDTOS = animalRepository.findByIds(recommendedAnimalIds).stream()
                .map(animal -> createAnimalDTO(animal, likedAnimalIds))
                .collect(Collectors.toList());

        return new AnimalResponse.FindRecommendedAnimalList(animalDTOS);
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindLikeAnimalListDTO findLikeAnimalList(Long userId) {
        List<Animal> animalPage = favoriteAnimalRepository.findAnimalsByUserId(userId);

        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.stream()
                .map(animal -> {
                    Long likeNum = getCachedLikeNum(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId());
                    return new AnimalResponse.AnimalDTO(
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
                            true,
                            animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new AnimalResponse.FindLikeAnimalListDTO(animalDTOS);
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindAnimalByIdDTO findAnimalById(Long animalId, Long userId) {
        // 로그인을 한 경우, 추천을 위해 검색한 동물의 id 저장 (5개까지만 저장된다)
        if (userId != null) {
            String key = ANIMAL_SEARCH_KEY_PREFIX + ":" + userId;
            redisService.addListElement(key, animalId.toString(), 5L);
        }

        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        boolean isLike = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animal.getId()).isPresent();

        return new AnimalResponse.FindAnimalByIdDTO(animalId,
                animal.getName(),
                animal.getAge(),
                animal.getGender(),
                animal.getSpecialMark(),
                animal.getRegion(),
                isLike,
                animal.getProfileURL(),
                animal.getHappenPlace(),
                animal.getKind(),
                animal.getColor(),
                animal.getWeight(),
                animal.getNoticeSdt(),
                animal.getNoticeEdt(),
                animal.getProcessState(),
                animal.getNeuter(),
                animal.getIntroductionTitle(),
                animal.getIntroductionContent(),
                animal.isAdopted());
    }

    @Transactional
    public void likeAnimal(Long userId, Long animalId) {
        // 존재하지 않는 동물이면 에러
        if (!animalRepository.existsById(animalId)) {
            throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
        }

        Optional<FavoriteAnimal> favoriteAnimalOP = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animalId);

        // 좋아요가 이미 있다면 삭제, 없다면 추가
        if (favoriteAnimalOP.isPresent()) {
            favoriteAnimalRepository.delete(favoriteAnimalOP.get());
            redisService.decrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), 1L);
        } else {
            Animal animalRef = entityManager.getReference(Animal.class, animalId);
            User userRef = entityManager.getReference(User.class, userId);

            FavoriteAnimal favoriteAnimal = FavoriteAnimal.builder()
                    .user(userRef)
                    .animal(animalRef)
                    .build();

            favoriteAnimalRepository.save(favoriteAnimal);
            redisService.incrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), 1L);
        }
    }

    public List<Long> findRecommendedAnimalIds(Long userId) {
        Pageable pageable = PageRequest.of(0, 5);

        // 로그인 되지 않았으면, 추천을 할 수 없으니 그냥 최신순 반환
        if (userId == null) {
            return animalRepository.findAllIds(pageable).getContent();
        }

        Map<String, Long> requestBody = Map.of("user_id", userId);
        List<Long> recommendedAnimalIds = webClient.post()
                .uri(animalRecommendURI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(AnimalResponse.RecommendationDTO.class)
                .map(AnimalResponse.RecommendationDTO::recommendedAnimals)
                .onErrorResume(e -> {
                    log.info("FastAPI 호술 시 에러 발생: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();

        // 조회 기록이 없어서, 추천하는 ID 목록이 없으면 사용자 위치 기반으로 가져온다
        if (recommendedAnimalIds.isEmpty()) {
            recommendedAnimalIds = findAnimalIdListByUserLocation(userId);
        }

        return recommendedAnimalIds;
    }

    private Flux<Tuple2<Shelter, String>> fetchShelterJsonResponses(List<Shelter> shelters) {
        return Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(75))
                .flatMap(shelter -> buildAnimalOpenApiURI(serviceKey, shelter.getId())
                        .flatMap(uri -> webClient.get()
                                .uri(uri)
                                .retrieve()
                                .bodyToMono(String.class)
                                .retry(3)
                                .map(rawJsonResponse -> Tuples.of(shelter, rawJsonResponse)))
                        .onErrorResume(e -> Mono.empty()));
    }

    public void saveNewAnimalData(List<Tuple2<Shelter, String>> animalJsonResponses, List<Long> existingAnimalIds) {
        for (Tuple2<Shelter, String> tuple : animalJsonResponses) {
            Shelter shelter = tuple.getT1();
            String animalJsonData = tuple.getT2();

            List<Animal> animals = convertJsonResponseToAnimals(animalJsonData, shelter, existingAnimalIds);
            animalRepository.saveAll(animals);
        }
    }

    public List<Animal> convertJsonResponseToAnimals(String animalJsonData, Shelter shelter, List<Long> existingAnimalIds) {
        return jsonParser.parse(animalJsonData, AnimalDTO.class)
                .map(AnimalDTO::response)
                .map(AnimalDTO.ResponseDTO::body)
                .map(AnimalDTO.BodyDTO::items)
                .map(AnimalDTO.ItemsDTO::item)
                .orElse(Collections.emptyList())
                .stream()
                .filter(item -> isNewAnimal(item, existingAnimalIds))
                .filter(this::isActiveAnimal)
                .map(item -> createAnimalFromDTO(item, shelter))
                .collect(Collectors.toList());
    }

    private boolean isNewAnimal(AnimalDTO.ItemDTO item, List<Long> existingAnimalIds) {
        return !existingAnimalIds.contains(Long.valueOf(item.desertionNo()));
    }

    private boolean isActiveAnimal(AnimalDTO.ItemDTO item) {
        return LocalDate.parse(item.noticeEdt(), YEAR_HOUR_DAY_FORMAT).isAfter(LocalDate.now());
    }

    private Animal createAnimalFromDTO(AnimalDTO.ItemDTO itemDTO, Shelter shelter) {
        DateTimeFormatter formatter = YEAR_HOUR_DAY_FORMAT;
        return Animal.builder()
                .id(Long.valueOf(itemDTO.desertionNo()))
                .name(createAnimalName())
                .shelter(shelter)
                .happenDt(LocalDate.parse(itemDTO.happenDt(), formatter))
                .happenPlace(itemDTO.happenPlace())
                .kind(parseSpecies(itemDTO.kindCd()))
                .category(AnimalType.from(itemDTO.kindCd()))
                .color(itemDTO.colorCd())
                .age(itemDTO.age())
                .weight(itemDTO.weight())
                .noticeSdt(LocalDate.parse(itemDTO.noticeSdt(), formatter))
                .noticeEdt(LocalDate.parse(itemDTO.noticeEdt(), formatter))
                .profileURL(convertHttpToHttps(itemDTO.popfile()))
                .processState(itemDTO.processState())
                .gender(itemDTO.sexCd())
                .neuter(itemDTO.neuterYn())
                .specialMark(itemDTO.specialMark())
                .region(shelter.getRegionCode().getUprName() + " " + shelter.getRegionCode().getOrgName())
                .introductionContent("소개글을 작성중입니다!")
                .build();
    }

    private static String parseSpecies(String input) {
        Pattern pattern = Pattern.compile("\\[.*?\\] (.+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    // 동물 이름 지어주는 메서드
    private String createAnimalName() {
        int index = ThreadLocalRandom.current().nextInt(animalNames.length);
        return animalNames[index];
    }

    private void postProcessAfterAnimalUpdate() {
        List<Animal> expiredAnimals = animalRepository.findAllOutOfDateWithShelter(LocalDateTime.now().toLocalDate());
        Set<Shelter> updatedShelters = findUpdatedShelters(expiredAnimals);

        removeAnimalLikesFromCache(expiredAnimals);
        removeAnimalsFromUserSearchHistory(expiredAnimals);

        favoriteAnimalRepository.deleteByAnimalIn(expiredAnimals);
        animalRepository.deleteAll(expiredAnimals);

        updateShelterAnimalCounts(updatedShelters);
        updateAnimalIntroductions();
        resolveDuplicateShelters();
    }

    private void removeAnimalLikesFromCache(List<Animal> expiredAnimals) {
        expiredAnimals.forEach(animal ->
                redisService.removeValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId().toString())
        );
    }

    private void removeAnimalsFromUserSearchHistory(List<Animal> expiredAnimals) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            String key = ANIMAL_SEARCH_KEY_PREFIX + ":" + user.getId();
            expiredAnimals.forEach(animal ->
                    redisService.removeListElement(key, animal.getId().toString())
            );
        }
    }

    private void updateShelterAnimalCounts(Set<Shelter> updatedShelters) {
        updatedShelters.forEach(shelter ->
                shelter.updateAnimalCount(animalRepository.countByShelterId(shelter.getId()))
        );
    }

    private Set<Shelter> findUpdatedShelters(List<Animal> expiredAnimals) {
        return expiredAnimals.stream()
                .map(Animal::getShelter)
                .collect(Collectors.toSet());
    }

    private Mono<URI> buildAnimalOpenApiURI(String serviceKey, Long careRegNo) {
        String url = animalURI + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo + "&_type=json" + "&numOfRows=1000";
        try {
            return Mono.just(new URI(url));
        } catch (URISyntaxException e) {
            return Mono.empty();
        }
    }

    private void updateAnimalIntroductions() {
        webClient.post()
                .uri(updateAnimalIntroduceURI)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        this::mapError
                )
                .bodyToMono(Void.class)
                .retryWhen(createRetrySpec())
                .doOnError(this::handleError)
                .subscribe();
    }

    private Mono<Throwable> mapError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
                .map(errorBody -> new CustomException(ExceptionCode.INTRODUCTION_RETRY_FAIL,
                        "소개글 요청 실패: " + clientResponse.statusCode() + " - " + errorBody
                ));
    }

    private Retry createRetrySpec() {
        return Retry.fixedDelay(3, Duration.ofSeconds(2))
                .filter(CustomException.class::isInstance)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                        new CustomException(ExceptionCode.INTRODUCTION_RETRY_FAIL)
                );
    }

    private void handleError(Throwable error) {
        if (error instanceof CustomException) {
            log.error("소개글 업데이트 요청 실패: {}", error.getMessage());
        } else {
            log.error("예상치 못한 에러 발생: {}", error.getMessage(), error);
        }
    }

    private void resolveDuplicateShelters() {
        List<String> duplicateCareTels = shelterRepository.findDuplicateCareTels();
        duplicateCareTels.forEach(this::handleDuplicateSheltersForCareTel);
    }

    @Transactional
    public void handleDuplicateSheltersForCareTel(String careTel) {
        List<Shelter> shelters = shelterRepository.findByCareTel(careTel);
        Shelter targetShelter = findTargetShelter(shelters);
        List<Long> duplicateShelterIds = findDuplicateShelterIds(shelters, targetShelter);

        if (!duplicateShelterIds.isEmpty()) {
            animalRepository.updateShelterByShelterIds(targetShelter, duplicateShelterIds);
            shelterRepository.updateIsDuplicateByIds(duplicateShelterIds, true);
        }

        shelterRepository.updateIsDuplicate(targetShelter.getId(), false);
    }

    private Shelter findTargetShelter(List<Shelter> shelters) {
        return shelters.stream()
                .filter(shelter -> !shelter.isDuplicate())
                .min(Comparator.comparingLong(Shelter::getId))
                .orElse(shelters.get(0));
    }

    private List<Long> findDuplicateShelterIds(List<Shelter> shelters, Shelter targetShelter) {
        return shelters.stream()
                .filter(shelter -> !shelter.equals(targetShelter))
                .map(Shelter::getId)
                .collect(Collectors.toList());
    }


    private URI buildKakaoGeocodingURI(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(kakaoGeoCodingURI);
        uriBuilder.queryParam("query", address);

        return uriBuilder.build().encode().toUri();
    }

    private List<Long> findAnimalIdListByUserLocation(Long userId) {
        PageRequest pageRequest = PageRequest.of(0, 5);

        // 우선 사용자 district를 바탕으로 조회
        List<Long> animalIds = userRepository.findDistrictById(userId)
                .map(district -> animalRepository.findIdsByDistrict(district, pageRequest))
                .orElseGet(ArrayList::new);

        // 조회된 동물 ID의 수가 5개 미만인 경우, province 범위까지 확대해서 추가 조회
        if (animalIds.size() < 5) {
            animalIds.addAll(userRepository.findProvinceById(userId)
                    .map(province -> animalRepository.findIdsByProvince(province, pageRequest))
                    .orElseGet(ArrayList::new));

            return animalIds.subList(0, Math.min(5, animalIds.size()));
        }

        return animalIds;
    }

    private AnimalType converStringToAnimalType(String type) {
        if (type.equals("date")) {
            return null;
        }

        return Optional.ofNullable(ANIMAL_TYPE_MAP.get(type))
                .orElseThrow(() -> new CustomException(ExceptionCode.WRONG_ANIMAL_TYPE));
    }

    private Long getCachedLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = animalRepository.countLikesByAnimalId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), ANIMAL_EXP);
        }

        return likeNum;
    }

    public String convertHttpToHttps(String url) {
        if (Objects.requireNonNull(url).startsWith("http://")) {
            return url.replaceFirst("http://", "https://");
        }

        return url;
    }

    private AnimalResponse.AnimalDTO createAnimalDTO(Animal animal, List<Long> likedAnimalIds) {
        Long likeNum = getCachedLikeNum(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId());
        return new AnimalResponse.AnimalDTO(
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
    }
}