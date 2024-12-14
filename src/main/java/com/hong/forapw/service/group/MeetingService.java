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
import com.hong.forapw.domain.user.UserRole;
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
        // 존재하지 않는 그룹이면 에러
        checkGroupExist(groupId);

        // 이름 중복 체크
        if (meetingRepository.existsByNameAndGroupId(requestDTO.name(), groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }

        // 권한 체크 (메니저급만 생성 가능)
        User creator = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
        validateAdminAuthorization(groupId, creator.getId());

        Group group = groupRepository.getReferenceById(groupId);
        Meeting meeting = buildMeeting(requestDTO, group, creator);

        // 주최자를 맴버로 저장
        MeetingUser meetingUser = MeetingUser.builder()
                .user(creator)
                .build();

        // 양방향 관계 설정 후 meeting 저장 (cascade에 의해 meetingUser도 자동으로 저장됨)
        meeting.addMeetingUser(meetingUser);
        meetingRepository.save(meeting);

        // 미팅 참여자 수 증가 (미팅 생성자 참여)
        meetingRepository.incrementParticipantNum(meeting.getId());

        // 알람 생성
        List<User> users = groupUserRepository.findUserByGroupIdWithoutMe(groupId, userId);

        for (User user : users) {
            String content = "새로운 정기 모임: " + requestDTO.name();
            String redirectURL = "/volunteer/" + groupId;
            createAlarm(user.getId(), content, redirectURL, AlarmType.NEW_MEETING);
        }

        return new GroupResponse.CreateMeetingDTO(meeting.getId());
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
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
        // 존재하지 않는 모임이면 에러 처리
        checkMeetingExist(meetingId);

        // 권한 체크(메니저급만 수정 가능)
        validateAdminAuthorization(groupId, userId);

        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        meeting.updateMeeting(requestDTO.name(), requestDTO.meetDate(), requestDTO.location(), requestDTO.cost(), requestDTO.maxNum(), requestDTO.description(), requestDTO.profileURL());
    }

    @Transactional
    public void joinMeeting(Long groupId, Long meetingId, Long userId) {
        // 존재하지 않는 모임이면 에러 처리
        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        // 그룹의 맴버가 아니면 에러 처리
        validateIsMember(groupId, userId);

        // 이미 참가중인 모임이면 에러 처리
        if (meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new CustomException(ExceptionCode.MEETING_ALREADY_JOIN);
        }

        // 기본 프로필은 나중에 주소를 설정해야 함
        User joiner = userRepository.getReferenceById(userId);
        MeetingUser meetingUser = MeetingUser.builder()
                .user(joiner)
                .build();

        // 양방향 관계 설정 후 meeting 저장
        meeting.addMeetingUser(meetingUser);
        meetingUserRepository.save(meetingUser);

        // 미팅 참가자 수 증가
        meetingRepository.incrementParticipantNum(meetingId);
    }

    @Transactional
    public void withdrawMeeting(Long groupId, Long meetingId, Long userId) {
        // 존재하지 않는 모임이면 에러 처리
        checkMeetingExist(meetingId);

        // 그룹의 맴버가 아니면 에러 처리
        validateIsMember(groupId, userId);

        // 참가중이 맴버가 아니라면 에러 처리
        if (!meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new CustomException(ExceptionCode.MEETING_NOT_MEMBER);
        }

        meetingUserRepository.deleteByMeetingIdAndUserId(meetingId, userId);

        // 참가자 수 감소
        meetingRepository.decrementParticipantNum(meetingId);
    }

    @Transactional
    public void deleteMeeting(Long groupId, Long meetingId, Long userId) {
        // 존재하지 않는 모임이면 에러 처리
        checkMeetingExist(meetingId);

        // 권한 체크 (메니저급만 삭제 가능)
        validateAdminAuthorization(groupId, userId);

        meetingUserRepository.deleteAllByMeetingId(meetingId);
        meetingRepository.deleteById(meetingId);
    }

    public GroupResponse.FindMeetingByIdDTO findMeetingById(Long meetingId, Long groupId, Long userId) {
        // 그룹 존재 여부 체크
        checkGroupExist(groupId);

        // 맴버인지 체크
        validateIsMember(groupId, userId);

        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        List<GroupResponse.ParticipantDTO> participantDTOS = meetingUserRepository.findUserByMeetingId(meeting.getId()).stream()
                .map(user -> new GroupResponse.ParticipantDTO(user.getProfileURL(), user.getNickname()))
                .toList();

        return new GroupResponse.FindMeetingByIdDTO(meeting.getId(), meeting.getName(), meeting.getMeetDate(), meeting.getLocation(), meeting.getCost(), meeting.getParticipantNum(), meeting.getMaxNum(), meeting.getCreator().getNickname(), meeting.getProfileURL(), meeting.getDescription(), participantDTOS);
    }

    @Transactional
    public GroupResponse.FindGroupMemberListDTO findGroupMemberList(Long userId, Long groupId) {
        // 그룹 존재 여부 체크
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        // 관리자만 멤버들을 볼 수 있음
        validateAdminAuthorization(groupId, userId);

        List<GroupResponse.MemberDetailDTO> memberDetailDTOS = groupUserRepository.findByGroupIdWithGroup(groupId).stream()
                .filter(groupUser -> !groupUser.getGroupRole().equals(GroupRole.TEMP))
                .map(GroupMapper::toMemberDetailDTO)
                .toList();

        return new GroupResponse.FindGroupMemberListDTO(group.getParticipantNum(), group.getMaxNum(), memberDetailDTOS);
    }

    private void checkMeetingExist(Long meetingId) {
        if (!meetingRepository.existsById(meetingId)) {
            throw new CustomException(ExceptionCode.MEETING_NOT_FOUND);
        }
    }

    private void validateIsMember(Long groupId, Long userId) {
        Set<GroupRole> roles = EnumSet.of(GroupRole.USER, GroupRole.ADMIN, GroupRole.CREATOR);
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> roles.contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_MEMBER));
    }

    private void validateAdminAuthorization(Long groupId, Long userId) {
        // 서비스 운영자는 접근 가능
        if (checkServiceAdminAuthority(userId)) {
            return;
        }

        // ADMIN, CREATOR만 허용
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> EnumSet.of(GroupRole.ADMIN, GroupRole.CREATOR).contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN));
    }

    private boolean checkServiceAdminAuthority(Long userId) {
        UserRole role = userRepository.findRoleById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return role.equals(UserRole.ADMIN) || role.equals(UserRole.SUPER);
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

    private void checkGroupExist(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }
}
