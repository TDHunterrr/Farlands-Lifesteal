<div align="center">

# ❤️ Farlands Lifesteal

**A modern, high-performance Lifesteal mod & server plugin supporting Fabric, Quilt, NeoForge, Paper, and Folia.**

Developed by **TD_Hunterrr**

---

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B%20%2F%2026.x-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/Platform-Fabric%20%7C%20Quilt%20%7C%20NeoForge%20%7C%20Paper%20%7C%20Folia-blue.svg)]()
[![License](https://img.shields.io/badge/License-ARR%20(Modpack%20Allowed)-red.svg)](LICENSE)

</div>

---

## 📖 Overview

**Farlands Lifesteal** brings a balanced, battle-tested Lifesteal SMP experience to modern Minecraft. Built from the ground up on a unified multi-loader architecture, Farlands Lifesteal runs identically whether you are running a Fabric modded client/server, a NeoForge modpack, or a high-performance Paper / Folia multi-threaded dedicated server.

---

## ✨ Features

### ⚔️ Core Lifesteal Mechanics
- **Heart Loss & Drops**: When a player dies, they lose **1 heart** (2.0 health), and a physical **Heart** item is dropped at their death location.
- **Elimination & Banning**: If a player loses all their hearts (reaches 0 max health), they are banned from the server with the message: `You ran out of hearts!`.
- **Elimination Bounty**: Upon elimination, a personalized **Revive Token** bound to the fallen player is dropped at the scene.
- **Heart Limit**: Maximum health cap is set to **20 hearts** (40.0 health) to ensure balanced PvP.


### 📦 Custom Items
| Item | Appearance | Functionality |
|---|---|---|
| **Heart** | Custom 3D Hardcore Heart Skull | Right-click to consume and gain **+1 heart** (up to 20 hearts max). |
| **Revive Token** | Player Skull (bound to target) | Right-click on any block to **unban the player**. The revived player will be teleported directly to the targeted block with full health upon joining. |

---

## 🕹️ Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/withdrawheart <amount>` | `farlands.lifesteal.use` (default: all) | Converts your current max health into physical Heart items. You cannot withdraw down to 0 hearts. |
| `/removeheart <player> <amount>` | `farlands.lifesteal.admin` (default: op) | Removes hearts from the target player (capped at minimum 1 heart unless lethal). |
| `/revive <player> [hearts]` | `farlands.lifesteal.admin` (default: op) | Unbans an eliminated player and assigns their starting heart count (1–20 hearts, default: 10). |

---

## 🚀 Supported Platforms & Loaders

Farlands Lifesteal is packaged with a **Jar-in-Jar (JiJ)** universal distribution that runs across 5 major platforms:

- **Fabric**: Native support via pure Fabric Loader (**zero dependencies** — no Fabric API required!).
- **Quilt**: Native detection via embedded `quilt.mod.json` (zero dependencies).
- **NeoForge**: Native support via NeoForge `@Mod` entrypoint and event bus.
- **Paper**: Server plugin running on Paper, Purpur, and standard Bukkit servers.
- **Folia**: Thread-safe regional scheduling using `GlobalRegionScheduler` and `Entity.getScheduler()` (`folia-supported: true`).

---

## 🛠️ Building from Source

### Prerequisites
- **Java 25** (or compatible JDK)
- Internet connection (for Gradle dependency resolution)

### Build Commands

```powershell
# Build the Universal All-in-One Jar (works on all 5 platforms)
./gradlew universalJar

# Build all platform-specific jars at once
./gradlew build

# Build individual loader jars
./gradlew :fabric:remapJar   # Fabric & Quilt
./gradlew :neoforge:jar      # NeoForge
./gradlew :paper:jar         # Paper & Folia
```

### Artifact Outputs
- **Universal Jar:** `build/libs/farlands-lifesteal-<version>-universal.jar`
- **Fabric/Quilt Jar:** `fabric/build/libs/farlands-lifesteal-fabric-<version>.jar`
- **NeoForge Jar:** `neoforge/build/libs/farlands-lifesteal-neoforge-<version>.jar`
- **Paper/Folia Jar:** `paper/build/libs/farlands-lifesteal-paper-<version>.jar`

---

## 📜 License

Farlands Lifesteal is licensed under **All Rights Reserved (ARR) with Modpack Permissions** by **TD_Hunterrr**.

- **Standalone Distribution**: Exclusively reserved for TD_Hunterrr. You may not re-upload or mirror standalone `.jar` files on third-party websites or services.
- **Modpack Permissions**: You **are permitted** to include the unmodified compiled `.jar` in public or private Minecraft modpacks (CurseForge, Modrinth, FTB, ATLauncher, Prism, Technic) provided attribution is given.

See the full [LICENSE](LICENSE) file for complete legal terms.
