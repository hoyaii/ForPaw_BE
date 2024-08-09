package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GoogleOauthDTO;
import com.hong.ForPaw.controller.DTO.KakaoOauthDTO;
import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.controller.DTO.UserResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.core.utils.MailTemplate;
import com.hong.ForPaw.domain.Authentication.LoginAttempt;
import com.hong.ForPaw.domain.Group.GroupRole;
import com.hong.ForPaw.domain.Inquiry.InquiryAnswer;
import com.hong.ForPaw.domain.Inquiry.Inquiry;
import com.hong.ForPaw.domain.Inquiry.InquiryStatus;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.UserStatus;
import com.hong.ForPaw.repository.Alarm.AlarmRepository;
import com.hong.ForPaw.repository.Animal.FavoriteAnimalRepository;
import com.hong.ForPaw.repository.Authentication.LoginAttemptRepository;
import com.hong.ForPaw.repository.Authentication.VisitRepository;
import com.hong.ForPaw.repository.Chat.ChatUserRepository;
import com.hong.ForPaw.repository.Group.FavoriteGroupRepository;
import com.hong.ForPaw.repository.Group.GroupUserRepository;
import com.hong.ForPaw.repository.Group.MeetingUserRepository;
import com.hong.ForPaw.repository.Inquiry.InquiryAnswerRepository;
import com.hong.ForPaw.repository.Inquiry.InquiryRepository;
import com.hong.ForPaw.repository.Post.CommentLikeRepository;
import com.hong.ForPaw.repository.Post.PostLikeRepository;
import com.hong.ForPaw.repository.UserRepository;
import com.hong.ForPaw.repository.UserStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hong.ForPaw.core.security.JWTProvider;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    private final CommentLikeRepository commentLikeRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository answerRepository;
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
    private String fromEmail;

    @Value("${kakao.key}")
    private String kakaoAPIKey;

    @Value("${kakao.oauth.token.uri}")
    private String kakaoTokenURI;

    @Value("${kakao.oauth.userInfo.uri}")
    private String kakaoUserInfoURI;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.oauth.token.uri}")
    private String googleTokenURI;

    @Value("${google.client.passowrd}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect.uri}")
    private String googleRedirectURI;

    @Value("${google.oauth.userInfo.uri}")
    private String googleUserInfoURI;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Transactional
    public void initSuperAdmin(){
        // SuperAdmin이 등록되어 있지 않다면 등록
        if(!userRepository.existsByNickWithRemoved("admin")){
            User admin = User.builder()
                    .email(adminEmail)
                    .name("admin")
                    .nickName("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.SUPER)
                    .build();

            userRepository.save(admin);

            UserStatus status = UserStatus.builder()
                    .user(admin)
                    .isActive(true)
                    .build();

            userStatusRepository.save(status);
            admin.updateStatus(status);

            setAlarm(admin);
        }
    }

    @Transactional
    public Map<String, String> login(UserRequest.LoginDTO requestDTO, HttpServletRequest request){
        User user = userRepository.findByEmailWithUserStatusAndRemoved(requestDTO.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_ACCOUNT_WRONG)
        );

        // 탈퇴한 계정
        if(user.getRemovedAt() != null){
            throw new CustomException(ExceptionCode.USER_ALREADY_EXIT);
        }

        // 계정 정지 상태 체크
        checkAccountSuspension(user);

        // 로그인 횟수 체크 => 실패 회수 초과 시 에러 던짐
        Long loginFailNum = checkLoginFailures(user);

        // 비밀번호가 일치하지 않음
        if(!passwordEncoder.matches(requestDTO.password(), user.getPassword())){
            redisService.storeValue("loginFail", user.getId().toString(), Long.toString(++loginFailNum), 300000L); // 5분
            throw new CustomException(ExceptionCode.USER_ACCOUNT_WRONG);
        }

        // 로그인 IP 로깅
        recordLoginAttempt(user, request);

        return createToken(user);
    }

    @Transactional
    public Map<String, String> kakaoLogin(String code, HttpServletRequest request) {
        // 카카오 엑세스 토큰 획득 => 유저 정보 획득
        KakaoOauthDTO.TokenDTO token = getKakaoToken(code);
        KakaoOauthDTO.UserInfoDTO userInfo = getKakaoUserInfo(token.access_token());

        // 카카오 계정으로 가입한 계정의 이메일은 {카카오 id}@kakao.com로 구성
        // 카카오의 경우 비즈니스 계정이어야 이메일을 제공해줘서, 직접 카카오 아이디를 바탕으로 이메일을 구성
        String email = userInfo.id().toString() + "@kakao.com";

        // 가입되지 않음 => email을 넘겨서 추가 정보를 입력하도록 함
        if(isNotMember(email)){
            Map<String, String> response = new HashMap<>();
            response.put("email", email);
            return response;
        }

        User user = processLogin(email);

        // 로그인 IP 로깅
        recordLoginAttempt(user, request);

        return createToken(user);
    }

    @Transactional
    public Map<String, String> googleLogin(String code, HttpServletRequest request){
        // 구글 엑세스 토큰 획득
        GoogleOauthDTO.TokenDTO token = getGoogleToken(code);
        GoogleOauthDTO.UserInfoDTO userInfoDTO = getGoogleUserInfo(token.access_token());

        // 구글의 경우 메일을 제공해줌
        String email = userInfoDTO.email();

        if(isNotMember(email)){
            Map<String, String> response = new HashMap<>();
            response.put("email", email);
            return response;
        }

        User user = processLogin(email);

        // 로그인 IP 로깅
        recordLoginAttempt(user, request);

        return createToken(user);
    }

    @Transactional
    public void join(UserRequest.JoinDTO requestDTO){
        if (!requestDTO.password().equals(requestDTO.passwordConfirm()))
            throw new CustomException(ExceptionCode.USER_PASSWORD_WRONG);

        // 비정상 경로를 통한 요청을 대비해, 이메일/닉네임 다시 체크
        if(userRepository.existsByEmailWithRemoved(requestDTO.email()))
            throw new CustomException(ExceptionCode.USER_EMAIL_EXIST);

        if(userRepository.existsByNickWithRemoved(requestDTO.nickName()))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);

        User user = User.builder()
                .name(requestDTO.name())
                .nickName(requestDTO.nickName())
                .email(requestDTO.email())
                .password(passwordEncoder.encode(requestDTO.password()))
                .role(UserRole.USER)
                .profileURL(requestDTO.profileURL())
                .province(requestDTO.province())
                .district(requestDTO.district())
                .subDistrict(requestDTO.subDistrict())
                .build();

        userRepository.save(user);

        // 유저 상태 설정
        setUserStatus(user);

        // 알람 사용을 위한 설정
        setAlarm(user);
    }

    @Transactional
    public void socialJoin(UserRequest.SocialJoinDTO requestDTO){
        // 비정상 경로를 통한 요청을 대비해, 이메일/닉네임 다시 체크
        if(userRepository.existsByEmailWithRemoved(requestDTO.email()))
            throw new CustomException(ExceptionCode.USER_EMAIL_EXIST);

        if(userRepository.existsByNickWithRemoved(requestDTO.nickName()))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);

        User user = User.builder()
                .name(requestDTO.name())
                .nickName(requestDTO.nickName())
                .email(requestDTO.email())
                .password(passwordEncoder.encode(generatePassword())) // 임의의 비밀번호로 생성
                .role(UserRole.USER)
                .profileURL(requestDTO.profileURL())
                .province(requestDTO.province())
                .district(requestDTO.district())
                .subDistrict(requestDTO.subDistrict())
                .build();

        userRepository.save(user);

        setUserStatus(user);
        setAlarm(user);
    }

    public void checkEmailExist(UserRequest.EmailDTO requestDTO){
        // 가입한 이메일이 존재 한다면
        if(userRepository.existsByEmailWithRemoved(requestDTO.email()))
            throw new CustomException(ExceptionCode.USER_EMAIL_EXIST);

        // 계속 이메일을 보내는 건 방지. 5분 후에 다시 시도할 수 있다
        if(redisService.isDateExist("emailCode", requestDTO.email())){
            throw new CustomException(ExceptionCode.ALREADY_SEND_EMAIL);
        }
    }

    @Async
    public void sendCodeByEmail(UserRequest.EmailDTO requestDTO){
        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = generateVerificationCode();
        sendMail(requestDTO.email(), MailTemplate.VERIFICATION_CODE, verificationCode);
        redisService.storeValue("emailCode", requestDTO.email(), verificationCode, 5 * 60 * 1000L); // 5분 동안 유효
    }

    public void verifyRegisterCode(UserRequest.VerifyCodeDTO requestDTO){
        // 레디스를 통해 해당 코드가 유효한지 확인
        if(!redisService.validateData("emailCode", requestDTO.email(), requestDTO.code()))
            throw new CustomException(ExceptionCode.CODE_WRONG);
        redisService.removeData("emailCode", requestDTO.email()); // 검증 후 토큰 삭제
    }

    public void checkNick(UserRequest.CheckNickDTO requestDTO){
        if(userRepository.existsByNickWithRemoved(requestDTO.nickName()))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);
    }

    public void checkAccountExist(UserRequest.EmailDTO requestDTO){
        // 가입된 계정이 아니라면
        if(userRepository.findByEmail(requestDTO.email()).isEmpty())
            throw new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND);

        // 계속 이메일을 보내는 건 방지. 5분 후에 다시 시도할 수 있다
        if(redisService.isDateExist("emailCode", requestDTO.email())){
            throw new CustomException(ExceptionCode.ALREADY_SEND_EMAIL);
        }
    }

    public void verifyRecoveryCode(UserRequest.VerifyCodeDTO requestDTO){
        if(!redisService.validateData("emailCode", requestDTO.email(), requestDTO.code()))
            throw new CustomException(ExceptionCode.CODE_WRONG);

        // resetPassword()에서 서버에 요청으로 이메일과 비밀번호를 동시에 보내지 않도록, 코드에 대한 이메일을 저장
        redisService.storeValue("emailCodeToEmail", requestDTO.code(), requestDTO.email(), 5 * 60 * 1000L);
    }

    @Transactional
    public void resetPassword(UserRequest.ResetPasswordDTO requestDTO){
        String email = redisService.getDataInStr("emailCodeToEmail", requestDTO.code());

        // 해당 email이 계정 복구중이 아님. (verifyRecoveryCode()를 거쳐야 값이 존재한다)
        if(email == null){
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND)
        );

        // 다시 한번 이메일 코드 체크
        if(!redisService.validateData("emailCode", email, requestDTO.code()))
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        redisService.removeData("emailCode", email);

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
        User user = userRepository.findById(userId).get();

        // verifyPassword()로 일치 여부를 확인하지만, 해당 단계를 거치지 않고 인위적으로 요청을 보낼 수 있어서 한 번더 검증
        if(!passwordEncoder.matches(requestDTO.curPassword(), user.getPassword())){
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);
        }

        if (!requestDTO.newPassword().equals(requestDTO.newPasswordConfirm()))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);

        user.updatePassword(passwordEncoder.encode(requestDTO.newPassword()));
    }

    @Transactional(readOnly = true)
    public UserResponse.ProfileDTO findProfile(Long userId){
        User user = userRepository.findById(userId).get();
        return new UserResponse.ProfileDTO(user.getEmail(), user.getName(), user.getNickName(), user.getProvince(), user.getDistrict(), user.getSubDistrict(), user.getProfileURL());
    }

    @Transactional
    public void updateProfile(UserRequest.UpdateProfileDTO requestDTO, Long userId){
        User user = userRepository.findById(userId).get();

        // 닉네임 중복 체크 (현재 닉네임은 통과)
        if(!user.getNickName().equals(requestDTO.nickName()) && userRepository.existsByNickWithRemoved(requestDTO.nickName()))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);

        user.updateProfile(requestDTO.nickName(), requestDTO.province(), requestDTO.district(), requestDTO.subDistrict(), requestDTO.profileURL());
    }

    @Transactional
    public Map<String, String> updateAccessToken(String refreshToken){
        // 잘못된 토큰 형식인지 체크
        if(!JWTProvider.validateToken(refreshToken)) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }

        Long userId = JWTProvider.getUserIdFromToken(refreshToken);

        // 토큰 만료 여부 체크
        if(!redisService.isDateExist("refreshToken", String.valueOf(userId)))
            throw new CustomException(ExceptionCode.TOKEN_EXPIRED);

        // 탈퇴한 회원의 refreshToken이 요청으로 왔으면, USER_NOT_FOUND 에러 발생
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return createAccessToken(user);
    }

    // 관지라 API
    @Transactional
    public void updateRole(UserRequest.UpdateRoleDTO requestDTO, UserRole userRole){
        // 관리자만 사용 가능 (테스트 상황에선 주석 처리)
        //if(role.equals(Role.ADMIN)){
        //    throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        //}

        User user = userRepository.findById(requestDTO.userId()).get();
        user.updateRole(requestDTO.role());
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
        alarmRepository.deleteAllByUserId(userId);

        // 방문 기록 삭제
        visitRepository.deleteAllByUserId(userId);

        // 로그인 기록 삭제
        loginAttemptRepository.deleteAllByUserId(userId);

        // 중간 테이블 역할의 엔티티 삭제
        postLikeRepository.deleteAllByUserId(userId);
        commentLikeRepository.deleteAllByUserId(userId);
        favoriteAnimalRepository.deleteAllByUserId(userId);
        favoriteGroupRepository.deleteByGroupId(userId);
        chatUserRepository.deleteAllByUserId(userId);
        groupUserRepository.findAllByUserIdWithGroup(userId).forEach(
                groupUser -> {
                    redisService.decrementCnt("groupParticipantNum", groupUser.getGroup().getId().toString(), 1L);
                    groupUserRepository.delete(groupUser);
                }
        );
        meetingUserRepository.findAllByUserIdWithMeeting(userId).forEach(
                meetingUser -> {
                    redisService.decrementCnt("meetingParticipantNum", meetingUser.getMeeting().getId().toString(), 1L);
                    meetingUserRepository.delete(meetingUser);
                }
        );

        // 유저 상태 변경
        user.getStatus().updateIsActive(false);

        // 유저 삭제 (soft delete 처리) => soft delete의 side effect 고려 해야함
        userRepository.deleteById(userId);
    }

    // 탈퇴한지 6개월 지난 유저 데이터 삭제 (매일 자정 30분에 실행)
    @Transactional
    @Scheduled(cron = "0 30 0 * * ?")
    public void deleteExpiredUserData(){
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minus(6, ChronoUnit.MONTHS);
        userRepository.deleteAllWithRemovedBefore(sixMonthsAgo);
    }

    @Transactional
    public UserResponse.SubmitInquiryDTO submitInquiry(UserRequest.SubmitInquiry requestDTO, Long userId){
        // 프록시 객체
        User user = entityManager.getReference(User.class, userId);

        Inquiry inquiry = Inquiry.builder()
                .questioner(user)
                .title(requestDTO.title())
                .description(requestDTO.description())
                .contactMail(requestDTO.contactMail())
                .status(InquiryStatus.PROCESSING)
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
        checkInquiryAuthority(userId, inquiry.getQuestioner());

        inquiry.updateCustomerInquiry(requestDTO.title(), requestDTO.description(), requestDTO.contactMail());
    }

    @Transactional(readOnly = true)
    public UserResponse.FindInquiryListDTO findInquiryList(Long userId){
        List<Inquiry> customerInquiries = inquiryRepository.findAllByQuestionerId(userId);

        List<UserResponse.InquiryDTO> inquiryDTOS = customerInquiries.stream()
                .map(inquiry -> new UserResponse.InquiryDTO(
                        inquiry.getId(),
                        inquiry.getTitle(),
                        inquiry.getStatus(),
                        inquiry.getCreatedDate()))
                .toList();

        if(inquiryDTOS.isEmpty()){
            throw new CustomException(ExceptionCode.INQUIRY_NOT_FOUND);
        }

        return new UserResponse.FindInquiryListDTO(inquiryDTOS);
    }

    @Transactional(readOnly = true)
    public UserResponse.FindInquiryByIdDTO findInquiryById(Long userId, Long inquiryId){
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        // 권한 체크
        checkInquiryAuthority(userId, inquiry.getQuestioner());

        // 답변
        List<InquiryAnswer> answers = answerRepository.findAllByInquiryId(inquiryId);
        List<UserResponse.AnswerDTO> answerDTOS = answers.stream()
                .map(answer -> new UserResponse.AnswerDTO(
                        answer.getId(),
                        answer.getContent(),
                        answer.getCreatedDate(),
                        answer.getAnswerer().getName()))
                .toList();

        return new UserResponse.FindInquiryByIdDTO(inquiry.getTitle(), inquiry.getDescription(), inquiry.getStatus(), inquiry.getCreatedDate(), answerDTOS);
    }

    @Transactional(readOnly = true)
    public UserResponse.ValidateAccessTokenDTO validateAccessToken(@CookieValue String accessToken){
        // 잘못된 토큰 형식인지 체크
        if(!JWTProvider.validateToken(accessToken)) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }

        Long userIdFromToken = JWTProvider.getUserIdFromToken(accessToken);
        if(!redisService.validateData("accessToken", String.valueOf(userIdFromToken), accessToken)){
            throw new CustomException(ExceptionCode.ACCESS_TOKEN_WRONG);
        }

        String profile = userRepository.findProfileById(userIdFromToken).orElse(null);

        return new UserResponse.ValidateAccessTokenDTO(profile);
    }

    private Long checkLoginFailures(User user){
        // 하루 동안 5분 잠금이 세 번을 초과하면, 24시간 동안 로그인이 불가
        Long loginFailNumDaily = redisService.getDataInLong("loginFailDaily", user.getId().toString());
        if (loginFailNumDaily >= 3L) {
            throw new CustomException(ExceptionCode.ACCOUNT_LOCKED);
        }

        // 로그이 실패 횟수가 3회 이상이면, 5분 동안 로그인 불가
        Long loginFailNum = redisService.getDataInLong("loginFail", user.getId().toString());
        if(loginFailNum >= 3L) {
            loginFailNumDaily++;
            redisService.storeValue("loginFailDaily", user.getId().toString(), loginFailNumDaily.toString(), 86400000L);  // 24시간

            if(loginFailNumDaily == 3L){
                sendMail(user.getEmail(), MailTemplate.ACCOUNT_SUSPENSION);
            }

            throw new CustomException(ExceptionCode.LOGIN_ATTEMPT_EXCEEDED);
        }

        return loginFailNum;
    }

    @Async
    public void sendPasswordByMail(String toEmail, String password) {
        sendMail(toEmail, MailTemplate.TEMPORARY_PASSWORD, password);
    }

    @Async
    public void sendMail(String toEmail, MailTemplate template, String... args) {
        // template에서 subject와 text 추출
        String subject = template.getSubject();
        String text = template.formatText(args);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
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
        SecureRandom random = new SecureRandom();

        // 각 범주에서 랜덤하게 하나씩 선택
        String mandatoryChars =
                Stream.of(specialChars, numbers, upperCaseLetters, lowerCaseLetters)
                        .map(s -> s.charAt(random.nextInt(s.length())))
                        .map(Object::toString)
                        .collect(Collectors.joining());

        // 나머지 자리를 전체 문자 집합에서 선택
        String allChars = specialChars + numbers + upperCaseLetters + lowerCaseLetters;
        String randomChars = IntStream.range(mandatoryChars.length(), 8)
                .map(i -> random.nextInt(allChars.length()))
                .mapToObj(allChars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());

        // 최종 문자열 생성을 위해 섞기
        String verificationCode = Stream.of(mandatoryChars.split(""), randomChars.split(""))
                .flatMap(Arrays::stream)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.shuffle(list);
                    return list.stream();
                }))
                .collect(Collectors.joining());

        return verificationCode;
    }

    private Map<String, String> createToken(User user){
        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = JWTProvider.createRefreshToken(user);

        // Access Token 갱신
        redisService.storeValue("accessToken", String.valueOf(user.getId()), accessToken, JWTProvider.ACCESS_EXP_MILLI);

        // Refresh Token 갱신
        redisService.storeValue("refreshToken", String.valueOf(user.getId()), refreshToken, JWTProvider.REFRESH_EXP_MILLI);

        // Map으로 토큰들을 담아 반환
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    private Map<String, String> createAccessToken(User user){
        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = redisService.getDataInStr("refreshToken", String.valueOf(user.getId()));

        redisService.storeValue("accessToken", String.valueOf(user.getId()), accessToken, JWTProvider.ACCESS_EXP_MILLI);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    private KakaoOauthDTO.TokenDTO getKakaoToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoAPIKey);
        formData.add("code", code);

        Mono<KakaoOauthDTO.TokenDTO> response = webClient.post()
                .uri(kakaoTokenURI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KakaoOauthDTO.TokenDTO.class);

        return response.block();
    }

    private KakaoOauthDTO.UserInfoDTO getKakaoUserInfo(String token) {
        Flux<KakaoOauthDTO.UserInfoDTO> response = webClient.get()
                .uri(kakaoUserInfoURI)
                .header("Authorization", "Bearer " + token)
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
        formData.add("redirect_uri", googleRedirectURI);
        formData.add("grant_type", "authorization_code");

        Mono<GoogleOauthDTO.TokenDTO> response = webClient.post()
                .uri(googleTokenURI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(GoogleOauthDTO.TokenDTO.class);

        return response.block();
    }

    private GoogleOauthDTO.UserInfoDTO getGoogleUserInfo(String token) {
        Flux<GoogleOauthDTO.UserInfoDTO> response = webClient.get()
                .uri(googleUserInfoURI)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToFlux(GoogleOauthDTO.UserInfoDTO.class);

        return response.blockFirst();
    }

    private boolean isNotMember(String email){
        return userRepository.findByEmail(email).isEmpty();
    }

    private void setAlarm(User user) {
        // 알람 전송을 위한 큐 등록
        String exchangeName = "alarm.exchange";
        String queueName = "user." + user.getId();
        String listenerId = "user." + user.getId();

        brokerService.registerDirectExQueue(exchangeName, queueName);
        brokerService.registerAlarmListener(listenerId, queueName);
    }

    private void checkInquiryAuthority(Long accessorId, User writer){
        if(!accessorId.equals(writer.getId())){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkAccountSuspension(User user){
        if(!user.getStatus().isActive()){
            throw new CustomException(ExceptionCode.USER_SUSPENDED);
        }
    }

    private User processLogin(String email){
        User user = userRepository.findByEmailWithUserStatusAndRemoved(email).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        if(user.getRemovedAt() != null){
            throw new CustomException(ExceptionCode.USER_ALREADY_EXIT);
        }

        // 정지 상태 체크
        checkAccountSuspension(user);

        return user;
    }

    public void recordLoginAttempt(User user, HttpServletRequest request) {
        String clientIp = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        LoginAttempt attempt = LoginAttempt.builder()
                .user(user)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();

        loginAttemptRepository.save(attempt);
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        // nginx와 같은 proxy를 사용 대비 로직
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
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