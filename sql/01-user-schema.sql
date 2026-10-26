-- 当前注册工程的 MySQL 8+ 建表脚本，仅新建 user 表，不修改旧 users 表。
USE shiguang_market_dev;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(32) NOT NULL,
    avatar_file_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT ck_user_username CHECK (REGEXP_LIKE(username, '^[A-Za-z0-9_]{3,32}$')),
    CONSTRAINT ck_user_nickname CHECK (CHAR_LENGTH(TRIM(nickname)) BETWEEN 1 AND 32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户注册账号';
