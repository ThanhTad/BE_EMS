package io.event.ems.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class RequestUtils {

    private RequestUtils() {
    }

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * Lấy địa chỉ IP của client từ request.
     * Phương thức này sẽ kiểm tra các header phổ biến được sử dụng bởi reverse proxy
     * và load balancer trước khi fallback về getRemoteAddr().
     *
     * @param request HttpServletRequest đến.
     * @return Địa chỉ IP của client, hoặc một địa chỉ mặc định nếu không tìm thấy.
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // Header X-Forwarded-For có thể chứa nhiều IP, IP đầu tiên là của client
                return ip.split(",")[0].trim();
            }
        }

        // Nếu không tìm thấy header nào, sử dụng getRemoteAddr() làm phương án cuối cùng
        return request.getRemoteAddr();
    }
}
