# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Mod Does

QuickStack is a NeoForge Minecraft mod (mod ID: `quickstack`) that adds a Terraria-style quick stack keybind (default: `V`). When pressed, items in the player's hotbar-excluded inventory (slots 9–35) are moved into nearby containers (within 8 blocks) that already contain matching items. A flying item visual animates from the player to the target container on the client side.

## Build & Run Commands

```powershell
# Build the mod JAR
./gradlew build

# Run Minecraft client for testing
./gradlew runClient

# Run Minecraft server for testing
./gradlew runServer

# Refresh dependencies (use when IDE shows missing libraries)
./gradlew --refresh-dependencies

# Clean build outputs
./gradlew clean
```

The built JAR is output to `build/libs/`. CI runs `./gradlew build` on every push/PR.

## Tech Stack

- **NeoForge** `26.1.0.19-beta` on Minecraft `26.1`
- **Java 25** (Mojang ships Java 25 to end users for this version)
- **Gradle** with NeoForge userdev plugin `7.1.27`

## Architecture

The mod follows a client→server→client packet flow:

1. **Client input** (`QuickStackKeybinds`): Registers keybind category + key (`V`), listens on `ClientTickEvent.Post`, and sends a `QuickStackPayload` to the server when pressed.

2. **Server logic** (`QuickStackHandler`): Receives the payload, scans a 17×17×17 cube around the player for approved containers (chest, trapped chest, barrel, shulker box), and for each matching item in the player's main inventory (slots 9–35), inserts it — first filling partial stacks, then empty slots. After each successful transfer, sends a `QuickStackVisualPayload` back to the triggering client.

3. **Client visual** (`QuickStackVisualPayload` → `FlyingItemVisualManager` → `FlyingItemVisual`): On receipt, spawns a `FlyingItemVisual` that linearly interpolates from the player's head to the target container center over 40 ticks (2 seconds). `FlyingItemRenderer` ticks all active visuals each `ClientTickEvent.Post`.

### Network Payloads

| Class | Direction | Purpose |
|---|---|---|
| `QuickStackPayload` | Client → Server | Trigger the quick stack action (no data) |
| `QuickStackVisualPayload` | Server → Client | Spawn a flying item visual (`BlockPos targetPos`, `ItemStack stack`) |

Both payloads are registered in `QuickStackMod` via `RegisterPayloadHandlersEvent`.

### Adding New Container Types

To support additional blocks in `QuickStackHandler.isApprovedInventory()`, add a block check. The block must also implement the `Container` interface via its `BlockEntity` or the check at line 44 will skip it.

## Package Structure

```
com.TickingClockWork.quickstack
├── QuickStackMod.java          # @Mod entry point, payload registration
├── client/
│   ├── QuickStackKeybinds.java # Keybind registration + input handling
│   ├── FlyingItemRenderer.java # Ticks FlyingItemVisualManager each client tick
│   ├── FlyingItemVisualManager.java # Manages list of active flying item visuals
│   └── FlyingItemVisual.java   # Single lerp-animated item visual
├── network/
│   ├── QuickStackPayload.java       # C→S trigger packet
│   └── QuickStackVisualPayload.java # S→C visual spawn packet
└── server/
    └── QuickStackHandler.java  # Core stacking logic
```

## Key Properties (`gradle.properties`)

Update these when changing mod identity or targeting a new Minecraft/NeoForge version:

```
mod_id=quickstack        # must match @Mod annotation and neoforge.mods.toml
mod_version=1.0.0
mod_group_id=com.TickingClockWork.quickstack
minecraft_version=26.1
neo_version=26.1.0.19-beta
```

Note: `gradle.properties` still shows leftover `mod_group_id=com.example.examplemod` — this should be updated to `com.TickingClockWork.quickstack` to match the actual source package.
