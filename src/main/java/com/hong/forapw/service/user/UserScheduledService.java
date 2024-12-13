package com.hong.forapw.service.user;

import com.hong.forapw.domain.authentication.Visit;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.domain.user.UserRole;
import com.hong.forapw.domain.user.UserStatus;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.repository.UserStatusRepository;
import com.hong.forapw.repository.authentication.VisitRepository;
import com.hong.forapw.service.BrokerService;
import com.hong.forapw.service.RedisService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserScheduledService {

    private final PasswordEncoder passwordEncoder;
    private final BrokerService brokerService;
    private final RedisService redisService;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final EntityManager entityManager;
    private final UserStatusRepository userStatusRepository;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPwd;

    @Value("${admin.name}")
    private String adminName;

    private static final String USER_QUEUE_PREFIX = "user.";
    private static final String ALARM_EXCHANGE = "alarm.exchange";

    // 테스트 기간에만 사용하고, 운영에는 사용 X
    public void initSuperAdmin() {
        if (!userRepository.existsByNicknameWithRemoved(adminName)) {
            User admin = User.builder()
                    .email(adminEmail)
                    .name(adminName)
                    .nickName(adminName)
                    .password(passwordEncoder.encode(adminPwd))
                    .role(UserRole.SUPER)
                    .build();
            userRepository.save(admin);

            UserStatus status = UserStatus.builder()
                    .user(admin)
                    .isActive(true)
                    .build();
            userStatusRepository.save(status);
            admin.updateStatus(status);

            setAlarmQueue(admin);
        }
    }

    @Scheduled(cron = "0 3 * * * *") // 매 시간 3분에 실행
    public void syncVisits() {
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

        redisService.removeValue(key);
    }

    // 탈퇴한지 6개월 지난 유저 데이터 삭제 (매일 자정 30분에 실행)
    @Transactional
    @Scheduled(cron = "0 30 0 * * ?")
    public void deleteExpiredUserData() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        userRepository.deleteBySoftDeletedBefore(sixMonthsAgo);
    }

    private void setAlarmQueue(User user) {
        String queueName = USER_QUEUE_PREFIX + user.getId();
        String listenerId = USER_QUEUE_PREFIX + user.getId();

        brokerService.registerDirectExQueue(ALARM_EXCHANGE, queueName);
        brokerService.registerAlarmListener(listenerId, queueName);
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
