## Resumo das Alterações
Descreva o que foi alterado e por quê.

## Tipo de Mudança
- [ ] Correção de bug (`fix`)
- [ ] Novo recurso (`feat`)
- [ ] Refatoração / melhoria (`refactor`)
- [ ] Documentação / CI (`docs` / `chore`)

## Checklist por Plataforma
Marque o que foi testado e verificado:

### Android (`app/`)
- [ ] `gradle assembleDebug` passou sem erros
- [ ] Fluxos afetados testados no emulador/dispositivo

### iOS (`ios/BolaoDosVei/`)
- [ ] `xcodegen generate` / build no Xcode verificado (ou via workflow `build-ios-ipa.yml`)
- [ ] Contrato de sincronização compatível com Android e Servidor

### Servidor (`server/`)
- [ ] `node --check server/server.js` passou sem erros
- [ ] Endpoints e formato JSON compatíveis com os clientes

## Observações Adicionais
Informe se há qualquer ponto de atenção para quem for revisar.
