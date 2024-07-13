package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.AuthenticationResponse;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.UserDTO;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.Authentication.Visit;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.UserStatus;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.ApplyRepository;
import com.hong.ForPaw.repository.Authentication.VisitRepository;
import com.hong.ForPaw.repository.Post.CommentRepository;
import com.hong.ForPaw.repository.Post.PostRepository;
import com.hong.ForPaw.repository.UserRepository;
import com.hong.ForPaw.repository.UserStatusRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AnimalRepository animalRepository;
    private final ApplyRepository applyRepository;
    private final EntityManager entityManager;
    private final RedisService redisService;
    private final UserStatusRepository userStatusRepository;

    @Transactional
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

        redisService.removeData(key);
    }

    @Transactional
    public AuthenticationResponse.FindDashboardStatsDTO findDashboardStats(Long userId) {
        // 권한 체크
        checkAdminAuthority(userId);

        LocalDateTime now = LocalDateTime.now();

        // 유저 통계
        Long activeUsersNum = userRepository.countActiveUsers();
        Long inActiveUsersNum = userRepository.countInActiveUsers();

        AuthenticationResponse.UserStatsDTO userStatsDTO = new AuthenticationResponse.UserStatsDTO(
            activeUsersNum, inActiveUsersNum);

        // 유기 동물 통계
        Long waitingForAdoptionNum = animalRepository.countAnimal();
        Long adoptionProcessingNum = applyRepository.countProcessing();
        Long adoptedRecentlyNum = applyRepository.countProcessedWithinDate(now.minusWeeks(1));
        Long adoptedTotalNum = applyRepository.countProcessed();

        AuthenticationResponse.AnimalStatsDTO animalStatsDTO = new AuthenticationResponse.AnimalStatsDTO(
            waitingForAdoptionNum,
            adoptionProcessingNum,
            adoptedRecentlyNum,
            adoptedTotalNum
        );

        // 일주일 전까지의 Vist 엔티티 불러오기
        LocalDateTime nowDateOnly = now.minusHours(0).withMinute(0).withSecond(0).withNano(0);
        List<Visit> visits = visitRepository.findALlWithinDate(nowDateOnly.minusWeeks(1));

        // 일별 방문자 수 집계
        Map<LocalDate, Long> dailyVisitors = visits.stream()
            .collect(Collectors.groupingBy(
                visit -> visit.getDate().toLocalDate(),
                Collectors.counting()
            ));

        List<AuthenticationResponse.DailyVisitorDTO> dailyVisitorDTOS = dailyVisitors.entrySet()
            .stream()
            .map(entry -> new AuthenticationResponse.DailyVisitorDTO(entry.getKey(),
                entry.getValue()))
            .sorted(Comparator.comparing(AuthenticationResponse.DailyVisitorDTO::date))
            .collect(Collectors.toList());

        // 시간별 방문자 수 집계 (오늘 날짜)
        Map<LocalTime, Long> hourlyVisitors = visits.stream()
            .filter(visit -> visit.getDate().toLocalDate().isEqual(LocalDate.now()))
            .collect(Collectors.groupingBy(
                visit -> visit.getDate().toLocalTime().truncatedTo(ChronoUnit.HOURS),
                Collectors.counting()
            ));

        List<AuthenticationResponse.HourlyVisitorDTO> hourlyVisitorDTOS = hourlyVisitors.entrySet()
            .stream()
            .map(entry -> new AuthenticationResponse.HourlyVisitorDTO(
                LocalDateTime.of(LocalDate.now(), entry.getKey()), entry.getValue()))
            .sorted(Comparator.comparing(AuthenticationResponse.HourlyVisitorDTO::hour))
            .collect(Collectors.toList());

        // 오늘 발생한 이벤트 요약
        Long entryNum = userRepository.countALlWithinDate(nowDateOnly);
        Long newPostNum = postRepository.countALlWithinDate(nowDateOnly);
        Long newCommentNum = commentRepository.countALlWithinDate(nowDateOnly);
        Long newAdoptApplicationNum = applyRepository.countProcessingWithinDate(nowDateOnly);

        AuthenticationResponse.DailySummaryDTO dailySummaryDTO = new AuthenticationResponse.DailySummaryDTO(
            entryNum,
            newPostNum,
            newCommentNum,
            newAdoptApplicationNum
        );

        return new AuthenticationResponse.FindDashboardStatsDTO(userStatsDTO, animalStatsDTO,
            dailyVisitorDTOS, hourlyVisitorDTOS, dailySummaryDTO);
    }

    @Transactional
    public AuthenticationResponse.findUserList findUserList(Long Id, UserRole Role, int page) {
        PageRequest pageRequest = PageRequest.of(page, 5);

        checkAdminAuthority(Id);

        userRepository.findById(Id).orElseThrow(
            () -> new CustomException(ExceptionCode.ADMIN_NOT_FOUND)
        );

        List<UserDTO> userDTOS = null;

        if (Role.equals(UserRole.SUPER)){
            Page<UserStatus> userStatusPage = userStatusRepository.findBySuperRole(pageRequest);
            userDTOS = userStatusPage.getContent().stream()
                .map(userStatus -> new UserDTO(
                    userStatus.getUser().getId(),
                    userStatus.getUser().getNickName(),
                    userStatus.getUser().getEmail(),
                    userStatus.getUser().getCreatedDate(),
                    userStatus.getVisit().getDate(),
                    applyRepository.countByUserIdProcessed(userStatus.getUser().getId()),
                    applyRepository.countByUserIdProcessing(userStatus.getUser().getId()),
                    userStatus.getUser().getRole(),
                    userStatus.isActive(),
                    userStatus.getSuspensionStart(),
                    userStatus.getSuspensionDays(),
                    userStatus.getSuspensionReason())
                ).toList();
        }
        if (Role.equals(UserRole.ADMIN)){
            Page<UserStatus> userStatusPage = userStatusRepository.findByAdminRole(pageRequest);
            userDTOS = userStatusPage.getContent().stream()
                .map(userStatus -> new UserDTO(
                    userStatus.getUser().getId(),
                    userStatus.getUser().getNickName(),
                    userStatus.getUser().getEmail(),
                    userStatus.getUser().getCreatedDate(),
                    userStatus.getVisit().getDate(),
                    applyRepository.countByUserIdProcessed(userStatus.getUser().getId()),
                    applyRepository.countByUserIdProcessing(userStatus.getUser().getId()),
                    userStatus.getUser().getRole(),
                    userStatus.isActive(),
                    userStatus.getSuspensionStart(),
                    userStatus.getSuspensionDays(),
                    userStatus.getSuspensionReason())
                ).toList();
        }


        return new AuthenticationResponse.findUserList(userDTOS);
    }

    @Transactional
    public AuthenticationResponse.UserRoleDTO changeUserRole(Long id,
        AuthenticationResponse.UserRoleDTO userRoleDTO){
        checkAdminAuthority(id);
        User user = userRepository.findById(userRoleDTO.userId()).orElseThrow(
            () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
        user.updateRole(userRoleDTO.role());
        return userRoleDTO;
    }

    @Transactional
    public void BanUser(Long id, AuthenticationResponse.UserBanDTO userBanDTO){
        checkAdminAuthority(id);

        userRepository.findById(userBanDTO.userId()).orElseThrow(
            () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
        UserStatus byUserIdone = userStatusRepository.findByUserIdOne(userBanDTO.userId());
        byUserIdone.UpdateisActive(false);
        byUserIdone.UpdatesuspensionStart(LocalDateTime.now());
        byUserIdone.UpdatesuspensionDays(userBanDTO.duration());
        byUserIdone.UpdatesuspensionReason(userBanDTO.reason());
    }

    @Transactional
    public void unSuspend(Long id, Long userId){
        checkAdminAuthority(id);

        userRepository.findById(userId).orElseThrow(
            () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
        UserStatus byUserIdone = userStatusRepository.findByUserIdOne(userId);
        byUserIdone.UpdateisActive(true);
        byUserIdone.UpdatesuspensionStart(null);
        byUserIdone.UpdatesuspensionDays(null);
        byUserIdone.UpdatesuspensionReason(null);

    }
    @Transactional
    public void deleteUser(Long id, Long userId){
        checkSuperAuthority(id);

        userRepository.findById(userId).orElseThrow(
            () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
        userRepository.deleteByUserId(userId);
    }

    @Transactional
    public AuthenticationResponse.ApplyDTO getApplyList(Long id, ApplyStatus applyStatus, int page){
        checkAdminAuthority(id);


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

    private void checkAdminAuthority(Long userId) {
        UserRole role = userRepository.findRoleById(userId).orElseThrow(
            () -> new CustomException(ExceptionCode.USER_FORBIDDEN)
        );

        if (!role.equals(UserRole.ADMIN) && !role.equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkSuperAuthority(Long userId) {
        UserRole role = userRepository.findRoleById(userId).orElseThrow(
            () -> new CustomException(ExceptionCode.USER_FORBIDDEN)
        );

        if (!role.equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }
}

