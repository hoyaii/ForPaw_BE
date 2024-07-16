package com.hong.ForPaw.controller.DTO;

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
}
