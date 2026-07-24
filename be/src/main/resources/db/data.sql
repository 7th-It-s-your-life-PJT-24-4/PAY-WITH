INSERT INTO users (role, name, phone, password, birth_date, gender, status)
VALUES ('GUARD', 'Admin', '01000000000', 'password123', '1990-01-01', '1', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name);
