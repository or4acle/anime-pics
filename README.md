# AnimePics (RusherHack)

Plugin RusherHack baseado no módulo Femhack `AnimePics`: overlay 2D com imagens/GIFs aleatórios.

## Fontes

- **WaifuIM** — tags SFW e NSFW, com `NsfwMode` (`All` / `Only` / `None`)
- **NekosLife** — API pública SFW
- **Safebooru**
- **LocalFolder** — arquivos em `.minecraft/rusherhack/animepics/` (png, jpg, jpeg, gif)

## Build

JDK 21. Na pasta do projeto:

```
./gradlew build
```

No Windows:

```
.\gradlew.bat build
```

O jar fica em `build/libs/`. Coloque em `.minecraft/rusherhack/plugins/` e use `-Drusherhack.enablePlugins=true`.

Minecraft alvo: **1.21.4** (mesmo template do [example-plugin](https://github.com/RusherDevelopment/example-plugin)).
