package io.event.ems.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${app.jwt.cookie.name:accessToken}")
    private String accessTokenCookieName;

    @Value("${app.jwt.cookie.max-age:900}") // 15 phút cho access token
    private int accessTokenMaxAge;

    @Value("${app.jwt.cookie.secure:false}")
    private boolean secure;

    @Value("${app.jwt.cookie.http-only:true}")
    private boolean httpOnly;

    @Value("${app.jwt.cookie.domain:}")
    private String domain;

    @Value("${app.jwt.cookie.path:/}")
    private String path;

    @Value("${app.jwt.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.jwt.cookie.refresh-name:refreshToken}")
    private String refreshTokenCookieName;

    @Value("${app.jwt.cookie.refresh-max-age:604800}") // 7 ngày cho refresh token
    private int refreshTokenMaxAge;

    public void createAccessTokenCookie(HttpServletResponse response, String token) {
        String header = buildSetCookieHeader(accessTokenCookieName, token, accessTokenMaxAge, false);
        response.addHeader("Set-Cookie", header);
    }

    public void createRefreshTokenCookie(HttpServletResponse response, String token) {
        String header = buildSetCookieHeader(refreshTokenCookieName, token, refreshTokenMaxAge, true);
        response.addHeader("Set-Cookie", header);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        String header = buildSetCookieHeader(accessTokenCookieName, "", 0, true);
        response.addHeader("Set-Cookie", header);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        String header = buildSetCookieHeader(refreshTokenCookieName, "", 0, true);
        response.addHeader("Set-Cookie", header);
    }

    private String buildSetCookieHeader(String name, String value, int maxAge, boolean isDelete) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value == null ? "" : value);
        sb.append("; Max-Age=").append(maxAge);
        sb.append("; Path=").append(path);
        if (httpOnly)
            sb.append("; HttpOnly");
        if (secure)
            sb.append("; Secure");
        if (!domain.isEmpty())
            sb.append("; Domain=").append(domain);
        if (sameSite != null && !sameSite.isBlank())
            sb.append("; SameSite=").append(sameSite);
        if (isDelete)
            sb.append("; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
        return sb.toString();
    }
}