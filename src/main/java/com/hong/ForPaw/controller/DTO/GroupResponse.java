package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Group.GroupRole;
import com.hong.ForPaw.domain.Province;

import java.time.LocalDateTime;
import java.util.List;

public class GroupResponse {

    public record FindGroupByIdDTO(String name,
                                   Province province,
                                   District district,
                                   String subDistrict,
                                   String description,
                                   String category,
                                   String profileURL,
                                   Long maxNum) {}

    public record FindAllGroupListDTO(List<RecommendGroupDTO> recommendGroups,
                                      List<NewGroupDTO> newGroups,
                                      List<LocalGroupDTO> localGroups,
                                      List<MyGroupDTO> myGroups) {}

    public record FindLocalGroupListDTO(List<LocalGroupDTO> localGroups) {}

    public record FindNewGroupListDTO(List<NewGroupDTO> newGroups) {}

    public record FindMyGroupListDTO(List<MyGroupDTO> myGroups) {}

    public record RecommendGroupDTO(Long id,
                                    String name,
                                    String description,
                                    Long participationNum,
                                    String category,
                                    Province province,
                                    District district,
                                    String profileURL,
                                    Long likeNum,
                                    boolean isLike) {}

    public record NewGroupDTO(Long id,
                              String name,
                              String category,
                              Province province,
                              District district,
                              String profileURL) {}

    public record LocalGroupDTO(Long id,
                                String name,
                                String description,
                                Long participationNum,
                                String category,
                                Province province,
                                District district,
                                String profileURL,
                                Long likeNum,
                                boolean isLike) {}

    public record MyGroupDTO(Long id,
                             String name,
                             String description,
                             Long participationNum,
                             String category,
                             Province province,
                             District district,
                             String profileURL,
                             Long likeNum,
                             boolean isLike) {}

    public record FindGroupDetailByIdDTO(String profileURL,
                                         String name,
                                         String description,
                                         List<NoticeDTO> notices,
                                         List<MeetingDTO> meetings,
                                         List<MemberDTO> members) {}

    public record FindNoticeListDTO(List<NoticeDTO> notices) {}

    public record FindMeetingListDTO(List<MeetingDTO> meetings) {}

    public record NoticeDTO(Long id,
                            String name,
                            LocalDateTime date,
                            String title,
                            Boolean isRead) {}

    public record MeetingDTO(Long id,
                             String name,
                             LocalDateTime meetDate,
                             String location,
                             Long cost,
                             Long participantNum,
                             Integer maxNum,
                             String profileURL,
                             String description,
                             List<ParticipantDTO> participants) {}

    public record ParticipantDTO(String profileURL) {}

    public record MemberDTO(Long id, String name, GroupRole role, String profileURL) {}

    public record CreateGroupDTO(Long id) {}

    public record CreateMeetingDTO(Long id) {}

    public record CreateNoticeDTO(Long id) {}
}