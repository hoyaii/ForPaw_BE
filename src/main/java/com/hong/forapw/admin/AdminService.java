package com.hong.forapw.admin;

import com.hong.forapw.admin.repository.ReportRepository;
import com.hong.forapw.domain.apply.ApplyRepository;
import com.hong.forapw.admin.model.AdminRequest;
import com.hong.forapw.admin.model.AdminResponse;
import com.hong.forapw.admin.model.AdminResponse.ApplyDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.apply.entity.Apply;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.admin.entity.Visit;
import com.hong.forapw.domain.faq.FAQ;
import com.hong.forapw.domain.faq.FaqRepository;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.entity.Report;
import com.hong.forapw.admin.constant.ReportStatus;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.UserStatus;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.admin.repository.VisitRepository;
import com.hong.forapw.domain.inquiry.InquiryRepository;
import com.hong.forapw.domain.post.repository.CommentRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AnimalRepository animalRepository;
    private final ReportRepository reportRepository;
    private final ApplyRepository applyRepository;
    private final InquiryRepository inquiryRepository;
    private final UserStatusRepository userStatusRepository;
    private final FaqRepository faqRepository;

    private static final String POST_SCREENED = "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다.";
    private static final String COMMENT_SCREENED = "커뮤니티 규정을 위반하여 가려진 댓글입니다.";

    @Transactional(readOnly = true)
    public AdminResponse.FindDashboardStatsDTO findDashboardStats(Long userId) {
        // 권한 체크
        checkAdminAuthority(userId);

        LocalDateTime now = LocalDateTime.now();

        // 유저 통계
        Long activeUserCount = userRepository.countActiveUsers();
        Long notActiveUserCount = userRepository.countNotActiveUsers();

        AdminResponse.UserStatsDTO userStatsDTO = new AdminResponse.UserStatsDTO(activeUserCount, notActiveUserCount);

        // 유기 동물 통계
        Long waitingForAdoptionNum = animalRepository.countAnimal();
        Long adoptionProcessingNum = applyRepository.countByStatus(ApplyStatus.PROCESSING);
        Long adoptedRecentlyNum = applyRepository.countByStatusWithinDate(ApplyStatus.PROCESSED, now.minusWeeks(1));
        Long adoptedTotalNum = applyRepository.countByStatus(ApplyStatus.PROCESSED);

        AdminResponse.AnimalStatsDTO animalStatsDTO = new AdminResponse.AnimalStatsDTO(
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

        List<AdminResponse.DailyVisitorDTO> dailyVisitorDTOS = dailyVisitors.entrySet().stream()
                .map(entry -> new AdminResponse.DailyVisitorDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AdminResponse.DailyVisitorDTO::date))
                .collect(Collectors.toList());

        // 시간별 방문자 수 집계 (오늘 날짜)
        Map<LocalTime, Long> hourlyVisitors = visits.stream()
                .filter(visit -> visit.getDate().toLocalDate().isEqual(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        visit -> visit.getDate().toLocalTime().truncatedTo(ChronoUnit.HOURS),
                        Collectors.counting()
                ));

        List<AdminResponse.HourlyVisitorDTO> hourlyVisitorDTOS = hourlyVisitors.entrySet().stream()
                .map(entry -> new AdminResponse.HourlyVisitorDTO(
                        LocalDateTime.of(LocalDate.now(),
                                entry.getKey()),
                        entry.getValue()))
                .sorted(Comparator.comparing(AdminResponse.HourlyVisitorDTO::hour))
                .collect(Collectors.toList());

        // 오늘 발생한 이벤트 요약
        Long entryNum = userRepository.countAllUsersCreatedAfter(nowDateOnly);
        Long newPostNum = postRepository.countALlWithinDate(nowDateOnly);
        Long newCommentNum = commentRepository.countALlWithinDate(nowDateOnly);
        Long newAdoptApplicationNum = applyRepository.countByStatusWithinDate(ApplyStatus.PROCESSING, nowDateOnly);

        AdminResponse.DailySummaryDTO dailySummaryDTO = new AdminResponse.DailySummaryDTO(
                entryNum,
                newPostNum,
                newCommentNum,
                newAdoptApplicationNum
        );

        return new AdminResponse.FindDashboardStatsDTO(userStatsDTO, animalStatsDTO, dailyVisitorDTOS, hourlyVisitorDTOS, dailySummaryDTO);
    }

    @Transactional(readOnly = true)
    public AdminResponse.FindUserListDTO findUserList(Long adminId, Pageable pageable) {
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

        Page<User> userPage = userRepository.findAll(pageable);

        List<AdminResponse.ApplicantDTO> applicantDTOS = userPage.getContent().stream()
                .map(user -> new AdminResponse.ApplicantDTO(
                        user.getId(),
                        user.getNickname(),
                        user.getCreatedDate(),
                        Optional.ofNullable(latestVisitMap.get(user.getId()))
                                .map(Visit::getDate)
                                .orElse(null),
                        processingApplyMap.get(user.getId()) != null ? processingApplyMap.get(user.getId()) : 0L,
                        processedApplyMap.get(user.getId()) != null ? processedApplyMap.get(user.getId()) : 0L,
                        user.getRole(),
                        user.getStatus().isActive(),
                        user.getStatus().getSuspensionStart(),
                        user.getStatus().getSuspensionDays(),
                        user.getStatus().getSuspensionReason()
                ))
                .toList();

        return new AdminResponse.FindUserListDTO(applicantDTOS, userPage.getTotalPages());
    }

    @Transactional
    public void changeUserRole(AdminRequest.ChangeUserRoleDTO requestDTO, Long adminId, UserRole adminRole) {
        checkAdminAuthority(adminId);

        User user = userRepository.findNonWithdrawnById(requestDTO.userId()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 현재 유저의 Role과 동일한 값이 요청으로 들어옴
        if (requestDTO.role().equals(user.getRole())) {
            throw new CustomException(ExceptionCode.SAME_STATUS);
        }

        // Supser로의 권한 변경은 불가능
        if (requestDTO.role().equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }

        // Admin은 Super를 건들일 수 없음
        checkAdminPrivileges(adminRole, user.getRole());
        user.updateRole(requestDTO.role());
    }

    @Transactional
    public void suspendUser(AdminRequest.SuspendUserDTO requestDTO, Long adminId, UserRole adminRole) {
        checkAdminAuthority(adminId);

        UserStatus userStatus = userStatusRepository.findByUserId(requestDTO.userId()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 이미 정지되어 있음
        if (!userStatus.isActive()) {
            throw new CustomException(ExceptionCode.USER_ALREADY_SUSPENDED);
        }

        checkAdminPrivileges(adminRole, userStatus.getUser().getRole());
        userStatus.updateForSuspend(LocalDateTime.now(), requestDTO.suspensionDays(), requestDTO.suspensionReason());
    }

    @Transactional
    public void unSuspendUser(Long userId, Long adminId, UserRole adminRole) {
        checkAdminAuthority(adminId);

        UserStatus userStatus = userStatusRepository.findByUserId(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 이미 활성화된 상태
        if (userStatus.isActive()) {
            throw new CustomException(ExceptionCode.USER_ALREADY_SUSPENDED);
        }

        checkAdminPrivileges(adminRole, userStatus.getUser().getRole());
        userStatus.updateForUnSuspend();
    }

    @Transactional(readOnly = true)
    public AdminResponse.FindApplyListDTO findApplyList(Long adminId, ApplyStatus status, Pageable pageable) {
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
                            apply.getAddressDetail(),
                            animal.getShelter().getName(),
                            animal.getShelter().getCareTel(),
                            apply.getStatus()
                    );
                }).toList();

        return new AdminResponse.FindApplyListDTO(applyDTOS, applyPage.getTotalPages());
    }

    @Transactional
    public void changeApplyStatus(AdminRequest.ChangeApplyStatusDTO requestDTO, Long adminId) {
        checkAdminAuthority(adminId);

        Apply apply = applyRepository.findByIdWithAnimal(requestDTO.id()).orElseThrow(
                () -> new CustomException(ExceptionCode.APPLY_NOT_FOUND)
        );

        // 이미 처리 됌
        if (apply.getStatus().equals(ApplyStatus.PROCESSED)) {
            throw new CustomException(ExceptionCode.ANIMAL_APPLY_PROCESSED);
        }

        // 지원서 상태 변경
        apply.updateApplyStatus(requestDTO.status());

        // 입양이 완료된거면 입양 완료 상태로 변경
        if (requestDTO.status().equals(ApplyStatus.PROCESSED)) {
            apply.getAnimal().finishAdoption();
        }
    }

    @Transactional(readOnly = true)
    public AdminResponse.FindReportListDTO findReportList(Long adminId, ReportStatus status, Pageable pageable) {
        checkAdminAuthority(adminId);

        Page<Report> reportPage = reportRepository.findAllByStatus(status, pageable);

        List<AdminResponse.ReportDTO> reportDTOS = reportPage.getContent().stream()
                .map(report -> new AdminResponse.ReportDTO(
                        report.getId(),
                        report.getCreatedDate(),
                        report.getContentType(),
                        report.getContentId(),
                        report.getType(),
                        report.getReason(),
                        report.getReporter().getNickname(),
                        report.getOffender().getId(),
                        report.getOffender().getNickname(),
                        report.getStatus())
                ).toList();

        return new AdminResponse.FindReportListDTO(reportDTOS, reportPage.getTotalPages());
    }

    @Transactional
    public void processReport(AdminRequest.ProcessReportDTO requestDTO, Long adminId) {
        checkAdminAuthority(adminId);

        Report report = reportRepository.findById(requestDTO.id()).orElseThrow(
                () -> new CustomException(ExceptionCode.REPORT_NOT_FOUND)
        );

        // 이미 처리함
        if (report.getStatus().equals(ReportStatus.PROCESSED)) {
            throw new CustomException(ExceptionCode.ALREADY_REPORTED);
        }

        // 유저 정지 처리
        if (requestDTO.hasSuspension()) {
            // SUPER를 정지 시킬 수는 없다 (악용 방지)
            if (report.getOffender().getRole().equals(UserRole.SUPER)) {
                throw new CustomException(ExceptionCode.REPORT_NOT_APPLY_TO_SUPER);
            }

            report.getOffender().getStatus()
                    .updateForSuspend(LocalDateTime.now(), requestDTO.suspensionDays(), report.getReason());
        }

        // 가림 처리
        if (requestDTO.hasBlocking()) {
            processBlocking(report);
        }

        // 신고 내역 완료 처리
        report.updateStatus(ReportStatus.PROCESSED);
    }

    @Transactional(readOnly = true)
    public AdminResponse.FindSupportListDTO findSupportList(Long adminId, InquiryStatus status, Pageable pageable) {
        checkAdminAuthority(adminId);

        Page<Inquiry> inquiryPage = inquiryRepository.findByStatusWithUser(status, pageable);

        List<AdminResponse.InquiryDTO> inquiryDTOS = inquiryPage.getContent().stream()
                .map(inquiry -> new AdminResponse.InquiryDTO(
                        inquiry.getId(),
                        inquiry.getCreatedDate(),
                        inquiry.getQuestioner().getNickname(),
                        inquiry.getType(),
                        inquiry.getTitle(),
                        inquiry.getStatus())
                )
                .toList();

        return new AdminResponse.FindSupportListDTO(inquiryDTOS, inquiryPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminResponse.FindSupportByIdDTO findSupportById(Long adminId, Long inquiryId) {
        checkAdminAuthority(adminId);

        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        return new AdminResponse.FindSupportByIdDTO(
                inquiry.getId(),
                inquiry.getQuestioner().getNickname(),
                inquiry.getTitle(),
                inquiry.getDescription()
        );
    }

    @Transactional
    public AdminResponse.AnswerInquiryDTO answerInquiry(AdminRequest.AnswerInquiryDTO requestDTO, Long adminId, Long inquiryId) {
        checkAdminAuthority(adminId);

        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        // 답변은 하나만 할 수 있음
        if (inquiry.getAnswer() != null) {
            throw new CustomException(ExceptionCode.INQUIRY_ALREADY_ANSWER);
        }

        User admin = userRepository.getReferenceById(adminId);
        inquiry.updateAnswer(requestDTO.content(), admin);

        // 문의글은 처리 완료
        inquiry.updateStatus(InquiryStatus.PROCESSED);

        return new AdminResponse.AnswerInquiryDTO(inquiryId);
    }

    @Transactional(readOnly = true)
    public AdminResponse.FindFAQListDTO findFAQList() {
        List<FAQ> faqs = faqRepository.findAll();

        List<AdminResponse.FaqDTO> faqDTOS = faqs.stream()
                .map(faq -> new AdminResponse.FaqDTO(faq.getQuestion(), faq.getAnswer(), faq.getType(), faq.isTop()))
                .toList();

        return new AdminResponse.FindFAQListDTO(faqDTOS);
    }

    @Transactional
    public void createFAQ(AdminRequest.CreateFaqDTO requestDTO, Long adminId) {
        checkAdminAuthority(adminId);

        FAQ faq = FAQ.builder()
                .question(requestDTO.question())
                .answer(requestDTO.answer())
                .type(requestDTO.type())
                .isTop(requestDTO.isTop())
                .build();

        faqRepository.save(faq);
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
        if (adminRole.equals(UserRole.ADMIN) && userRole.equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void processBlocking(Report report) {
        // 게시글은 가림 처리
        if (report.getContentType() == ContentType.POST) {
            Post post = postRepository.findById(report.getContentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.BAD_APPROACH)
            );
            post.updateTitle(POST_SCREENED);
            post.processBlock();
        }
        // 댓글은 가림 처리
        else if (report.getContentType() == ContentType.COMMENT) {
            Comment comment = commentRepository.findById(report.getContentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.BAD_APPROACH)
            );
            comment.updateContent(COMMENT_SCREENED);
        } else {
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }
    }
}