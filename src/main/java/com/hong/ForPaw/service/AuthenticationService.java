package com.hong.ForPaw.service;

import com.hong.ForPaw.domain.Authentication.Visit;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.Authentication.VisitRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

    private final VisitRepository visitRepository;
    private final EntityManager entityManager;
    private final RedisService redisService;

    @Transactional
    @Scheduled(cron = "0 3 * * * *") // 매 시간 3분에 실행
    public void syncVisits(){
        String key = getPreviousHourKey();
        LocalDateTime visitTime = parseDateTimeFromKey(key);

        Set<String> visitSet = redisService.getMembersOfSet(key);
        visitSet.forEach(visitorId -> {
            User userRef = entityManager.getReference(User.class, visitorId);
            Visit visit = Visit.builder()
                    .user(userRef)
                    .date(visitTime)
                    .build();

            visitRepository.save(visit);
        });

        redisService.removeData(key);
    }

    private String getPreviousHourKey() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
        return "visit:" + oneHourAgo.format(formatter);
    }

    private LocalDateTime parseDateTimeFromKey(String key) {
        int lastColon = key.lastIndexOf(':');
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
        return LocalDateTime.parse(key.substring(lastColon + 1), formatter); // 마지막 ':' 이후부터 문자열을 파싱
    }
}

