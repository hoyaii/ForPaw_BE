package com.hong.forapw.service;

import com.hong.forapw.controller.dto.GoogleOauthDTO;
import com.hong.forapw.controller.dto.KakaoOauthDTO;
import com.hong.forapw.controller.dto.UserRequest;
import com.hong.forapw.controller.dto.UserResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.authentication.LoginAttempt;
import com.hong.forapw.domain.group.GroupRole;
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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hong.forapw.core.utils.MailTemplate.ACCOUNT_SUSPENSION;
import static com.hong.forapw.core.utils.MailTemplate.VERIFICATION_CODE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    private final JavaMailSender mailSender;
    private final WebClient webClient;
    private final BrokerService brokerService;
    private final EntityManager entityManager;

    @Value("${spring.mail.username}")
    private String serviceMailAccount;

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

    private final SpringTemplateEngine templateEngine;
    private static final String ADMIN_NAME = "admin";
    private static final String MAIL_TEMPLATE_FOR_CODE = "verification_code_email.html";
    private static final String MAIL_TEMPLATE_FOR_LOCK_ACCOUNT = "lock_account.html";
    private static final String EMAIL_CODE_KEY_PREFIX = "code:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refreshToken";
    private static final String ACCESS_TOKEN_KEY_PREFIX = "accessToken";
    private static final String EMAIL = "email";
    private static final String LOGIN_FAIL_DAILY_KEY_PREFIX = "loginFailDaily";
    private static final String LOGIN_FAIL_KEY_PREFIX = "loginFail";
    private static final String CODE_TO_EMAIL_KEY_PREFIX = "codeToEmail";
    private static final String USER_QUEUE_PREFIX = "user.";
    private static final String ALARM_EXCHANGE = "alarm.exchange";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
    private static final String UNKNOWN = "unknown";
    private static final String UTF_EIGHT_ENCODING = "UTF-8";
    private static final String[] IP_HEADER_CANDIDATES = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
    private static final String CODE_TYPE_RECOVERY = "recovery";

    @Transactional
    public void initSuperAdmin(){
        // SuperAdmin이 등록되어 있지 않다면 등록
        if(!userRepository.existsByNickname(ADMIN_NAME)){
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
    public Map<String, String> login(UserRequest.LoginDTO requestDTO, HttpServletRequest request) throws MessagingException {
        User user = userRepository.findByEmailWithRemoved(requestDTO.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_ACCOUNT_WRONG)
        );

        checkIsExitMember(user);
        checkIsActiveMember(user);
        Long loginFailNum = checkLoginFailures(user);

        if(isPasswordUnmatched(requestDTO, user)){
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
    public Map<String, String> googleLogin(String code, HttpServletRequest request){
        // 구글 엑세스 토큰 획득
        GoogleOauthDTO.TokenDTO token = getGoogleToken(code);
        GoogleOauthDTO.UserInfoDTO userInfoDTO = getGoogleUserInfo(token.access_token());

        String email = userInfoDTO.email();

        return processSocialLogin(email, request);
    }

    @Transactional
    public void join(UserRequest.JoinDTO requestDTO){
        if (!requestDTO.password().equals(requestDTO.passwordConfirm()))
            throw new CustomException(ExceptionCode.USER_PASSWORD_WRONG);

        // 이미 가입된 계정인지 체크
        checkAlreadyJoin(requestDTO.email());

        // 중복된 닉네임 다시 체크 (프론트에서 체크하고 이중 체크)
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

        // 유저 상태 설정
        setUserStatus(user);

        // 알람 사용을 위한 설정
        setAlarmQueue(user);
    }

    @Transactional
    public void socialJoin(UserRequest.SocialJoinDTO requestDTO){
        // 악의적 접근 방지를 위한 이중 체크
        checkAlreadyJoin(requestDTO.email());

        // 중복된 닉네임 다시 체크 (프론트에서 체크하고 이중 체크)
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

        // 유저 상태 설정
        setUserStatus(user);

        // 알람 사용을 위한 설정
        setAlarmQueue(user);
    }

    @Transactional(readOnly = true)
    public UserResponse.CheckEmailExistDTO checkEmailExist(String email){
        boolean isValid = !userRepository.existsByEmailWithRemoved(email);
        return new UserResponse.CheckEmailExistDTO(isValid);
    }

    @Async
    public void sendCodeByEmail(String email, String codeType) throws MessagingException {
        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = generateVerificationCode();

        // 메일 전송 템플릿 보낼 데이터는 map에 담음
        Map<String, Object> model = new HashMap<>();
        model.put("code", verificationCode);

        sendMail(email, VERIFICATION_CODE.getSubject(), MAIL_TEMPLATE_FOR_CODE, model);

        redisService.storeValue(EMAIL_CODE_KEY_PREFIX + codeType, email, verificationCode, 175 * 1000L); // 3분 동안 유효
    }

    @Async
    public void sendCodeWithValidation(String email, String codeType, boolean isValid) throws MessagingException {
        // TTL 체크
        if(redisService.isDateExist(EMAIL_CODE_KEY_PREFIX + codeType, email)){
            throw new CustomException(ExceptionCode.ALREADY_SEND_EMAIL);
        }

        // 유효한 경우에만 메일 전송
        if(isValid){
            sendCodeByEmail(email, codeType);
        }
    }

    public UserResponse.VerifyEmailCodeDTO verifyCode(UserRequest.VerifyCodeDTO requestDTO, String codeType){
        // 레디스를 통해 해당 코드가 유효한지 확인
        if(!redisService.validateValue(EMAIL_CODE_KEY_PREFIX + codeType, requestDTO.email(), requestDTO.code()))
            return new UserResponse.VerifyEmailCodeDTO(false);

        // RECOVERY의 경우, resetPassword()에서 서버에 요청으로 이메일과 비밀번호를 동시에 보내지 않도록, 코드에 대한 이메일을 저장
        if(codeType.equals(CODE_TYPE_RECOVERY))
            redisService.storeValue(CODE_TO_EMAIL_KEY_PREFIX, requestDTO.code(), requestDTO.email(), 5 * 60 * 1000L);

        return new UserResponse.VerifyEmailCodeDTO(true);
    }

    @Transactional
    public UserResponse.CheckNickNameDTO checkNickName(UserRequest.CheckNickDTO requestDTO){
        boolean isDuplicate = userRepository.existsByNicknameWithRemoved(requestDTO.nickName());
        return new UserResponse.CheckNickNameDTO(isDuplicate);
    }

    @Transactional(readOnly = true)
    public UserResponse.CheckLocalAccountExistDTO checkLocalAccountExist(UserRequest.EmailDTO requestDTO){
        Optional<User> userOP = userRepository.findByEmail(requestDTO.email());
        boolean isValid = userOP.isPresent();
        boolean isLocal = false;

        if(isValid){
            isLocal = userOP.get().getAuthProvider().equals(AuthProvider.LOCAL);
        }

        return new UserResponse.CheckLocalAccountExistDTO(isValid, isLocal);
    }

    @Transactional(readOnly = true)
    public UserResponse.CheckAccountExistDTO checkAccountExist(UserRequest.EmailDTO requestDTO){
        boolean isValid = userRepository.existsByEmail(requestDTO.email());
        return new UserResponse.CheckAccountExistDTO(isValid);
    }

    @Transactional
    public void resetPassword(UserRequest.ResetPasswordDTO requestDTO){
        // 전송한 코드로 세션에서 해당 이메일을 꺼내옴 (비밀번호 재설정 시 코드 전송을 거침)
        String email = redisService.getValueInStr(CODE_TO_EMAIL_KEY_PREFIX, requestDTO.code());
        if(email == null){
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }

        // CODE_TO_EMAIL 키 삭제
        redisService.removeData(CODE_TO_EMAIL_KEY_PREFIX, requestDTO.code());

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND)
        );

        // 새로운 비밀번호로 업데이트
        user.updatePassword(passwordEncoder.encode(requestDTO.newPassword()));
    }

    // 재설정 화면에서 실시간으로 일치여부를 확인하기 위해 사용
    @Transactional
    public UserResponse.VerifyPasswordDTO verifyPassword(UserRequest.CurPasswordDTO requestDTO, Long userId){
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        if(!passwordEncoder.matches(requestDTO.password(), user.getPassword())){
            return new UserResponse.VerifyPasswordDTO(false);
        }

        return new UserResponse.VerifyPasswordDTO(true);
    }

    @Transactional
    public void updatePassword(UserRequest.UpdatePasswordDTO requestDTO, Long userId){
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 현재 비민번호 값이 맞는지 검증
        if(!passwordEncoder.matches(requestDTO.curPassword(), user.getPassword()))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);

        // 새 비밀번호와 새 비밀번호 확인값이 맞는지 검증
        if (!requestDTO.newPassword().equals(requestDTO.newPasswordConfirm()))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);

        user.updatePassword(passwordEncoder.encode(requestDTO.newPassword()));
    }

    @Transactional(readOnly = true)
    public UserResponse.ProfileDTO findProfile(Long userId){
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 소셜 회원가입으로 가입했는지 여부
        boolean isSocialJoined = !user.getAuthProvider().equals(AuthProvider.LOCAL);

        // 보호소에서 관리하는 계정인지 여부
        boolean isShelterOwns = user.getRole().equals(UserRole.SHELTER);

        return new UserResponse.ProfileDTO(user.getEmail(),
                user.getName(),
                user.getNickName(),
                user.getProvince(),
                user.getDistrict(),
                user.getSubDistrict(),
                user.getProfileURL(),
                isSocialJoined,
                isShelterOwns,
                user.isMarketingAgreed());
    }

    @Transactional
    public void updateProfile(UserRequest.UpdateProfileDTO requestDTO, Long userId){
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 닉네임 중복 체크 (현재 닉네임은 통과)
        if(!user.getNickName().equals(requestDTO.nickName()) && userRepository.existsByNicknameWithRemoved(requestDTO.nickName()))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);

        user.updateProfile(requestDTO.nickName(), requestDTO.province(), requestDTO.district(), requestDTO.subDistrict(), requestDTO.profileURL());
    }

    @Transactional
    public Map<String, String> updateAccessToken(String refreshToken){
        // 잘못된 토큰 형식인지 체크
        if(!JWTProvider.validateToken(refreshToken)) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }

        // 리프레쉬 토큰에서 추출한 userId
        Long userId = JWTProvider.getUserIdFromToken(refreshToken);

        // 리프레쉬 토큰 만료 여부 체크
        if(!redisService.isDateExist(REFRESH_TOKEN_KEY_PREFIX, String.valueOf(userId)))
            throw new CustomException(ExceptionCode.TOKEN_EXPIRED);

        // 유효하지 않는 유저의 토큰이면 에러 발생
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return createAccessToken(user);
    }

    // 게시글, 댓글, 좋아요은 남겨둔다. (정책에 따라 변경 가능)
    @Transactional
    public void withdrawMember(Long userId){
        User user = userRepository.findByIdWithRemoved(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 이미 탈퇴한 회원이면 예외
        if(user.getRemovedAt() != null){
            throw new CustomException(ExceptionCode.USER_ALREADY_EXIT);
        };

        // 그룹장 상태에서는 탈퇴 불가능
        groupUserRepository.findAllByUserId(userId)
                .forEach(groupUser -> {
                    groupUser.getGroupRole().equals(GroupRole.CREATOR);
                    throw new CustomException(ExceptionCode.CREATOR_CANT_EXIT);
                });

        // 알람 삭제
        alarmRepository.deleteByUserId(userId);

        // 방문 기록 삭제
        visitRepository.deleteByUserId(userId);

        // 로그인 기록 삭제
        loginAttemptRepository.deleteByUserId(userId);

        // 중간 테이블 역할의 엔티티 삭제
        postLikeRepository.deleteByUserId(userId);
        commentLikeRepository.deleteByUserId(userId);
        favoriteAnimalRepository.deleteAllByUserId(userId);
        favoriteGroupRepository.deleteByGroupId(userId);
        chatUserRepository.deleteByUserId(userId);
        groupUserRepository.findByUserIdWithGroup(userId)
                .forEach(groupUser -> {
                    groupUser.getGroup().decrementParticipantNum();
                    groupUserRepository.delete(groupUser);
                }
        );
        meetingUserRepository.findByUserIdWithMeeting(userId)
                .forEach(meetingUser -> {
                    meetingUser.getMeeting().decrementParticipantNum();
                    meetingUserRepository.delete(meetingUser);
                }
        );

        // 유저 상태 변경
        user.getStatus().updateIsActive(false);

        // 세션에 저장된 토큰 삭제
        redisService.removeData(ACCESS_TOKEN_KEY_PREFIX, userId.toString());
        redisService.removeData(REFRESH_TOKEN_KEY_PREFIX, userId.toString());

        // 유저 삭제 (soft delete 처리) => soft delete의 side effect 고려 해야함
        userRepository.deleteById(userId);
    }

    // 탈퇴한지 6개월 지난 유저 데이터 삭제 (매일 자정 30분에 실행)
    @Transactional
    @Scheduled(cron = "0 30 0 * * ?")
    public void deleteExpiredUserData(){
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minus(6, ChronoUnit.MONTHS);
        userRepository.hardDeleteRemovedBefore(sixMonthsAgo);
    }

    @Transactional
    public UserResponse.SubmitInquiryDTO submitInquiry(UserRequest.SubmitInquiry requestDTO, Long userId){
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
    public void updateInquiry(UserRequest.UpdateInquiry requestDTO, Long inquiryId, Long userId){
        // 존재하지 않는 문의면 에러
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        // 권한 체크
        checkWriterAuthority(userId, inquiry.getQuestioner());

        inquiry.updateCustomerInquiry(requestDTO.title(), requestDTO.description(), requestDTO.contactMail());
    }

    @Transactional(readOnly = true)
    public UserResponse.FindInquiryListDTO findInquiryList(Long userId){
        List<Inquiry> customerInquiries = inquiryRepository.findAllByQuestionerId(userId);

        List<UserResponse.InquiryDTO> inquiryDTOS = customerInquiries.stream()
                .map(inquiry -> {
                    UserResponse.AnswerDTO answerDTO = null;

                    if(inquiry.getAnswer() != null){
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
    public UserResponse.ValidateAccessTokenDTO validateAccessToken(@CookieValue String accessToken){
        // 잘못된 토큰 형식인지 체크
        if(!JWTProvider.validateToken(accessToken)) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }

        Long userIdFromToken = JWTProvider.getUserIdFromToken(accessToken);
        if(!redisService.validateValue(ACCESS_TOKEN_KEY_PREFIX, String.valueOf(userIdFromToken), accessToken)){
            throw new CustomException(ExceptionCode.ACCESS_TOKEN_WRONG);
        }

        String profile = userRepository.findProfileById(userIdFromToken).orElse(null);

        return new UserResponse.ValidateAccessTokenDTO(profile);
    }

    @Transactional(readOnly = true)
    public UserResponse.FindCommunityRecord findCommunityStats(Long userId){
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

        return new UserResponse.FindCommunityRecord(user.getNickName(), user.getEmail(), adoptionNum + fosteringNum, commentNum, questionNum, answerNum);
    }

    @Transactional
    public void checkAlreadySend(String email, String codeType){
        if(!userRepository.existsByEmail(email)){
            throw new CustomException(ExceptionCode.CODE_NOT_SENDED);
        }

        if(redisService.isDateExist(EMAIL_CODE_KEY_PREFIX + codeType, email)){
            throw new CustomException(ExceptionCode.CODE_ALREADY_SENDED);
        }
    }

    private Long checkLoginFailures(User user) throws MessagingException {
        // 하루 동안 5분 잠금이 세 번을 초과하면, 24시간 동안 로그인이 불가
        Long loginFailNumDaily = redisService.getValueInLong(LOGIN_FAIL_DAILY_KEY_PREFIX, user.getId().toString());
        if (loginFailNumDaily >= 3L) {
            throw new CustomException(ExceptionCode.ACCOUNT_LOCKED);
        }

        // 로그이 실패 횟수가 3회 이상이면, 5분 동안 로그인 불가
        Long loginFailNum = redisService.getValueInLong(LOGIN_FAIL_KEY_PREFIX, user.getId().toString());
        if(loginFailNum >= 3L) {
            loginFailNumDaily++;
            redisService.storeValue(LOGIN_FAIL_DAILY_KEY_PREFIX, user.getId().toString(), loginFailNumDaily.toString(), 86400000L);  // 24시간

            if(loginFailNumDaily == 3L){
                sendMail(user.getEmail(), ACCOUNT_SUSPENSION.getSubject(), MAIL_TEMPLATE_FOR_LOCK_ACCOUNT, new HashMap<>());
            }

            throw new CustomException(ExceptionCode.LOGIN_ATTEMPT_EXCEEDED);
        }

        return loginFailNum;
    }

    @Async
    public void sendMail(String toEmail, String subject, String templateName, Map<String, Object> templateModel) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_EIGHT_ENCODING);

        // 템플릿 설정
        Context context = new Context();
        templateModel.forEach(context::setVariable);
        String htmlContent = templateEngine.process(templateName, context);
        helper.setText(htmlContent, true);

        helper.setFrom(serviceMailAccount);
        helper.setTo(toEmail);
        helper.setSubject(subject);

        mailSender.send(message);
    }

    public void processOAuthRedirect(Map<String, String> tokenOrEmail, String authProvider, HttpServletResponse response) throws IOException {
        String redirectUri;
        if(tokenOrEmail.get(EMAIL) != null) {
            redirectUri = UriComponentsBuilder.fromUriString(redirectJoinUri)
                    .queryParam("email", URLEncoder.encode(tokenOrEmail.get(EMAIL), StandardCharsets.UTF_8))
                    .queryParam("authProvider", URLEncoder.encode(authProvider, StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
        } else if(tokenOrEmail.get(ACCESS_TOKEN_KEY_PREFIX) != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokenOrEmail.get(REFRESH_TOKEN_KEY_PREFIX)));
            redirectUri = UriComponentsBuilder.fromUriString(redirectHomeUri)
                    .queryParam("accessToken", URLEncoder.encode(tokenOrEmail.get(ACCESS_TOKEN_KEY_PREFIX), StandardCharsets.UTF_8))
                    .queryParam("authProvider", URLEncoder.encode(authProvider, StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
        } else{
            redirectUri = UriComponentsBuilder.fromUriString(redirectLoginUri)
                    .queryParam("isDuplicate", URLEncoder.encode("true", StandardCharsets.UTF_8))
                    .queryParam("authProvider", URLEncoder.encode(authProvider, StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
        }

        response.sendRedirect(redirectUri);
    }

    public String createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_KEY_PREFIX, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(JWTProvider.REFRESH_EXP_SEC)
                .build().toString();
    }

    public String createAccessTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_KEY_PREFIX, refreshToken)
                .httpOnly(false)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(JWTProvider.REFRESH_EXP_SEC)
                .build().toString();
    }

    // 알파벳, 숫자를 조합해서 인증 코드 생성
    private String generateVerificationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();

        return IntStream.range(0, 8) // 8자리
                .map(i -> random.nextInt(chars.length()))
                .mapToObj(chars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
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

    private Map<String, String> createToken(User user){
        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = JWTProvider.createRefreshToken(user);

        // Access Token 갱신
        redisService.storeValue(ACCESS_TOKEN_KEY_PREFIX, String.valueOf(user.getId()), accessToken, JWTProvider.ACCESS_EXP_MILLI);

        // Refresh Token 갱신
        redisService.storeValue(REFRESH_TOKEN_KEY_PREFIX, String.valueOf(user.getId()), refreshToken, JWTProvider.REFRESH_EXP_MILLI);

        // Map으로 토큰들을 담아 반환
        Map<String, String> tokens = new HashMap<>();
        tokens.put(ACCESS_TOKEN_KEY_PREFIX, accessToken);
        tokens.put(REFRESH_TOKEN_KEY_PREFIX, refreshToken);

        return tokens;
    }

    private Map<String, String> createAccessToken(User user){
        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = redisService.getValueInStr(REFRESH_TOKEN_KEY_PREFIX, String.valueOf(user.getId()));

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
        checkIsExitMember(user);
        checkIsActiveMember(user);
        checkIsLocalJoined(user);

        recordLoginAttempt(user, request);

        return createToken(user);
    }

    private Map<String, String> createEmailMap(String email) {
        return Map.of("email", email);
    }

    private void checkIsLocalJoined(User user){
        if(user.isLocalJoined()){
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
        // 알람 전송을 위한 큐 등록
        String queueName = USER_QUEUE_PREFIX + user.getId();
        String listenerId = USER_QUEUE_PREFIX + user.getId();

        brokerService.registerDirectExQueue(ALARM_EXCHANGE, queueName);
        brokerService.registerAlarmListener(listenerId, queueName);
    }

    private void checkAlreadyJoin(String email) {
        // 로컬 회원 가입을 통해 이미 가입함
        if(userRepository.existsByEmailAndAuthProviders(email, List.of(AuthProvider.LOCAL)))
            throw new CustomException(ExceptionCode.JOINED_BY_LOCAL);

        // 소셜 회원 가입을 통해 이미 가입함
        if(userRepository.existsByEmailAndAuthProviders(email, List.of(AuthProvider.KAKAO, AuthProvider.GOOGLE))){
            throw new CustomException(ExceptionCode.JOINED_BY_SOCIAL);
        }
    }

    private void checkDuplicateNickname(String nickName) {
        if(userRepository.existsByNicknameWithRemoved(nickName))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);
    }

    private void checkWriterAuthority(Long accessorId, User writer){
        if(!accessorId.equals(writer.getId())){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkIsExitMember(User user) {
        if(user.isExitMember()){
            throw new CustomException(ExceptionCode.USER_ALREADY_EXIT);
        }
    }

    private boolean isPasswordUnmatched(UserRequest.LoginDTO requestDTO, User user) {
        return !passwordEncoder.matches(requestDTO.password(), user.getPassword());
    }

    private void infoLoginFail(User user, Long loginFailNum) {
        redisService.storeValue(LOGIN_FAIL_KEY_PREFIX, user.getId().toString(), Long.toString(++loginFailNum), 300000L); // 5분
        String message = String.format("로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해 주세요. (%d회 실패)", loginFailNum);
        throw new CustomException(ExceptionCode.USER_ACCOUNT_WRONG, message);
    }

    private void checkIsActiveMember(User user){
        if(user.isUnActive()){
            throw new CustomException(ExceptionCode.USER_SUSPENDED);
        }
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

    private void setUserStatus(User user){
        UserStatus status = UserStatus.builder()
                .user(user)
                .isActive(true)
                .build();

        userStatusRepository.save(status);
        user.updateStatus(status);
    }
}