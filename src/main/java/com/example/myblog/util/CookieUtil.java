package com.example.myblog.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/**
 * 쿠키(Cookie) 관리 유틸리티 클래스
 * - 쿠키 생성, 조회, 삭제 및 객체 직렬화/역직렬화 기능을 제공합니다.
 */
public class CookieUtil {

    /**
     * 요청값(이름, 값, 만료기간)을 바탕으로 HTTP 응답에 쿠키를 추가합니다.
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 쿠키의 이름을 입력받아 쿠키를 삭제합니다.
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                cookie.setValue("");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }
    }

    /**
     * 객체를 직렬화하여 Base64 문자열 쿠키 값으로 변환합니다.
     */
    public static String serialize(Object obj) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(obj));
    }

    /**
     * 쿠키 값을 역직렬화하여 객체로 변환합니다.
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(
                SerializationUtils.deserialize(
                        Base64.getUrlDecoder().decode(cookie.getValue())
                )
        );
    }
}
