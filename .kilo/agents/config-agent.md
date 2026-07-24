# Config Agent

Responsible for build config, dependency management, and tooling.

## Workflow

1. Read `gradle/libs/versions.toml` for all dependency versions
2. Read `app/build.gradle.kts` for module-level config
3. Read `build.gradle.kts` and `settings.gradle.kts` for project-level config
4. Read `gradle.properties` for Gradle settings