package com.hong.forapw.admin.model;


import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.domain.faq.FaqType;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.inquiry.constant.InquiryType;
import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.constant.ReportStatus;
import com.hong.forapw.admin.constant.ReportType;
import com.hong.forapw.domain.user.constant.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdminResponse {

    public record FindDashboardStatsDTO(UserStatsDTO userStatsDTO,
                                        AnimalStatsDTO animalStatsDTO,
                                        List<DailyVisitorDTO> dailyVisitorDTOS,
                                        List<HourlyVisitorDTO> hourlyVisitorDTOS,
                                        DailySummaryDTO dailySummaryDTO) {
    }

    public record UserStatsDTO(Long activeUsers, Long inactiveUsers) {
    }

    public record AnimalStatsDTO(Long waitingForAdoption,
                                 Long adoptionProcessing,
                                 Long adoptedRecently,
                                 Long adoptedTotal) {
    }

    public record DailyVisitorDTO(LocalDate date, Long visitors) {
    }

    public record HourlyVisitorDTO(LocalDateTime hour, Long visitors) {
    }

    public record DailySummaryDTO(Long entries,
                                  Long newPost,
                                  Long newComment,
                                  Long newAdoptApplication) {
    }

    public record FindUserListDTO(List<ApplicantDTO> users, int totalPages) {
    }

    public record ApplicantDTO(Long id,
                               String nickName,
                               LocalDateTime signUpDate,
                               LocalDateTime lastLogin,
                               Long applicationsSubmitted,
                               Long applicationsCompleted,
                               UserRole role,
                               boolean isActive,
                               LocalDateTime suspensionStart,
                               Long suspensionDays,
                               String suspensionReason) {
    }

    public record UnSuspendUserDTO(Long userId) {
    }

    public record WithdrawUserDTO(Long userId) {
    }

    public record ApplyDTO(Long applyId,
                           LocalDateTime applyDate,
                           Long animalId,
                           String kind,
                           String gender,
                           String age,
                           String userName,
                           String tel,
                           String residence,
                           String careName,
                           String careTel,
                           ApplyStatus status
    ) {
    }

    ;

    public record FindApplyListDTO(List<ApplyDTO> applies, int totalPages) {
    }

    public record FindReportListDTO(List<ReportDTO> reports, int totalPages) {
    }

    public record ReportDTO(Long id,
                            LocalDateTime reportDate,
                            ContentType contentType,
                            Long contentId,
                            ReportType type,
                            String reason,
                            String reporterNickName,
                            Long offenderId,
                            String offenderNickName,
                            ReportStatus status) {
    }

    public record FindSupportListDTO(List<InquiryDTO> inquiries, int totalPages) {
    }

    public record InquiryDTO(Long id,
                             LocalDateTime date,
                             String questionerNick,
                             InquiryType type,
                             String title,
                             InquiryStatus status) {
    }

    public record FindSupportByIdDTO(Long id,
                                     String questionerNick,
                                     String title,
                                     String description) {
    }

    public record AnswerInquiryDTO(Long id) {
    }

    public record FindFAQListDTO(List<FaqDTO> faqs) {
    }

    public record FaqDTO(String question,
                         String answer,
                         FaqType type,
                         boolean isTop) {
    }
}
