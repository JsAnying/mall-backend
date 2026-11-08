-- 可选：在确认目标 user 表为空、列结构正确后，手动复制旧账号。
-- 先执行 01-user-schema.sql。本脚本只读旧 users 表，不删除或更名任何表。
-- 不使用 INSERT IGNORE 或 REPLACE；存在 ID/用户名冲突时应报错并停止处理。
-- 仅用于仍保留旧 users 表的数据库。本地开发库已在 07 迁移中删除旧表。
USE shiguang_market_dev;

INSERT INTO `user` (id, username, password_hash, nickname, avatar_file_id, created_at, updated_at)
SELECT id, username, password_hash, nickname, avatar_file_id, created_at, updated_at
FROM users;
