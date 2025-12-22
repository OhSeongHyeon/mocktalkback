-- =================================
-- mocktalk project
-- mocktalk_dml_insert.sql
-- dml 쿼리는 이쪽에서 작성한다.
-- =================================

-- DROP DATABASE IF EXISTS mocktalk;
-- CREATE DATABASE mocktalk;
-- USE mocktalk;

-- admin 64, teacher 32, mentor 16, student 8, member 4
-- select * from tb_role;

INSERT INTO tb_role
    (role_name, auth_lv)
VALUES
    ('admin', 64),
    ('teacher', 32),
    ('mentor', 16),
    ('student', 8),
    ('member', 4)
;

-- select * from tb_member;

INSERT INTO tb_member
SET
    role_id = 1,
    profile_image_id = NULL,
    login_id = 'admin',
    login_pw_hash = '$2a$10$4oIZJpmCXgLw/eGB6ELtNO614Wp6N0c/5ehh511IDgQNE5ULrA/Y6', -- '1111'
    member_name = '어드민',
    nick_name = '주인장',
    email = 'admin@tmp.com',
    grow_point = 0
;
INSERT INTO tb_member
SET
    role_id = 2,
    profile_image_id = NULL,
    login_id = 'teacher',
    login_pw_hash = '$2a$10$8cvZFePaZTb.F5aqMmm65O1A63zhJxP9nE4LiDucnFr4Gri2E.mtS', -- '1111'
    member_name = '선생님',
    nick_name = '티처',
    email = 'teacher@tmp.com',
    grow_point = 0
;
INSERT INTO tb_member
SET
    role_id = 3,
    profile_image_id = NULL,
    login_id = 'mentor',
    login_pw_hash = '$2a$10$oyj95GG5MlAI.rhC8rbkPeQZk9QM5hK74Q0XLGFzTD0L7kt3XkS0O', -- '1111'
    member_name = '멘토',
    nick_name = '메멘토',
    email = 'mentor@tmp.com',
    grow_point = 0
;
INSERT INTO tb_member
SET
    role_id = 4,
    profile_image_id = NULL,
    login_id = 'student',
    login_pw_hash = '$2a$10$3YkYyfotWA2TqGxgbM2/4ORon7JrAKr4PI/YRlKyN7CZ5x/fE9dEu', -- '1111'
    member_name = '학생',
    nick_name = '고기스튜',
    email = 'student@tmp.com',
    grow_point = 0
;
INSERT INTO tb_member
SET
    role_id = 5,
    profile_image_id = NULL,
    login_id = 'member',
    login_pw_hash = '$2a$10$s1KXOqSwdBZHs9Vut3cKPOgByNhXZzbzd9qV.XHP2pc4wkIXsddIm', -- '1111'
    member_name = '김멤버',
    nick_name = '리멤버',
    email = 'member@tmp.com',
    grow_point = 0
;
INSERT INTO tb_member
SET
    role_id = 1,
    profile_image_id = NULL,
    login_id = 'qqqq',
    login_pw_hash = '$2a$10$31nCUhSmMClTlprvUESOc.nCxofzCYgyulSVsncLdSdS.WhoDY8jG', -- 'qqqq'
    member_name = '어드민qqqq',
    nick_name = '어드민닉네임qqqq',
    email = 'qqqq@tmp.com',
    grow_point = 0
;

