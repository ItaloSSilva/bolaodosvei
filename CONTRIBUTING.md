# Contribuindo com o Bolão dos Véi

Obrigado pelo interesse em contribuir com o **Bolão dos Véi**! Este guia explica como configurar o ambiente, validar suas alterações e enviar um Pull Request.

## 1. Fluxo de Trabalho (Fork e Branch)

1. Faça um **fork** deste repositório para a sua conta do GitHub.
2. Clone o seu fork localmente:
   ```bash
   git clone https://github.com/SEU_USUARIO/bolaodosvei.git
   cd bolaodosvei
   ```
3. Crie uma branch descritiva a partir da `main`:
   ```bash
   git checkout -b feat/minha-melhoria
   # ou: fix/correcao-do-bug
   ```

## 2. Verificações Antes do Pull Request

Antes de abrir o PR, execute os checks correspondentes às áreas que você alterou:

- **Android** (requer JDK 17 e Gradle 8.13):
  ```bash
  gradle assembleDebug
  ```
  *(No Windows: `& "C:\Gradle\gradle-8.13\bin\gradle.bat" assembleDebug`)*

- **Servidor Node.js** (requer Node.js >= 18):
  ```bash
  node --check server/server.js
  ```

- **iOS** (em macOS com XcodeGen):
  ```bash
  cd ios/BolaoDosVei && xcodegen generate
  ```

## 3. Diretrizes de Código

- Mantenha as regras de pontuação (`calculatePredictionPoints` em `Models.kt` / `Models.swift`) e o contrato de sincronização entre `AppRepository.kt`, `AppRepository.swift` e `server/server.js` consistentes.
- **Nunca** inclua chaves de API, tokens, senhas, arquivos de keystore, `local.properties` ou `server/salas_db.json` nos commits.
- Prefira alterações focadas e fáceis de revisar; evite refatorações fora do escopo da sua tarefa.
- Preserve comentários e docstrings existentes que não estejam relacionados à sua mudança.

## 4. Abrindo o Pull Request

1. Faça commit das suas alterações com mensagens claras.
2. Envie a branch para o seu fork (`git push origin feat/minha-melhoria`).
3. Abra o Pull Request para a branch `main`, preenchendo o template de PR com o resumo das mudanças e o checklist de testes executados.
