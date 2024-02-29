package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.AnimalRequest;
import com.hong.ForPaw.controller.DTO.AnimalResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Apply.Apply;
import com.hong.ForPaw.domain.Apply.Status;
import com.hong.ForPaw.domain.Favorite;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.controller.DTO.AnimalDTO;
import com.hong.ForPaw.domain.Animal;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ApplyRepository applyRepository;
    private final EntityManager entityManager;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.animalURL}")
    private String baseUrl;

    @Value("${animal.names}")
    private String[] animalNames;

    @Transactional
    public void loadAnimalDate() {
        // shelter 돌면서 => animal 정보 쭉 떙겨온다 => shelter를 패치하고 => animal 등록
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        List<Shelter> shelters = shelterRepository.findAll();

        for(Shelter shelter : shelters){
            Long careRegNo = shelter.getCareRegNo();

            try {
                String url = baseUrl + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo + "&_type=json" + "&numOfRows=1000";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "*/*;q=0.9"); // HTTP_ERROR 방지
                HttpEntity<?> entity = new HttpEntity<>(null, headers);

                URI uri = new URI(url);

                ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
                String response = responseEntity.getBody();
                System.out.println(response);

                AnimalDTO json = mapper.readValue(response, AnimalDTO.class);
                List<AnimalDTO.ItemDTO> itemDTOS = json.response().body().items().item();
                boolean isShelterUpdate = false; // 보호소 업데이트 여부 (동물을 조회할 때 보호소 나머지 정보도 등장함)

                for (AnimalDTO.ItemDTO itemDTO : itemDTOS) {
                    if (!isShelterUpdate) {
                        shelter.updateShelterInfo(itemDTO.careTel(), itemDTO.careAddr());
                        isShelterUpdate = true;
                    }

                    Animal animal = Animal.builder()
                            .id(Long.valueOf(itemDTO.desertionNo()))
                            .shelter(shelter) // 연관관계 매핑
                            .happenDt(LocalDate.parse(itemDTO.happenDt(), formatter))
                            .happenPlace(itemDTO.happenPlace())
                            .kind(itemDTO.kindCd())
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
                            .build();

                    animalRepository.save(animal);
                }
            }
            catch (Exception e){
                System.err.println("JSON 파싱 오류가 발생했습니다. 재시도 중...: ");
                System.out.println(e);
            }
        }
    }

    @Transactional
    public AnimalResponse.AllAnimalsDTO findAllAnimals(Pageable pageable, Long userId){

        Page<Animal> animalPage = animalRepository.findAll(pageable);

        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> new AnimalResponse.AnimalDTO(animal.getId(), getAnimalName(), animal.getAge()
                        , animal.getGender(), animal.getSpecialMark(), animal.getShelter().getRegionCode().getUprName()+" "+animal.getShelter().getRegionCode().getOrgName()
                        , animal.getInquiryNum(), animal.getLikeNum(), favoriteRepository.findByUserIdAndAnimalId(userId, animal.getId()).isPresent(), animal.getProfileURL() ))
                .collect(Collectors.toList());

        return new AnimalResponse.AllAnimalsDTO(animalDTOS);
    }

    @Transactional
    public AnimalResponse.AnimalDetailDTO findAnimalById(Long animalId){

        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        return new AnimalResponse.AnimalDetailDTO(animalId, animal.getHappenPlace(), animal.getKind(), animal.getColor(),
                animal.getWeight(), animal.getNoticeSdt(), animal.getNoticeEdt(), animal.getProcessState(), animal.getNeuter());
    }

    @Transactional
    public void likeAnimal(Long userId, Long animalId){

        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        User userRef = entityManager.getReference(User.class, userId);

        Optional<Favorite> favoriteOptional = favoriteRepository.findByUserIdAndAnimalId(userId, animalId);

        // 좋아요가 이미 있다면 삭제, 없다면 추가
        if (favoriteOptional.isPresent()) {
            favoriteRepository.delete(favoriteOptional.get());
        } else {
            Favorite favorite = Favorite.builder()
                    .user(userRef)
                    .animal(animal)
                    .build();
            favoriteRepository.save(favorite);
        }
    }

    @Transactional
    public void applyAdoption(AnimalRequest.AdoptionApplyDTO requestDTO, Long userId, Long animalId){
        // 동물은 pathVariable을 통해 id를 얻는데, 잘못된 요청이 올 수 있기 때문에 DB를 조회한다.
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        User userRef = entityManager.getReference(User.class, userId);

        Apply apply = Apply.builder()
                .user(userRef)
                .animal(animal)
                .status(Status.PROCESSING)
                .name(requestDTO.name())
                .tel(requestDTO.tel())
                .residence(requestDTO.residence())
                .build();

        applyRepository.save(apply);
    }

    @Transactional
    public AnimalResponse.AllAppliesDTO findAllApply(Long userId){

        List<Apply> applies = applyRepository.findByUserId(userId);

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

        return new AnimalResponse.AllAppliesDTO(applyDTOS);
    }

    // 동물 이름 지어주는 메서드
    public String getAnimalName() {
        int index = ThreadLocalRandom.current().nextInt(animalNames.length);
        return animalNames[index];
    }
}