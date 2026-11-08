package com.zhf.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhf.mall.common.ResultCode;
import com.zhf.mall.entity.User;
import com.zhf.mall.entity.UserAvatar;
import com.zhf.mall.exception.BusinessException;
import com.zhf.mall.mapper.UserAvatarMapper;
import com.zhf.mall.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ProfileService {
    @Resource private UserMapper users; // 用户Mapper
    @Resource private UserAvatarMapper avatars; // 头像Mapper

    public record Profile(String id, String username, String nickname, String avatarUrl) {}
    public record Upload(String avatarFileId, String avatarUrl) {}
    public record Avatar(String contentType, byte[] bytes) {}

    /** 根据用户名获取用户。 */
    private User user(String username) {
        User user = users.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        return user;
    }
    /** 获取用户资料。 */
    public Profile get(String username) { return profile(user(username)); }

    /** 将用户转换为用户资料。 */
    private Profile profile(User user) {
        return new Profile(user.getId().toString(), user.getUsername(), user.getNickname(),
                user.getAvatarFileId() == null ? null : "/api/media/avatars/" + user.getAvatarFileId());
    }

    /** 判断头像是否存在且属于指定用户。 */
    private boolean ownsAvatar(Long userId, String avatarId) {
        Long count = avatars.selectCount(new LambdaQueryWrapper<UserAvatar>()
                .eq(UserAvatar::getId, avatarId)
                .eq(UserAvatar::getUserId, userId));
        return count != null && count > 0;
    }

    /**
     * 更新用户资料。
     * @param username 当前用户
     * @param body 请求体
     * @return 更新后的用户资料
     */
    @Transactional
    public Profile update(String username, JsonNode body) {
        if (!body.isObject() || (!body.has("nickname") && !body.has("avatarFileId")) ||
                body.properties().stream().anyMatch(e -> !e.getKey().equals("nickname") && !e.getKey().equals("avatarFileId")))
            throw new BusinessException(ResultCode.BAD_REQUEST, "请提供昵称或头像");
        User current = user(username);
        if (body.has("nickname")) {
            JsonNode value = body.get("nickname");
            if (!value.isTextual() || value.textValue().trim().isEmpty() || value.textValue().trim().length() > 32)
                throw new BusinessException(ResultCode.BAD_REQUEST, "昵称须为 1～32 位非空字符");
        }
        if (body.has("avatarFileId")) {
            JsonNode value = body.get("avatarFileId");
            if (!value.isNull() && (!value.isTextual() || value.textValue().length() > 64 ||
                    !ownsAvatar(current.getId(), value.textValue())))
                throw new BusinessException(ResultCode.BAD_REQUEST, "头像不存在或不属于当前用户");
        }
        // 仅更新请求明确提供的字段，允许显式清除头像。
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>()
                .eq(User::getId, current.getId())
                .set(User::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC));
        if (body.has("nickname")) {
            update.set(User::getNickname, body.get("nickname").textValue().trim());
        }
        if (body.has("avatarFileId")) {
            JsonNode value = body.get("avatarFileId");
            update.set(User::getAvatarFileId, value.isNull() ? null : value.textValue());
        }
        users.update(null, update);
        if (body.has("nickname")) current.setNickname(body.get("nickname").textValue().trim());
        if (body.has("avatarFileId")) current.setAvatarFileId(body.get("avatarFileId").isNull() ? null : body.get("avatarFileId").textValue());
        return profile(current);
    }

    /**
     * 上传头像。
     * @param username 当前用户
     * @param file 头像文件
     * @return 头像文件 ID 与 URL
     * @throws IOException 读取文件失败
     */
    public Upload upload(String username, MultipartFile file) throws IOException {
        User current = user(username);
        if (file.isEmpty() || file.getSize() > 512 * 1024)
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像大小须在 512KB 以内");
        byte[] bytes = file.getBytes();
        String type;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException(ResultCode.BAD_REQUEST, "请选择 PNG 或 JPEG 图片");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg"))
                    throw new BusinessException(ResultCode.BAD_REQUEST, "请选择 PNG 或 JPEG 图片");
                if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096 || reader.read(0) == null)
                    throw new BusinessException(ResultCode.BAD_REQUEST, "头像尺寸过大或图片无效");
                type = format.equalsIgnoreCase("png") ? "image/png" : "image/jpeg";
            } finally { reader.dispose(); }// 释放资源
        } catch (IOException e) { throw new BusinessException(ResultCode.BAD_REQUEST, "图片文件无效"); }
        String id = UUID.randomUUID().toString();
        avatars.insert(new UserAvatar(id, current.getId(), type, bytes));
        return new Upload(id, "/api/media/avatars/" + id);
    }

    /**
     * 获取头像。
     * @param id 头像文件 ID
     * @return 头像文件的 MIME 类型与二进制数据
     */
    public Avatar avatar(String id) {
        UserAvatar file = avatars.selectOne(new LambdaQueryWrapper<UserAvatar>()
                .select(UserAvatar::getContentType, UserAvatar::getData)
                .eq(UserAvatar::getId, id));
        if (file == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return new Avatar(file.getContentType(), file.getData());
    }
}
