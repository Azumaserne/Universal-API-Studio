<div align="center">

# Universal API Studio (Open Source Edition)

**Lightweight Native Android Universal API & LLM Debug Client (~1.2 MB)**

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

*Designed & Authored by **[@Azumaserne](https://github.com/kaka714/Universal-API-Studio)***

**English** | [简体中文](README.zh-CN.md) | [Deutsch](README.de.md)

</div>

---

## ✨ Overview

**Universal API Studio (OSS)** is a pure native Android client built with **Kotlin + Jetpack Compose + OkHttp**, designed for calling LLM APIs and debugging custom HTTP/JSON endpoints right from your phone:

- **📦 Ultra-Lightweight & Private**: The signed Release APK is only **~1.2 MB** with smooth native UI (no WebView), zero ads, and zero telemetry. All API keys and chat histories stay 100% local on your device.
- **🌐 Multilingual Animated Onboarding & UI (`v1.2.0`)**: First-launch Apple-inspired bouncy animated Welcome screen (`欢迎` / `Welcome` / ...) using royalty-free system sans typography, with an instant 8-language selector (**简体中文 / English / 粵語 / 日本語 / 한국어 / Deutsch / Français / Español**) changeable anytime in Settings or the side drawer.
- **📊 Real-Time Context Window & Cache Hit HUD**: A live status pill sits right beside the active API template in the top bar, showing context window usage (e.g., `1.2k/128k (1%)`) and Prompt Cache hit rate (`Cache 78% (960)`). Tap it for full breakdowns and instant switching between `32K / 64K / 128K / 200K / 1M` context windows.
- **🔌 Broad Protocol Compatibility & Ready-Made OpenRouter Presets**: Pre-configured **OpenRouter · ChatGPT** (`GPT-6.2 Astra / Sol`, `GPT-6.1 Terra`, `GPT-6 Luna`), **OpenRouter · Claude** (`Opus / Sonnet / Haiku / Fable`), and **OpenRouter · DeepSeek** / direct **DeepSeek** (`DeepSeek V4.1 Flash`, `DeepSeek V4.0 Pro`) profiles — just paste your OpenRouter API key once to share it across all OpenRouter presets. Also works with OneAPI/NewAPI relays or **Strict Exact URL** for any custom REST POST API.
- **🧠 Reasoning Effort & Speed / Tier Selector**: Quick chips right above the chat bar let you switch same-family models, set reasoning effort (`none` / `minimal` / `low` / `medium` / `high` / `xhigh` / `max`), and pick speed/routing tiers (`Fast`, `Ultrafast`, OpenRouter `:nitro`, `Low Latency`, or `Flex`).
- **⚡ SSE Streaming & Reasoning Foldout**: Real-time streaming output with collapsible `reasoning_content` thinking cards and instant request cancellation.
- **🛠️ Advanced Request Controls**: Manage multiple API profiles (JSON import/export), fetch available `/models`, toggle custom HTTP headers, set per-session System Prompts, tune `Temperature` / `Top P` / `Max Tokens`, inject `Extra Body` JSON, customize request body templates, and specify custom JSON response extraction paths.
- **🔍 Built-In HTTP Inspector**: Every response includes a one-tap packet inspector displaying masked request headers, request body, HTTP status code, latency (ms), and raw response payload.

---

## 📥 Download

Pre-built signed APKs are available on the **[GitHub Releases](https://github.com/kaka714/Universal-API-Studio/releases)** page.

---

## 📊 Context Window & Cache Hit Tracking

The client automatically parses token usage and prompt cache metrics across major providers:

- **Automatic Streaming Usage**: Standard OpenAI-compatible streaming requests inject `"stream_options": {"include_usage": true}` automatically (and transparently retry without it if a legacy gateway rejects the field with HTTP 400/422).
- **Multi-Provider Metric Parsing**:
  - **DeepSeek / OneAPI**: `usage.prompt_cache_hit_tokens` & `usage.prompt_cache_miss_tokens`
  - **OpenAI / Compatible**: `usage.prompt_tokens_details.cached_tokens` / `usage.cached_tokens`
  - **Anthropic Claude**: `usage.input_tokens`, `usage.output_tokens`, `usage.cache_read_input_tokens`
  - **Google Gemini**: `usageMetadata.promptTokenCount`, `candidatesTokenCount`, `cachedContentTokenCount`
- **Smart Local Fallback**: If an endpoint omits `usage` fields (or before the first request), the built-in `TokenEstimator` estimates context occupancy from message text so the usage indicator remains informative.

---

## 🧩 Custom Body Template Variables

Leave `Custom Body Template` blank to send standard OpenAI `messages` payloads, or define your own JSON template using these placeholders:

| Variable | Description |
| :--- | :--- |
| `{{model}}` | Active model name (JSON-escaped, without outer quotes) |
| `{{model_json}}` | Valid JSON-quoted model name string |
| `{{prompt}}` | Latest user prompt (JSON-escaped, without outer quotes) |
| `{{prompt_json}}` | Valid JSON-quoted latest user prompt string |
| `{{system_prompt}}` | Active system prompt (JSON-escaped, without outer quotes) |
| `{{system_prompt_json}}` | Valid JSON-quoted system prompt string |
| `{{messages_json}}` | Full conversation history JSON array `[{"role":...,"content":...}]` |
| `{{temperature}}` | Configured `Temperature` value |
| `{{top_p}}` | Configured `Top P` value |
| `{{stream}}` | Streaming boolean flag (`true` / `false`) |
| `{{max_tokens}}` | Configured `Max Tokens` value (defaults to `2048` if unset) |

---

## 🛠️ Build from Source

### Requirements
- **JDK 17**
- **Android SDK** (`compileSdk = 34`, `minSdk = 24`)

### Command Line Build

```bash
# Linux / macOS
./gradlew assembleRelease

# Windows PowerShell
.\gradlew.bat assembleRelease
```

The compiled APK is generated at:

```text
app/build/outputs/apk/release/app-release.apk
```

> If `app/oss-release.keystore` (or `KEYSTORE_PATH`) is not provided, Gradle automatically falls back to debug signing so local builds and CI produce an immediately installable APK.

### GitHub Actions CI/CD

This repository includes [`.github/workflows/android-release.yml`](.github/workflows/android-release.yml):
- Every push to `main` builds the APK and uploads it as a workflow artifact.
- Pushing a version tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`) automatically builds and publishes a **GitHub Release**.

---

## 📄 License & Copyright

This project is licensed under the **[GNU General Public License v3.0 (GPLv3)](LICENSE)** (strong copyleft):
- ✅ **Permitted**: Anyone may freely run, study, modify, and distribute this software and its source code.
- 📜 **Share-Alike & Attribution**: Any modified version, derivative work, or redistributed binary **must retain the original author attribution (`@Azumaserne`) and release its complete corresponding source code under GPLv3** (closed-source redistribution or proprietary resale is prohibited).

Copyright © 2026 **[@Azumaserne](https://github.com/kaka714/Universal-API-Studio)**
