# Repository working agreement

## Project

- Project: My Buddies.
- Root package: `com.austin`.
- Stack: Java 21, Spring Boot 4.1, Maven, MySQL, Redis,
  MyBatis-Plus, Spring Security, JWT, Lombok and Jakarta Validation.
- Architecture: modular monolith.
- Follow the existing structure:

```text
controller -> service -> mapper -> database
```

- Keep request, response and persistence entities separated.
- Do not implement unrelated or unconfirmed business features.

## Review boundary

When the user says `review`, `给我 review`, `评审`, `分析`, `探索` or `计划`,
treat the request as read-only.

For review-only tasks:

- Inspect existing code and documentation.
- Provide design, database, API, security, testing and risk recommendations.
- Do not edit files, create migrations, operate the database, start the application,
  or commit and push Git changes.
- Explicitly state that nothing was executed.

If a request contains both implementation language and `review`, treat it as
review-only until the user explicitly says `执行` or `开始实现`.

## Implementation boundary

When the user explicitly says `执行`, `实现`, `修改` or `修复`:

- Implement only the confirmed scope.
- Add or update code, tests, migrations and local documentation as needed.
- Preserve unrelated user changes.
- Run compilation, tests and `git diff --check`.
- Report changed files, behavior, test results and limitations.

Creating a Flyway migration file does not automatically authorize applying it
to the user's real MySQL database.

## Runtime and database

Only operate the real development database when the user explicitly asks to run
the project, apply migrations, create tables, or insert, update or delete records.

- Before starting the application, warn the user if Flyway will apply a migration.
- Use H2 and the test profile for automated tests by default.
- Never silently add example data to MySQL.
- Never modify an already-applied Flyway migration.
- Add a new migration version for every schema change.
- Use database constraints for foreign keys, uniqueness and valid states.
- Use Service validation for cross-table rules.
- Prefer lifecycle states or soft deletion when history must be preserved.

## Git

Do not commit, push, force-push, rewrite history, or create or delete branches
without explicit user authorization.

Before committing:

- Inspect `git status`.
- Exclude unrelated changes.
- Run appropriate tests.
- Summarize the exact commit scope.

Never force-push or rewrite history without immediate explicit confirmation.

## Documentation

- Every completed business feature must update the local
  `docs/My Buddies技术文档.md`.
- Keep it aligned with code, Flyway version, database model, API, security rules,
  tests, limitations and interview review points.
- The `docs` directory is local-only and ignored by Git.
- Review-only and formatting-only work does not require a business update.
- If the technical document is missing, report it before completing a feature.

Never store passwords, production secrets, JWT signing keys, access or refresh
tokens, verification codes, real identity numbers, or private credentials in
documentation or Git-tracked files.

## Security

- Public endpoints must be explicitly configured.
- Other endpoints require authentication by default.
- Use method-level authorization for business permissions.
- Validate resource ownership in the Service layer.
- Obtain the account ID from Spring Security.
- Do not trust account IDs or derived relationships from the frontend.
- Calculate or verify Topic/Circle relationships on the backend.
- Do not return persistence entities directly from Controllers.
- Do not expose unnecessary sensitive fields in responses.

## State and concurrency

- Represent business lifecycles with explicit enum states.
- Validate state transitions in the Service layer.
- Use transactions when updating state and audit records together.
- Use `version` optimistic locking for ordinary concurrent updates.
- When exactly one row should change, an affected-row count other than one is a conflict.
- Record sensitive administrator operations in audit tables.
- Do not physically delete audit records.

## Java formatting

In records, every annotation and component must use separate lines:

```java
public record CreatePostRequest(
        @NotBlank
        @Size(max = 2000)
        String content) {
}
```

In entities:

- Each class annotation occupies one line.
- Each field annotation occupies one line.
- The annotated field starts on the next line.
- Keep one blank line between fields.

In enums, never put constants on the declaration line. Each constant occupies
one line, with one blank line between constants:

```java
public enum PostStatus {

    PENDING_REVIEW,

    PUBLISHED,

    REJECTED
}
```

## API conventions

- Use `/api/v1`.
- Validate requests with Jakarta Validation.
- Use the existing `ApiResponse` and global exception handling.
- Put request and response records under their Controller packages.
- Collection endpoints should use pagination: page 1, size 20, maximum size 100.
- Use 404 for missing or invisible resources.
- Use 403 for insufficient permission.
- Use 409 for invalid states, conflicting relations and optimistic-lock failure.
- JSON request bodies require `Content-Type: application/json`.

## Testing

Each business feature should test relevant successful flows, authentication,
authorization, ownership, validation, state transitions, database constraints,
association consistency, public visibility, audit records and concurrency.

After implementation:

- Run the full Maven test suite.
- Run `git diff --check`.
- Scan records, entities and enums for formatting violations.
- Stop temporary application processes.
- Do not leave test records in MySQL.

## Communication

- Lead with the result.
- Distinguish implemented behavior from proposed behavior.
- Never describe unimplemented functionality as complete.
- State limitations honestly.
- Use the smallest safe interpretation of ambiguous requirements.
- Ask before changing a major database, security or product boundary.
- Never expose secrets in responses, logs or documentation.
