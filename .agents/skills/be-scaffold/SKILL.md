---
name: be-scaffold
description: "Spring Legacy backend scaffolding for this repository. Use when the user asks to create or modify a REST resource end to end in `be`: controller, service, MyBatis mapper interface/XML, domain class, and request/response DTOs. Also use for DB schema or seed changes tied to the new resource. Do not use when the main deliverable is JUnit/Mockito tests; use be-test. Do not use for PR creation or review comment handling."
---

# be-scaffold

Use this skill to add or modify production BE files in `be` using the conventions in root `AGENTS.md`.

## Workflow

1. Read root `AGENTS.md` first and follow its BE layering, API contract, and validation conventions.
2. Inspect the current backend before editing:

```bash
cd be
find src/main/java/com/paywith -type f | sort
sed -n '1,80p' src/main/java/com/paywith/controller/UserController.java
sed -n '1,80p' src/main/java/com/paywith/service/UserService.java
sed -n '1,40p' src/main/java/com/paywith/mapper/UserMapper.java
sed -n '1,60p' src/main/resources/mappers/UserMapper.xml
sed -n '1,80p' src/main/resources/db/schema.sql
```

3. Decide the file targets for resource `<Name>`:
   - Controller: `src/main/java/com/paywith/controller/<Name>Controller.java`
   - Service: `src/main/java/com/paywith/service/<Name>Service.java`
   - Mapper interface: `src/main/java/com/paywith/mapper/<Name>Mapper.java`
   - Mapper XML: `src/main/resources/mappers/<Name>Mapper.xml`
   - Domain: `src/main/java/com/paywith/domain/<Name>.java`
   - DTOs: `src/main/java/com/paywith/dto/<Name>CreateRequest.java`, `<Name>UpdateRequest.java`, `<Name>Response.java`
4. Create only the files needed by the request. Do not add a layer the request does not touch.
5. Keep the `controller -> service -> mapper -> domain/dto` flow. Controllers stay thin and only call the service, then wrap the result with `ApiResponse.success(...)`. Put validation annotations (`@Valid`, Bean Validation) on request DTOs, not in the controller body.
6. Put business rules, existence checks, and `@Transactional` boundaries in the service layer. Throw `BusinessException` with an appropriate `HttpStatus` for not-found/conflict cases; do not catch and reformat it in the controller, `GlobalExceptionHandler` already handles it.
7. Keep the mapper interface method signatures and the XML `id`s in 1:1 sync. Match the `resultMap` columns (snake_case) to domain fields (camelCase) the way `UserMapper.xml` does.
8. If the resource needs a new table or column, update `src/main/resources/db/schema.sql` (and `data.sql` only if seed rows are actually needed for local dev). Do not rename or drop existing columns without explicit confirmation from the user.
9. If the endpoint needs auth rules beyond the current defaults, check `src/main/java/com/paywith/config/SecurityConfig.java` before editing it; do not loosen JWT/security matchers without the user's explicit request.
10. Run validation from `be`:

```bash
mvn -B clean package
```

## Layer pattern

```java
// controller
@RestController
@RequestMapping("/api/<resources>")
public class <Name>Controller {

    private final <Name>Service <name>Service;

    public <Name>Controller(<Name>Service <name>Service) {
        this.<name>Service = <name>Service;
    }

    @GetMapping
    public ApiResponse<List<<Name>Response>> findAll() {
        return ApiResponse.success(<name>Service.findAll());
    }
}
```

```java
// service
@Service
public class <Name>Service {

    private final <Name>Mapper <name>Mapper;

    public <Name>Service(<Name>Mapper <name>Mapper) {
        this.<name>Mapper = <name>Mapper;
    }

    @Transactional
    public <Name>Response create(<Name>CreateRequest request) {
        // 존재/중복 검증 후 domain 생성 -> mapper.insert -> Response 변환
    }
}
```

```java
// mapper interface
public interface <Name>Mapper {
    List<Name> findAll();
    <Name> findById(@Param("id") Long id);
    int insert(<Name> entity);
    int update(<Name> entity);
    int delete(@Param("id") Long id);
}
```

Keep DTOs immutable-style with a constructor that copies fields from the domain object, matching `UserResponse`.
