package com.hong.forapw.service.group;

import com.hong.forapw.controller.dto.AlarmRequest;
import com.hong.forapw.controller.dto.GroupRequest;
import com.hong.forapw.controller.dto.GroupResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.mapper.GroupMapper;
import com.hong.forapw.domain.alarm.AlarmType;
import com.hong.forapw.domain.group.Group;
import com.hong.forapw.domain.group.GroupRole;
import com.hong.forapw.domain.group.Meeting;
import com.hong.forapw.domain.group.MeetingUser;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.repository.group.GroupRepository;
import com.hong.forapw.repository.group.GroupUserRepository;
import com.hong.forapw.repository.group.MeetingRepository;
import com.hong.forapw.repository.group.MeetingUserRepository;
import com.hong.forapw.service.BrokerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.hong.forapw.core.utils.mapper.GroupMapper.buildMeeting;
import static com.hong.forapw.core.utils.mapper.MeetingMapper.toParticipantDTOs;
import static com.hong.forapw.core.utils.mapper.MeetingMapper.toFindMeetingByIdDTO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;
    private final BrokerService brokerService;

    @Transactional
    public GroupResponse.CreateMeetingDTO createMeeting(GroupRequest.CreateMeetingDTO requestDTO, Long groupId, Long userId) {
        validateGroupExists(groupId);
        validateMeetingNameNotDuplicate(requestDTO, groupId);

        User creator = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
        validateGroupAdminAuthorization(creator, groupId);

        Group group = groupRepository.getReferenceById(groupId);
        Meeting meeting = buildMeeting(requestDTO, group, creator);
        addMeetingCreatorToParticipants(creator, meeting);

        notifyGroupMembersAboutNewMeeting(groupId, userId, requestDTO.name());

        return new GroupResponse.CreateMeetingDTO(meeting.getId());
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredMeetings() {
        LocalDateTime now = LocalDateTime.now();
        List<Meeting> expiredMeetings = meetingRepository.findByMeetDateBefore(now);

        expiredMeetings.forEach(meeting -> {
            meetingUserRepository.deleteAllByMeetingId(meeting.getId());
            meetingRepository.deleteById(meeting.getId());
        });
    }

    @Transactional
    public void updateMeeting(GroupRequest.UpdateMeetingDTO requestDTO, Long groupId, Long meetingId, Long userId) {
        validateMeetingExists(meetingId);

        User groupAdmin = userRepository.getReferenceById(groupId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        updateMeeting(requestDTO, meeting);
    }

    @Transactional
    public void joinMeeting(Long groupId, Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        validateIsGroupMember(groupId, userId);
        validateNotAlreadyParticipant(meetingId, userId);

        User joiner = userRepository.getReferenceById(userId);
        addParticipantToMeeting(joiner, meeting);
    }

    @Transactional
    public void withdrawMeeting(Long groupId, Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        validateIsGroupMember(groupId, userId);
        validateMeetingParticipation(meetingId, userId);

        meetingUserRepository.deleteByMeetingIdAndUserId(meetingId, userId);
        meeting.decrementParticipantCount();
    }

    @Transactional
    public void deleteMeeting(Long groupId, Long meetingId, Long userId) {
        validateMeetingExists(meetingId);

        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        meetingUserRepository.deleteAllByMeetingId(meetingId);
        meetingRepository.deleteById(meetingId);
    }

    public GroupResponse.FindMeetingByIdDTO findMeetingById(Long meetingId, Long groupId, Long userId) {
        validateGroupExists(groupId);
        validateIsGroupMember(groupId, userId);

        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        List<User> participants = meetingUserRepository.findUserByMeetingId(meeting.getId());
        List<GroupResponse.ParticipantDTO> participantDTOS = toParticipantDTOs(participants);

        return toFindMeetingByIdDTO(meeting, participantDTOS);
    }

    @Transactional
    public GroupResponse.FindGroupMemberListDTO findGroupMemberList(Long userId, Long groupId) {
        // 그룹 존재 여부 체크
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        // 관리자만 멤버들을 볼 수 있음
        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        List<GroupResponse.MemberDetailDTO> memberDetailDTOS = groupUserRepository.findByGroupIdWithGroup(groupId).stream()
                .filter(groupUser -> !groupUser.getGroupRole().equals(GroupRole.TEMP))
                .map(GroupMapper::toMemberDetailDTO)
                .toList();

        return new GroupResponse.FindGroupMemberListDTO(group.getParticipantNum(), group.getMaxNum(), memberDetailDTOS);
    }

    private void addMeetingCreatorToParticipants(User creator, Meeting meeting) {
        MeetingUser meetingUser = MeetingUser.builder()
                .user(creator)
                .build();
        meeting.addMeetingUser(meetingUser); // cascade에 의해 meetingUser도 자동으로 저장됨
        meetingRepository.save(meeting);

        meeting.incrementParticipantCount();
    }

    private void validateMeetingNameNotDuplicate(GroupRequest.CreateMeetingDTO requestDTO, Long groupId) {
        if (meetingRepository.existsByNameAndGroupId(requestDTO.name(), groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }
    }

    private void notifyGroupMembersAboutNewMeeting(Long groupId, Long creatorId, String meetingName) {
        List<User> groupMembers = groupUserRepository.findUserByGroupIdWithoutMe(groupId, creatorId);
        for (User member : groupMembers) {
            String content = "새로운 정기 모임: " + meetingName;
            String redirectURL = "/volunteer/" + groupId;
            createAlarm(member.getId(), content, redirectURL, AlarmType.NEW_MEETING);
        }
    }

    private void addParticipantToMeeting(User joiner, Meeting meeting) {
        MeetingUser meetingUser = MeetingUser.builder()
                .user(joiner)
                .build();
        meeting.addMeetingUser(meetingUser);
        meetingUserRepository.save(meetingUser);

        meeting.incrementParticipantCount();
    }

    private void validateMeetingParticipation(Long meetingId, Long userId) {
        if (!meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new CustomException(ExceptionCode.MEETING_NOT_MEMBER);
        }
    }

    private void validateNotAlreadyParticipant(Long meetingId, Long userId) {
        if (meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new CustomException(ExceptionCode.MEETING_ALREADY_JOIN);
        }
    }

    private void validateMeetingExists(Long meetingId) {
        if (!meetingRepository.existsById(meetingId)) {
            throw new CustomException(ExceptionCode.MEETING_NOT_FOUND);
        }
    }

    private void updateMeeting(GroupRequest.UpdateMeetingDTO requestDTO, Meeting meeting) {
        meeting.updateMeeting(
                requestDTO.name(),
                requestDTO.meetDate(),
                requestDTO.location(),
                requestDTO.cost(),
                requestDTO.maxNum(),
                requestDTO.description(),
                requestDTO.profileURL()
        );
    }

    private void validateIsGroupMember(Long groupId, Long userId) {
        Set<GroupRole> roles = EnumSet.of(GroupRole.USER, GroupRole.ADMIN, GroupRole.CREATOR);
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> roles.contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_MEMBER));
    }

    private void validateGroupAdminAuthorization(User user, Long groupId) {
        if (user.isAdmin()) {
            return;
        }

        groupUserRepository.findByGroupIdAndUserId(groupId, user.getId())
                .filter(groupUser -> EnumSet.of(GroupRole.ADMIN, GroupRole.CREATOR).contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN));
    }

    private void createAlarm(Long userId, String content, String redirectURL, AlarmType alarmType) {
        AlarmRequest.AlarmDTO alarmDTO = new AlarmRequest.AlarmDTO(
                userId,
                content,
                redirectURL,
                LocalDateTime.now(),
                alarmType);

        brokerService.sendAlarmToUser(userId, alarmDTO);
    }

    private void validateGroupExists(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }
}
