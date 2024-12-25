package com.hong.forapw.domain.user.service;

import com.hong.forapw.domain.user.model.*;
import com.hong.forapw.domain.post.model.PostTypeCountDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.admin.entity.LoginAttempt;
import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.user.constant.AuthProvider;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.entity.UserStatus;
import com.hong.forapw.domain.alarm.repository.AlarmRepository;
import com.hong.forapw.domain.animal.repository.FavoriteAnimalRepository;
import com.hong.forapw.admin.repository.LoginAttemptRepository;
import com.hong.forapw.admin.repository.VisitRepository;
import com.hong.forapw.domain.chat.repository.ChatUserRepository;
import com.hong.forapw.domain.group.repository.FavoriteGroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import com.hong.forapw.domain.meeting.repository.MeetingUserRepository;
import com.hong.forapw.domain.post.repository.CommentLikeRepository;
import com.hong.forapw.domain.post.repository.CommentRepository;
import com.hong.forapw.domain.post.repository.PostLikeRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.user.repository.UserStatusRepository;
import com.hong.forapw.integration.email.model.BlankTemplate;
import com.hong.forapw.integration.email.model.EmailVerificationTemplate;
import com.hong.forapw.integration.email.model.TemplateModel;
import com.hong.forapw.integration.rabbitmq.RabbitMqUtils;
import com.hong.forapw.integration.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hong.forapw.common.utils.JwtUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.domain.user.UserMapper.*;
import static com.hong.forapw.integration.email.EmailService.generateVerificationCode;
import static com.hong.forapw.integration.email.EmailTemplate.ACCOUNT_SUSPENSION;
import static com.hong.forapw.integration.email.EmailTemplate.VERIFICATION_CODE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final GroupUserRepository groupUserRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final ChatUserRepository chatUserRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserStatusRepository userStatusRepository;
    private final VisitRepository visitRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final RabbitMqUtils rabbitMqUtils;
    private final EmailService emailService;
    private final UserCacheService userCacheService;
    private final JwtUtils jwtUtils;

    private static final String MAIL_TEMPLATE_FOR_CODE = "verification_code_email.html";
    private static final String MAIL_TEMPLATE_FOR_LOCK_ACCOUNT = "lock_account.html";
    private static final String ALL_CHARS = "!@#$%^&*0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String USER_QUEUE_PREFIX = "user.";
    private static final String ALARM_EXCHANGE = "alarm.exchange";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String UNKNOWN = "unknown";
    private static final String[] IP_HEADER_CANDIDATES = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
    private static final String CODE_TYPE_RECOVERY = "recovery";
    private static final String MODEL_KEY_CODE = "code";
    private static final long CURRENT_FAILURE_LIMIT = 3L;
    private static final long DAILY_FAILURE_LIMIT = 3L;

    @Transactional
    public LoginResult login(UserRequest.LoginDTO requestDTO, HttpServletRequest request) {
        User user = userRepository.findByEmailWithRemoved(requestDTO.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_ACCOUNT_WRONG)
        );

        validateUserNotExited(user);
        validateUserActive(user);
        validateLoginAttempts(user);

        if (isPasswordUnmatched(user, requestDTO.password())) {
            handleLoginFailures(user);
            infoLoginFail(user);
        }

        recordLoginAttempt(user, request);
        return createToken(user);
    }

    @Transactional
    public void join(UserRequest.JoinDTO requestDTO) {
        validateConfirmPasswordMatch(requestDTO.password(), requestDTO.passwordConfirm());
        checkEmailNotRegistered(requestDTO.email());
        validateNicknameUniqueness(requestDTO.nickName());

        User user = buildUser(requestDTO, passwordEncoder.encode(requestDTO.password()));
        userRepository.save(user);

        setUserStatus(user);
        setAlarmQueue(user);
    }

    @Transactional
    public void socialJoin(UserRequest.SocialJoinDTO requestDTO) {
        checkEmailNotRegistered(requestDTO.email());
        validateNicknameUniqueness(requestDTO.nickName());

        User user = buildUser(requestDTO, passwordEncoder.encode(generatePassword()));
        userRepository.save(user);

        setUserStatus(user);
        setAlarmQueue(user);
    }

    @Async
    public void sendCodeByEmail(String email, String codeType) {
        userCacheService.validateEmailCodeNotSent(email, codeType);

        String verificationCode = generateVerificationCode();
        userCacheService.storeVerificationCode(email, codeType, verificationCode);

        EmailVerificationTemplate templateModel = new EmailVerificationTemplate(verificationCode);
        emailService.sendMail(email, VERIFICATION_CODE.getSubject(), MAIL_TEMPLATE_FOR_CODE, templateModel);
    }

    public UserResponse.VerifyEmailCodeDTO verifyCode(UserRequest.VerifyCodeDTO requestDTO, String codeType) {
        if (userCacheService.isCodeMismatch(requestDTO.email(), requestDTO.code(), codeType))
            return new UserResponse.VerifyEmailCodeDTO(false);

        if (CODE_TYPE_RECOVERY.equals(codeType))
            userCacheService.storeCodeToEmail(requestDTO.code(), requestDTO.email());

        return new UserResponse.VerifyEmailCodeDTO(true);
    }

    public UserResponse.CheckNickNameDTO checkNickName(UserRequest.CheckNickDTO requestDTO) {
        boolean isDuplicate = userRepository.existsByNicknameWithRemoved(requestDTO.nickName());
        return new UserResponse.CheckNickNameDTO(isDuplicate);
    }

    public UserResponse.CheckLocalAccountExistDTO checkLocalAccountExist(UserRequest.EmailDTO requestDTO) {
        return userRepository.findByEmail(requestDTO.email())
                .map(user -> new UserResponse.CheckLocalAccountExistDTO(true, user.isLocalJoined()))
                .orElse(new UserResponse.CheckLocalAccountExistDTO(false, false));
    }

    public UserResponse.CheckAccountExistDTO checkAccountExist(String email) {
        boolean isValid = userRepository.existsByEmail(email);
        return new UserResponse.CheckAccountExistDTO(isValid);
    }

    @Transactional
    public void resetPassword(UserRequest.ResetPasswordDTO requestDTO) {
        String email = userCacheService.getEmailByVerificationCode(requestDTO.code());
        if (email == null) {
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }

        userCacheService.deleteCodeToEmail(requestDTO.code());
        updateNewPassword(email, requestDTO.newPassword());
    }

    public UserResponse.VerifyPasswordDTO verifyPassword(UserRequest.CurPasswordDTO requestDTO, Long userId) {
        User user = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        if (isPasswordUnmatched(user, requestDTO.password())) {
            return new UserResponse.VerifyPasswordDTO(false);
        }

        return new UserResponse.VerifyPasswordDTO(true);
    }

    @Transactional
    public void updatePassword(UserRequest.UpdatePasswordDTO requestDTO, Long userId) {
        User user = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validatePasswordMatch(user, requestDTO.curPassword());
        validateConfirmPasswordMatch(requestDTO.curPassword(), requestDTO.newPasswordConfirm());

        user.updatePassword(passwordEncoder.encode(requestDTO.newPassword()));
    }

    public UserResponse.ProfileDTO findProfile(Long userId) {
        User user = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return toProfileDTO(user);
    }

    @Transactional
    public void updateProfile(UserRequest.UpdateProfileDTO requestDTO, Long userId) {
        User user = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validateNickname(user, requestDTO.nickName());
        user.updateProfile(requestDTO.nickName(), requestDTO.province(), requestDTO.district(), requestDTO.subDistrict(), requestDTO.profileURL());
    }

    public TokenResponse updateAccessToken(String refreshToken) {
        jwtUtils.validateTokenFormat(refreshToken);

        Long userId = jwtUtils.extractUserId(refreshToken);
        User user = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return createAccessToken(user);
    }

    // 게시글, 댓글, 좋아요은 남겨둔다. (정책에 따라 변경 가능)
    @Transactional
    public void withdrawMember(Long userId) {
        User user = userRepository.findByIdWithRemoved(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validateUserNotExited(user);
        checkIsGroupCreator(user);

        deleteUserRelatedData(userId);
        deleteUserAssociations(userId);
        userCacheService.deleteUserTokens(userId);
        user.deactivateUser();

        userRepository.markAsRemovedById(userId);
    }

    public UserResponse.ValidateAccessTokenDTO validateAccessToken(String accessToken) {
        jwtUtils.validateTokenFormat(accessToken);

        Long userId = jwtUtils.extractUserId(accessToken);
        userCacheService.validateAccessToken(accessToken, userId);

        String profile = userRepository.findProfileById(userId).orElse(null);
        return new UserResponse.ValidateAccessTokenDTO(profile);
    }

    public UserResponse.FindCommunityRecord findCommunityStats(Long userId) {
        User user = userRepository.findNonWithdrawnById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        List<PostTypeCountDTO> postTypeCounts = postRepository.countByUserIdAndType(userId, List.of(PostType.ADOPTION, PostType.FOSTERING, PostType.QUESTION, PostType.ANSWER));
        Map<PostType, Long> postCountMap = postTypeCounts.stream()
                .collect(Collectors.toMap(PostTypeCountDTO::postType, PostTypeCountDTO::count));

        Long adoptionNum = postCountMap.getOrDefault(PostType.ADOPTION, 0L);
        Long fosteringNum = postCountMap.getOrDefault(PostType.FOSTERING, 0L);
        Long questionNum = postCountMap.getOrDefault(PostType.QUESTION, 0L);
        Long answerNum = postCountMap.getOrDefault(PostType.ANSWER, 0L);
        Long commentNum = commentRepository.countByUserId(userId);

        return new UserResponse.FindCommunityRecord(user.getNickname(), user.getEmail(), adoptionNum + fosteringNum, commentNum, questionNum, answerNum);
    }

    public LoginResult processSocialLogin(String email, HttpServletRequest request) {
        Optional<User> userOP = userRepository.findByEmailWithRemoved(email);
        if (userOP.isEmpty()) {
            return new LoginResult(email, null, null, false);
        }

        User user = userOP.get();
        validateUserNotExited(user);
        validateUserActive(user);
        checkNotLocalJoined(user);

        recordLoginAttempt(user, request);

        return createToken(user);
    }

    private void validateLoginAttempts(User user) {
        long dailyLoginFailures = userCacheService.getDailyLoginFailures(user.getId());
        if (dailyLoginFailures >= DAILY_FAILURE_LIMIT) {
            throw new CustomException(ExceptionCode.ACCOUNT_LOCKED);
        }

        long currentLoginFailures = userCacheService.getCurrentLoginFailures(user.getId());
        if (currentLoginFailures >= CURRENT_FAILURE_LIMIT) {
            throw new CustomException(ExceptionCode.LOGIN_ATTEMPT_EXCEEDED);
        }
    }

    private void handleLoginFailures(User user) {
        long currentFailures = userCacheService.incrementCurrentLoginFailures(user.getId());

        if (currentFailures >= 3L) {
            long dailyFailures = userCacheService.incrementDailyLoginFailures(user);

            if (dailyFailures == 3L) {
                emailService.sendMail(user.getEmail(), ACCOUNT_SUSPENSION.getSubject(), MAIL_TEMPLATE_FOR_LOCK_ACCOUNT, new BlankTemplate());
            }
            throw new CustomException(ExceptionCode.LOGIN_ATTEMPT_EXCEEDED);
        }
    }

    private void infoLoginFail(User user) {
        Long currentLoginFailures = userCacheService.getCurrentLoginFailures(user.getId());
        String message = String.format("로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해 주세요. (%d회 실패)", currentLoginFailures);
        throw new CustomException(ExceptionCode.USER_ACCOUNT_WRONG, message);
    }

    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder()
                .append(pickRandomChar(random, 8, 0))  // 특수 문자
                .append(pickRandomChar(random, 10, 8)) // 숫자
                .append(pickRandomChar(random, 26, 18)) // 대문자
                .append(pickRandomChar(random, 26, 44)); // 소문자

        for (int i = 4; i < 8; i++) {
            password.append(pickRandomChar(random, ALL_CHARS.length(), 0));  // 나머지 문자 랜덤 추가
        }

        return shufflePassword(password);
    }

    private char pickRandomChar(SecureRandom random, int range, int offset) {
        return ALL_CHARS.charAt(random.nextInt(range) + offset);
    }

    private String shufflePassword(StringBuilder password) {
        List<Character> characters = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(characters);
        return characters.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private void updateNewPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND)
        );

        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    private LoginResult createToken(User user) {
        String accessToken = jwtUtils.createAccessToken(user);
        String refreshToken = jwtUtils.createRefreshToken(user);

        LoginResult loginResult = new LoginResult(null, accessToken, refreshToken, true);
        userCacheService.storeUserTokens(user.getId(), loginResult);

        return loginResult;
    }

    private TokenResponse createAccessToken(User user) {
        String refreshToken = userCacheService.getValidRefreshToken(user.getId());
        String accessToken = jwtUtils.createAccessToken(user);
        userCacheService.storeAccessToken(user.getId(), accessToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    private void checkNotLocalJoined(User user) {
        if (user.isLocalJoined()) {
            throw new CustomException(ExceptionCode.JOINED_BY_LOCAL);
        }
    }

    private void setAlarmQueue(User user) {
        String queueName = USER_QUEUE_PREFIX + user.getId();
        String listenerId = USER_QUEUE_PREFIX + user.getId();

        rabbitMqUtils.bindDirectExchangeToQueue(ALARM_EXCHANGE, queueName);
        rabbitMqUtils.registerAlarmListener(listenerId, queueName);
    }

    private void checkIsGroupCreator(User user) {
        groupUserRepository.findAllByUser(user)
                .stream()
                .filter(GroupUser::isCreator)
                .findFirst()
                .ifPresent(groupUser -> {
                    throw new CustomException(ExceptionCode.CREATOR_CANT_EXIT);
                });
    }

    private void validateConfirmPasswordMatch(String password, String confirmPassword) {
        if (!password.equals(confirmPassword))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);
    }

    private void validatePasswordMatch(User user, String inputPassword) {
        if (!passwordEncoder.matches(user.getPassword(), inputPassword))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);
    }

    private void checkEmailNotRegistered(String email) {
        userRepository.findAuthProviderByEmail(email).ifPresent(authProvider -> {
            if (authProvider == AuthProvider.LOCAL) {
                throw new CustomException(ExceptionCode.JOINED_BY_LOCAL);
            }
            if (authProvider == AuthProvider.GOOGLE || authProvider == AuthProvider.KAKAO) {
                throw new CustomException(ExceptionCode.JOINED_BY_SOCIAL);
            }
        });
    }

    private void validateNicknameUniqueness(String nickName) {
        if (userRepository.existsByNicknameWithRemoved(nickName))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);
    }

    private void validateUserNotExited(User user) {
        if (user.isExitMember()) {
            throw new CustomException(ExceptionCode.USER_ALREADY_EXIT);
        }
    }

    private boolean isPasswordUnmatched(User user, String inputPassword) {
        return !passwordEncoder.matches(inputPassword, user.getPassword());
    }

    private void validateUserActive(User user) {
        if (user.isUnActive()) {
            throw new CustomException(ExceptionCode.USER_SUSPENDED);
        }
    }

    private void deleteUserRelatedData(Long userId) {
        alarmRepository.deleteByUserId(userId);
        visitRepository.deleteByUserId(userId);
        loginAttemptRepository.deleteByUserId(userId);
    }

    private void deleteUserAssociations(Long userId) {
        postLikeRepository.deleteByUserId(userId);
        commentLikeRepository.deleteByUserId(userId);
        favoriteAnimalRepository.deleteAllByUserId(userId);
        favoriteGroupRepository.deleteByGroupId(userId);
        chatUserRepository.deleteByUserId(userId);
        deleteAllGroupUserData(userId);
        deleteAllMeetingUserData(userId);
    }

    private void deleteAllMeetingUserData(Long userId) {
        meetingUserRepository.findByUserIdWithMeeting(userId)
                .forEach(meetingUser -> {
                            meetingUser.getMeeting().decrementParticipantCount();
                            meetingUserRepository.delete(meetingUser);
                        }
                );
    }

    private void deleteAllGroupUserData(Long userId) {
        groupUserRepository.findByUserIdWithGroup(userId)
                .forEach(groupUser -> {
                            groupUser.getGroup().decrementParticipantNum();
                            groupUserRepository.delete(groupUser);
                        }
                );
    }

    public void recordLoginAttempt(User user, HttpServletRequest request) {
        String clientIp = getClientIP(request);
        String userAgent = request.getHeader(USER_AGENT_HEADER);

        LoginAttempt attempt = LoginAttempt.builder()
                .user(user)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();

        loginAttemptRepository.save(attempt);
    }

    private String getClientIP(HttpServletRequest request) {
        // IP_HEADER_CANDIDATES에 IP 주소를 얻기 위해 참조할 수 있는 헤더 이름 목록 존재
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);

            // 해당 헤더가 비어 있지 않고, "unknown"이라는 값이 아닌 경우에만 해당 IP를 반환
            if (ip != null && ip.length() != 0 && !UNKNOWN.equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    private void validateNickname(User user, String newNickname) {
        if (user.isNickNameUnequal(newNickname) // 현재 닉네임을 유지하고 있으면 굳이 DB까지 접근해서 검증 필요 X
                && userRepository.existsByNicknameWithRemoved(newNickname)) {
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);
        }
    }

    private void setUserStatus(User user) {
        UserStatus status = UserStatus.builder()
                .user(user)
                .isActive(true)
                .build();

        userStatusRepository.save(status);
        user.updateStatus(status);
    }
}