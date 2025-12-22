-- =================================
-- mocktalk project
-- mocktalk_dml.sql
-- dml 쿼리는 이쪽에서 작성한다.
-- =================================

-- DROP DATABASE IF EXISTS mocktalk;
-- CREATE DATABASE mocktalk;
-- USE mocktalk;


SELECT
    m.member_id AS m_member_id
,   m.role_id AS m_role_id
,   m.profile_image_id AS m_profile_image_id
,   m.login_id AS m_login_id
,   m.login_pw_hash AS m_login_pw_hash
,   m.member_name AS m_member_name
,   m.nick_name AS m_nick_name
,   m.email AS m_email
,   m.grow_point AS m_grow_point
,   m.created_at AS m_created_at
,   m.updated_at AS m_updated_at
,   m.deleted_flag AS m_deleted_flag
,   r.role_id AS r_role_id
,   r.role_name AS r_role_name
,   r.auth_lv AS r_auth_lv
,   r.created_at AS r_created_at
,   r.updated_at AS r_updated_at
,   r.deleted_flag AS r_deleted_flag
,   p.profile_image_id AS p_profile_image_id
,   p.file_name AS p_file_name
,   p.file_path AS p_file_path
,   p.file_uuid AS p_file_uuid
,   p.file_size AS p_file_size
,   p.file_type AS p_file_type
,   p.created_at AS p_created_at
,   p.updated_at AS p_updated_at
,   p.deleted_flag AS p_deleted_flag
FROM
    tb_member m
LEFT JOIN
    tb_role r
ON
    m.role_id = r.role_id
LEFT JOIN
    tb_profile_image p
ON
    m.profile_image_id = p.profile_image_id;



