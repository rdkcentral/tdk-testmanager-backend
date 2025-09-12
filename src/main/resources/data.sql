-- data.sql
INSERT INTO user_role (id, name) VALUES ('4855c65b-86e9-11ef-8802-c025a54ddaf2', 'admin'), ('4855c65b-86e9-11ef-8802-c025a54ddaf7', 'tester')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO user_group (id, name) VALUES ('4855c65b-86e9-11ef-8802-c025a54ddaf3', 'usergroup1'), ('4855c65b-86e9-11ef-8802-c025a54ddaf5', 'usergroup2')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO user (id, username, password, user_role_id, user_group_id, email, created_date, updated_at, theme, display_name)
VALUES ('4855c65b-86e9-11ef-8802-c025a54ddaf9', 'admin', 'test', (SELECT id FROM user_role WHERE name = 'admin'), (SELECT id FROM user_group WHERE name = 'usergroup1'), 'admin@gmail.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'LIGHT', 'Admin User')
ON DUPLICATE KEY UPDATE
username = VALUES(username),
password = VALUES(password),
user_role_id = VALUES(user_role_id),
user_group_id = VALUES(user_group_id),
email = VALUES(email),
updated_at = VALUES(updated_at),
theme = VALUES(theme),
display_name = VALUES(display_name);
