package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.*;
import com.review.system.entity.SysRole;
import com.review.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserVO>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(sysUserService.listUsers(keyword));
    }

    @GetMapping("/api/users/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysRole>> roles() {
        return Result.ok(sysUserService.listRoles());
    }

    @PostMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest req) {
        return Result.ok(sysUserService.createUser(req));
    }

    @PutMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> update(@PathVariable Long id, @RequestBody UserUpdateRequest req) {
        return Result.ok(sysUserService.updateUser(id, req));
    }

    @GetMapping("/api/users/applicants")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REVIEW_ADMIN')")
    public Result<List<UserVO>> applicants() {
        return Result.ok(sysUserService.listApplicantUsers());
    }

    @PostMapping("/api/auth/change-password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        sysUserService.changePassword(req);
        return Result.ok();
    }

    @Data
    public static class TestEmailRequest {
        private String to;
        private String subject;
        private String content;
    }
}
