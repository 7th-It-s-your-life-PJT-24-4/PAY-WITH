package com.paywith.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.merchant.controller.MerchantController;
import com.paywith.merchant.mapper.MerchantMapper;
import com.paywith.merchant.service.MerchantService;
import com.paywith.payment.mapper.PaymentRequestMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * 배포(WebAppInitializer)와 동일한 조립으로 루트·서블릿 컨텍스트를 실제 로딩하는 스모크 테스트.
 *
 * <p>배경: PR#35의 WAR 기동 실패 2건(@Mapper 누락, Redis 빈 타입 불일치)은 스프링 조립 단계의
 * 오류라 목 기반 유닛 테스트 28건이 전부 통과하고도 잡히지 않았다. 이 테스트는 그 공백을 메운다 —
 * 빈 등록 누락·타입 불일치·매퍼 XML 파싱 오류가 커밋 시점에 드러난다.
 *
 * <p>실행 조건: RiskRuleCache 생성자가 risk_rules를 실제 조회하므로 로컬 MySQL(docker compose)이
 * 필요하다. 3306 미기동이면 실패 대신 스킵한다 — DB 없는 환경에서 빌드를 막지 않기 위해서다.
 */
class RootContextSmokeTest {

    @BeforeAll
    static void requireLocalMysql() {
        Assumptions.assumeTrue(reachable("localhost", 3306),
            "로컬 MySQL(3306) 미기동 — 컨텍스트 스모크 테스트를 스킵한다 (be에서 docker compose up -d mysql)");
    }

    @Test
    void 배포와_동일한_루트_서블릿_컨텍스트가_로딩된다() {
        AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
        AnnotationConfigWebApplicationContext servlet = new AnnotationConfigWebApplicationContext();
        try {
            // WebAppInitializer.getRootConfigClasses()와 동일 조립
            root.setServletContext(new MockServletContext());
            root.register(AppConfig.class, MyBatisConfig.class, SecurityConfig.class, RedisConfig.class);
            root.refresh();

            // 루트: 서비스·매퍼가 조립되는 컨텍스트 (@Mapper 누락 시 refresh 또는 여기서 실패)
            assertThat(root.getBean(MerchantMapper.class)).isNotNull();
            assertThat(root.getBean(MerchantService.class)).isNotNull();
            assertThat(root.getBean(PaymentRequestMapper.class)).isNotNull();

            // 매퍼 인터페이스 <-> XML id 정합 (XML 미배치·네임스페이스 오타 시 여기서 실패)
            SqlSessionFactory sqlSessionFactory = root.getBean(SqlSessionFactory.class);
            assertThat(sqlSessionFactory.getConfiguration()
                .hasStatement("com.paywith.merchant.mapper.MerchantMapper.findAllPayable")).isTrue();
            assertThat(sqlSessionFactory.getConfiguration()
                .hasStatement("com.paywith.merchant.mapper.MerchantMapper.findById")).isTrue();

            // WebAppInitializer.getServletConfigClasses()와 동일 조립 — 컨트롤러가 서블릿
            // 컨텍스트에 등록되는지 검증한다 (과거 WAR 전체 404의 원인 지점)
            servlet.setParent(root);
            servlet.setServletContext(new MockServletContext());
            servlet.register(WebConfig.class, SwaggerConfig.class);
            servlet.refresh();

            assertThat(servlet.getBean(MerchantController.class)).isNotNull();
        } finally {
            servlet.close();
            root.close();
        }
    }

    private static boolean reachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
