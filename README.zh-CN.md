<div align="center">

# Universal API Studio (Open Source Edition)

**轻量级纯原生 Android 通用 API & 大模型对话调试终端（~1.2MB）**

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

*Designed & Authored by **[@Azumaserne](https://github.com/kaka714/Universal-API-Studio)***

[English](README.md) | **简体中文** | [Deutsch](README.de.md)

</div>

---

## ✨ 项目简介

**Universal API Studio (OSS)** 是一款基于 **Kotlin + Jetpack Compose + OkHttp** 构建的纯原生 Android 客户端，专为高频调用大模型 API 与调试自定义 HTTP/JSON 接口而设计：

- **📦 极简轻量**：Release APK 仅约 **1.2MB**，原生渲染零 WebView 卡顿，零广告、零遥测，所有密钥与聊天记录 100% 本地存储。
- **🌐 8 国语言自适应 & 动效欢迎页（`v1.2.0`）**：首次启动呈现基于系统无版权字体打造的「欢迎」跳动渐变引导页，支持 **简体中文 / English / 粵語 / 日本語 / 한국어 / Deutsch / Français / Español** 一键切换，之后也可随时在侧边栏与设置中更改界面语言。
- **📊 顶部栏实时上下文 & Cache Hit 监控**：API 模板右侧内置实时状态胶囊，直观展示当前上下文窗口占用（如 `上下文 1.2k/128k (1%)`）与 Prompt Cache 命中率（如 `Cache 78% (960)`），点击即可展开详情并快捷切换 `32K / 64K / 128K / 200K / 1M` 上下文窗口。
- **🔌 全协议兼容 & 开箱即用 OpenRouter 预设**：内置 **OpenRouter · ChatGPT**（`GPT-6.2 Astra / Sol`、`GPT-6.1 Terra`、`GPT-6 Luna`）、**OpenRouter · Claude**（`Opus / Sonnet / Haiku / Fable`）以及 **OpenRouter / 官方直连 DeepSeek**（`DeepSeek V4.1 Flash`、`DeepSeek V4.0 Pro`）标准渠道，粘贴一次 OpenRouter Key 即可自动同步共用；同时支持 OneAPI / NewAPI 中转，或开启「严格原始 URL」对接任意自定义 POST 接口。
- **🧠 思考等级 (Effort) & 速度模式速切**：输入框上方内置快捷条，单手即可切换同家族模型、调整思考等级（`none / minimal / low / medium / high / xhigh / max`），以及选择速度/路由档位（`Fast`、`Ultrafast`、OpenRouter `:nitro`、低延迟或 `Flex`）。
- **⚡ SSE 流式与深度思考折叠**：流式实时输出 + `reasoning_content` 思考过程折叠卡片，支持随时中断请求。
- **🛠️ 硬核高级参数**：多渠道方案管理（JSON 导入导出）、一键拉取 `/models`、自定义 HTTP Headers、独立会话 System Prompt、Temperature / Top P / Max Tokens、附加顶层 JSON (`Extra Body`)、自定义 Body 模板引擎与 JSON 响应提取路径。
- **🔍 内置 HTTP 报文抓包**：每条回复一键查看完整脱敏请求头、请求体、状态码、耗时（ms）与原始返回报文。

---

## 📥 安装包下载

已编译签名的安装包请前往 **[GitHub Releases](https://github.com/kaka714/Universal-API-Studio/releases)** 页面直接下载安装。

---

## 📊 上下文占用与 Cache Hit 统计说明

客户端会自动处理并兼容各大厂商的 Token 与缓存统计字段：

- **流式请求自动收集**：标准兼容接口发送 SSE 请求时会自动注入 `"stream_options": {"include_usage": true}`（若遇不兼容的老旧网关报错 400/422，将自动降级移除并重试）。
- **多厂商字段解析**：
  - **DeepSeek / OneAPI**：解析 `usage.prompt_cache_hit_tokens` 与 `usage.prompt_cache_miss_tokens`。
  - **OpenAI / 兼容接口**：解析 `usage.prompt_tokens_details.cached_tokens` / `usage.cached_tokens`。
  - **Anthropic Claude**：解析 `usage.input_tokens`、`usage.output_tokens`、`usage.cache_read_input_tokens`。
  - **Google Gemini**：解析 `usageMetadata.promptTokenCount`、`candidatesTokenCount`、`cachedContentTokenCount`。
- **智能兜底估算**：当接口未返回 `usage` 字段或尚未发起请求时，内置 `TokenEstimator` 会按文本权重估算当前上下文占用，确保进度条始终可用。

---

## 🧩 自定义 Body 模板变量

在接口配置的 `Custom Body Template` 中留空将发送标准 OpenAI `messages` 格式；若填写模板，支持以下占位变量：

| 模板变量 | 替换说明 |
| :--- | :--- |
| `{{model}}` | 当前模型名（JSON 转义后，不含外层引号） |
| `{{model_json}}` | 带双引号的合法 JSON 模型名字符串 |
| `{{prompt}}` | 最新一条用户输入内容（JSON 转义后，不含外层引号） |
| `{{prompt_json}}` | 带双引号的合法 JSON 最新用户输入 |
| `{{system_prompt}}` | 当前生效的系统提示词（不含外层引号） |
| `{{system_prompt_json}}` | 带双引号的合法 JSON 系统提示词 |
| `{{messages_json}}` | 完整对话历史 `[{"role":...,"content":...}]` JSON 数组 |
| `{{temperature}}` | 当前 `Temperature` 数值 |
| `{{top_p}}` | 当前 `Top P` 数值 |
| `{{stream}}` | 当前流式开关布尔值 (`true` / `false`) |
| `{{max_tokens}}` | 当前 `Max Tokens` 数值（未填时默认 `2048`） |

---

## 🛠️ 从源码编译

### 环境要求
- **JDK 17**
- **Android SDK** (`compileSdk = 34`, `minSdk = 24`)

### 命令行构建

```bash
# Linux / macOS
./gradlew assembleRelease

# Windows PowerShell
.\gradlew.bat assembleRelease
```

编译生成的 APK 位于：

```text
app/build/outputs/apk/release/app-release.apk
```

> 若未提供 `app/oss-release.keystore`（或环境变量 `KEYSTORE_PATH`），构建脚本会自动使用 Android Debug 证书签名，便于本地与 CI 直接产出自安装 APK。

### GitHub Actions 自动构建与发布

仓库已内置 [`.github/workflows/android-release.yml`](.github/workflows/android-release.yml)：
- 每次 `push` 到 `main` 分支都会自动编译 APK 并上传构建产物 Artifact。
- 推送版本标签（例如 `git tag v1.0.0 && git push origin v1.0.0`）会自动触发构建并创建 **GitHub Release**。

---

## 📄 开源协议与版权声明

本项目采用 [**GNU General Public License v3.0 (GPLv3)**](LICENSE) 开源协议（强传染性 Copyleft）：
- ✅ **允许**：任何个人与组织自由运行、研究、修改和分发本软件及其源码。
- 📜 **必须开源与保留署名**：任何基于本项目的修改版、衍生作品或打包分发，**必须完整保留原作者 `@Azumaserne` 的版权声明，且必须以同样的 GPLv3 协议向所有接收者公开全部源代码**（禁止将其闭源修改后独占或闭源倒卖）。

Copyright © 2026 **[@Azumaserne](https://github.com/kaka714/Universal-API-Studio)**
