package io.event.ems.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class VNPayConfig {

    @Value("${vnpay.url}")
    private String vnpUrl;

    @Value("${vnpay.api.url}")
    private String vnpApiUrl;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.returnUrl}")
    private String vpnReturnUrl;

    @Value("${vnpay.ipnUrl}")
    private String vnpIpnUrl;

    public static final String VNP_COMMAND_PAY = "pay";
    public static final String VNP_CURR_CODE = "VND";
    public static final String VNP_LOCALE = "vn";
    public static final String VNP_ORDER_TYPE = "other";

}
