package com.paywith.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.paywith.external.fcm.FirebasePushClient;
import com.paywith.external.fcm.NoOpPushClient;
import com.paywith.external.fcm.PushClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FCM 발송 빈을 고른다.
 *
 * <p>Spring Boot 가 아니라 {@code @ConditionalOnProperty} 를 쓸 수 없어 팩토리 메서드 안에서
 * 직접 분기한다. 자격증명이 없는 로컬에서는 {@link NoOpPushClient} 가 주입되므로 서비스 계정
 * 키 없이도 기동한다.
 *
 * <p>반대로 {@code fcm.enabled=true} 인데 키를 읽지 못하면 기동을 막는다. 켜 두라고 명시한
 * 환경에서 조용히 발송이 빠지면, 알림이 안 가는 걸 아무도 모른 채 운영된다.
 */
@Configuration
public class FirebaseConfig {

    // FirebaseConfig에서 로그 출력하기 위한 slf4j 로거
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    // 초기화된 Firebase Admin SDK 애플리케이션을 보관하는 필드
    private FirebaseApp firebaseApp;

    // Spring 설정에서 fcm.enabled 값을 주입받음
    @Value("${fcm.enabled:false}")
    private boolean enabled;

    // Firebase 서비스 계정 JSON 파일의 경로를 주입받음
    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    @Bean
    public PushClient pushClient() {
        if (!enabled) {
            log.warn("FCM 발송이 꺼져 있어 알림은 저장만 되고 발송되지 않는다 (fcm.enabled=false)");
            return new NoOpPushClient();
        }
        firebaseApp = initializeFirebaseApp();
        log.info("FCM 발송 활성화됨");
        return new FirebasePushClient(FirebaseMessaging.getInstance(firebaseApp));
    }

    private FirebaseApp initializeFirebaseApp() {
        // 같은 JVM 에 WAR 를 다시 올리면 정적 등록이 남아 있어 initializeApp 이 예외를 던진다.
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(loadCredentials())
                .build();
            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new IllegalStateException(
                "FCM 자격증명을 읽지 못했습니다. fcm.credentials-path 를 확인하세요. path="
                    + (credentialsPath.isBlank() ? "(GOOGLE_APPLICATION_CREDENTIALS)" : credentialsPath),
                e);
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (credentialsPath.isBlank()) {
            return GoogleCredentials.getApplicationDefault();
        }
        try (InputStream in = new FileInputStream(credentialsPath)) {
            return GoogleCredentials.fromStream(in);
        }
    }

    /** 재배포 때 FirebaseApp 이 띄운 스레드가 남지 않게 정리한다. */
    @PreDestroy
    public void shutdown() {
        if (firebaseApp != null) {
            firebaseApp.delete();
            firebaseApp = null;
        }
    }
}
