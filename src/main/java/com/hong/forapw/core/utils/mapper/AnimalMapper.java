package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.AnimalDTO;
import com.hong.forapw.controller.dto.AnimalResponse;
import com.hong.forapw.domain.Shelter;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.animal.AnimalType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.hong.forapw.core.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;
import static com.hong.forapw.core.utils.UriUtils.convertHttpUrlToHttps;

public class AnimalMapper {

    private AnimalMapper() {
    }

    public static AnimalResponse.AnimalDTO toAnimalDTO(Animal animal, Long likeCount, List<Long> likedAnimalIds) {
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
                likeCount,
                likedAnimalIds.contains(animal.getId()),
                animal.getProfileURL());
    }

    public static AnimalResponse.FindAnimalByIdDTO toFindAnimalByIdDTO(Animal animal, boolean isLikedAnimal) {
        return new AnimalResponse.FindAnimalByIdDTO(
                animal.getId(),
                animal.getName(),
                animal.getAge(),
                animal.getGender(),
                animal.getSpecialMark(),
                animal.getRegion(),
                isLikedAnimal,
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

    public static Animal buildAnimal(AnimalDTO.ItemDTO itemDTO, String name, String kind, Shelter shelter) {
        DateTimeFormatter formatter = YEAR_HOUR_DAY_FORMAT;
        return Animal.builder()
                .id(Long.valueOf(itemDTO.desertionNo()))
                .name(name)
                .shelter(shelter)
                .happenDt(LocalDate.parse(itemDTO.happenDt(), formatter))
                .happenPlace(itemDTO.happenPlace())
                .kind(kind)
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
}
