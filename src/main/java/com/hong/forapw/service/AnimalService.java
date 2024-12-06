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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

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
import static com.hong.forapw.core.utils.UriUtils.buildAnimalOpenApiURI;
import static com.hong.forapw.core.utils.UriUtils.convertHttpUrlToHttps;

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

    @Value("${animal.names}")
    private String[] animalNames;

    @Value("${recommend.uri}")
    private String animalRecommendURI;

    @Value("${animal.update.uri}")
    private String updateAnimalIntroduceURI;

    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animalLikeNum";
    private static final String ANIMAL_SEARCH_KEY_PREFIX = "animalSearch";
    public static final Long ANIMAL_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달
    private static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 5);

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

    @Transactional(readOnly = true)
    public AnimalResponse.FindAnimalListDTO findAnimalList(String type, Long userId, Pageable pageable) {
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();

        Page<Animal> animalPage = animalRepository.findByAnimalType(AnimalType.fromString(type), pageable);
        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> createAnimalDTO(animal, likedAnimalIds))
                .collect(Collectors.toList());

        return new AnimalResponse.FindAnimalListDTO(animalDTOS, isLastPage(animalPage));
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindRecommendedAnimalList findRecommendedAnimalList(Long userId) {
        List<Long> recommendedAnimalIds = findRecommendedAnimalIds(userId);
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();

        List<Animal> animals = animalRepository.findByIds(recommendedAnimalIds);
        List<AnimalResponse.AnimalDTO> animalDTOS = animals.stream()
                .map(animal -> createAnimalDTO(animal, likedAnimalIds))
                .collect(Collectors.toList());

        return new AnimalResponse.FindRecommendedAnimalList(animalDTOS);
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindLikeAnimalListDTO findLikeAnimalList(Long userId) {
        List<Animal> animalPage = favoriteAnimalRepository.findAnimalsByUserId(userId);
        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.stream()
                .map(animal -> createAnimalDTO(animal, Collections.emptyList()))
                .collect(Collectors.toList());

        return new AnimalResponse.FindLikeAnimalListDTO(animalDTOS);
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindAnimalByIdDTO findAnimalById(Long animalId, Long userId) {
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        boolean isLike = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animal.getId()).isPresent();
        saveSearchRecord(animalId, userId);

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
        validateAnimalExistence(animalId);

        favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animalId)
                .ifPresentOrElse(
                        this::removeAnimalLike,
                        () -> addAnimalLike(userId, animalId)
                );
    }

    // 로그인 X => 그냥 최신순, 로그인 O => 검색 기록을 바탕으로 추천 => 검색 기록이 없다면 위치를 기준으로 주변 보호소의 동물 추천
    public List<Long> findRecommendedAnimalIds(Long userId) {
        if (userId == null) {
            return findLatestAnimalIds();
        }

        List<Long> recommendedAnimalIds = fetchRecommendedAnimalIds(userId);
        if (recommendedAnimalIds.isEmpty()) {
            recommendedAnimalIds = findAnimalIdsByUserLocation(userId);
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

    private void saveNewAnimalData(List<Tuple2<Shelter, String>> animalJsonResponses, List<Long> existingAnimalIds) {
        for (Tuple2<Shelter, String> tuple : animalJsonResponses) {
            Shelter shelter = tuple.getT1();
            String animalJsonData = tuple.getT2();

            List<Animal> animals = convertJsonResponseToAnimals(animalJsonData, shelter, existingAnimalIds);
            animalRepository.saveAll(animals);
        }
    }

    private List<Animal> convertJsonResponseToAnimals(String animalJsonData, Shelter shelter, List<Long> existingAnimalIds) {
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
                .category(AnimalType.fromPrefix(itemDTO.kindCd()))
                .color(itemDTO.colorCd())
                .age(itemDTO.age())
                .weight(itemDTO.weight())
                .noticeSdt(LocalDate.parse(itemDTO.noticeSdt(), formatter))
                .noticeEdt(LocalDate.parse(itemDTO.noticeEdt(), formatter))
                .profileURL(convertHttpUrlToHttps(itemDTO.popfile()))
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

    private void validateAnimalExistence(Long animalId) {
        if (!animalRepository.existsById(animalId)) {
            throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
        }
    }

    private void removeAnimalLike(FavoriteAnimal favoriteAnimal) {
        favoriteAnimalRepository.delete(favoriteAnimal);
        redisService.decrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, favoriteAnimal.getAnimal().getId().toString(), 1L);
    }

    private void addAnimalLike(Long userId, Long animalId) {
        Animal animalRef = entityManager.getReference(Animal.class, animalId);
        User userRef = entityManager.getReference(User.class, userId);
        FavoriteAnimal favoriteAnimal = FavoriteAnimal.builder()
                .user(userRef)
                .animal(animalRef)
                .build();

        favoriteAnimalRepository.save(favoriteAnimal);
        redisService.incrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), 1L);
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

    private Long getCachedLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = animalRepository.countLikesByAnimalId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), ANIMAL_EXP);
        }

        return likeNum;
    }

    private void saveSearchRecord(Long animalId, Long userId) {
        if (userId != null) {
            String key = ANIMAL_SEARCH_KEY_PREFIX + ":" + userId;
            redisService.addListElement(key, animalId.toString(), 5L);
        }
    }

    private List<Long> findLatestAnimalIds() {
        return animalRepository.findAllIds(DEFAULT_PAGE_REQUEST).getContent();
    }

    private List<Long> fetchRecommendedAnimalIds(Long userId) {
        Map<String, Long> requestBody = Map.of("user_id", userId);
        return webClient.post()
                .uri(animalRecommendURI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(AnimalResponse.RecommendationDTO.class)
                .map(AnimalResponse.RecommendationDTO::recommendedAnimals)
                .onErrorResume(e -> {
                    log.warn("FastAPI 호출 시 에러 발생: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    private List<Long> findAnimalIdsByUserLocation(Long userId) {
        List<Long> animalIds = findAnimalIdsByDistrict(userId);
        if (animalIds.size() < 5) {
            addAnimalIdsFromProvince(userId, animalIds);
        }

        return limitToMaxSize(animalIds, 5);
    }

    private List<Long> findAnimalIdsByDistrict(Long userId) {
        return userRepository.findDistrictById(userId)
                .map(district -> animalRepository.findIdsByDistrict(district, AnimalService.DEFAULT_PAGE_REQUEST))
                .orElseGet(ArrayList::new);
    }

    private void addAnimalIdsFromProvince(Long userId, List<Long> animalIds) {
        userRepository.findProvinceById(userId).ifPresent(province -> {
            List<Long> provinceAnimalIds = animalRepository.findIdsByProvince(province, AnimalService.DEFAULT_PAGE_REQUEST);
            animalIds.addAll(provinceAnimalIds);
        });
    }

    private List<Long> limitToMaxSize(List<Long> animalIds, int maxSize) {
        return animalIds.size() > maxSize ? animalIds.subList(0, maxSize) : animalIds;
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