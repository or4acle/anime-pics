# AnimePics (RusherHack)

Plugin para RusherHack baseado no módulo `AnimePics`: overlay 2D no HUD exibindo imagens e GIFs de anime NSFW de diversas fontes, com pesquisa de tags personalizadas, envio automático para Webhooks do Discord e decodificação otimizada anti-lag.

---

## ✨ Principais Recursos e Melhorias

### 🔞 100% Focado em Conteúdo NSFW (`StrictNSFW`)
- **Filtro Estrito Ativo**: Descarta automaticamente resultados SFW/Safe e prioriza `rating:explicit`.
- **Injeção de Tags Explícitas**: Caso nenhuma tag seja informada na pesquisa, injeta tags NSFW dinâmicas para sempre garantir artes adultas.

### 🌐 8 Fontes de Imagens e GIFs
1. **YandeRE** (`yande.re`): Grande acervo de artes em alta resolução com filtro `Explicit` e suporte a tags.
2. **Konachan** (`konachan.com`): Papéis de parede e artes explícitas com tags e paginação aleatória.
3. **AIBooru** (`aibooru.online`): Milhares de artes NSFW em estilo anime geradas por IA com metadados completos.
4. **PurrBot** (`purrbot.site v2`): GIFs animados Hentai (`fuck`, `blowjob`, `cum`, `anal`, `pussylick`, `solo`, `yaoi`, `yuri`, `neko`) com ciclo automático.
5. **WaifuIM** (`waifu.im`): Tags adultas (`ero`, `ecchi`, `oppai`, `hentai`, `milf`, `ass`, `paizuri`, `oral`) ou tags personalizadas.
6. **E621** (`e621.net`): Grande catálogo anthro/monster-girl com filtro `rating:explicit`.
7. **NekosLife** (`nekos.life`): Imagens lewd de nekos e garotas anime.
8. **LocalFolder**: Carrega imagens e GIFs da sua pasta local `.minecraft/rusherhack/animepics/`.

### ⚡ Otimização Anti-Lag e Redução de Memória (Sem Stutter)
- **Processamento 100% em Segundo Plano**: A decodificação de GIFs e conversão de PNG ocorre em uma thread dedicada (`AnimePics-Worker`), liberando a thread principal do Minecraft para 0 quedas de FPS.
- **Downscaling Inteligente**: Resoluções gigantescas são redimensionadas de forma otimizada para economizar VRAM e memória Heap.
- **Controle de Frames de GIF**: Limite configurável (`MaxGifFrames`) e amostragem inteligente para evitar estouro de memória com GIFs longos.

### 🖼️ Aspect Mode (`Fit` vs `Stretch`)
- **Modo Fit**: Preserva a proporção original da imagem/GIF sem deformar ou esticar.
- **Modo Stretch**: Estica a imagem para preencher exatamente o retângulo configurado.

### 📡 Discord Webhook Integrado
- Envia automaticamente cards de metadados (título, autor, fonte, link do post e miniatura) para seu canal do Discord.
- Comando de teste imediato `.animepics testwebhook`.

---

## ⌨️ Comandos no Chat

| Comando | Descrição |
|---|---|
| `.animepics` | Exibe o status atual, fonte ativa e tags configuradas. |
| `.animepics next` | Força o carregamento imediato da próxima imagem/GIF. |
| `.animepics source <fonte>` | Troca a fonte (`YandeRE`, `Konachan`, `AIBooru`, `PurrBot`, `WaifuIM`, `E621`, `NekosLife`, `LocalFolder`). |
| `.animepics yande <tags>` | Define tags de pesquisa para o Yande.re e recarrega. |
| `.animepics konachan <tags>` | Define tags para o Konachan e recarrega. |
| `.animepics aibooru <tags>` | Define tags para o AIBooru e recarrega. |
| `.animepics e621 <tags>` | Define tags para o E621 e recarrega. |
| `.animepics purr <tag>` | Define a categoria de GIF do PurrBot (`fuck`, `blowjob`, `cum`, `anal`, etc.). |
| `.animepics waifu <tag>` | Define a tag do Waifu.im. |
| `.animepics search <tags>` | Pesquisa tags na fonte atualmente selecionada. |
| `.animepics clear` | Limpa todas as tags de busca. |
| `.animepics strict <on/off>` | Ativa ou desativa o filtro NSFW estrito. |
| `.animepics webhook <url>` | Configura ou atualiza a URL do Webhook do Discord. |
| `.animepics testwebhook` | Envia um card de teste para verificar seu canal do Discord. |

---

## 🔨 Como Compilar

Requer **JDK 21**. No terminal / prompt do projeto:

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

## 💾 Instalação no Minecraft

1. Copie o arquivo `AnimePics-1.0.0.jar` para a pasta `.minecraft/rusherhack/plugins/`.
2. Inicie o Minecraft com o parâmetro `-Drusherhack.enablePlugins=true` nos argumentos da JVM.
3. Compatível com Minecraft **1.21.1** (RusherHack **2.0.5**).
