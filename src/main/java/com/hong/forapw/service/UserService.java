package com.hong.forapw.service;

import com.hong.forapw.controller.dto.GoogleOauthDTO;
import com.hong.forapw.controller.dto.KakaoOauthDTO;
import com.hong.forapw.controller.dto.UserRequest;
import com.hong.forapw.controller.dto.UserResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.authentication.LoginAttempt;
import com.hong.forapw.domain.group.GroupUser;
import com.hong.forapw.domain.inquiry.Inquiry;
import com.hong.forapw.domain.inquiry.InquiryStatus;
import com.hong.forapw.domain.post.PostType;
import com.hong.forapw.domain.user.AuthProvider;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.domain.user.UserRole;
import com.hong.forapw.domain.user.UserStatus;
import com.hong.forapw.repository.alarm.AlarmRepository;
import com.hong.forapw.repository.animal.FavoriteAnimalRepository;
import com.hong.forapw.repository.authentication.LoginAttemptRepository;
import com.hong.forapw.repository.authentication.VisitRepository;
import com.hong.forapw.repository.chat.ChatUserRepository;
import com.hong.forapw.repository.group.FavoriteGroupRepository;
import com.hong.forapw.repository.group.GroupUserRepository;
import com.hong.forapw.repository.group.MeetingUserRepository;
import com.hong.forapw.repository.inquiry.InquiryRepository;
import com.hong.forapw.repository.post.CommentLikeRepository;
import com.hong.forapw.repository.post.CommentRepository;
import com.hong.forapw.repository.post.PostLikeRepository;
import com.hong.forapw.repository.post.PostRepository;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.repository.UserStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hong.forapw.core.security.JWTProvider;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.core.security.JWTProvider.createRefreshTokenCookie;
import static com.hong.forapw.core.utils.UriUtils.buildRedirectUri;
import static com.hong.forapw.service.EmailService.generateVerificationCode;
import static com.hong.forapw.core.utils.MailTemplate.ACCOUNT_SUSPENSION;
import static com.hong.forapw.core.utils.MailTemplate.VERIFICATION_CODE;

