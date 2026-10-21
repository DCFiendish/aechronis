# Aechronis

Kotlin/Gradle Minestom server (`server/`) plus reloadable runtime modules (`modules/*`), a VitePress docs site (`site/`) and a deck.gl nodes map (`map/`).

## Requirements
- JDK 25 (Gradle toolchain is pinned to 25; the cloud SessionStart hook installs it at `/opt/jdk25`).
- Node >= 22 for `site/` and `map/`.
- Maven repos used: Maven Central, ViaVersion, JitPack, lucko, hypera, smolder, everbuild, conceptmc, enginehub, and GitHub Packages (`Error11O/MinecraftPlugin`, needs `GITHUB_ACTOR`/`GITHUB_TOKEN` with `read:packages`). In cloud sessions these hosts must be in the environment's network allowlist.

## Commands
- `./gradlew build` — compile, ktlint check and tests (what CI runs, with `assembleServerDistribution`).
- `./gradlew ktlintFormat` — fix lint issues.
- `./gradlew devRun` — run the dev server in `run/` with module hot reload.
- `./gradlew assembleServerDistribution` — runnable layout in `build/distributions/aechronis/`.
- `cd site && npm run dev|build`, `cd map && npm run build`.

## Conventions
- New runtime modules must be added to both `settings.gradle.kts` and `runtimeModuleProjects` in `build.gradle.kts`.
- Runtime data dirs (`run/`, `logs/`, etc.) are gitignored; don't commit them.
