package com.zhf.mall.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhf.mall.common.Result;
import com.zhf.mall.service.ProfileService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProfileController {
    @Resource private ProfileService profiles;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/users/me")
    public Result<ProfileService.Profile> me(Authentication auth) {
        return Result.success(profiles.get(auth.getName()));
    }

    /**
     * 更新当前用户信息
     */
    @PatchMapping("/users/me")
    public Result<ProfileService.Profile> update(Authentication auth, @RequestBody JsonNode body) {
        return Result.success(profiles.update(auth.getName(), body));
    }

    /**
     * 上传当前用户头像
     */
    @PostMapping(value = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ProfileService.Upload> upload(Authentication auth, @RequestParam("file") MultipartFile file) throws java.io.IOException {
        return Result.success(profiles.upload(auth.getName(), file));
    }

    /**
     * 获取用户头像
     */
    @GetMapping("/media/avatars/{id}")
    public ResponseEntity<byte[]> avatar(@PathVariable String id) {
        ProfileService.Avatar avatar = profiles.avatar(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(avatar.contentType()))
                .header("X-Content-Type-Options", "nosniff").body(avatar.bytes());
    }
}
