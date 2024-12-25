package com.hong.forapw.domain.group.model;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.meeting.model.MeetingResponse;

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
                                   Long maxNum) {
    }

    public record FindAllGroupListDTO(List<RecommendGroupDTO> recommendGroups,
                                      List<NewGroupDTO> newGroups,
                                      //List<LocalGroupDTO> localGroups,
                                      List<MyGroupDTO> myGroups) {
    }

    public record FindLocalGroupListDTO(List<LocalGroupDTO> localGroups) {
    }

    public record FindNewGroupListDTO(List<NewGroupDTO> newGroups) {
    }

    public record FindLocalAndNewGroupListDTO(List<LocalGroupDTO> localGroups, List<NewGroupDTO> newGroups) {
    }

    public record FindMyGroupListDTO(List<MyGroupDTO> myGroups) {
    }

    public record RecommendGroupDTO(Long id,
                                    String name,
                                    String description,
                                    Long participationNum,
                                    String category,
                                    Province province,
                                    District district,
                                    String profileURL,
                                    Long likeNum,
                                    boolean isLike,
                                    boolean isShelterOwns,
                                    String shelterName) {
    }

    public record NewGroupDTO(Long id,
                              String name,
                              String category,
                              Province province,
                              District district,
                              String profileURL) {
    }

    public record LocalGroupDTO(Long id,
                                String name,
                                String description,
                                Long participationNum,
                                String category,
                                Province province,
                                District district,
                                String profileURL,
                                Long likeNum,
                                boolean isLike,
                                boolean isShelterOwns,
                                String shelterName) {
    }

    public record MyGroupDTO(Long id,
                             String name,
                             String description,
                             Long participationNum,
                             String category,
                             Province province,
                             District district,
                             String profileURL,
                             Long likeNum,
                             boolean isLike,
                             boolean isShelterOwns,
                             String shelterName) {
    }

    public record FindGroupDetailByIdDTO(String profileURL,
                                         String name,
                                         String description,
                                         List<NoticeDTO> notices,
                                         List<MeetingResponse.MeetingDTO> meetings,
                                         List<MemberDTO> members) {
    }

    public record FindNoticeListDTO(List<NoticeDTO> notices) {
    }

    public record NoticeDTO(Long id,
                            String name,
                            LocalDateTime date,
                            String title,
                            Boolean isRead) {
    }

    public record MemberDTO(Long id,
                            String name,
                            GroupRole role,
                            String profileURL,
                            LocalDateTime joinDate) {
    }

    public record MemberDetailDTO(Long id,
                                  String nickName,
                                  String profileURL,
                                  LocalDateTime joinDate,
                                  GroupRole role) {
    }

    public record CreateGroupDTO(Long id) {
    }

    public record CreateNoticeDTO(Long id) {
    }

    public record FindApplicantListDTO(List<ApplicantDTO> applicants) {
    }

    public record FindGroupMemberListDTO(Long participantCnt, Long maxNum, List<MemberDetailDTO> members) {
    }

    public record ApplicantDTO(Long id,
                               String nickName,
                               String greeting,
                               String email,
                               String profileURL,
                               Province province,
                               District district,
                               LocalDateTime applyDate) {
    }
}