@Service
@RequiredArgsConstructor
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
    private final InquiryRepository inquiryRepository;
    private final PostRepository postRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserStatusRepository userStatusRepository;
    private final VisitRepository visitRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final RedisService redisService;
    private final WebClient webClient;
    private final BrokerService brokerService;
    private final EntityManager entityManager;
    private final EmailService emailService;

    @Value("${kakao.key}")
    private String kakaoApiKey;

    @Value("${kakao.oauth.token.uri}")
    private String kakaoTokenUri;

    @Value("${kakao.oauth.userInfo.uri}")
    private String kakaoUserInfoUri;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.oauth.token.uri}")
    private String googleTokenUri;

    @Value("${google.client.passowrd}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect.uri}")
    private String googleRedirectUri;

    @Value("${google.oauth.userInfo.uri}")
    private String googleUserInfoUri;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPwd;

    @Value("${social.join.redirect.uri}")
    private String redirectJoinUri;

    @Value("${social.home.redirect.uri}")
    private String redirectHomeUri;

    @Value("${social.login.redirect.uri}")
    private String redirectLoginUri;

    private static final String ADMIN_NAME = "admin";
    private static final String MAIL_TEMPLATE_FOR_CODE = "verification_code_email.html";
    private static final String MAIL_TEMPLATE_FOR_LOCK_ACCOUNT = "lock_account.html";
    private static final String EMAIL_CODE_KEY_PREFIX = "code:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refreshToken";
    private static final String ACCESS_TOKEN_KEY_PREFIX = "accessToken";
    private static final String EMAIL = "email";
    private static final String LOGIN_FAIL_DAILY_KEY_PREFIX = "loginFailDaily";
    private static final String LOGIN_FAIL_CURRENT_KEY_PREFIX = "loginFail";
    private static final String CODE_TO_EMAIL_KEY_PREFIX = "codeToEmail";
    private static final String USER_QUEUE_PREFIX = "user.";
    private static final String ALARM_EXCHANGE = "alarm.exchange";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
    private static final String UNKNOWN = "unknown";
    private static final String[] IP_HEADER_CANDIDATES = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
    private static final String CODE_TYPE_RECOVERY = "recovery";
    private static final long VERIFICATION_CODE_EXPIRATION_MS = 175 * 1000L;
    private static final String MODEL_KEY_CODE = "code";
    private static final String QUERY_PARAM_EMAIL = "email";
    private static final String QUERY_PARAM_AUTH_PROVIDER = "authProvider";
    private static final String QUERY_PARAM_ACCESS_TOKEN = "accessToken";

    // 테스트 기간에만 사용하고, 운영에는 사용 X
    @Transactional
    public void initSuperAdmin() {
        if (!userRepository.existsByNickname(ADMIN_NAME)) {
            User admin = User.builder()
                    .email(adminEmail)
                    .name(ADMIN_NAME)
                    .nickName(ADMIN_NAME)
                    .password(passwordEncoder.encode(adminPwd))
                    .role(UserRole.SUPER)
                    .build();
            userRepository.save(admin);

            UserStatus status = UserStatus.builder()
                    .user(admin)
                    .isActive(true)
                    .build();
            userStatusRepository.save(status);
            admin.updateStatus(status);

            setAlarmQueue(admin);
        }
    }

    @Transactional
    public Map<String, String> login(UserRequest.LoginDTO requestDTO, HttpServletRequest request) {
        User user = userRepository.findByEmailWithRemoved(requestDTO.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_ACCOUNT_WRONG)
        );

        checkAlreadyExit(user);
        checkIsActiveMember(user);
        Long loginFailNum = checkLoginFailures(user);

        if (isPasswordUnmatched(user, requestDTO.password())) {
            infoLoginFail(user, loginFailNum);
        }

        recordLoginAttempt(user, request);

        return createToken(user);
    }

    @Transactional
    public Map<String, String> kakaoLogin(String code, HttpServletRequest request) {
        KakaoOauthDTO.TokenDTO token = getKakaoToken(code);
        KakaoOauthDTO.UserInfoDTO userInfo = getKakaoUserInfo(token.access_token());
        String email = userInfo.kakao_account().email();

        return processSocialLogin(email, request);
    }

    @Transactional
    public Map<String, String> googleLogin(String code, HttpServletRequest request) {
        GoogleOauthDTO.TokenDTO token = getGoogleToken(code);
        GoogleOauthDTO.UserInfoDTO userInfoDTO = getGoogleUserInfo(token.access_token());
        String email = userInfoDTO.email();

        return processSocialLogin(email, request);
    }

    @Transactional
    public void join(UserRequest.JoinDTO requestDTO) {
        checkConfirmPasswordCorrect(requestDTO.password(), requestDTO.passwordConfirm());
        checkAlreadyJoin(requestDTO.email());
        checkDuplicateNickname(requestDTO.nickName());

        User user = User.builder()
                .name(requestDTO.name())
                .nickName(requestDTO.nickName())
                .email(requestDTO.email())
                .password(passwordEncoder.encode(requestDTO.password()))
                .role(requestDTO.isShelterOwns() ? UserRole.SHELTER : UserRole.USER) // 보호소가 관리하는 계정이면 Role은 SHELTER
                .profileURL(requestDTO.profileURL())
                .province(requestDTO.province())
                .district(requestDTO.district())
                .subDistrict(requestDTO.subDistrict())
                .authProvider(AuthProvider.LOCAL)
                .isMarketingAgreed(requestDTO.isMarketingAgreed())
                .build();
        userRepository.save(user);

        setUserStatus(user);
        setAlarmQueue(user);
    }

    @Transactional
    public void socialJoin(UserRequest.SocialJoinDTO requestDTO) {
        checkAlreadyJoin(requestDTO.email());
        checkDuplicateNickname(requestDTO.nickName());

        User user = User.builder()
                .name(requestDTO.name())
                .nickName(requestDTO.nickName())
                .email(requestDTO.email())
                .password(passwordEncoder.encode(generatePassword())) // 임의의 비밀번호로 생성
                .role(requestDTO.isShelterOwns() ? UserRole.SHELTER : UserRole.USER)
                .profileURL(requestDTO.profileURL())
                .province(requestDTO.province())
                .district(requestDTO.district())
                .subDistrict(requestDTO.subDistrict())
                .authProvider(requestDTO.authProvider())
                .isMarketingAgreed(requestDTO.isMarketingAgreed())
                .build();
        userRepository.save(user);

        setUserStatus(user);
        setAlarmQueue(user);
    }

    @Transactional(readOnly = true)
    public UserResponse.CheckEmailExistDTO checkEmailExist(String email) {
        boolean isValid = !userRepository.existsByEmailWithRemoved(email);
        return new UserResponse.CheckEmailExistDTO(isValid);
    }

    @Async
    public void sendCodeByEmail(String email, String codeType) {
        checkAlreadySendCode(email, codeType);

        String verificationCode = generateVerificationCode();
        storeVerificationCode(email, codeType, verificationCode);

        Map<String, Object> templateModel = createTemplateModel(verificationCode);
        emailService.sendMail(email, VERIFICATION_CODE.getSubject(), MAIL_TEMPLATE_FOR_CODE, templateModel);
    }

    public UserResponse.VerifyEmailCodeDTO verifyCode(UserRequest.VerifyCodeDTO requestDTO, String codeType) {
        if (redisService.isNotStoredValue(getCodeTypeKey(codeType), requestDTO.email(), requestDTO.code()))
            return new UserResponse.VerifyEmailCodeDTO(false);

        cacheVerificationInfoIfRecovery(requestDTO, codeType);

        return new UserResponse.VerifyEmailCodeDTO(true);
    }

    @Transactional
    public UserResponse.CheckNickNameDTO checkNickName(UserRequest.CheckNickDTO requestDTO) {
        boolean isDuplicate = userRepository.existsByNicknameWithRemoved(requestDTO.nickName());
        return new UserResponse.CheckNickNameDTO(isDuplicate);
    }

    @Transactional(readOnly = true)
    public UserResponse.CheckLocalAccountExistDTO checkLocalAccountExist(UserRequest.EmailDTO requestDTO) {
        return userRepository.findByEmail(requestDTO.email())
                .map(user -> new UserResponse.CheckLocalAccountExistDTO(true, user.isLocalJoined()))
                .orElse(new UserResponse.CheckLocalAccountExistDTO(false, false));
    }

    @Transactional(readOnly = true)
    public UserResponse.CheckAccountExistDTO checkAccountExist(UserRequest.EmailDTO requestDTO) {
        boolean isValid = userRepository.existsByEmail(requestDTO.email());
        return new UserResponse.CheckAccountExistDTO(isValid);
    }

    @Transactional
    public void resetPassword(UserRequest.ResetPasswordDTO requestDTO) {
        String email = getEmailByVerificationCode(requestDTO);
        if (email == null) {
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }

        redisService.removeValue(CODE_TO_EMAIL_KEY_PREFIX, requestDTO.code());
        updateNewPassword(email, requestDTO.newPassword());
    }

    // 재설정 화면에서 실시간으로 일치여부를 확인하기 위해 사용
    @Transactional
    public UserResponse.VerifyPasswordDTO verifyPassword(UserRequest.CurPasswordDTO requestDTO, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        if (isPasswordUnmatched(user, requestDTO.password())) {
            return new UserResponse.VerifyPasswordDTO(false);
        }

        return new UserResponse.VerifyPasswordDTO(true);
    }

    @Transactional
    public void updatePassword(UserRequest.UpdatePasswordDTO requestDTO, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        checkIsPasswordMatched(user, requestDTO.curPassword());
        checkConfirmPasswordCorrect(requestDTO.curPassword(), requestDTO.newPasswordConfirm());

        user.updatePassword(passwordEncoder.encode(requestDTO.newPassword()));
    }

    @Transactional(readOnly = true)
    public UserResponse.ProfileDTO findProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return new UserResponse.ProfileDTO(user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getProvince(),
                user.getDistrict(),
                user.getSubDistrict(),
                user.getProfileURL(),
                user.isSocialJoined(),
                user.isShelterOwns(),
                user.isMarketingAgreed());
    }

    @Transactional
    public void updateProfile(UserRequest.UpdateProfileDTO requestDTO, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validateNickname(user, requestDTO.nickName());
        user.updateProfile(
                requestDTO.nickName(),
                requestDTO.province(),
                requestDTO.district(),
                requestDTO.subDistrict(),
                requestDTO.profileURL()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, String> updateAccessToken(String refreshToken) {
        checkTokenFormat(refreshToken);

        Long userId = JWTProvider.extractUserIdFromToken(refreshToken);
        checkIsAccessTokenStored(userId);

        User user = userRepository.findById(userId).orElseThrow(
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

        checkAlreadyExit(user);
        checkIsGroupCreator(user);

        deleteUserRelatedData(userId);
        deleteUserAssociations(userId);
        deleteTokenInSession(userId);
        user.deactivateUser();

        userRepository.deleteById(userId);
    }

    // 탈퇴한지 6개월 지난 유저 데이터 삭제 (매일 자정 30분에 실행)
    @Transactional
    @Scheduled(cron = "0 30 0 * * ?")
    public void deleteExpiredUserData() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        userRepository.hardDeleteRemovedBefore(sixMonthsAgo);
    }

    @Transactional
    public UserResponse.SubmitInquiryDTO submitInquiry(UserRequest.SubmitInquiry requestDTO, Long userId) {
        User user = entityManager.getReference(User.class, userId);
        Inquiry inquiry = Inquiry.builder()
                .questioner(user)
                .title(requestDTO.title())
                .description(requestDTO.description())
                .contactMail(requestDTO.contactMail())
                .status(InquiryStatus.PROCESSING)
                .type(requestDTO.inquiryType())
                .imageURL(requestDTO.imageURL())
                .build();

        inquiryRepository.save(inquiry);

        return new UserResponse.SubmitInquiryDTO(inquiry.getId());
    }

    @Transactional
    public void updateInquiry(UserRequest.UpdateInquiry requestDTO, Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        checkAuthority(userId, inquiry.getQuestioner());
        inquiry.updateInquiry(requestDTO.title(), requestDTO.description(), requestDTO.contactMail());
    }

    @Transactional(readOnly = true)
    public UserResponse.FindInquiryListDTO findInquiryList(Long userId) {
        List<Inquiry> customerInquiries = inquiryRepository.findAllByQuestionerId(userId);

        List<UserResponse.InquiryDTO> inquiryDTOS = customerInquiries.stream()
                .map(inquiry -> {
                    UserResponse.AnswerDTO answerDTO = null;

                    if (inquiry.getAnswer() != null) {
                        answerDTO = new UserResponse.AnswerDTO(
                                inquiry.getAnswer(),
                                inquiry.getAnswerer().getName()
                        );
                    }

                    return new UserResponse.InquiryDTO(
                            inquiry.getId(),
                            inquiry.getTitle(),
                            inquiry.getDescription(),
                            inquiry.getStatus(),
                            inquiry.getImageURL(),
                            inquiry.getType(),
                            inquiry.getCreatedDate(),
                            answerDTO);
                })
                .toList();

        return new UserResponse.FindInquiryListDTO(inquiryDTOS);
    }

    @Transactional(readOnly = true)
    public UserResponse.ValidateAccessTokenDTO validateAccessToken(@CookieValue String accessToken) {
        checkTokenFormat(accessToken);

        Long userIdFromToken = JWTProvider.extractUserIdFromToken(accessToken);
        if (!redisService.isStoredValue(ACCESS_TOKEN_KEY_PREFIX, String.valueOf(userIdFromToken), accessToken)) {
            throw new CustomException(ExceptionCode.ACCESS_TOKEN_WRONG);
        }

        String profile = userRepository.findProfileById(userIdFromToken).orElse(null);

        return new UserResponse.ValidateAccessTokenDTO(profile);
    }

    @Transactional(readOnly = true)
    public UserResponse.FindCommunityRecord findCommunityStats(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        List<Object[]> postTypeCounts = postRepository.countByUserIdAndType(userId, List.of(PostType.ADOPTION, PostType.FOSTERING, PostType.QUESTION, PostType.ANSWER));
        Map<PostType, Long> postCountMap = postTypeCounts.stream()
                .collect(Collectors.toMap(
                        result -> (PostType) result[0],
                        result -> (Long) result[1]
                ));

        Long adoptionNum = postCountMap.getOrDefault(PostType.ADOPTION, 0L);
        Long fosteringNum = postCountMap.getOrDefault(PostType.FOSTERING, 0L);
        Long questionNum = postCountMap.getOrDefault(PostType.QUESTION, 0L);
        Long answerNum = postCountMap.getOrDefault(PostType.ANSWER, 0L);
        Long commentNum = commentRepository.countByUserId(userId);

        return new UserResponse.FindCommunityRecord(user.getNickname(), user.getEmail(), adoptionNum + fosteringNum, commentNum, questionNum, answerNum);
    }

    @Transactional
    public void checkAlreadySend(String email, String codeType) {
        if (!userRepository.existsByEmail(email)) {
            throw new CustomException(ExceptionCode.CODE_NOT_SENDED);
        }

        if (redisService.isValueExist(EMAIL_CODE_KEY_PREFIX + codeType, email)) {
            throw new CustomException(ExceptionCode.CODE_ALREADY_SENDED);
        }
    }

    public void processOAuthRedirect(Map<String, String> tokenOrEmail, String authProvider, HttpServletResponse response) {
        try {
            String redirectUri = determineRedirectUri(tokenOrEmail, authProvider, response);
            response.sendRedirect(redirectUri);
        } catch (IOException e) {
            log.error("소셜 로그인 증 리다이렉트 에러 발생", e);
            throw new CustomException(ExceptionCode.REDIRECT_FAILED);
        }
    }

    private Long checkLoginFailures(User user) {
        Long dailyFailureCount = redisService.getValueInLong(LOGIN_FAIL_DAILY_KEY_PREFIX, user.getId().toString());
        if (dailyFailureCount >= 3L) {
            throw new CustomException(ExceptionCode.ACCOUNT_LOCKED);
        }

        Long currentFailureCount = redisService.getValueInLong(LOGIN_FAIL_CURRENT_KEY_PREFIX, user.getId().toString());
        if (currentFailureCount >= 3L) {
            handleDailyFailureLimitExceeded(user, dailyFailureCount);
            throw new CustomException(ExceptionCode.LOGIN_ATTEMPT_EXCEEDED);
        }

        return currentFailureCount;
    }

    private void handleDailyFailureLimitExceeded(User user, Long dailyFailureCount) {
        dailyFailureCount++;
        updateDailyFailureCount(user, dailyFailureCount);

        if (dailyFailureCount == 3L) {
            sendAccountSuspensionEmail(user);
        }
    }

    private void updateDailyFailureCount(User user, Long dailyFailureCount) {
        redisService.storeValue(LOGIN_FAIL_DAILY_KEY_PREFIX, user.getId().toString(), dailyFailureCount.toString(), 86400000L);  // 24시간
    }

    private void sendAccountSuspensionEmail(User user) {
        emailService.sendMail(user.getEmail(), ACCOUNT_SUSPENSION.getSubject(), MAIL_TEMPLATE_FOR_LOCK_ACCOUNT, new HashMap<>());
    }

    private String determineRedirectUri(Map<String, String> tokenOrEmail, String authProvider, HttpServletResponse response) throws IOException {
        if (isNotJoined(tokenOrEmail)) {
            return buildRedirectUri(redirectJoinUri, Map.of(
                    QUERY_PARAM_EMAIL, tokenOrEmail.get(EMAIL),
                    QUERY_PARAM_AUTH_PROVIDER, authProvider
            ));
        } else if (isJoined(tokenOrEmail)) {
            response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokenOrEmail.get(REFRESH_TOKEN_KEY_PREFIX)));
            return buildRedirectUri(redirectHomeUri, Map.of(
                    QUERY_PARAM_ACCESS_TOKEN, tokenOrEmail.get(ACCESS_TOKEN_KEY_PREFIX),
                    QUERY_PARAM_AUTH_PROVIDER, authProvider
            ));
        } else {
            return buildRedirectUri(redirectLoginUri, Map.of(
                    QUERY_PARAM_AUTH_PROVIDER, authProvider
            ));
        }
    }

    private boolean isNotJoined(Map<String, String> tokenOrEmail) {
        return tokenOrEmail.get(EMAIL) != null;
    }

    private boolean isJoined(Map<String, String> tokenOrEmail) {
        return tokenOrEmail.get(ACCESS_TOKEN_KEY_PREFIX) != null;
    }

    private void cacheVerificationInfoIfRecovery(UserRequest.VerifyCodeDTO requestDTO, String codeType) {
        if (CODE_TYPE_RECOVERY.equals(codeType))
            redisService.storeValue(CODE_TO_EMAIL_KEY_PREFIX, requestDTO.code(), requestDTO.email(), 5 * 60 * 1000L);
    }

    // 알파벳, 숫자, 특수문자가 모두 포함되도록 해서 임시 비밀번호 생성
    private String generatePassword() {
        String specialChars = "!@#$%^&*";
        String numbers = "0123456789";
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String allChars = specialChars + numbers + upperCaseLetters + lowerCaseLetters;

        SecureRandom random = new SecureRandom();

        // 각 범주에서 랜덤하게 하나씩 선택
        StringBuilder password = new StringBuilder(8);
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
        password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));

        // 나머지 자리를 전체 문자 집합에서 선택
        for (int i = 4; i < 8; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // 최종 문자열 섞기
        List<Character> passwordChars = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(passwordChars);

        return passwordChars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private Map<String, Object> createTemplateModel(String verificationCode) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(MODEL_KEY_CODE, verificationCode);
        return templateModel;
    }

    private String getEmailByVerificationCode(UserRequest.ResetPasswordDTO requestDTO) {
        return redisService.getValueInString(CODE_TO_EMAIL_KEY_PREFIX, requestDTO.code());
    }

    private void updateNewPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND)
        );

        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    private void checkAlreadySendCode(String email, String codeType) {
        if (redisService.isValueExist(getCodeTypeKey(codeType), email)) {
            throw new CustomException(ExceptionCode.ALREADY_SEND_EMAIL);
        }
    }

    private void storeVerificationCode(String email, String codeType, String verificationCode) {
        redisService.storeValue(getCodeTypeKey(codeType), email, verificationCode, VERIFICATION_CODE_EXPIRATION_MS);
    }

    private String getCodeTypeKey(String codeType) {
        return UserService.EMAIL_CODE_KEY_PREFIX + codeType;
    }

    private Map<String, String> createToken(User user) {
        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = JWTProvider.createRefreshToken(user);

        redisService.storeValue(ACCESS_TOKEN_KEY_PREFIX, String.valueOf(user.getId()), accessToken, JWTProvider.ACCESS_EXP_MILLI);
        redisService.storeValue(REFRESH_TOKEN_KEY_PREFIX, String.valueOf(user.getId()), refreshToken, JWTProvider.REFRESH_EXP_MILLI);

        Map<String, String> tokens = new HashMap<>();
        tokens.put(ACCESS_TOKEN_KEY_PREFIX, accessToken);
        tokens.put(REFRESH_TOKEN_KEY_PREFIX, refreshToken);

        return tokens;
    }

    private Map<String, String> createAccessToken(User user) {
        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = redisService.getValueInString(REFRESH_TOKEN_KEY_PREFIX, String.valueOf(user.getId()));

        redisService.storeValue(ACCESS_TOKEN_KEY_PREFIX, String.valueOf(user.getId()), accessToken, JWTProvider.ACCESS_EXP_MILLI);

        Map<String, String> tokens = new HashMap<>();
        tokens.put(ACCESS_TOKEN_KEY_PREFIX, accessToken);
        tokens.put(REFRESH_TOKEN_KEY_PREFIX, refreshToken);

        return tokens;
    }

    private Map<String, String> processSocialLogin(String email, HttpServletRequest request) {
        Optional<User> userOP = userRepository.findByEmailWithRemoved(email);
        if (userOP.isEmpty()) {
            return createEmailMap(email);
        }

        User user = userOP.get();
        checkAlreadyExit(user);
        checkIsActiveMember(user);
        checkIsLocalJoined(user);

        recordLoginAttempt(user, request);

        return createToken(user);
    }

    private Map<String, String> createEmailMap(String email) {
        return Map.of("email", email);
    }

    private void checkIsLocalJoined(User user) {
        if (user.isLocalJoined()) {
            throw new CustomException(ExceptionCode.JOINED_BY_LOCAL);
        }
    }

    private KakaoOauthDTO.TokenDTO getKakaoToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", AUTH_CODE_GRANT_TYPE);
        formData.add("client_id", kakaoApiKey);
        formData.add("code", code);

        Mono<KakaoOauthDTO.TokenDTO> response = webClient.post()
                .uri(kakaoTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KakaoOauthDTO.TokenDTO.class);

        return response.block();
    }

    private KakaoOauthDTO.UserInfoDTO getKakaoUserInfo(String token) {
        Flux<KakaoOauthDTO.UserInfoDTO> response = webClient.get()
                .uri(kakaoUserInfoUri)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(KakaoOauthDTO.UserInfoDTO.class);

        return response.blockFirst();
    }

    private GoogleOauthDTO.TokenDTO getGoogleToken(String code) {
        String decode = URLDecoder.decode(code, StandardCharsets.UTF_8);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", decode);
        formData.add("client_id", googleClientId);
        formData.add("client_secret", googleClientSecret);
        formData.add("redirect_uri", googleRedirectUri);
        formData.add("grant_type", AUTH_CODE_GRANT_TYPE);

        Mono<GoogleOauthDTO.TokenDTO> response = webClient.post()
                .uri(googleTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(GoogleOauthDTO.TokenDTO.class);

        return response.block();
    }

    private GoogleOauthDTO.UserInfoDTO getGoogleUserInfo(String token) {
        Flux<GoogleOauthDTO.UserInfoDTO> response = webClient.get()
                .uri(googleUserInfoUri)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(GoogleOauthDTO.UserInfoDTO.class);

        return response.blockFirst();
    }

    private void setAlarmQueue(User user) {
        String queueName = USER_QUEUE_PREFIX + user.getId();
        String listenerId = USER_QUEUE_PREFIX + user.getId();

        brokerService.registerDirectExQueue(ALARM_EXCHANGE, queueName);
        brokerService.registerAlarmListener(listenerId, queueName);
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

    private void checkConfirmPasswordCorrect(String password, String confirmPassword) {
        if (!password.equals(confirmPassword))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);
    }

    private void checkIsPasswordMatched(User user, String inputPassword) {
        if (!passwordEncoder.matches(user.getPassword(), inputPassword))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);
    }

    private void checkAlreadyJoin(String email) {
        userRepository.findAuthProviderByEmail(email).ifPresent(authProvider -> {
            if (authProvider == AuthProvider.LOCAL) {
                throw new CustomException(ExceptionCode.JOINED_BY_LOCAL);
            }
            if (authProvider == AuthProvider.GOOGLE || authProvider == AuthProvider.KAKAO) {
                throw new CustomException(ExceptionCode.JOINED_BY_SOCIAL);
            }
        });
    }

    private void checkDuplicateNickname(String nickName) {
        if (userRepository.existsByNicknameWithRemoved(nickName))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);
    }

    private void checkAuthority(Long userId, User writer) {
        if (writer.isNotSameUser(userId)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkAlreadyExit(User user) {
        if (user.isExitMember()) {
            throw new CustomException(ExceptionCode.USER_ALREADY_EXIT);
        }
    }

    private void checkIsAccessTokenStored(Long userId) {
        if (redisService.isValueNotExist(REFRESH_TOKEN_KEY_PREFIX, String.valueOf(userId)))
            throw new CustomException(ExceptionCode.TOKEN_EXPIRED);
    }

    private boolean isPasswordUnmatched(User user, String inputPassword) {
        return !passwordEncoder.matches(inputPassword, user.getPassword());
    }

    private void infoLoginFail(User user, Long loginFailNum) {
        redisService.storeValue(LOGIN_FAIL_CURRENT_KEY_PREFIX, user.getId().toString(), Long.toString(++loginFailNum), 300000L); // 5분
        String message = String.format("로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해 주세요. (%d회 실패)", loginFailNum);
        throw new CustomException(ExceptionCode.USER_ACCOUNT_WRONG, message);
    }

    private void checkIsActiveMember(User user) {
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
                            meetingUser.getMeeting().decrementParticipantNum();
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

    private void deleteTokenInSession(Long userId) {
        redisService.removeValue(ACCESS_TOKEN_KEY_PREFIX, userId.toString());
        redisService.removeValue(REFRESH_TOKEN_KEY_PREFIX, userId.toString());
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

    private void checkTokenFormat(String refreshToken) {
        if (JWTProvider.isInvalidJwtFormat(refreshToken)) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }
    }
}