CREATE TABLE IF NOT EXISTS users (
  user_id     BIGINT       NOT NULL AUTO_INCREMENT,
  role        ENUM('SENIOR','GUARD') NOT NULL,
  name        VARCHAR(50)  NOT NULL,
  phone       VARCHAR(20)  NOT NULL,
  password    VARCHAR(255) NOT NULL,
  birth_date  DATE         NOT NULL,
  gender      CHAR(1)      NOT NULL COMMENT '주민등록번호 뒷자리 첫 번째 숫자 (1/2/3/4)',
  fcm_token   VARCHAR(255) NULL,
  status      ENUM('PENDING_PAIRING','ACTIVE','WITHDRAWN') NOT NULL DEFAULT 'PENDING_PAIRING',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS guard_senior (
  relation_id  BIGINT   NOT NULL AUTO_INCREMENT,
  guard_id     BIGINT   NOT NULL,
  senior_id    BIGINT   NOT NULL,
  status       ENUM('PENDING','ACTIVE','REJECTED','REVOKED') NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING=연동요청 / ACTIVE=승인 / REJECTED=연동거절 / REVOKED=해제. 여기 REJECTED는 페어링 거절이며 transactions.REJECTED와 무관',
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  connected_at DATETIME NULL,
  revoked_at   DATETIME NULL,
  PRIMARY KEY (relation_id),
  UNIQUE KEY uk_guard_senior (guard_id, senior_id),
  KEY idx_gs_senior (senior_id),
  CONSTRAINT fk_gs_guard  FOREIGN KEY (guard_id)  REFERENCES users (user_id),
  CONSTRAINT fk_gs_senior FOREIGN KEY (senior_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
