package com.luna.skin.global.security;

import com.luna.skin.global.exception.CommonErrorCode;
import com.luna.skin.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private static final String CURRENT_USER_ID_HEADER = "X-USER-ID";
    private final HttpServletRequest request;

    public Long getCurrentUserId() {
        String headerValue = request.getHeader(CURRENT_USER_ID_HEADER);
        if (!StringUtils.hasText(headerValue)) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(headerValue);
        } catch (NumberFormatException e) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
    }
}
