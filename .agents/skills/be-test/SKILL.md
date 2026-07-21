---
name: be-test
description: "Backend testing skill for this repository. Use when the user asks to add or update JUnit/Mockito unit tests for existing Spring Legacy controllers, services, or mappers in `be`. Use when the main deliverable is tests. Do not use to create production controllers, services, mappers, domain classes, or DTOs; use be-scaffold. Do not use for PR creation or review comment handling."
---

# be-test

Use this skill to test existing BE behavior in `be`.

## Workflow

1. Read root `AGENTS.md` and follow its BE conventions.
2. Inspect the target production code and current test setup:

```bash
cd be
sed -n '1,150p' pom.xml
find src/test -type f | sort
find src/main/java/com/paywith -type f | sort
```

3. Confirm test dependencies exist in `pom.xml` under `<dependencies>` with `<scope>test</scope>`: `org.junit.jupiter:junit-jupiter`, `org.mockito:mockito-junit-jupiter`, and `org.springframework:spring-test` (for controller-layer `MockMvc`). If any are missing, add them with a version consistent with `java.version` 17 and the existing `spring.version` property before writing tests. Do not add a full Spring Boot test starter; this is a Spring Framework legacy WAR project.
4. Choose test type per layer:
   - Service layer: mock the mapper with Mockito, assert business rules and `BusinessException` cases (not-found, conflict).
   - Controller layer: use `MockMvcBuilders.standaloneSetup(controller)` so no full Spring context is required, assert HTTP status and the `ApiResponse` JSON envelope.
   - Mapper layer: only add MyBatis-backed tests when the user explicitly asks for DB-integration coverage; otherwise leave mapper XML untested at the unit level.
5. Add tests without changing production code unless a real bug blocks the test. If production changes are needed, stop and explain the bug before editing.
6. Place test files mirroring the main package path:
   - `src/test/java/com/paywith/service/<Name>ServiceTest.java`
   - `src/test/java/com/paywith/controller/<Name>ControllerTest.java`
7. Run:

```bash
mvn test
```

## Service test pattern

```java
@ExtendWith(MockitoExtension.class)
class <Name>ServiceTest {

    @Mock
    private <Name>Mapper <name>Mapper;

    @InjectMocks
    private <Name>Service <name>Service;

    @Test
    void findById_존재하지_않으면_BusinessException() {
        given(<name>Mapper.findById(1L)).willReturn(null);

        assertThatThrownBy(() -> <name>Service.findById(1L))
            .isInstanceOf(BusinessException.class);
    }
}
```

## Controller test pattern

```java
class <Name>ControllerTest {

    private MockMvc mockMvc;

    @Mock
    private <Name>Service <name>Service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new <Name>Controller(<name>Service)).build();
    }

    @Test
    void findAll_성공시_success_true() throws Exception {
        given(<name>Service.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/<resources>"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```
