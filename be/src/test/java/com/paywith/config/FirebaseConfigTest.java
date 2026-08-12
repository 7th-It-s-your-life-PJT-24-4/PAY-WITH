package com.paywith.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.firebase.FirebaseApp;
import com.paywith.external.fcm.FirebasePushClient;
import com.paywith.external.fcm.NoOpPushClient;
import com.paywith.external.fcm.PushClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * build.gradle 에서 firebase-admin 의 firestore·storage·netty·commons-logging 을 제외했다.
 * 그 제외가 FCM 초기화 경로를 건드리면 컴파일이 아니라 실행 시점에 NoClassDefFoundError 로
 * 터지므로, 실제로 FirebaseApp 을 띄워 확인한다. 발송까지는 하지 않는다(네트워크·실제 키 필요).
 */
class FirebaseConfigTest {

    @TempDir
    Path tempDir;

    private final FirebaseConfig firebaseConfig = new FirebaseConfig();

    @AfterEach
    void tearDown() {
        // FirebaseApp 은 JVM 전역 정적 상태라 지우지 않으면 다음 테스트가 이걸 물려받는다.
        firebaseConfig.shutdown();
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
    }

    @Test
    @DisplayName("발송이 꺼져 있으면 무동작 구현을 준다")
    void pushClient_returnsNoOpWhenDisabled() {
        ReflectionTestUtils.setField(firebaseConfig, "enabled", false);
        ReflectionTestUtils.setField(firebaseConfig, "credentialsPath", "");

        PushClient pushClient = firebaseConfig.pushClient();

        assertThat(pushClient).isInstanceOf(NoOpPushClient.class);
    }

    @Test
    @DisplayName("자격증명이 있으면 FirebaseApp 을 띄우고 실제 구현을 준다")
    void pushClient_initializesFirebaseWhenEnabled() throws Exception {
        Path credentials = writeServiceAccountJson();
        ReflectionTestUtils.setField(firebaseConfig, "enabled", true);
        ReflectionTestUtils.setField(firebaseConfig, "credentialsPath", credentials.toString());

        PushClient pushClient = firebaseConfig.pushClient();

        assertThat(pushClient).isInstanceOf(FirebasePushClient.class);
        assertThat(FirebaseApp.getApps()).isNotEmpty();
    }

    @Test
    @DisplayName("켜 두고 자격증명이 없으면 기동을 막는다")
    void pushClient_throwsWhenCredentialsFileMissing() {
        ReflectionTestUtils.setField(firebaseConfig, "enabled", true);
        ReflectionTestUtils.setField(
            firebaseConfig, "credentialsPath", tempDir.resolve("없는파일.json").toString());

        assertThatThrownBy(firebaseConfig::pushClient)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("FCM 자격증명을 읽지 못했습니다");
    }

    /**
     * 서비스 계정 키 형식의 파일을 만든다. 키는 테스트 실행마다 새로 만들어 쓰고 버린다 —
     * 형식만 맞으면 초기화는 통과하므로 진짜 키를 저장소에 둘 이유가 없다.
     */
    private Path writeServiceAccountJson() throws IOException, NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String encodedKey = Base64.getMimeEncoder(64, new byte[] {'\n'})
            .encodeToString(keyPair.getPrivate().getEncoded());
        String privateKeyPem =
            "-----BEGIN PRIVATE KEY-----\\n" + encodedKey.replace("\n", "\\n")
                + "\\n-----END PRIVATE KEY-----\\n";

        String json = "{\n"
            + "  \"type\": \"service_account\",\n"
            + "  \"project_id\": \"pay-with-test\",\n"
            + "  \"private_key_id\": \"test-key-id\",\n"
            + "  \"private_key\": \"" + privateKeyPem + "\",\n"
            + "  \"client_email\": \"test@pay-with-test.iam.gserviceaccount.com\",\n"
            + "  \"client_id\": \"123456789\",\n"
            + "  \"token_uri\": \"https://oauth2.googleapis.com/token\"\n"
            + "}";

        Path path = tempDir.resolve("service-account.json");
        Files.writeString(path, json);
        return path;
    }
}
