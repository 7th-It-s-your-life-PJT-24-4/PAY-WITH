INSERT INTO users (email, password, name)
VALUES ('admin@example.com', 'password123', 'Admin')
ON DUPLICATE KEY UPDATE name = VALUES(name);
