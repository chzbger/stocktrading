package com.example.stocktrading.common.security;

import com.example.stocktrading.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthAspect {

    private final JwtService jwtService;

    @Around("@within(requireAuth) || @annotation(requireAuth)")
    public Object checkAuth(ProceedingJoinPoint joinPoint, RequireAuth requireAuth) throws Throwable {
        try {
            RequireAuth annotation = requireAuth;
            if (annotation == null) {
                annotation = joinPoint.getTarget().getClass().getAnnotation(RequireAuth.class);
            }

            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[Auth] invalid Authorization");
                return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "인증 토큰이 필요합니다.");
            }

            String token = authHeader.substring(7);
            AuthContext.AuthInfo authInfo = jwtService.validateToken(token);
            if (authInfo == null) {
                log.warn("[Auth] Invalid JWT token");
                return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "유효하지 않거나 만료된 토큰입니다.");
            }

            if (annotation != null && annotation.adminOnly()) {
                if (!"ROLE_ADMIN".equals(authInfo.role())) {
                    log.warn("[Auth] Admin required: {}", authInfo.role());
                    return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "관리자 권한이 필요합니다.");
                }
            }

            AuthContext.set(authInfo);
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("[Auth] Authentication failed", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "인증 처리 중 오류가 발생했습니다.");
        } finally {
            AuthContext.clear();
        }
    }
}
