# AnimePics (RusherHack)

RusherHack plugin based on the Femhack `AnimePics` module: a 2D HUD overlay displaying random NSFW anime pictures & animated GIFs on screen with custom tag search capabilities.

## WARNING: Can get EXTREMELY laggy (especially with Purrbot on)!!!!!!!!!!!!!!!!!

Original idea by oehrasa (Anime-Pics module on Oehrasa-Bookies-Addon)

## Features

- **Tag Search System (`SearchTags`)**:
  - **Yande.re & Konachan Search**: Type any character, franchise, clothing, or descriptive tags (e.g. `genshin_impact`, `blue_archive`, `swimsuit`, `thighs`, `maid`, etc.). Supports multiple tags separated by space or comma.
  - **Waifu.im Search**: Type custom tag keywords or select from predefined NSFW categories.
  - **Instant Live Refresh**: Automatically reloads and fetches matching results immediately when you update your search tags.
  - **Smart Fallback**: If a random high page has no results for specific niche tags, it automatically queries earlier pages to always deliver pictures.

- **NSFW Focused Image & GIF Sources**:
  - **YandeRE** — High-resolution anime artwork with `Explicit` and `Questionable` rating filters, custom tag search, and random pagination.
  - **Konachan** — Anime wallpapers via `konachan.com` with NSFW ratings (`Explicit` / `Questionable`), custom tag search, and random pages.
  - **PurrBot** — High-quality animated NSFW GIFs (`fuck`, `blowjob`, `cum`, `anal`, `pussylick`, `solo`, `yaoi`, `yuri`, `neko`) with full frame animation and automatic tag cycling.
  - **WaifuIM** — NSFW tags (`ero`, `ecchi`, `oppai`, `hentai`, `milf`, `ass`, `paizuri`, `oral`, or custom tags) with tag cycling.
  - **LocalFolder** — Load and display your own local images/GIFs from `.minecraft/rusherhack/animepics/` (`.png`, `.jpg`, `.jpeg`, `.gif`).

- **Automatic Downloads (`AutoDownload`)**:
  - Automatically saves every fetched image and animated GIF into your `.minecraft/rusherhack/animepics/downloads/` folder.

- **Animated GIF Support**:
  - Real-time GIF decoding with configurable frame limits (`MaxGifFrames`) and toggleable animation (`AnimateGifs`).

- **Clean & Lightweight**:
  - Native 2D rendering using RusherHack's `IRenderer2D`, customizable positions (`X`, `Y`), dimensions (`Width`, `Height`), and refresh delay in game ticks.

 ## Why is this addon on 1.21.1? nobody uses that shi

 cuz it works on pluto's rusher crack 🤑🤑🤑🤑🤑🤑

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
