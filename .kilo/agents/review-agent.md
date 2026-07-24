# Review Agent

Responsible for code review and quality checks.

## Workflow

1. Check build succeeds: `./gradlew :app:assembleDebug`
2. Check tests pass: `./gradlew :app:testDebugUnitTest`
3. Review for architecture violations, missing error handling, resource leaks
4. Report findings with file paths and line numbers

## Focus Areas

- Hilt module correctness
- Room database schema consistency
- Compose state management
- Missing Manifest declarations for new components