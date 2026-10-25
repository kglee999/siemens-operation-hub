# Repository Guidelines

## Project Structure & Module Organization

Operation Hub is a Java 17 Spring Boot 4.1.2 application packaged as a WAR. The repository currently contains the application skeleton; domain modules and persistence are not implemented.

- `src/main/java/com/operation/hub/`: application code, including `SiemensOperationHubApplication` and the external-container entry point, `ServletInitializer`. Place new packages beneath `com.operation.hub`.
- `src/main/resources/application.yaml`: application configuration.
- `src/main/resources/static/` and `templates/`: static assets and server-rendered templates.
- `src/test/java/com/operation/hub/`: tests; mirror production package paths.
- `pom.xml` and `.mvn/wrapper/`: dependencies, build configuration, and Maven wrapper settings. Generated artifacts belong in `target/`.

## Build, Test, and Development Commands

Use the Maven wrapper from the repository root with JDK 17 installed. PowerShell examples:

- `.\mvnw.cmd spring-boot:run`: start the application locally with DevTools available.
- `.\mvnw.cmd test`: run all tests.
- `.\mvnw.cmd test -Dtest=SiemensOperationHubApplicationTests`: run one test class.
- `.\mvnw.cmd clean package`: run tests and build the WAR in `target/`.
- `java -jar target/operation-hub-0.0.1-SNAPSHOT.war`: run the packaged application.

On Bash, replace `.\mvnw.cmd` with `./mvnw`.

## Coding Style & Naming Conventions

Match existing Java files: four-space indentation, opening braces on the declaration line, and explicit imports. Use lowercase package names, `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Use two spaces per YAML indentation level. Lombok annotation processing is configured; no formatter or linter is configured. Check Spring Boot 4 compatibility when adding dependencies or configuration.

## Testing Guidelines

Use JUnit Jupiter and Spring Boot testing support. Name test classes `*Tests` and methods descriptively. Retain the existing `@SpringBootTest` context-load check; add focused behavior tests for new functionality and security rules. Run `.\mvnw.cmd test` before submitting changes. No coverage threshold is configured.

## Commit & Pull Request Guidelines

Git metadata is unavailable in this checkout, so existing commit conventions cannot be verified. Use concise imperative subjects, such as `Add health endpoint tests`. Keep changes focused. Pull requests should explain the purpose, describe behavior changes, link relevant issues, and report validation commands and results. Include screenshots for visible UI changes.

## Security & Configuration

Keep credentials outside committed configuration; use environment variables or external configuration. Spring Security is included without custom rules. Test authentication and authorization explicitly when adding endpoints.
