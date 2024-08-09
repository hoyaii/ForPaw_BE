package com.hong.ForPaw.core.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExceptionCode {
    // 사용자 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    USER_EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이메일을 찾을 수 없습니다."),
    ALREADY_SEND_EMAIL(HttpStatus.NOT_FOUND, "이미 이메일을 전송하였습니다. 5분 후에 다시 시도해주세요"),
    USER_EMAIL_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
    USER_NICKNAME_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다."),
    USER_ACCOUNT_WRONG(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호를 다시 확인해 주세요"),
    USER_PASSWORD_WRONG(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    USER_PASSWORD_MATCH_WRONG(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."), // 두 비밀번호 일치 여부 확인
    USER_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "사용자 정보를 수정할 권한이 없습니다."),
    USER_FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 되지 않았습니다."),
    USER_ALREADY_EXIT(HttpStatus.NOT_FOUND, "이미 탈퇴한 계정입니다."),
    CREATOR_CANT_EXIT(HttpStatus.BAD_REQUEST, "그룹장은 권한 양도 후 탈퇴 가능합니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "계정이 비활성화 상태입니다."),
    LOGIN_ATTEMPT_EXCEEDED(HttpStatus.FORBIDDEN, "로그인 횟수를 초과했습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "로그인 횟수를 초과하여 계정이 비활성화 되었습니다."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 관리자를 찾을 수 없습니다."),
    ACCESS_TOKEN_WRONG(HttpStatus.BAD_REQUEST, "엑세스 토큰 검증에 실패하였습니다."),
    CANNOT_CHANGE_TO_SUPER(HttpStatus.BAD_REQUEST, "SUPER 권한으로의 변경은 불가능합니다."),
    USER_ALREADY_SUSPENDED(HttpStatus.BAD_REQUEST, "이미 정지된 상태입니다."),
    USER_NOT_SUSPENDED(HttpStatus.BAD_REQUEST, "정지된 유저가 아닙니다."),
    JOINED_BY_LOCAL(HttpStatus.BAD_REQUEST, "이미 가입된 계정입니다."),
    JOINED_BY_SOCIAL(HttpStatus.BAD_REQUEST, "소셜 회원 가입을 통해 이미 가입된 계정입니다."),

    // 고객 문의 관련 에러
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의 내역이 존재하지 않습니다"),

    // 신고 관련 에러
    WRONG_REPORT_TARGET(HttpStatus.BAD_REQUEST, "잘못된 컨텐츠 타입 입니다."),
    ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "이미 신고하셨습니다."),
    CANNOT_REPORT_OWN_CONTENT(HttpStatus.BAD_REQUEST, "자신의 컨텐츠에는 신고할 수 없습니다"),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신고 내역입니다."),
    REPORT_NOT_APPLY_TO_SUPER(HttpStatus.BAD_REQUEST, "관리자는 정지시킬 수 없습니다."),

    // 이메일 코드 관련 에러
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "유효기간이 만료되었습니다."),
    CODE_WRONG(HttpStatus.BAD_REQUEST, "잘못된 인증코드입니다."),
    CODE_NOT_SEND(HttpStatus.INTERNAL_SERVER_ERROR, "인증코드를 전송하지 못했습니다."),

    // 토큰 관련 에러
    TOKEN_WRONG(HttpStatus.BAD_REQUEST, "잘못된 토큰 형식입니다."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "토큰이 만료됐습니다."),

    // 동물 관련 에러
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 동물을 찾을 수 없습니다."),
    ANIMAL_NOT_EXIST(HttpStatus.NOT_FOUND, "목록에 동물이 존재하지 않습니다."),
    ANIMAL_ALREADY_APPLY(HttpStatus.BAD_REQUEST, "이미 지원하였습니다."),
    APPLY_NOT_FOUND(HttpStatus.BAD_REQUEST, "지원서가 존재하지 않습니다."),
    SHELTER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 보호소를 찾을 수 없습니다."),
    WRONG_ANIMAL_TYPE(HttpStatus.BAD_REQUEST, "잘못된 sort 타입 입니다. (date, dog, cat, other)"),

    // 그룹 관련 에러
    GROUP_NAME_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 그룹 이름입니다."),
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 그룹 입니다."),
    GROUP_ALREADY_JOIN(HttpStatus.BAD_REQUEST, "그룹에 이미 가입하였거나, 신청이 완료되었습니다."),
    GROUP_NOT_APPLY(HttpStatus.BAD_REQUEST, "가입 신청을 하지 않았습니다"),
    GROUP_NOT_MEMBER(HttpStatus.BAD_REQUEST, "그룹의 맴버가 아닙니다."),
    GROUP_FULL(HttpStatus.BAD_REQUEST, "그룹의 수용 인원이 초과되었습니다."),
    ROLE_CANT_UPDATE(HttpStatus.BAD_REQUEST, "그룹장으로의 변경은 불가능합니다."),
    CANT_UPDATE_FOR_CREATOR(HttpStatus.BAD_REQUEST, "그룹장은 자신의 역할을 변경할 수 없습니다."),
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 정기모임 입니다."),
    MEETING_ALREADY_JOIN(HttpStatus.BAD_REQUEST, "모임에 이미 참가하였습니다."),
    MEETING_NOT_MEMBER(HttpStatus.BAD_REQUEST, "모임에 참가중이지 않습니다."),
    MEETING_NAME_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 미팅 이름입니다."),

    // 게시글 관련 에러
    POST_TYPE_INCORRECT(HttpStatus.BAD_REQUEST, "게시글의 요청 타입이 올바르지 않습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 글입니다."),
    CANT_LIKE_MY_POST(HttpStatus.BAD_REQUEST, "자신의 글에는 좋아요를 할 수 없습니다."),
    NOT_QUESTION_TYPE(HttpStatus.BAD_REQUEST, "질문글이 아닙니다."),
    NOT_ANSWER_TYPE(HttpStatus.BAD_REQUEST, "답변글이 아닙니다."),
    IS_QUESTION_TYPE(HttpStatus.BAD_REQUEST, "질문글은 질문 조회 API를 호출해주세요."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    NOT_POSTS_COMMENT(HttpStatus.BAD_REQUEST, "게시글의 댓글이 아닙니다."),
    CANT_LIKE_MY_COMMENT(HttpStatus.BAD_REQUEST, "자신의 댓글에는 좋아요를 할 수 없습니다."),
    POST_LIKE_EXPIRED(HttpStatus.BAD_REQUEST, "오래된 글은 공감할 수 없습니다."),
    SCREENED_POST(HttpStatus.BAD_REQUEST, "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다."),
    CANT_REPLY_TO_REPLY(HttpStatus.BAD_REQUEST, "대댓글에 댓글을 달 수 없습니다."),
    POST_MUST_CONTAIN_IMAGE(HttpStatus.BAD_REQUEST, "게시글에는 이미지를 반드시 포함해야 합니다."),

    // 알람 관련 에러
    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알람입니다."),
    ALARM_NOT_EXIST(HttpStatus.NOT_FOUND, "알람 목록이 존재하지 않습니다."),

    // 채팅방 관련 에어
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다"),
    CAN_ACCESS_PREVIOUS_MESSAGE(HttpStatus.BAD_REQUEST, "이전에 조회한 메시지의 페이지만 조회할 수 있습니다."),

    // 잘못된 접근
    BAD_APPROACH(HttpStatus.BAD_REQUEST, "잘못된 접근입니다."),
    EXCEED_REQUEST_NUM(HttpStatus.BAD_REQUEST, "가능한 요청 횟수를 초과하였습니다."),
    SAME_STATUS(HttpStatus.BAD_REQUEST, "현재 상태와 동일합니다."),

    // 검색
    SEARCH_NOT_FOUND(HttpStatus.NOT_FOUND, "검색 결과값이 존재하지 않습니다"),
    SEARCH_KEYWORD_EMPTY(HttpStatus.BAD_REQUEST, "검색어는 빈 문자열일 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
