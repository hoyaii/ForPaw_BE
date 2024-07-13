package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.User.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AuthenticationResponse {

    public record FindDashboardStatsDTO(UserStatsDTO userStatsDTO,
                                        AnimalStatsDTO animalStatsDTO,
                                        List<DailyVisitorDTO> dailyVisitorDTOS,
                                        List<HourlyVisitorDTO> hourlyVisitorDTOS,
                                        DailySummaryDTO dailySummaryDTO) {}

    public record UserStatsDTO(Long activeUsers, Long inactiveUsers) {}

    public record AnimalStatsDTO(Long waitingForAdoption,
                                 Long adoptionProcessing,
                                 Long adoptedRecently,
                                 Long adoptedTotal) {}

    public record DailyVisitorDTO(LocalDate date, Long visitors) {}

    public record HourlyVisitorDTO(LocalDateTime hour, Long visitors) {}

    public record DailySummaryDTO(Long entries,
                                  Long newPost,
                                  Long newComment,
                                  Long newAdoptApplication) {}

    public record findUserList(List<UserDTO> users){}

    public record UserDTO(Long id,
                          String nickname,
                          String email,
                          LocalDateTime signUpDate,
                          LocalDateTime lastLogin,
                          Long applicationsSubmitted,
                          Long applicationsCompleted,
                          UserRole role,
                          boolean isActive,
                          LocalDateTime suspensionStart,
                          Long suspensionDays,
                          String suspensionReason
    ){}

    public record UserRoleDTO(Long userId, UserRole role){}

    public record UserBanDTO(Long userId, Long duration, String reason){};

    public record GetApplyDTO(List<ApplyDTO> applyDTOS){};

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
    ){};

}
