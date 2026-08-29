# AnimePics + JOI Companion (RusherHack Plugin)

Plugin para RusherHack 1.21.1 com overlay 2D no HUD exibindo imagens e GIFs NSFW de diversas fontes, com **JOI (Jerk Off Incentive) interativo em tempo real**, metrônomo de ritmo, contador e histórico de sessões/nuts/edges com persistência local, sistema de avaliação por pontuação/rank e integração com Webhooks do Discord.

---

## ✨ Principais Recursos

### 🎭 Modo JOI (Jerk Off Incentive) e HUD Dinâmico
- **HUD Elegante no Minecraft**: Exibe textos de incentivo dinâmicos, metrônomo de ritmo para controle de velocidade e badges de status logo abaixo da arte.
- **6 Estilos de Personalidade (`.ap style <estilo>`)**:
  - `Humiliation` (ou `zueira` / `roast`): Frases cômicas e humilhantes de zueira ("Look at you stroking to 2D pixels in Minecraft kkkk", "Mãos pra cima seu viciado", etc.).
  - `Gentle`: Focado em ritmo lento, apreciação dos detalhes e respiração suave.
  - `Strict`: Comandos de obediência rígida, proibição de acelerar e foco absoluto.
  - `Hardcore`: Ritmo acelerado, estímulos intensos e foco no clímax.
  - `Edging`: Foco em controle de borda, comandos de pausa (Hands-Off) e acúmulo de sensibilidade.
  - `Dynamic`: Alterna inteligentemente entre os estilos com base no tempo de sessão.
- **Metrônomo Visual de Ritmo**:
  - `Slow & Steady` (40 BPM)
  - `Moderate Pace` (75 BPM)
  - `Intense Speed` (120 BPM)
  - `Full Speed Frenzy` (160 BPM)
  - `HOLD & EDGE!` (Pausa total / mãos fora)
  - `Hands Free! Look Only`

---

### 📈 Rastreador de Estatísticas & Sistema de Pontuação / Ranks
As estatísticas são salvas automaticamente em `.minecraft/rusherhack/animepics/joi_stats.json`:
- **Total de Nuts** acumulados.
- **Total de Edges** realizados.
- **Duração da Sessão** atual e tempo total acumulado.
- **Recordes Pessoais**: Sessão mais rápida e sessão mais longa.
- **Avaliação de Desempenho (Ranks)**:
  - ⚡ **Rank C** (`Quickshot / Instant Release`): < 2 minutos.
  - 🔥 **Rank B** (`Rapid Fire`): 2 a 6 minutos.
  - 🎯 **Rank A** (`Sweet Spot` ou `Controlled Spark`): 6 a 15 minutos (ou com edges múltiplos).
  - 💎 **Rank S** (`Iron Will` ou `Edge Connoisseur`): 15 a 30 minutos com alta resistência.
  - 👑 **Rank SS** (`Stamina Overlord`): 30 a 45 minutos com múltiplos edges.
  - 🌌 **Rank SSS** (`Ascended Transcendence`): 45+ minutos de controle supremo.

---

### 📡 Webhooks do Discord para Eventos e Marcos
- **Marcos de Clímax (`.ap nut [nota]`)**: Envia um embed especial para o Discord com a avaliação/rank, tempo de sessão, imagem que provocou o clímax, total de nuts, contagem de edges e notas do jogador.
- **Marcos de Edge (`.ap edge`)**: Registra e notifica edges e tempo acumulado.
- **Feed NSFW Contínuo**: Envia cada arte nova com miniatura, autor, resolução, tags e link original.

---

### 🔞 100% Focado em Conteúdo NSFW (`StrictNSFW`)
- Descarta automaticamente resultados SFW/Safe e aceita apenas `rating:explicit` (`rating:e`).
- Injeta automaticamente tags adultas explícitas quando não houver termos de busca manuais.

---

## ⌨️ Lista Completa de Comandos (`.ap`)

| Comando | Descrição |
|---|---|
| `.ap` | Exibe o resumo do status atual (fonte, JOI, estatísticas da sessão). |
| `.ap nut [nota]` | Registra um clímax, calcula score/rank, salva estatísticas e envia embed para o Discord. |
| `.ap edge` | Registra um edge na sessão atual e inicia cooldown. |
| `.ap stats` | Exibe no chat todas as suas estatísticas, recordes e médias acumuladas. |
| `.ap resetstats` | Reseta todo o histórico e estatísticas salvas. |
| `.ap joi <on/off>` | Ativa ou desativa o modo JOI no HUD. |
| `.ap style <gentle/strict/hardcore/edging/dynamic>` | Altera o estilo do JOI. |
| `.ap next` | Carrega a próxima imagem/GIF imediatamente. |
| `.ap testwebhook` | Envia uma mensagem de teste para o Discord Webhook configurado. |
| `.ap webhook <url>` | Configura a URL do Webhook do Discord. |
| `.ap debug <on/off>` | Ativa o modo de logs detalhados no console do MultiMC. |
| `.ap strict <on/off>` | Ativa/desativa o filtro estrito de NSFW. |
| `.ap source <fonte>` | Troca a fonte (`YandeRE`, `Konachan`, `AIBooru`, `PurrBot`, `E621`, `NekosLife`, `LocalFolder`). |
| `.ap yande <tags>` | Define tags de pesquisa para o Yande.re e recarrega. |
| `.ap konachan <tags>` | Define tags para o Konachan e recarrega. |
| `.ap aibooru <tags>` | Define tags para o AIBooru e recarrega. |
| `.ap e621 <tags>` | Define tags para o E621 e recarrega. |
| `.ap purr <tag>` | Define a categoria de GIF do PurrBot (`fuck`, `blowjob`, `cum`, `anal`, etc.). |
| `.ap search <tags>` | Pesquisa tags na fonte ativa. |
| `.ap clear` | Limpa todas as tags de busca. |

---

## 🔨 Como Compilar

Requer **JDK 21**. Execute:

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
2. Certifique-se de que `-Drusherhack.enablePlugins=true` está presente nas configurações Java do MultiMC.
3. Inicie o jogo (Minecraft 1.21.1 com Fabric).
