package com.hong.forapw.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Set;

public class CookieUtils {

    private CookieUtils() {
    }

    public static void setCookieToResponse(String cookieKey, String cookieValue, Long maxAgeSec, boolean secureCookie,
                                           boolean isHttpOnly, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieKey, cookieValue)
                .httpOnly(isHttpOnly)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSec)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void syncRequestCookiesToResponse(HttpServletRequest request, HttpServletResponse response, Set<String> excludedCookies) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        Arrays.stream(cookies)
                .filter(cookie -> isIncludedCookie(cookie, excludedCookies))
                .map(CookieUtils::convertToResponseCookie)
                .forEach(responseCookie -> response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString()));
    }

    public static String getFromRequest(String cookieKey, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieKey.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private static boolean isIncludedCookie(Cookie cookie, Set<String> excludedCookies) {
        return !excludedCookies.contains(cookie.getName());
    }

    private static ResponseCookie convertToResponseCookie(Cookie cookie) {
        return ResponseCookie.from(cookie.getName(), cookie.getValue())
                .httpOnly(cookie.isHttpOnly())
                .secure(cookie.getSecure())
                .maxAge(cookie.getMaxAge())
                .path("/")
                .sameSite("None")
                .build();
    }
}
