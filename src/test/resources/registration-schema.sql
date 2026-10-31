-- 仅用于 H2 内存测试，不会创建或清理开发 MySQL 数据。
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(32) NOT NULL,
    avatar_file_id VARCHAR(64),
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT uk_user_username UNIQUE (username)
);
CREATE TABLE IF NOT EXISTS revoked_token (
    jti VARCHAR(36) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS user_avatar (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    data BLOB NOT NULL
);
