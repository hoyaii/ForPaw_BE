package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.AuthenticationRequest;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.ApplyDTO;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Animal.Animal;
import com.hong.ForPaw.domain.Apply.Apply;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.Authentication.Visit;
import com.hong.ForPaw.domain.Inquiry.Inquiry;
import com.hong.ForPaw.domain.Inquiry.InquiryAnswer;
import com.hong.ForPaw.domain.Inquiry.InquiryStatus;
import com.hong.ForPaw.domain.Post.Comment;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Report.ContentType;
import com.hong.ForPaw.domain.Report.Report;
import com.hong.ForPaw.domain.Report.ReportStatus;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.UserStatus;
import com.hong.ForPaw.repository.*;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.Authentication.VisitRepository;
import com.hong.ForPaw.repository.Inquiry.InquiryAnswerRepository;
import com.hong.ForPaw.repository.Inquiry.InquiryRepository;
import com.hong.ForPaw.repository.Post.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ReportRepository reportRepository;
    private final ApplyRepository applyRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final EntityManager entityManager;
    private final RedisService redisService;
    private final UserStatusRepository userStatusRepository;
    private final FaqRepository faqRepository;

    private static final String POST_SCREENED = "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다.";
    private static final String COMMENT_SCREENED = "커뮤니티 규정을 위반하여 가려진 댓글입니다.";

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

    @Transactional(readOnly = true)
    public AuthenticationResponse.FindDashboardStatsDTO findDashboardStats(Long userId) {
        // 권한 체크
        checkAdminAuthority(userId);

        LocalDateTime now = LocalDateTime.now();

        // 유저 통계
        Long activeUsersNum = userRepository.countActiveUsers();
        Long inActiveUsersNum = userRepository.countInActiveUsers();

        AuthenticationResponse.UserStatsDTO userStatsDTO = new AuthenticationResponse.UserStatsDTO(activeUsersNum, inActiveUsersNum);

        // 유기 동물 통계
        Long waitingForAdoptionNum = animalRepository.countAnimal();
        Long adoptionProcessingNum = applyRepository.countByStatus(ApplyStatus.PROCESSING);
        Long adoptedRecentlyNum = applyRepository.countByStatusWithinDate(ApplyStatus.PROCESSED, now.minusWeeks(1));
        Long adoptedTotalNum = applyRepository.countByStatus(ApplyStatus.PROCESSED);

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

        List<AuthenticationResponse.DailyVisitorDTO> dailyVisitorDTOS = dailyVisitors.entrySet().stream()
                .map(entry -> new AuthenticationResponse.DailyVisitorDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AuthenticationResponse.DailyVisitorDTO::date))
                .collect(Collectors.toList());

        // 시간별 방문자 수 집계 (오늘 날짜)
        Map<LocalTime, Long> hourlyVisitors = visits.stream()
            .filter(visit -> visit.getDate().toLocalDate().isEqual(LocalDate.now()))
            .collect(Collectors.groupingBy(
                visit -> visit.getDate().toLocalTime().truncatedTo(ChronoUnit.HOURS),
                Collectors.counting()
            ));

        List<AuthenticationResponse.HourlyVisitorDTO> hourlyVisitorDTOS = hourlyVisitors.entrySet().stream()
                .map(entry -> new AuthenticationResponse.HourlyVisitorDTO(LocalDateTime.of(LocalDate.now(), entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(AuthenticationResponse.HourlyVisitorDTO::hour))
                .collect(Collectors.toList());

        // 오늘 발생한 이벤트 요약
        Long entryNum = userRepository.countALlWithinDate(nowDateOnly);
        Long newPostNum = postRepository.countALlWithinDate(nowDateOnly);
        Long newCommentNum = commentRepository.countALlWithinDate(nowDateOnly);
        Long newAdoptApplicationNum = applyRepository.countByStatusWithinDate(ApplyStatus.PROCESSING, nowDateOnly);

        AuthenticationResponse.DailySummaryDTO dailySummaryDTO = new AuthenticationResponse.DailySummaryDTO(
                entryNum,
                newPostNum,
                newCommentNum,
                newAdoptApplicationNum
        );

        return new AuthenticationResponse.FindDashboardStatsDTO(userStatsDTO, animalStatsDTO, dailyVisitorDTOS, hourlyVisitorDTOS, dailySummaryDTO);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse.FindUserListDTO findUserList(Long adminId, Pageable pageable){
        checkAdminAuthority(adminId);

        // <userId, 가장 최근의 Visit 객체> 맵
        List<Visit> visits = visitRepository.findAll();
        Map<Long, Visit> latestVisitMap = visits.stream()
                .collect(Collectors.toMap(
                        visit -> visit.getUser().getId(),
                        visit -> visit,
                        (visit1, visit2) -> visit1.getDate().isAfter(visit2.getDate()) ? visit1 : visit2
                ));

        // 진행중인 지원서
        List<Apply> processingApplies = applyRepository.findAllProcessing();
        Map<Long, Long> processingApplyMap = processingApplies.stream()
                .collect(Collectors.groupingBy(
                        apply -> apply.getUser().getId(),
                        Collectors.counting()
                ));

        // 처리 완료된 지원서
        List<Apply> processedApplies = applyRepository.findAllProcessing();
        Map<Long, Long> processedApplyMap = processedApplies.stream()
                .collect(Collectors.groupingBy(
                        apply -> apply.getUser().getId(),
                        Collectors.counting()
                ));

        Page<User> users = userRepository.findAll(pageable);

        List<AuthenticationResponse.ApplicantDTO> applicantDTOS = users.getContent().stream()
                .map(user -> new AuthenticationResponse.ApplicantDTO(
                        user.getId(),
                        user.getNickName(),
                        user.getCreatedDate(),
                        Optional.ofNullable(latestVisitMap.get(user.getId()))
                                .map(Visit::getDate)
                                .orElse(null),
                        processingApplyMap.get(user.getId()),
                        processedApplyMap.get(user.getId()),
                        user.getRole(),
                        user.getStatus().isActive(),
                        user.getStatus().getSuspensionStart(),
                        user.getStatus().getSuspensionDays(),
                        user.getStatus().getSuspensionReason()
                ))
                .toList();

        return new AuthenticationResponse.FindUserListDTO(applicantDTOS);
    }

    @Transactional
    public void changeUserRole(AuthenticationRequest.ChangeUserRoleDTO requestDTO, Long adminId, UserRole adminRole){
        checkAdminAuthority(adminId);

        User user = userRepository.findById(requestDTO.userId()).orElseThrow(
            () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 현재 유저의 Role과 동일한 값이 요청으로 들어옴
        if(user.getRole().equals(user.getRole())){
            throw new CustomException(ExceptionCode.SAME_STATUS);
        }

        // Supser로의 권한 변경은 불가능
        if(requestDTO.role().equals(UserRole.SUPER)){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }

        // Admin은 Super를 건들일 수 없음
        checkAdminPrivileges(adminRole, user.getRole());
        user.updateRole(requestDTO.role());
    }

    @Transactional
    public void suspendUser(AuthenticationRequest.SuspendUserDTO requestDTO, Long adminId, UserRole adminRole){
        checkAdminAuthority(adminId);

        UserStatus userStatus = userStatusRepository.findByUserId(requestDTO.userId()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 이미 정지되어 있음
        if(!userStatus.isActive()){
            throw new CustomException(ExceptionCode.USER_ALREADY_SUSPENDED);
        }

        checkAdminPrivileges(adminRole, userStatus.getUser().getRole());
        userStatus.updateForSuspend(LocalDateTime.now(), requestDTO.suspensionDays(), requestDTO.suspensionReason());
    }

    @Transactional
    public void unSuspendUser(Long userId, Long adminId, UserRole adminRole){
        checkAdminAuthority(adminId);

        UserStatus userStatus = userStatusRepository.findByUserId(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 이미 활성화된 상태
        if(userStatus.isActive()){
            throw new CustomException(ExceptionCode.USER_ALREADY_SUSPENDED);
        }

        checkAdminPrivileges(adminRole, userStatus.getUser().getRole());
        userStatus.updateForUnSuspend();
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse.FindApplyListDTO findApplyList(Long adminId, ApplyStatus status, Pageable pageable){
        checkAdminAuthority(adminId);

        Page<Apply> applyPage = applyRepository.findAllByStatusWithAnimal(status, pageable);

        // Apply의 animalId를 리스트로 만듦 => shleter와 fetch 조인해서 Animal 객체를 조회 => <animalId, Anmal 객체> 맵 생성
        List<Long> animalIds = applyPage.getContent().stream()
                .map(apply -> apply.getAnimal().getId())
                .collect(Collectors.toList());

        List<Animal> animals = animalRepository.findByIdsWithShelter(animalIds);
        Map<Long, Animal> animalMap = animals.stream()
                .collect(Collectors.toMap(Animal::getId, animal -> animal));

        List<ApplyDTO> applyDTOS = applyPage.getContent().stream()
                .map(apply -> {
                    Animal animal = animalMap.get(apply.getAnimal().getId());
                    return new ApplyDTO(
                            apply.getId(),
                            apply.getCreatedDate(),
                            animal.getId(),
                            animal.getKind(),
                            animal.getGender(),
                            animal.getAge(),
                            apply.getName(),
                            apply.getTel(),
                            apply.getResidence(),
                            animal.getShelter().getName(),
                            animal.getShelter().getCareTel(),
                            apply.getStatus()
                    );
                }).toList();

        return new AuthenticationResponse.FindApplyListDTO(applyDTOS);
    }

    @Transactional
    public void changeApplyStatus(AuthenticationRequest.ChangeApplyStatusDTO requestDTO, Long adminId){
        checkAdminAuthority(adminId);

        Apply apply = applyRepository.findById(requestDTO.id()).orElseThrow(
            () -> new CustomException(ExceptionCode.APPLY_NOT_FOUND)
        );

        // 현재 상태와 동일한 값이 요청으로 들어옴
        if(requestDTO.status().equals(apply.getStatus())){
            throw new CustomException(ExceptionCode.SAME_STATUS);
        }

        apply.updateApplyStatus(requestDTO.status());
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse.FindReportListDTO findReportList(Long adminId, ReportStatus status, Pageable pageable){
        checkAdminAuthority(adminId);

        List<AuthenticationResponse.ReportDTO> reportDTOS = reportRepository.findAllByStatus(status, pageable).getContent().stream()
                .map(report -> new AuthenticationResponse.ReportDTO(
                        report.getId(),
                        report.getCreatedDate(),
                        report.getContentType(),
                        report.getContentId(),
                        report.getType(),
                        report.getReason(),
                        report.getReporter().getNickName(),
                        report.getOffender().getId(),
                        report.getOffender().getNickName(),
                        report.getStatus())
                ).toList();

        return new AuthenticationResponse.FindReportListDTO(reportDTOS);
    }

    @Transactional
    public void processReport(AuthenticationRequest.ProcessReportDTO requestDTO, Long adminId){
        checkAdminAuthority(adminId);

        Report report = reportRepository.findById(requestDTO.id()).orElseThrow(
                () -> new CustomException(ExceptionCode.REPORT_NOT_FOUND)
        );

        // 유저 정지 처리
        if(requestDTO.hasSuspension()){
            // SUPER를 정지 시킬 수는 없다 (악용 방지)
            if(report.getOffender().getRole().equals(UserRole.SUPER)){
                throw new CustomException(ExceptionCode.REPORT_NOT_APPLY_TO_SUPER);
            }

            report.getOffender().getStatus()
                    .updateForSuspend(LocalDateTime.now(), requestDTO.suspensionDays(), report.getReason());
        }

        // 가림 처리
        if(requestDTO.hasBlocking()){
            processBlocking(report);
        }

        // 신고 내역 완료 처리
        report.updateStatus(ReportStatus.PROCESSED);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse.FindSupportListDTO findSupportList(Long adminId, InquiryStatus status, Pageable pageable){
        checkAdminAuthority(adminId);

        List<AuthenticationResponse.InquiryDTO> inquiryDTOS = inquiryRepository.findByStatusWithUser(status, pageable).getContent().stream()
                .map(inquiry -> new AuthenticationResponse.InquiryDTO(
                        inquiry.getId(),
                        inquiry.getCreatedDate(),
                        inquiry.getQuestioner().getNickName(),
                        inquiry.getType(),
                        inquiry.getTitle(),
                        inquiry.getStatus())
                )
                .toList();

        return new AuthenticationResponse.FindSupportListDTO(inquiryDTOS);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse.FindSupportByIdDTO findSupportById(Long adminId, Long inquiryId){
        checkAdminAuthority(adminId);

        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        AuthenticationResponse.FindSupportByIdDTO findSupportByIdDTO = new AuthenticationResponse.FindSupportByIdDTO(
                inquiry.getId(),
                inquiry.getQuestioner().getNickName(),
                inquiry.getTitle(),
                inquiry.getDescription()
        );

        return findSupportByIdDTO;
    }

    @Transactional
    public AuthenticationResponse.AnswerInquiryDTO answerInquiry(AuthenticationRequest.AnswerInquiryDTO requestDTO, Long adminId, Long inquiryId){
        checkAdminAuthority(adminId);

        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        User adminRef = entityManager.getReference(User.class, adminId);
        InquiryAnswer answer = InquiryAnswer.builder()
                .answerer(adminRef)
                .inquiry(inquiry)
                .content(requestDTO.content())
                .build();

        inquiryAnswerRepository.save(answer);

        return new AuthenticationResponse.AnswerInquiryDTO(inquiryId);
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

    public void checkAdminAuthority(Long userId) {
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

    private void checkAdminPrivileges(UserRole adminRole, UserRole userRole) {
        // ADMIN 권한의 관리자가 SUPER 권한의 유저를 변경 방지
        if(adminRole.equals(UserRole.ADMIN) && userRole.equals(UserRole.SUPER)){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void processBlocking(Report report) {
        // 게시글은 가림 처리
        if(report.getContentType() == ContentType.POST) {
            Post post = postRepository.findById(report.getContentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.BAD_APPROACH)
            );
            post.updateTitleAndContent(POST_SCREENED, POST_SCREENED);
        }
        // 댓글은 가림 처리
        else if (report.getContentType() == ContentType.COMMENT) {
            Comment comment = commentRepository.findById(report.getContentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.BAD_APPROACH)
            );
            comment.updateContent(COMMENT_SCREENED);
        }
        else{
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }
    }
}