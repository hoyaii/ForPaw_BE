package com.hong.forapw.domain.user.service;

import com.hong.forapw.admin.entity.Visit;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.UserStatus;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.user.repository.UserStatusRepository;
import com.hong.forapw.admin.repository.VisitRepository;
import com.hong.forapw.integration.rabbitmq.RabbitMqUtils;
import com.hong.forapw.integration.redis.RedisService;
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
    private final RabbitMqUtils brokerService;
    private final RedisService redisService;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
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
            User visitor = userRepository.getReferenceById(Long.parseLong(visitorId));
            Visit visit = Visit.builder()
                    .user(visitor)
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

        brokerService.bindDirectExchangeToQueue(ALARM_EXCHANGE, queueName);
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
