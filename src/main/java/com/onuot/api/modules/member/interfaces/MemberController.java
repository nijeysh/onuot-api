package com.onuot.api.modules.member.interfaces;

import com.onuot.api.modules.member.application.MemberService;
import com.onuot.api.modules.member.application.dto.LoginRequest;
import com.onuot.api.modules.member.application.dto.MemberResponse;
import com.onuot.api.modules.member.application.dto.SignUpRequest;
import com.onuot.api.global.auth.dto.TokenResponse;
import com.onuot.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/members/signup")
    public ApiResponse<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.ok(memberService.signUp(request));
    }

    @PostMapping("/auth/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(memberService.login(request));
    }

    @GetMapping("/members/me")
    public ApiResponse<MemberResponse> getMyInfo(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(memberService.getMember(memberId));
    }
}
