package com.hong.forapw.domain.meeting;

import com.hong.forapw.domain.alarm.model.AlarmRequest;
import com.hong.forapw.domain.meeting.model.MeetingUserProfileDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.meeting.entity.Meeting;
import com.hong.forapw.domain.meeting.entity.MeetingUser;
import com.hong.forapw.domain.meeting.model.MeetingRequest;
import com.hong.forapw.domain.meeting.model.MeetingResponse;
import com.hong.forapw.domain.meeting.repository.MeetingRepository;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import com.hong.forapw.domain.meeting.repository.MeetingUserRepository;
import com.hong.forapw.integration.rabbitmq.RabbitMqUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.domain.meeting.MeetingMapper.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;
    private final RabbitMqUtils brokerService;

    @Transactional
    public MeetingResponse.CreateMeetingDTO createMeeting(MeetingRequest.CreateMeetingDTO requestDTO, Long groupId, Long userId) {
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

        return new MeetingResponse.CreateMeetingDTO(meeting.getId());
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
    public void updateMeeting(MeetingRequest.UpdateMeetingDTO requestDTO, Long groupId, Long meetingId, Long userId) {
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

    public List<MeetingResponse.MeetingDTO> findMeetings(Long groupId, Pageable pageable) {
        Page<Meeting> meetingPage = meetingRepository.findByGroupId(groupId, pageable);
        Map<Long, List<String>> meetingUserProfiles = getMeetingUserProfilesByGroupId(groupId);

        return meetingPage.getContent().stream()
                .map(meeting -> toMeetingDTO(meeting, meetingUserProfiles.getOrDefault(meeting.getId(), Collections.emptyList())))
                .toList();
    }

    public MeetingResponse.FindMeetingByIdDTO findMeetingById(Long meetingId, Long groupId, Long userId) {
        validateGroupExists(groupId);
        validateIsGroupMember(groupId, userId);

        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        List<User> participants = meetingUserRepository.findUserByMeetingId(meeting.getId());
        List<MeetingResponse.ParticipantDTO> participantDTOS = toParticipantDTOs(participants);

        return toFindMeetingByIdDTO(meeting, participantDTOS);
    }

    private void addMeetingCreatorToParticipants(User creator, Meeting meeting) {
        MeetingUser meetingUser = MeetingUser.builder()
                .user(creator)
                .build();
        meeting.addMeetingUser(meetingUser); // cascade에 의해 meetingUser도 자동으로 저장됨
        meetingRepository.save(meeting);

        meeting.incrementParticipantCount();
    }

    private void validateMeetingNameNotDuplicate(MeetingRequest.CreateMeetingDTO requestDTO, Long groupId) {
        if (meetingRepository.existsByNameAndGroupId(requestDTO.name(), groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }
    }

    private void notifyGroupMembersAboutNewMeeting(Long groupId, Long creatorId, String meetingName) {
        List<User> groupMembers = groupUserRepository.findUserByGroupIdWithoutMe(groupId, creatorId);
        for (User member : groupMembers) {
            String content = "새로운 정기 모임: " + meetingName;
            String redirectURL = "/volunteer/" + groupId;
            createNewMeetingAlarm(member.getId(), content, redirectURL);
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

    private void updateMeeting(MeetingRequest.UpdateMeetingDTO requestDTO, Meeting meeting) {
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

    private Map<Long, List<String>> getMeetingUserProfilesByGroupId(Long groupId) {
        // <Long meetingId, List<String> userProfiles>
        return meetingUserRepository.findMeetingUsersByGroupId(groupId).stream()
                .collect(Collectors.groupingBy(
                        MeetingUserProfileDTO::meetingId,
                        Collectors.mapping(MeetingUserProfileDTO::profileURL, Collectors.toList())
                ));
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

    private void createNewMeetingAlarm(Long userId, String content, String redirectURL) {
        AlarmRequest.AlarmDTO alarmDTO = new AlarmRequest.AlarmDTO(
                userId,
                content,
                redirectURL,
                LocalDateTime.now(),
                AlarmType.NEW_MEETING);

        brokerService.sendAlarmToUser(userId, alarmDTO);
    }

    private void validateGroupExists(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }
}
