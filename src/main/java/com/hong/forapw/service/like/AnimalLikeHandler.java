package com.hong.forapw.service.like;

import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.animal.FavoriteAnimal;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.repository.animal.AnimalRepository;
import com.hong.forapw.repository.animal.FavoriteAnimalRepository;
import com.hong.forapw.service.AnimalService;
import com.hong.forapw.service.RedisService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AnimalLikeHandler implements LikeHandler {

    private final RedisService redisService;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;

    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animal:like:count";
    private static final String ANIMAL_LIKED_SET_KEY_PREFIX = "user:%s:liked_animals";
    private static final long ANIMAL_CACHE_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 90; // 90 days

    @Override
    public void validateBeforeLike(Long animalId, Long userId) {
        if (!animalRepository.existsById(animalId)) {
            throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
        }
    }

    @Override
    public boolean isAlreadyLiked(Long animalId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), animalId.toString());
    }

    @Override
    public void addLike(Long animalId, Long userId) {
        Animal animal = animalRepository.getReferenceById(animalId);
        User userRef = userRepository.getReferenceById(userId);

        FavoriteAnimal favoriteAnimal = FavoriteAnimal.builder()
                .user(userRef)
                .animal(animal)
                .build();
        favoriteAnimalRepository.save(favoriteAnimal);

        redisService.addSetElement(buildUserLikedSetKey(userId), animalId);
        redisService.incrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), 1L);
    }

    @Override
    public void removeLike(Long animalId, Long userId) {
        Optional<FavoriteAnimal> favoriteAnimalOP = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animalId);
        favoriteAnimalOP.ifPresent(favoriteAnimalRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), animalId.toString());
        redisService.decrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long animalId) {
        Long likeNum = redisService.getValueInLongWithNull(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString());

        if (likeNum == null) {
            likeNum = animalRepository.countLikesByAnimalId(animalId);
            redisService.storeValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), likeNum.toString(), ANIMAL_CACHE_EXPIRATION_MS);
        }

        return likeNum;
    }

    @Override
    public String buildLockKey(Long animalId) {
        return "animal:" + animalId + ":like:lock";
    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(ANIMAL_LIKED_SET_KEY_PREFIX, userId);
    }
}
