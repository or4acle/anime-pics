# AnimePics (RusherHack)

RusherHack plugin based on the Femhack `AnimePics` module: a 2D HUD overlay displaying random NSFW anime pictures & animated GIFs on screen.

## Features

- **NSFW Focused Image & GIF Sources**:
  - **YandeRE** — High-resolution anime artwork with `Explicit` and `Questionable` rating filters, custom tag search, and random pagination.
  - **Konachan** — Anime wallpapers via `konachan.com` with NSFW ratings (`Explicit` / `Questionable`), custom tag filtering, and random pages.
  - **PurrBot** — High-quality animated NSFW GIFs (`fuck`, `blowjob`, `cum`, `anal`, `pussylick`, `solo`, `yaoi`, `yuri`, `neko`) with full frame animation and automatic tag cycling.
  - **WaifuIM** — NSFW tags (`ero`, `ecchi`, `oppai`, `hentai`, `milf`, `ass`, `paizuri`, `oral`) with tag cycling.
  - **LocalFolder** — Load and display your own local images/GIFs from `.minecraft/rusherhack/animepics/` (`.png`, `.jpg`, `.jpeg`, `.gif`).

- **Automatic Downloads (`AutoDownload`)**:
  - Automatically saves every fetched image and animated GIF into your `.minecraft/rusherhack/animepics/downloads/` folder.

- **Animated GIF Support**:
  - Real-time GIF decoding with configurable frame limits (`MaxGifFrames`) and toggleable animation (`AnimateGifs`).

- **Clean & Lightweight**:
  - Native 2D rendering using RusherHack's `IRenderer2D`, customizable positions (`X`, `Y`), dimensions (`Width`, `Height`), and refresh delay in game ticks.

## Build Instructions

Requires **JDK 21**. In the project root directory:

**Linux / macOS:**
```bash
./gradlew build
```

**Windows:**
```powershell
.\gradlew.bat build
```

The compiled `.jar` file will be generated in `build/libs/AnimePics-1.0.0.jar`.

## Installation

1. Copy `AnimePics-1.0.0.jar` into your `.minecraft/rusherhack/plugins/` directory.
2. Launch Minecraft with `-Drusherhack.enablePlugins=true` in your JVM launch arguments.
3. Target Minecraft version: **1.21.1** (RusherHack **2.0.5**).
