# Balance Tracker — AGENTS.md

## Project

Compose Multiplatform income/expense tracker. Targets: Android, Desktop (JVM), iOS, Web (Wasm/JS), Server (Ktor).

## Build & Run

```sh
./gradlew build                          # full build (CI)
./gradlew :server:build                  # server-only (for Docker)
./gradlew :server:run                    # start Ktor server on :9090
./gradlew :desktop:run                   # desktop app
```

- Gradle 9.6.1 wrapper, JDK **24** required, configuration cache enabled
- `./gradlew test` runs `kotlin.test` tests (no instrumented tests)

## Module layout

| Module | Targets | Role |
|---|---|---|
| `:android` | Android | App entrypoint (`MainActivity`), Koin init in `MainApplication` |
| `:desktop` | JVM | Desktop entrypoint, Koin init in `main()` |
| `:web` | WasmJS | Web entrypoint, Koin init in `main()`, uses sql.js web worker |
| `:server` | JVM | Ktor server, `io.ktor.server.netty.EngineMain`, SQLDelight (`ServerDatabase`) |
| `:shared:app` | Android, iOS, JVM, WasmJS | Navigation shell (`CommonApp`), screen wiring, rememberManager |
| `:shared:common` | Android, iOS, JVM, WasmJS | `BalanceTrackerSDK`, SQLDelight (`Database`), `appModule` (expect/actual per platform) |
| `:shared:core` | Android, iOS, JVM, WasmJS | `Manager`/`State`/`Intent`/`SideEffect` base classes |
| `:shared:home` | Android, iOS, JVM, WasmJS | Home screen |
| `:shared:transaction` | Android, iOS, JVM, WasmJS | Transaction CRUD screens (list, add, edit, details) |
| `:shared:resources` | Android, iOS, JVM, WasmJS | Compose resources, theme, colors |

## Architecture

- **MVI pattern**: each screen has a `Manager` (scope + sendIntent), a sealed `State` / `Intent` / `SideEffect`
- **DI**: Koin, `appModule` is platform `expect`/`actual`. Server has its own `serverModule`.
- **Navigation**: Jetpack Navigation Compose with `@Serializable` type-safe route classes
- **Data**: `BalanceTrackerSDK` interface, SQLDelight for local DB with per-platform drivers (Android: `AndroidSqliteDriver`, iOS: `NativeSqliteDriver`, JVM: `SqliteDriver`, WasmJS: `web-worker-driver`)
- **Networking**: Ktor client in shared module (CIO engine)

## Key quirks

- JVM target: `JVM_24` everywhere
- Compiler flags: `-Xreturn-value-checker=full` on `:shared:common`, `-Xwasm-kclass-fqn` on wasm targets
- SQLDelight schemas live in `shared/common/src/commonMain/sqldelight/` (client) and `server/src/main/sqldelight/` (server, `ServerDatabase`)
- Compose resources: `shared:resources` uses `publicResClass = true`, generated class at `me.ilker.balance_tracker.resources.Res`
- iOS framework exported as `BalanceTracker`, SwiftExport's `flattenPackage = "me.ilker.balance_tracker"`
- Web uses `wasmJs` (not legacy JS), sql.js WASM copied via `copy-webpack-plugin`

## CI / Deploy

- **Build workflow**: `ubuntu-latest`, JDK 24 (zulu), on push to `main` and PRs
- **Deploy workflow**: manual trigger, builds `:server:build`, pushes `ilkeraslan0/balance_tracker:latest` Docker image to DockerHub

## Docker

- Multi-stage: `eclipse-temurin:22-jdk` (build, Gradle 9.2.1), `eclipse-temurin:22-jre` (runtime)
- Runs `server.jar` from `:server:fatJar`
