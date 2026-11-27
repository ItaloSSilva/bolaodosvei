# Bolão dos Véi

![Android](https://img.shields.io/badge/Android-Kotlin%20%2B%20Jetpack%20Compose-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-SwiftUI%2016%2B-000000?logo=apple&logoColor=white)
![Server](https://img.shields.io/badge/Server-Node.js%20%3E%3D18-339933?logo=node.js&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

App open-source de bolão de futebol em tempo real para **Android (Kotlin + Jetpack Compose)**, **iOS (SwiftUI)** e **Servidor de Salas (Node.js)**.

---

## Principais Recursos

- **7 ligas oficiais via ESPN API**: Brasileirão Série A 2026, Premier League, La Liga, Champions League, Libertadores, Copa do Brasil e Sul-Americana.
- **Palpite Coringa (2x)**: dobre a pontuação de um palpite estratégico por rodada.
- **Trava automática ao vivo**: os palpites são bloqueados automaticamente assim que a partida começa.
- **60+ temas de clubes com mascotes ilustrados**: personalize o visual do aplicativo com as cores e o mascote do seu time, além de suporte a **foto da galeria** no perfil.
- **Loja de Pontos artesanal**: desbloqueie molduras, títulos e itens cosméticos com **prévia ao vivo** e **desafios diários** para acumular pontos.
- **Chat em tempo real**: conversa por sala sincronizada entre usuários de **Android** e **iOS**.

---

## Arquitetura do Projeto

```text
bolaodosvei/
├── app/                                      # Aplicativo Android (Kotlin + Jetpack Compose, minSdk 24, compileSdk 36)
│   ├── build.gradle.kts
│   └── src/main/java/com/bolaodosvei/app/
│       ├── MainActivity.kt
│       ├── data/
│       │   ├── AppConfig.kt                  # URL base de sincronização (SYNC_BASE_URL)
│       │   └── AppRepository.kt              # Persistência local, ESPN API e sync de salas/chat
│       ├── model/Models.kt                   # Modelos e regras de pontuação (calculatePredictionPoints)
│       ├── notifications/                    # Notificações locais de partidas
│       ├── ui/                               # Telas Compose, componentes e 60+ temas de clubes
│       └── viewmodel/BolaoViewModel.kt
├── ios/BolaoDosVei/                          # Aplicativo iOS (SwiftUI, iOS 16+, Swift 5.9)
│   ├── project.yml                           # Configuração do XcodeGen
│   └── Sources/
│       ├── AppRepository.swift               # Persistência, ESPN API e sync (syncBaseURL)
│       ├── BolaoDosVeiApp.swift
│       ├── BolaoViewModel.swift
│       ├── ContentView.swift
│       └── Models.swift
└── server/                                   # Servidor leve de sincronização em Node.js (zero dependências)
    ├── package.json
    └── server.js                             # API HTTP de salas, ranking e chat (salas_db.json)
```

---

## Como Rodar e Compilar Localmente

### 1. Android (Kotlin + Jetpack Compose)

- **Requisitos**: JDK 17 e Gradle 8.13 (ou Android Studio com SDK 36).
- **Compilar APK de debug**:

```bash
gradle assembleDebug
```

No Windows (usando o Gradle 8.13 local):

```powershell
& "C:\Gradle\gradle-8.13\bin\gradle.bat" assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Servidor de Salas (Node.js)

- **Requisitos**: Node.js `>=18` (sem dependências externas).
- **Verificar sintaxe e iniciar**:

```bash
node --check server/server.js
node server/server.js
```

Por padrão, o servidor sobe na porta `3000` (configurável via variável de ambiente `PORT`) e persiste os dados em `server/salas_db.json`.

### 3. iOS (SwiftUI)

- **Requisitos**: macOS com Xcode 16+ e [XcodeGen](https://github.com/yonaskolb/XcodeGen) (ou via GitHub Actions).
- **Gerar o projeto Xcode localmente**:

```bash
cd ios/BolaoDosVei
xcodegen generate
open BolaoDosVei.xcodeproj
```

- **Build automático de `.ipa` sem assinatura**: o workflow [`.github/workflows/build-ios-ipa.yml`](.github/workflows/build-ios-ipa.yml) gera e empacota o `BolaoDosVei.ipa` automaticamente em `macos-15` quando há alterações em `ios/**` ou via `workflow_dispatch`.

---

## Configurando `SYNC_BASE_URL`

Para apontar os aplicativos para o seu próprio servidor Node.js ou instância Firebase Realtime Database:

- **Android**: edite a constante `SYNC_BASE_URL` em [`app/src/main/java/com/bolaodosvei/app/data/AppConfig.kt`](app/src/main/java/com/bolaodosvei/app/data/AppConfig.kt):

```kotlin
object AppConfig {
    const val SYNC_BASE_URL: String = "https://seu-servidor-ou-firebase.example.com"
}
```

- **iOS**: edite a propriedade `syncBaseURL` em [`ios/BolaoDosVei/Sources/AppRepository.swift`](ios/BolaoDosVei/Sources/AppRepository.swift):

```swift
final class AppRepository {
    static let syncBaseURL = "https://seu-servidor-ou-firebase.example.com"
```

---

## Contribuindo

Contribuições são muito bem-vindas! Seja reportando bugs, sugerindo novos recursos ou enviando Pull Requests, confira o nosso [Guia de Contribuição](CONTRIBUTING.md) antes de começar.

## Licença

Este projeto está licenciado sob a **Licença MIT** — veja o arquivo [LICENSE](LICENSE) para mais detalhes.
