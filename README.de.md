<div align="center">

# Universal API Studio (Open Source Edition)

**Schlanker, nativer Android-Client für universelle APIs & KI-Modelle (~1,2 MB)**

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

*Designed & Authored by **[@Azumaserne](https://github.com/kaka714/Universal-API-Studio)***

[English](README.md) | [简体中文](README.zh-CN.md) | **Deutsch**

</div>

---

## ✨ Überblick

**Universal API Studio (OSS)** ist ein vollständig nativer Android-Client auf Basis von **Kotlin + Jetpack Compose + OkHttp**, entwickelt für den flexiblen Zugriff auf LLM-APIs sowie zum Debuggen beliebiger HTTP-/JSON-Endpunkte:

- **📦 Extrem kompakt & privat**: Die signierte Release-APK ist nur **ca. 1,2 MB** groß, läuft flüssig nativ ohne WebView, enthält keine Werbung und keine Telemetrie. Alle API-Keys und Chatverläufe bleiben zu 100 % lokal auf dem Gerät.
- **🌐 Mehrsprachiger animierter Willkommensbildschirm & UI (`v1.2.0`)**: Beim ersten Start begrüßt eine dynamisch federnde Willkommens-Animation („Willkommen“ / „欢迎“ / „Welcome“ …) mit lizenzfreier System-Sans-Typografie; 8 Sprachen (**简体中文 / English / 粵語 / 日本語 / 한국어 / Deutsch / Français / Español**) lassen sich direkt beim Start sowie jederzeit in den Einstellungen oder im Menü wechseln.
- **📊 Echtzeit-Monitor für Kontextfenster & Cache Hit**: Direkt neben der aktiven API-Vorlage in der oberen Leiste zeigt eine Statusanzeige die aktuelle Kontextauslastung (z. B. `1.2k/128k (1%)`) sowie die Prompt-Cache-Trefferquote (`Cache 78% (960)`). Ein Tipp öffnet die Detailansicht mit Schnellauswahl für `32K / 64K / 128K / 200K / 1M`.
- **🔌 Breite Kompatibilität & fertige OpenRouter-Presets**: Vorkonfigurierte Profile für **OpenRouter · ChatGPT** (`GPT-6.2 Astra / Sol`, `GPT-6.1 Terra`, `GPT-6 Luna`), **OpenRouter · Claude** (`Opus / Sonnet / Haiku / Fable`) sowie **OpenRouter / Direktes DeepSeek** (`DeepSeek V4.1 Flash`, `DeepSeek V4.0 Pro`) — einmal OpenRouter-Key einfügen und sofort alle OpenRouter-Familien nutzen. Funktioniert ebenso mit OneAPI/NewAPI oder per **Strikte Original-URL** für beliebige REST-POST-APIs.
- **🧠 Reasoning-Effort & Geschwindigkeitsstufen**: Über die Schnellleiste direkt über dem Eingabefeld lassen sich Modellvarianten derselben Familie, die Denktiefe (`none / minimal / low / medium / high / xhigh / max`) sowie Tempo-/Routing-Modi (`Fast`, `Ultrafast`, OpenRouter `:nitro`, `Low Latency` oder `Flex`) mit einem Tipp umschalten.
- **⚡ SSE-Streaming & einklappbares Reasoning**: Echtzeit-Streaming mit einklappbaren `reasoning_content`-Denkprozessen und jederzeitiger Abbruchmöglichkeit.
- **🛠️ Umfangreiche Profi-Optionen**: Mehrere API-Profile (inkl. JSON-Import/-Export), Modellliste per `/models` abrufen, eigene HTTP-Header, sitzungsspezifischer System-Prompt, `Temperature` / `Top P` / `Max Tokens`, `Extra Body` JSON, benutzerdefinierte Body-Templates und JSON-Antwortpfade.
- **🔍 Integrierter HTTP-Inspektor**: Zu jeder Antwort lassen sich maskierte Request-Header, Request-Body, HTTP-Statuscode, Latenz (ms) und Rohantwort mit einem Tipp einsehen.

---

## 📥 Download

Vorkompilierte und signierte APK-Dateien stehen unter **[GitHub Releases](https://github.com/kaka714/Universal-API-Studio/releases)** bereit.

---

## 📊 Kontextauslastung & Cache-Hit-Auswertung

Der Client liest Token- und Cache-Metriken aller gängigen Anbieter automatisch aus:

- **Automatisches Streaming-Usage**: Bei Standard-kompatiblen SSE-Anfragen wird automatisch `"stream_options": {"include_usage": true}` gesendet (bei Ablehnung durch ältere Gateways mit HTTP 400/422 erfolgt automatisch ein Retry ohne dieses Feld).
- **Unterstützte Anbieterfelder**:
  - **DeepSeek / OneAPI**: `usage.prompt_cache_hit_tokens` & `usage.prompt_cache_miss_tokens`
  - **OpenAI / Kompatibel**: `usage.prompt_tokens_details.cached_tokens` / `usage.cached_tokens`
  - **Anthropic Claude**: `usage.input_tokens`, `usage.output_tokens`, `usage.cache_read_input_tokens`
  - **Google Gemini**: `usageMetadata.promptTokenCount`, `candidatesTokenCount`, `cachedContentTokenCount`
- **Lokale Schätzung als Fallback**: Falls ein Endpunkt keine `usage`-Felder liefert (oder vor der ersten Anfrage), schätzt der integrierte `TokenEstimator` den aktuellen Kontextverbrauch lokal.

---

## 🧩 Variablen für Custom Body Templates

Bleibt `Custom Body Template` leer, wird das Standard-OpenAI-Format (`messages`) gesendet. Bei eigener JSON-Vorlage stehen folgende Platzhalter zur Verfügung:

| Variable | Beschreibung |
| :--- | :--- |
| `{{model}}` | Aktiver Modellname (JSON-escaped, ohne äußere Anführungszeichen) |
| `{{model_json}}` | Gültiger JSON-String des Modellnamens mit Anführungszeichen |
| `{{prompt}}` | Letzte Nutzereingabe (JSON-escaped, ohne äußere Anführungszeichen) |
| `{{prompt_json}}` | Gültiger JSON-String der letzten Nutzereingabe |
| `{{system_prompt}}` | Aktiver System-Prompt (JSON-escaped, ohne äußere Anführungszeichen) |
| `{{system_prompt_json}}` | Gültiger JSON-String des System-Prompts |
| `{{messages_json}}` | Vollständiger Gesprächsverlauf als JSON-Array `[{"role":...,"content":...}]` |
| `{{temperature}}` | Konfigurierter `Temperature`-Wert |
| `{{top_p}}` | Konfigurierter `Top P`-Wert |
| `{{stream}}` | Streaming-Schalter (`true` / `false`) |
| `{{max_tokens}}` | Konfigurierter `Max Tokens`-Wert (Standard `2048`, falls nicht gesetzt) |

---

## 🛠️ Aus dem Quellcode kompilieren

### Voraussetzungen
- **JDK 17**
- **Android SDK** (`compileSdk = 34`, `minSdk = 24`)

### Build per Kommandozeile

```bash
# Linux / macOS
./gradlew assembleRelease

# Windows PowerShell
.\gradlew.bat assembleRelease
```

Die fertige APK liegt anschließend unter:

```text
app/build/outputs/apk/release/app-release.apk
```

> Wenn kein `app/oss-release.keystore` (bzw. `KEYSTORE_PATH`) vorhanden ist, verwendet Gradle automatisch die Debug-Signatur, sodass lokal wie in CI direkt installierbare APKs entstehen.

### GitHub Actions CI/CD

Der Workflow [`.github/workflows/android-release.yml`](.github/workflows/android-release.yml) ist bereits enthalten:
- Jeder `push` auf `main` baut automatisch die APK und lädt sie als Build-Artifact hoch.
- Beim Pushen eines Versions-Tags (z. B. `git tag v1.0.0 && git push origin v1.0.0`) wird automatisch ein **GitHub Release** erstellt.

---

## 📄 Lizenz & Urheberrecht

Dieses Projekt steht unter der **[GNU General Public License v3.0 (GPLv3)](LICENSE)** (starkes Copyleft):
- ✅ **Erlaubt**: Jeder darf diese Software und den Quellcode frei ausführen, untersuchen, verändern und weitergeben.
- 📜 **Copyleft & Namensnennung**: Jede geänderte Version, abgeleitete Arbeit oder weitergegebene Binärdatei **muss den Urhebervermerk (`@Azumaserne`) beibehalten und den vollständigen zugehörigen Quellcode ebenfalls unter der GPLv3 veröffentlichen** (ein geschlossener Weiterverkauf ohne Quellcode ist untersagt).

Copyright © 2026 **[@Azumaserne](https://github.com/kaka714/Universal-API-Studio)**
