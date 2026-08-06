package com.paywith.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@MapperScan(basePackages = "com.paywith", annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EnableTransactionManagement
public class MyBatisConfig {

    @Bean
    public DataSource dataSource(
        @Value("${db.driver}") String driverClassName,
        @Value("${db.url}") String jdbcUrl,
        @Value("${db.username}") String username,
        @Value("${db.password}") String password,
        @Value("${db.pool.maximum-pool-size}") int maximumPoolSize
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maximumPoolSize);
        return dataSource;
    }

    /**
     * 기동 시 db/migration 의 마이그레이션을 적용한다.
     *
     * <p>{@code baselineOnMigrate} 는 이미 테이블이 있는 기존 DB(RDS)를 위한 설정이다. 스키마
     * 이력 테이블이 없고 스키마가 비어 있지 않으면 V1 을 실행하지 않고 적용된 것으로 기록만
     * 한다. 빈 DB(로컬 docker compose)에서는 V1 이 실제로 실행된다.
     *
     * <p>{@code outOfOrder} 는 PR 머지 순서가 파일명의 타임스탬프 순서와 어긋나도 뒤늦게 들어온
     * 마이그레이션을 적용하기 위한 설정이다. 끄면 조용히 건너뛴다.
     *
     * <p>이 빈을 별도 설정 클래스가 아니라 여기에 두는 이유: 루트 컨텍스트 구성 목록이
     * {@code WebAppInitializer} 와 {@code RootContextSmokeTest} 두 곳에 중복되어 있어, 새 설정
     * 클래스를 한쪽에만 추가하면 로컬에서만 스모크 테스트가 깨진다. 이미 양쪽에 등록된
     * MyBatisConfig 안에 두면 그 실수가 원천적으로 불가능하고, 아래 {@code @DependsOn} 과
     * 나란히 있어 순서 의도도 한눈에 드러난다.
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .outOfOrder(true)
            .load();
    }

    /**
     * {@code @DependsOn("flyway")} 은 매퍼가 준비되기 전에 스키마가 최신이어야 하기 때문이다.
     * 이게 없으면 마이그레이션과 매퍼 초기화 순서가 보장되지 않아, 옛 스키마 위에서 매퍼가
     * 뜨는 순간이 생길 수 있다.
     */
    @Bean
    @DependsOn("flyway")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.paywith");
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver().getResources("classpath:/mappers/**/*.xml")
        );

        // MyBatis 세부 옵션 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
