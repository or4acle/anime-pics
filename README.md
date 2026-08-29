# AnimePics (RusherHack Plugin)

Plugin para RusherHack baseado no módulo `AnimePics`: overlay 2D no HUD exibindo imagens e GIFs de anime NSFW de diversas fontes, com pesquisa de tags personalizadas, envio automático para Webhooks do Discord, modo debug com diagnósticos em tempo real e bypass automático de certificados SSL.

---

## ✨ Principais Recursos e Correções

### 🛡️ Correção de Certificados SSL (PKIX Path Validator Fix)
- Implementado o utilitário `SSLHelper` que bypassa problemas de validação de certificados SSL/TLS da JVM no Windows (como erros `PKIX path validation failed: java.security.cert.CertPathValidatorException`).
- Garante que os Webhooks do Discord e o download de imagens de todos os boorus e CDNs funcionem 100% sem erros de rede.

### 🐞 Modo Debug (`DebugMode` / `.ap debug on`)
- **Diagnóstico em Tempo Real**: Exibe no console do MultiMC/Minecraft todos os detalhes das requisições (URL completa, código de resposta HTTP, tags aplicadas, metadados do post, autor, resolução, tamanho em bytes e eventuais erros com stacktrace).
- Permite diagnosticar e monitorar facilmente o fluxo de imagens e o envio para o Discord.

### 🔞 100% Focado em Conteúdo NSFW (`StrictNSFW`)
- **Filtro Estrito Ativo**: Descarta automaticamente resultados SFW/Safe e aceita apenas `rating:explicit` (`rating:e`).
- **Injeção de Tags 100% Explícitas**: Caso nenhuma tag seja informada na pesquisa, injeta tags adultas explícitas (`nude`, `nipples`, `pussy`, `sex`, `fellatio`, `cunnilingus`, `masturbation`, `paizuri`, `cum`, `creampie`, `uncensored`, `breasts`, etc.) para garantir artes 100% NSFW.

### 📡 Discord Webhook Integrado com Teste Imediato
- Envia automaticamente cards completos com miniatura da arte, link do post original, autor, resolução e tags.
- Botão **`TestWebhookNow`** no menu do RusherHack (ClickGUI) e comando **`.ap testwebhook`** para testar a transmissão imediatamente.

### ⌨️ Comandos Rápidos (`.ap`)

O comando principal do plugin agora utiliza o prefixo **`.ap`** (evitando conflito com o comando de configurações nativo do RusherHack):

| Comando | Descrição |
|---|---|
| `.ap` | Exibe o status atual, fonte ativa, tags e status do webhook. |
| `.ap next` | Carrega a próxima imagem/GIF imediatamente. |
| `.ap testwebhook` | Envia um card de teste para verificar seu canal do Discord. |
| `.ap debug on/off` | Ativa ou desativa o modo debug com logs detalhados no console. |
| `.ap strict on/off` | Ativa ou desativa o filtro NSFW estrito. |
| `.ap webhook <url>` | Configura a URL do Webhook do Discord e ativa o envio. |
| `.ap source <fonte>` | Troca a fonte (`YandeRE`, `Konachan`, `AIBooru`, `PurrBot`, `E621`, `NekosLife`, `LocalFolder`). |
| `.ap yande <tags>` | Define tags de pesquisa para o Yande.re e recarrega. |
| `.ap konachan <tags>` | Define tags para o Konachan e recarrega. |
| `.ap aibooru <tags>` | Define tags para o AIBooru e recarrega. |
| `.ap e621 <tags>` | Define tags para o E621 e recarrega. |
| `.ap purr <tag>` | Define a categoria de GIF do PurrBot (`fuck`, `blowjob`, `cum`, `anal`, etc.). |
| `.ap search <tags>` | Pesquisa tags na fonte atualmente selecionada. |
| `.ap clear` | Limpa todas as tags de busca. |

---

## 🔨 Como Compilar

Requer **JDK 21**. No prompt de comando na pasta do projeto:

**Windows:**
```powershell
.\gradlew.bat build
```

**Linux / macOS:**
```bash
./gradlew build
```

O arquivo compilado estará em:
```
build/libs/AnimePics-1.0.0.jar
```

---

## 💾 Instalação no Minecraft / MultiMC

1. Copie o arquivo `AnimePics-1.0.0.jar` da pasta `build/libs/` para `.minecraft/rusherhack/plugins/`.
2. Verifique se o argumento `-Drusherhack.enablePlugins=true` está configurado nas configurações de Java da instância do MultiMC.
3. Inicie o jogo (Minecraft 1.21.1 com Fabric).
