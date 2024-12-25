package com.hong.forapw.domain.shelter;

import com.hong.forapw.domain.shelter.model.ShelterResponse;
import com.hong.forapw.domain.animal.entity.Animal;

public class ShelterMapper {

    private ShelterMapper() {
    }

    public static ShelterResponse.ShelterDTO toShelterDTO(Shelter shelter) {
        return new ShelterResponse.ShelterDTO(
                shelter.getId(),
                shelter.getName(),
                shelter.getLatitude(),
                shelter.getLongitude(),
                shelter.getCareAddr(),
                shelter.getCareTel());
    }

    public static ShelterResponse.FindShelterInfoByIdDTO toFindShelterInfoDTO(Shelter shelter) {
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

    public static ShelterResponse.AnimalDTO toAnimalDTO(Animal animal, boolean isLikedAnimal, Long likeNum) {
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
                isLikedAnimal,
                animal.getProfileURL());
    }
}
