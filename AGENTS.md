# AGENTS.md — Bolão dos Véi

> Stable rules stay at the top of this file so implicit prompt caching works across turns and workers.

## 1. Non-Negotiable Rules (NEVER)
- **NEVER** break:
  - Local auth & prediction persistence (`app/src/main/java/com/bolaodosvei/app/data/AppRepository.kt`).
  - Scoring rules (`calculatePredictionPoints` in `app/src/main/java/com/bolaodosvei/app/model/Models.kt`).
  - Real local-timezone ESPN match fetching & today-only filtering (`AppRepository.kt`).
  - Cloud room sync contract (`AppRepository.kt` <-> `server/server.js`).
  - Clean APK build (`BolaoDosVei.apk`).
- **NEVER** install packages, push to git, deploy, delete files/data, or spend money without explicit user approval.
- **NEVER** ask for API keys, tokens, or secrets in chat. Use environment variables only.
- **NEVER** pretend a command or test ran. Paste real command output (last 20 lines) for every claim.
- **NEVER** edit files outside your declared `OWNS` paths.
- **NEVER** edit any file if you are `@verifier`.
- **NEVER** send `thinking_budget` in API/workflow configs.

## 2. Project Overview
- **Android App (`app/`)**: Kotlin + Jetpack Compose + Material 3 + Coil, compiled with Gradle 8.13 (`compileSdk = 36`, `minSdk = 24`, JVM 17).
- **Sync Server (`server/`)**: Zero-dependency Node.js HTTP server (`server/server.js`) storing room participants in `server/salas_db.json`.
- **Deliverable APK**: `BolaoDosVei.apk` (copied from `app/build/outputs/apk/debug/app-debug.apk` after a passing build).

## 3. Verification Commands (Proof of Work)
- **Android compile & APK build**:
  ```powershell
  & "C:\Gradle\gradle-8.13\bin\gradle.bat" assembleDebug
  ```
- **Update root APK (only after `assembleDebug` exits 0)**:
  ```powershell
  Copy-Item -Path "app\build\outputs\apk\debug\app-debug.apk" -Destination "BolaoDosVei.apk" -Force
  ```
- **Node.js sync server syntax check**:
  ```powershell
  node --check server/server.js
  ```

## 4. Team Ownership Matrix
User talks **only** to `@coordinator`. Workers report to `@verifier`.

| Agent | Role / Model Policy | OWNS | MUST NOT TOUCH | DONE MEANS |
| :--- | :--- | :--- | :--- | :--- |
| `@coordinator` | Planner & Judge (Gemini 4 Neon; `high` for plan/review, `xhigh` only for one deep bug) | Planning, task split, final merge approval | Direct bulk edits when workers are assigned | All tracks verified by `@verifier` with passing logs |
| `@android-data` | Worker (Gemini 4 Hydrogen) | `app/src/main/java/com/bolaodosvei/app/data/**`, `app/src/main/java/com/bolaodosvei/app/model/**`, `app/src/main/java/com/bolaodosvei/app/viewmodel/**`, `app/src/main/java/com/bolaodosvei/app/notifications/**` | `app/src/main/java/com/bolaodosvei/app/ui/**`, `app/src/main/res/**`, `server/**`, `build.gradle.kts` | `gradle.bat assembleDebug` exits 0; data/ViewModel contracts intact |
| `@android-ui` | Worker (Gemini 4 Hydrogen) | `app/src/main/java/com/bolaodosvei/app/MainActivity.kt`, `app/src/main/java/com/bolaodosvei/app/ui/**`, `app/src/main/res/**` | `app/src/main/java/com/bolaodosvei/app/data/**`, `app/src/main/java/com/bolaodosvei/app/model/**`, `app/src/main/java/com/bolaodosvei/app/viewmodel/**`, `server/**` | `gradle.bat assembleDebug` exits 0; all resources & Compose screens compile |
| `@sync-server` | Worker (Gemini 4 Hydrogen) | `server/**` | `app/**`, `*.gradle.kts`, `BolaoDosVei.apk` | `node --check server/server.js` exits 0; REST contract matches `AppRepository.kt` |
| `@verifier` | Independent Checker (**different model than workers**; NEVER edits) | Read-only audit across entire repo | **ALL FILES (read-only)** | Re-opens every cited file, reruns build/checks, pastes last 20 lines, PASS or REJECT (max 2 rounds) |

## 5. Verifier Protocol
1. Open every file and 1-based line range cited by workers.
2. Rerun `C:\Gradle\gradle-8.13\bin\gradle.bat assembleDebug` (if `app/` changed) and `node --check server/server.js` (if `server/` changed).
3. Paste the **last 20 lines** of each command output verbatim.
4. Reject any metric, line number, or claim without a verified file read or command behind it.
5. **Max 2 reject rounds**: if issues remain after round 2, escalate directly to `@coordinator` / user with the exact remaining failures.

## 6. Gemini API Plugin (`gemini-api`)
- The `gemini-api` plugin is installed and available via `gemini-api` CLI (`gemini-api-cli` skill) and official docs MCP (`gemini_search_docs` / `gemini_get_doc`).
- Use it for:
  - **Deep Research**: run asynchronously with `--background` so it does not block the session.
  - **Media generation**: images, video, and voice/TTS (keep generated media inside tool results or project assets — **never** inject media as a new user turn).
- Always read `GEMINI_API_KEY` from environment variables; never ask for or hardcode API keys in chat or source files.
- Never send `thinking_budget` in API requests or workflow configs.
