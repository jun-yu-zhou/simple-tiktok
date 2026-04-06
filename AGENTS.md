# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/example/simpletiktok` holds the Spring Boot application code (entry point: `SimpleTiktokApplication.java`).
- `src/main/resources` contains configuration (`application.yml`) and static assets (`static/`, currently `init.sql`).
- `src/test/java/com/example/simpletiktok` contains tests (`*Tests.java`).
- Build metadata lives in `pom.xml` (Java 17, Spring Boot 3.5.x, MyBatis-Plus, RabbitMQ, MySQL, OSS, LangChain4j).

## Build, Test, and Development Commands
- `mvn clean test` runs unit/integration tests via `spring-boot-starter-test`.
- `mvn spring-boot:run` starts the app using the configured main class.
- `mvn -DskipTests package` builds a runnable JAR without executing tests.
- `mvn clean package` does a full build with tests.

## Coding Style & Naming Conventions
- Java 17, standard 4-space indentation.
- Package naming follows `com.example.simpletiktok`.
- Class names use `UpperCamelCase`; methods/fields use `lowerCamelCase`.
- Tests typically end with `Test` or `Tests` (e.g., `SimpleTiktokApplicationTests.java`).
- No formatter or linter is configured in this repo; use IDE defaults aligned with standard Java conventions.

## Testing Guidelines
- Framework: Spring Boot Test (JUnit 5).
- Place new tests under `src/test/java` mirroring production package structure.
- Name tests `*Test.java` or `*Tests.java` and keep them focused on one behavior per test.
- Run all tests with `mvn clean test` before opening a PR.

## Configuration & Secrets
- Runtime settings live in `src/main/resources/application.yml`.
- This repo includes default credentials and API keys for local development; override with environment variables (e.g., `DASHSCOPE_API_KEY`, `ALIYUN_OSS_ACCESS_KEY_ID`) for real deployments.
- Do not commit real credentials; prefer `.env` or CI secrets.

## Commit & Pull Request Guidelines
- No git history is available in this workspace, so no established commit convention can be inferred.
- Use concise, imperative commit subjects (e.g., “Add upload endpoint”) and group related changes.
- PRs should include a short description, configuration changes (if any), and testing notes (`mvn clean test`). Screenshots are only needed for UI changes.
