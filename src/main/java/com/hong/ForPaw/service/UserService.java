package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GoogleOauthDTO;
import com.hong.ForPaw.controller.DTO.KakaoOauthDTO;
import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.controller.DTO.UserResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Group.GroupRole;
import com.hong.ForPaw.domain.Inquiry.Answer;
import com.hong.ForPaw.domain.Inquiry.Inquiry;
import com.hong.ForPaw.domain.Inquiry.InquiryStatus;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.repository.Alarm.AlarmRepository;
import com.hong.ForPaw.repository.ApplyRepository;
import com.hong.ForPaw.repository.Chat.ChatUserRepository;
import com.hong.ForPaw.repository.Group.GroupUserRepository;
import com.hong.ForPaw.repository.Group.MeetingUserRepository;
import com.hong.ForPaw.repository.Inquiry.AnswerRepository;
import com.hong.ForPaw.repository.Inquiry.InquiryRepository;
import com.hong.ForPaw.repository.Post.PostReadStatusRepository;
import com.hong.ForPaw.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hong.ForPaw.core.security.JWTProvider;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
    private final ApplyRepository applyRepository;
    private final GroupUserRepository groupUserRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final PostReadStatusRepository postReadStatusRepository;
    private final ChatUserRepository chatUserRepository;
    private final InquiryRepository inquiryRepository;
    private final AnswerRepository answerRepository;
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

    @Value("${kakao.oauth.redirect.uri}")
    private String kakaoRedirectURI;

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
    
    @Transactional
    public Map<String, String> login(UserRequest.LoginDTO requestDTO){
        User user = userRepository.findByEmail(requestDTO.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_ACCOUNT_WRONG)
        );

        if(!passwordEncoder.matches(requestDTO.password(), user.getPassword())){
            throw new CustomException(ExceptionCode.USER_ACCOUNT_WRONG);
        }

        if(user.)

        checkDuplicateLogin(user);

        return createToken(user);
    }

    @Transactional
    public Map<String, String> kakaoLogin(String code) {
        // 카카오 엑세스 토큰 획득 => 유저 정보 획득
        KakaoOauthDTO.TokenDTO token = getKakaoToken(code);
        KakaoOauthDTO.UserInfoDTO userInfo = getKakaoUserInfo(token.access_token());

        // 카카오 계정으로 가입한 계정의 이메일은 {카카오 id}@kakao.com로 구성
        String email = userInfo.id().toString() + "@kakao.com";

        // 가입되지 않음 => email을 넘겨서 추가 정보를 입력하도록 함
        if(isNotMember(email)){
            Map<String, String> response = new HashMap<>();
            response.put("email", email);
            return response;
        }

        User user = userRepository.findByEmail(email).get();
        checkDuplicateLogin(user);

        return createToken(user);
    }

    @Transactional
    public Map<String, String> googleLogin(String code){
        // 구글 엑세스 토큰 획득
        GoogleOauthDTO.TokenDTO token = getGoogleToken(code);
        GoogleOauthDTO.UserInfoDTO userInfoDTO = getGoogleUserInfo(token.access_token());

        String email = userInfoDTO.email();

        if(isNotMember(email)){
            Map<String, String> response = new HashMap<>();
            response.put("email", email);
            return response;
        }

        User user = userRepository.findByEmail(email).get();
        checkDuplicateLogin(user);

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

        setAlarm(user);
    }

    // 중복 여부 확인 => 만약 사용 가능한 메일이면, 코드 전송
    @Transactional
    public void checkEmailAndSendCode(UserRequest.EmailDTO requestDTO){
        // 가입한 이메일이 존재 한다면
        if(userRepository.existsByEmailWithRemoved(requestDTO.email()))
            throw new CustomException(ExceptionCode.USER_EMAIL_EXIST);

        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = sendCodeByMail(requestDTO.email());
        redisService.storeDate("emailCode", requestDTO.email(), verificationCode, 5 * 60 * 1000L); // 5분 동안 유효
    }

    @Transactional
    public void verifyCode(UserRequest.VerifyCodeDTO requestDTO){
        // 레디스를 통해 해당 코드가 유효한지 확인
        if(!redisService.validateData("emailCode", requestDTO.email(), requestDTO.code()))
            throw new CustomException(ExceptionCode.CODE_WRONG);
        redisService.removeData("emailCode", requestDTO.email()); // 검증 후 토큰 삭제
    }

    @Transactional
    public void checkNick(UserRequest.CheckNickDTO requestDTO){
        if(userRepository.existsByNickWithRemoved(requestDTO.nickName()))
            throw new CustomException(ExceptionCode.USER_NICKNAME_EXIST);
    }

    @Transactional
    public void sendRecoveryCode(UserRequest.EmailDTO requestDTO){
        // 가입된 계정이 아니라면
        if(userRepository.findByEmail(requestDTO.email()).isEmpty())
            throw new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND);

        // 요청 횟수 3회 넘아가면 10분 동안 요청 불가
        Long recoveryNum = redisService.getDataInLong("requestNum", requestDTO.email());
        if(recoveryNum >= 3L){
            throw new CustomException(ExceptionCode.EXCEED_REQUEST_NUM);
        }

        // 요청 횟수 업데이트
        redisService.storeDate("requestNum", requestDTO.email(), String.valueOf(recoveryNum + 1), 10 * 60 * 1000L);

        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = sendCodeByMail(requestDTO.email());
        redisService.storeDate("emailCode", requestDTO.email(), verificationCode, 5 * 60 * 1000L);
    }

    @Transactional
    public void verifyAndSendPassword(UserRequest.VerifyCodeDTO requestDTO){
        // 레디스를 통해 해당 코드가 유효한지 확인
        if(!redisService.validateData("emailCode", requestDTO.email(), requestDTO.code()))
            throw new CustomException(ExceptionCode.CODE_WRONG);
        redisService.removeData("emailCode", requestDTO.email());

        // 임시 비밀번호 생성 후 업데이트
        String password = generatePassword();
        User user = userRepository.findByEmail(requestDTO.email()).get(); // 이미 앞에서 계정 존재 여부를 확인 했으니 바로 가져옴
        user.updatePassword(passwordEncoder.encode(password));

        sendPasswordByMail(requestDTO.email(), password);
    }

    // 재설정 화면에서 실시간으로 일치여부를 확인하기 위해 사용
    @Transactional
    public void verifyPassword(UserRequest.CurPasswordDTO requestDTO, Long userId){
        User user = userRepository.findById(userId).get();

        if(!passwordEncoder.matches(requestDTO.password(), user.getPassword())){
            throw new CustomException(ExceptionCode.USER_PASSWORD_WRONG);
        }
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

    @Transactional
    public UserResponse.ProfileDTO findProfile(Long userId){
        User user = userRepository.findById(userId).get();
        return new UserResponse.ProfileDTO(user.getName(), user.getNickName(), user.getProvince(), user.getDistrict(), user.getSubDistrict(), user.getProfileURL());
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
    public UserResponse.AccessTokenDTO updateAccessToken(UserRequest.UpdateAccessTokenDTO requestDTO){
        // 잘못된 토큰 형식인지 체크
        if(!JWTProvider.validateToken(requestDTO.refreshToken())) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }

        Long userId =JWTProvider.getUserIdFromToken(requestDTO.refreshToken());

        // 토큰 만료 여부 체크
        if(!redisService.isDateExist("refreshToken", String.valueOf(userId)))
            throw new CustomException(ExceptionCode.TOKEN_EXPIRED);

        // 탈퇴한 회원의 refreshToken이 요청으로 왔으면, USER_NOT_FOUND 에러 발생
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return new UserResponse.AccessTokenDTO(JWTProvider.createAccessToken(user));
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
        // 이미 탈퇴한 회원이면 예외
        userRepository.findByIdWithRemoved(userId)
                .map(user -> {
                    if(user.getRemovedAt() != null){
                        throw new CustomException(ExceptionCode.USER_ALREADY_EXIT);
                    }
                    return null;
                });

        // 그룹장 상태에서는 탈퇴 불가능
        groupUserRepository.findAllByUserId(userId)
                .forEach(groupUser -> {
                    groupUser.getGroupRole().equals(GroupRole.CREATOR);
                    throw new CustomException(ExceptionCode.CREATOR_CANT_EXIT);
                });

        // 알람 삭제
        alarmRepository.deleteAllByUserId(userId);

        // 지원서 삭제
        applyRepository.deleteAllByUserId(userId);

        // 유저와 연관 데이터 삭제
        postReadStatusRepository.deleteAllByUserId(userId);
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

        // 유저 삭제 (soft delete 처리)
        userRepository.deleteById(userId);
    }

    @Transactional
    public UserResponse.SubmitInquiryDTO submitInquiry(UserRequest.SubmitInquiry requestDTO, Long userId){
        // 프록시 객체
        User user = entityManager.getReference(User.class, userId);

        Inquiry inquiry = Inquiry.builder()
                .user(user)
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
        checkInquiryAuthority(userId, inquiry.getUser());

        inquiry.updateCustomerInquiry(requestDTO.title(), requestDTO.description(), requestDTO.contactMail());
    }

    @Transactional
    public UserResponse.FindInquiryListDTO findInquiryList(Long userId){
        List<Inquiry> customerInquiries = inquiryRepository.findAllByUserId(userId);

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

    @Transactional
    public UserResponse.FindInquiryByIdDTO findInquiryById(Long userId, Long inquiryId){
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        // 권한 체크
        checkInquiryAuthority(userId, inquiry.getUser());

        // 답변
        List<Answer> answers = answerRepository.findAllByInquiryId(inquiryId);
        List<UserResponse.AnswerDTO> answerDTOS = answers.stream()
                .map(answer -> new UserResponse.AnswerDTO(
                        answer.getId(),
                        answer.getContent(),
                        answer.getCreatedDate(),
                        answer.getUser().getName()))
                .toList();

        return new UserResponse.FindInquiryByIdDTO(inquiry.getTitle(), inquiry.getDescription(), inquiry.getStatus(), inquiry.getCreatedDate(), answerDTOS);
    }

    private String sendCodeByMail(String toEmail){
        String verificationCode = generateVerificationCode();
        String subject = "[ForPaw] 이메일 인증 코드입니다.";
        String text = "인증 코드는 다음과 같습니다: " + verificationCode + "\n이 코드를 입력하여 이메일을 인증해 주세요.";
        sendMail(fromEmail, toEmail, subject, text);

        return verificationCode;
    }

    private void sendPasswordByMail(String toEmail, String password){
        String subject = "[ForPaw] 임시 비밀번호 입니다.";
        String text = "임시 비밀번호: " + password + "\n로그인 후 비밀번호를 변경해 주세요.";
        sendMail(fromEmail, toEmail, subject, text);
    }

    private void sendMail(String fromEmail, String toEmail, String subject, String text){
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

    // 중복 로그인 체크 => 만약 이미 로그인된 상태라면 기존 세션은 삭제
    private void checkDuplicateLogin(User user){
        String accessToken = redisService.getDataInStr("accessToken", String.valueOf(user.getId()));
        if(accessToken != null){
            redisService.removeData("accessToken", String.valueOf(user.getId()));
        }
    }

    private Map<String, String> createToken(User user){
        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = JWTProvider.createRefreshToken(user);

        // Access Token 세션에 저장 (중복 로그인 방지)
        redisService.storeDate("accessToken", String.valueOf(user.getId()), accessToken, JWTProvider.ACCESS_EXP);

        // Refresh Token 갱신
        redisService.removeData("refreshToken", String.valueOf(user.getId()));
        redisService.storeDate("refreshToken", String.valueOf(user.getId()), refreshToken, JWTProvider.REFRESH_EXP);

        // Map으로 토큰들을 담아 반환
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    private KakaoOauthDTO.TokenDTO getKakaoToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoAPIKey);
        formData.add("redirect_uri", kakaoRedirectURI);
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
}