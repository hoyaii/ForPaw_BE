package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.*;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Animal.AnimalType;
import com.hong.ForPaw.domain.Apply.Apply;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.Animal.FavoriteAnimal;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.Animal.Animal;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.*;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.Animal.FavoriteAnimalRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final UserRepository userRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final ApplyRepository applyRepository;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;
    private final WebClient webClient;

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

    @Value("${google.map.geocoding.uri}")
    private String googleGeoCodingURI;

    @Value("${google.api.key}")
    private String googleAPIKey;

    @Value("${recommend.uri}")
    private String animalRecommendURI;

    @Transactional
    @Scheduled(cron = "0 0 0,12 * * *") // 매일 자정과 정오에 실행
    public void loadAnimalData() {
        List<Shelter> shelters = shelterRepository.findAllWithRegionCode();

        Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(50)) // 각 요청 사이에 0.05초 지연
                .flatMap(shelter -> {
                    Long careRegNo = shelter.getId();
                    URI uri = buildAnimalURI(serviceKey, careRegNo);

                    return webClient.get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class)
                            .retry(3)
                            .flatMapMany(response -> processAnimalData(response, shelter))
                            .collectList()
                            .doOnNext(animalRepository::saveAll);
                })
                .then()
                .doOnTerminate(this::updateAddressByGoogle) // loadAnimalData가 완료되면 updateAddressByGoogle 실행
                .subscribe();
    }

    // 보호소 정보 업데이트 후 이어서 위치 업데이트 진행
    @Transactional
    public void updateAddressByGoogle(){
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);

        Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(50))
                .flatMap(shelter -> {
                    URI uri = buildGoogleGeocodingURI(shelter.getCareAddr());
                    return webClient.get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(GoogleMapDTO.MapDTO.class)
                            .flatMap(mapDTO -> Mono.justOrEmpty(mapDTO.results().stream().findFirst()))
                            .doOnNext(resultDTO -> {
                                shelterRepository.updateAddressInfo(resultDTO.geometry().location().lat(), resultDTO.geometry().location().lng(), shelter.getId());
                            });
                })
                .subscribe();
    }

    @Transactional
    public void updateAddressByKakao(){
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

    @Transactional
    public AnimalResponse.FindAnimalListDTO findAnimalList(Integer page, String sort, Long userId){
        // sort 파라미터를 AnimalType으로 변환
        AnimalType animalType = getAnimalType(sort);

        Pageable pageable =createPageable(page, 5, "createdDate");
        Page<Animal> animalPage = animalRepository.findAllByCategory(animalType, pageable);

        if(animalPage.isEmpty()){
            throw new CustomException(ExceptionCode.ANIMAL_NOT_EXIST);
        }

        // 사용자가 '좋아요' 표시한 Animal의 ID 목록 => 만약 로그인 되어 있지 않다면, 빈 리스트로 처리한다.
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findLikedAnimalIdsByUserId(userId) : new ArrayList<>();

        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> {
                    Long likeNum = redisService.getDataInLong("animalLikeNum", animal.getId().toString());

                    return new AnimalResponse.AnimalDTO(
                        animal.getId(),
                        animal.getName(),
                        animal.getAge(),
                        animal.getGender(),
                        animal.getSpecialMark(),
                        animal.getRegion(),
                        animal.getInquiryNum(),
                        likeNum,
                        likedAnimalIds.contains(animal.getId()),
                        animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new AnimalResponse.FindAnimalListDTO(animalDTOS);
    }

    @Transactional
    public AnimalResponse.FindAnimalListDTO findRecommendedAnimalList(Long userId){
        Map<String, Long> jsonBody = Map.of("user_id", userId);

        List<Long> recommendedAnimalIds = webClient.post()
                .uri(animalRecommendURI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .bodyToMono(AnimalResponse.RecommendationDTO.class)
                .map(AnimalResponse.RecommendationDTO::recommendedAnimals)
                .block();

        // 조회 기록이 없어서, 추천하는 ID 목록이 없으면 사용자 위치 기반으로 추천
        if (recommendedAnimalIds.isEmpty()) {
            recommendedAnimalIds = findAnimalIdsByUserLocation(userId);
        }

        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findLikedAnimalIdsByUserId(userId) : new ArrayList<>();

        List<AnimalResponse.AnimalDTO> animalDTOS =animalRepository.findAllByIdList(recommendedAnimalIds).stream()
                .map(animal -> {
                    Long likeNum = redisService.getDataInLong("animalLikeNum", animal.getId().toString());

                    return new AnimalResponse.AnimalDTO(
                            animal.getId(),
                            animal.getName(),
                            animal.getAge(),
                            animal.getGender(),
                            animal.getSpecialMark(),
                            animal.getRegion(),
                            animal.getInquiryNum(),
                            likeNum,
                            likedAnimalIds.contains(animal.getId()),
                            animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new AnimalResponse.FindAnimalListDTO(animalDTOS);
    }

    @Transactional
    public AnimalResponse.FindLikeAnimalListDTO findLikeAnimalList(Integer page, Long userId){
        Pageable pageable =createPageable(page, 5, "id");
        Page<Animal> animalPage = favoriteAnimalRepository.findAnimalByUserId(userId, pageable);

        if(animalPage.isEmpty()){
            throw new CustomException(ExceptionCode.ANIMAL_NOT_EXIST);
        }

        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> {
                    Long likeNum = redisService.getDataInLong("animalLikeNum", animal.getId().toString());

                    return new AnimalResponse.AnimalDTO(
                        animal.getId(),
                        animal.getName(),
                        animal.getAge(),
                        animal.getGender(),
                        animal.getSpecialMark(),
                        animal.getRegion(),
                        animal.getInquiryNum(),
                        likeNum,
                        true,
                        animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new AnimalResponse.FindLikeAnimalListDTO(animalDTOS);
    }

    @Transactional
    public AnimalResponse.FindAnimalByIdDTO findAnimalById(Long animalId, Long userId){
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        boolean isLike = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animal.getId()).isPresent();

        // 추천을 위해 검색한 동물의 id 저장 (5개까지만 저장된다)
        String key = "animalSearch:" + userId;
        redisService.addListElementWithLimit(key, animalId.toString(), 5l);

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
                animal.getNeuter());
    }

    @Transactional
    public void likeAnimal(Long userId, Long animalId){
        // 존재하지 않는 동물이면 에러
        if(!animalRepository.existsById(animalId)){
                throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
            }

        Optional<FavoriteAnimal> favoriteAnimalOP = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animalId);

        // 좋아요가 이미 있다면 삭제, 없다면 추가
        if (favoriteAnimalOP.isPresent()) {
            favoriteAnimalRepository.delete(favoriteAnimalOP.get());
            redisService.decrementCnt("animalLikeNum", animalId.toString(), 1L);
        }
        else {
            Animal animalRef = entityManager.getReference(Animal.class, animalId);
            User userRef = entityManager.getReference(User.class, userId);

            FavoriteAnimal favoriteAnimal = FavoriteAnimal.builder()
                    .user(userRef)
                    .animal(animalRef)
                    .build();

            favoriteAnimalRepository.save(favoriteAnimal);
            redisService.incrementCnt("animalLikeNum", animalId.toString(), 1L);
        }
    }

    @Scheduled(cron = "0 20 0 * * *")
    @Transactional
    public void syncLikes() {
        List<Long> animalIds = animalRepository.findAllIds();

        for (Long postId : animalIds) {
            Long likeNum = redisService.getDataInLong("animalLikeNum", postId.toString());

            if (likeNum != null) {
                animalRepository.updateLikeNum(likeNum, postId);
            }
        }
    }

    @Transactional
    public AnimalResponse.CreateApplyDTO applyAdoption(AnimalRequest.ApplyAdoptionDTO requestDTO, Long userId, Long animalId){
        // 동물이 존재하지 않으면 에러
        if(!animalRepository.existsById(animalId)){
            throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
        }
        // 이미 지원하였으면 에러
        if(applyRepository.existsByUserIdAndAnimalId(userId, animalId)){
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_APPLY);
        }

        Animal animalRef = entityManager.getReference(Animal.class, animalId);
        User userRef = entityManager.getReference(User.class, userId);

        Apply apply = Apply.builder()
                .user(userRef)
                .animal(animalRef)
                .status(ApplyStatus.PROCESSING)
                .name(requestDTO.name())
                .tel(requestDTO.tel())
                .residence(requestDTO.residence())
                .build();

        applyRepository.save(apply);

        // 동물의 문의 횟수 증가
        animalRepository.incrementInquiryNumById(animalId);

        return new AnimalResponse.CreateApplyDTO(apply.getId());
    }

    @Transactional
    public AnimalResponse.FindApplyListDTO findApplyList(Long userId){
        List<Apply> applies = applyRepository.findAllByUserId(userId);

        // 지원서가 존재하지 않음
        if(applies.isEmpty()){
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }

        List<AnimalResponse.ApplyDTO> applyDTOS = applies.stream()
                .map(apply -> new AnimalResponse.ApplyDTO(
                        apply.getId(),
                        apply.getAnimal().getName(),
                        apply.getAnimal().getKind(),
                        apply.getAnimal().getGender(),
                        apply.getAnimal().getAge(),
                        apply.getName(),
                        apply.getTel(),
                        apply.getResidence(),
                        apply.getStatus()))
                .collect(Collectors.toList());

        return new AnimalResponse.FindApplyListDTO(applyDTOS);
    }

    @Transactional
    public void updateApply(AnimalRequest.UpdateApplyDTO requestDTO, Long applyId, Long userId){
        // 지원하지 않았거나, 권한이 없으면 에러
        if(!applyRepository.existsByApplyIdAndUserId(applyId, userId)){
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }

        Apply apply = applyRepository.findById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.APPLY_NOT_FOUND)
        );

        apply.updateApply(requestDTO.name(), requestDTO.tel(), requestDTO.residence());
    }

    @Transactional
    public void deleteApply(Long applyId, Long userId){
        // 지원하지 않았거나, 권한이 없으면 에러
        if(!applyRepository.existsByApplyIdAndUserId(applyId, userId)){
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }

        // 동물의 문의 횟수 감소
        Long animalId = applyRepository.findAnimalIdById(applyId);
        animalRepository.decrementInquiryNumById(animalId);

        applyRepository.deleteById(applyId);
    }

    // 공가가 종료된 것은 주기적으로 삭제
    @Transactional
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 실행
    public void deleteEndAdoptAnimal(){
        LocalDate now = LocalDateTime.now().toLocalDate();
        List<Animal> animals = animalRepository.findAllOutOfDate(now);

        // 캐싱한 '좋아요 수' 삭제
        animals.forEach(
                animal -> redisService.removeData("animalLikeNum", animal.getId().toString())
        );

        animalRepository.deleteAll(animals);
    }

    private Flux<Animal> processAnimalData(String response, Shelter shelter) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        try {
            AnimalDTO json = mapper.readValue(response, AnimalDTO.class);

            // 동물 데이터를 바탕으로 보호소 정보 업데이트
            updateShelterByAnimalData(json, shelter);

            // 동물 데이터를 바탕으로 동물 엔티티 생성
            return Flux.fromIterable(Optional.ofNullable(json.response().body().items())
                            .map(AnimalDTO.ItemsDTO::item)
                            .orElse(Collections.emptyList()))
                    .filter(itemDTO -> LocalDate.parse(itemDTO.noticeEdt(), formatter).isAfter(LocalDate.now())) // 공고 종료가 된 것은 필터링
                    .map(itemDTO -> createAnimal(itemDTO, shelter, formatter));
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private void updateShelterByAnimalData(AnimalDTO json, Shelter shelter) {
        Optional.ofNullable(json)
                .map(j -> j.response().body().items().item())
                .filter(items -> !items.isEmpty())
                .map(items -> items.get(0))
                .ifPresent(itemDTO -> shelterRepository.updateShelterInfo(itemDTO.careTel(), itemDTO.careAddr(), Long.valueOf(json.response().body().totalCount()), shelter.getId()));
    }

    private Animal createAnimal(AnimalDTO.ItemDTO itemDTO, Shelter shelter, DateTimeFormatter formatter) {
        return Animal.builder()
                .id(Long.valueOf(itemDTO.desertionNo()))
                .name(createAnimalName())
                .shelter(shelter)
                .happenDt(LocalDate.parse(itemDTO.happenDt(), formatter))
                .happenPlace(itemDTO.happenPlace())
                .kind(itemDTO.kindCd())
                .category(parseAnimalType(itemDTO.kindCd()))
                .color(itemDTO.colorCd())
                .age(itemDTO.age())
                .weight(itemDTO.weight())
                .noticeSdt(LocalDate.parse(itemDTO.noticeSdt(), formatter))
                .noticeEdt(LocalDate.parse(itemDTO.noticeEdt(), formatter))
                .profileURL(itemDTO.popfile())
                .processState(itemDTO.processState())
                .gender(itemDTO.sexCd())
                .neuter(itemDTO.neuterYn())
                .specialMark(itemDTO.specialMark())
                .region(shelter.getRegionCode().getUprName() + " " + shelter.getRegionCode().getOrgName())
                .build();
    }

    public AnimalType parseAnimalType(String input) {
        if (input.startsWith("[개]")) {
            return AnimalType.dog;
        } else if (input.startsWith("[고양이]")) {
            return AnimalType.cat;
        } else {
            return AnimalType.other;
        }
    }

    // 동물 이름 지어주는 메서드
    public String createAnimalName() {
        int index = ThreadLocalRandom.current().nextInt(animalNames.length);
        return animalNames[index];
    }

    private URI buildAnimalURI(String serviceKey, Long careRegNo) {
        String url = animalURI + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo + "&_type=json" + "&numOfRows=1000";

        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private URI buildKakaoGeocodingURI(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(kakaoGeoCodingURI);
        uriBuilder.queryParam("query", address);

        URI uri = uriBuilder.build().encode().toUri();

        return uri;
    }

    private URI buildGoogleGeocodingURI(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(googleGeoCodingURI);
        uriBuilder.queryParam("address", address);
        uriBuilder.queryParam("key", googleAPIKey);

        URI uri = uriBuilder.build().encode().toUri();

        return uri;
    }

    private Pageable createPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    private List<Long> findAnimalIdsByUserLocation(Long userId) {
        // 5개 반환
        PageRequest pageRequest = PageRequest.of(0, 5);

        // 로그인 되지 않았으면, 그냥 시간순으로 반환
        if (userId == null) {
            return animalRepository.findAllIds(pageRequest).getContent();
        }

        // 사용자 district를 바탕으로 조회
        List<Long> animalIds = userRepository.findDistrictById(userId)
                .map(district -> animalRepository.findAnimalIdsByDistrict(district, pageRequest))
                .orElseGet(ArrayList::new);

        // 조회된 동물 ID의 수가 5개 미만인 경우, province 범위까지 확대해서 추가 조회
        if (animalIds.size() < 5) {
            animalIds.addAll(userRepository.findProvinceById(userId)
                    .map(province -> animalRepository.findAnimalIdsByProvince(province, pageRequest))
                    .orElseGet(ArrayList::new));

            return animalIds.subList(0, Math.min(5, animalIds.size()));
        }

        return animalIds;
    }

    // sort가 date, dog, cat, other이 아니면 에러 발생
    private AnimalType getAnimalType(String sort) {
        if(sort.equals("date")) {
            return null;
        }

        return Optional.ofNullable(Map.of(
                        "dog", AnimalType.dog,
                        "cat", AnimalType.cat,
                        "other", AnimalType.other).get(sort))
                .orElseThrow(() -> new CustomException(ExceptionCode.BAD_APPROACH));
    }
}