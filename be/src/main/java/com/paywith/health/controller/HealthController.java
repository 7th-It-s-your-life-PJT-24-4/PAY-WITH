package com.paywith.health.controller;

import java.sql.Connection;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * 배포 확인용 상태 점검. Nginx 의 /healthz 가 이 경로로 프록시한다.
 *
 * <p>Nginx 가 직접 200 을 돌려주던 때에는 Spring 이 죽어 있어도 정상으로 보였다. 자동 배포에서는
 * 그 상태가 초록불로 끝나 아무도 장애를 모르게 되므로, 앱까지 실제로 닿는지 확인한다.
 *
 * <p>DataSource 까지 확인하는 이유는 HikariCP 가 지연 연결이라 잘못된 접속 정보로도 컨텍스트가
 * 뜨기 때문이다. 이 점검이 없으면 DB 설정 오류가 첫 실제 요청에서야 드러난다.
 */
@ApiIgnore
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /** 커넥션 검증 대기 한도(초). 배포 스크립트가 폴링하므로 짧게 잡아 빨리 실패시킨다. */
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<String> check() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                return ResponseEntity.ok("ok");
            }
            log.error("헬스 체크 실패 — DB 커넥션이 유효하지 않다");
        } catch (Exception exception) {
            log.error("헬스 체크 실패 — DB 연결 불가", exception);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("db unavailable");
    }
}
