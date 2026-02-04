package com.example.stocktrading.auth.adapter.in.web;

import com.example.stocktrading.auth.application.port.in.UserUseCase;
import com.example.stocktrading.auth.application.port.in.UserUseCase.LoginResult;
import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.RequireAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserUseCase userUseCase;

    @PostMapping("/register")
    public ApiResponse<ApiResponse.MsgData> register(@RequestBody AuthRequest request) {
        userUseCase.register(request.username(), request.password());
        return ApiResponse.successOnlyMsg("가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody AuthRequest request) {
        LoginResult result = userUseCase.login(request.username(), request.password());
        return ApiResponse.success(Map.of(
                "token", result.token(),
                "role", result.role()));
    }

    @GetMapping("/users")
    @RequireAuth(adminOnly = true)
    public ApiResponse<List<UserResponse>> getUsers() {
        List<UserResponse> users = userUseCase.getAllUsers().stream()
                .map(u -> new UserResponse(u.id(), u.username(), u.status()))
                .toList();
        return ApiResponse.success(users);
    }

    @PostMapping("/users/{userId}/approve")
    @RequireAuth(adminOnly = true)
    public ApiResponse<ApiResponse.MsgData> approveUser(@PathVariable Long userId) {
        userUseCase.approveUser(userId);
        return ApiResponse.successOnlyMsg("사용자가 승인되었습니다.");
    }

    @DeleteMapping("/users/{userId}")
    @RequireAuth(adminOnly = true)
    public ApiResponse<ApiResponse.MsgData> deleteUser(@PathVariable Long userId) {
        userUseCase.deleteUser(userId);
        return ApiResponse.successOnlyMsg("사용자가 삭제되었습니다.");
    }

    public record AuthRequest(String username, String password) {
    }

    public record UserResponse(Long id, String username, String status) {
    }
}
