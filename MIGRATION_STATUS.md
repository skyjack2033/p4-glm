# ECOAE Extension - GTNH Migration Status

## Overview

This document tracks the migration of **NovaEngineering-ECOAEExtension** from Minecraft 1.12.2 to the GTNH (GregTech New Horizons) 1.7.10 community mod template.

**Source Repository:** https://github.com/sddsd2332/NovaEngineering-ECOAEExtension
**Target Template:** GTNH ExampleMod1.7.10

---

## Migration Strategy

### What Changed

| Source (1.12.2) | Target (GTNH 1.7.10) |
|---|---|
| Modular Machinery Community Edition | GregTech Multiblock Framework |
| MMCE JSON machine definitions | StructureLib `IStructureDefinition` + manual `checkMachine()` |
| 13 AE2 Mixins | AE2 API-based integration (`IGridProxyable`, `AENetworkProxy`) |
| 4 MMCE Mixins | Not needed (using GT multiblocks) |
| 1.12.2 AE2 Extended Life | GTNH AE2 Fork (rv3-beta-982-GTNH) |
| Forge 1.12.2 capabilities | 1.7.10 TileEntity interfaces |
| 1.12.2 block model JSON | 1.7.10 IIcon/texture registration |
| GeckoLib animated models | Standard GTNH block textures |
| MMCE `EXECUTE_MANAGER` async | Server tick-based processing |

### What Was Preserved

- Three multiblock subsystems: EStorage, ECalculator, EFabricator
- Tier system: L4 (HV), L6 (IV), L9 (LuV)
- AE2 integration concept (storage, crafting, computing)
- Configurable parameters for each tier
- Modular structure with extendable multiblocks (1-16 segments)
- All localization strings

---

## Files Summary

### Core Framework (4 files)
- `ECOAEExtension.java` - Main @Mod entry point
- `CommonProxy.java` - Lifecycle management, calls loaders
- `ClientProxy.java` - Client-side initialization
- `Config.java` - Forge configuration with all tier parameters

### Registration Infrastructure (4 files)
- `loader/ItemLoader.java` - Item registration (6 items: 3 storage cells, 3 calculator cells)
- `loader/BlockLoader.java` - Block registration (3 meta blocks: EStorage, ECalculator, EFabricator)
- `loader/MachineLoader.java` - Machine ID management and registration (9 controllers)
- `loader/RecipeLoader.java` - Assembler recipes for all blocks (StainlessSteel casing, etc.)

### Multiblock System (4 files)
- `multiblock/ECOAEExtendedPowerMultiBlockBase.java` - Abstract base with AE2 proxy (`IGridProxyable`), tier detection, texture
- `multiblock/estorage/EStorageController.java` - Storage multiblock (2x2xN stackable, ~830 lines)
- `multiblock/ecalculator/ECalculatorController.java` - Computing multiblock (3x3x3 fixed, ~1150 lines)
- `multiblock/efabricator/EFabricatorController.java` - Crafting multiblock (3x3x3 fixed, ~970 lines)

### AE2 Integration (5 files)
- `ae2/AE2StorageHelper.java` - Grid access utilities (GTNH AE2 rv3 API)
- `ae2/EStorageCellHandler.java` - Custom cell handler (ICellHandler)
- `ae2/EStorageCellInventory.java` - NBT-backed cell inventory (inject/extract logic)
- `ae2/ECalculatorCraftingHandler.java` - Crafting acceleration handler
- `ae2/EFabricatorPatternHandler.java` - Pattern-based auto-crafting handler

### Item System (3 files)
- `item/ItemECOAEBase.java` - Base item class
- `item/ItemStorageCell.java` - Storage cell items (16M/64M/256M bytes)
- `item/ItemCalculatorCell.java` - Calculator cell items (64M/1024M/16384M bytes)

### Block System (2 files)
- `block/BlockECOAEMeta.java` - Meta block with Material.iron, metadata variants
- `block/BlockECOAEItemBlock.java` - ItemBlock for metadata blocks

### GUI System (3 files - MUI2 framework)
- `gui/mui2/EStorageGui.java` - EStorage MUI2 GUI
- `gui/mui2/ECalculatorGui.java` - ECalculator MUI2 GUI
- `gui/mui2/EFabricatorGui.java` - EFabricator MUI2 GUI

### Recipe System (2 files)
- `recipe/ECOAERecipeMaps.java` - Recipe map definitions (for future use)
- `recipe/ECOAENEIHandler.java` - NEI integration handler

### Utilities (2 files)
- `util/ECOAETier.java` - Tier enum with voltage mapping + config accessors
- `util/Textures.java` - Texture resource locations

### Resources
- `mcmod.info` - Mod metadata
- `assets/ecoaeext/lang/en_US.lang` - English localization (~265 lines)
- `assets/ecoaeext/lang/zh_CN.lang` - Chinese localization (~262 lines)
- 19 block textures (PNG) across 3 subsystems
- 3 GUI background textures

**Total: 25 Java source files + resources**

---

## Configuration

### gradle.properties
```properties
modName = ECOAE Extension
modId = ecoaeext
modGroup = com.github.GTNewHorizons.ecoaeextension
generateGradleTokenClass = com.github.GTNewHorizons.ecoaeextension.Tags
```

### dependencies.gradle
```groovy
dependencies {
    api("com.github.GTNewHorizons:GT5-Unofficial:5.09.52.608:dev")
    api("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-982-GTNH:dev")
    api("com.github.GTNewHorizons:StructureLib:1.4.38:dev")
    implementation("com.github.GTNewHorizons:GTNHLib:0.11.16:dev")
    compileOnly("com.github.GTNewHorizons:NotEnoughItems:2.8.103-GTNH:dev")
}
```

---

## Machine ID Allocation

| ID Range | Purpose |
|---|---|
| 19500-19502 | EStorage Controllers (L4/L6/L9) |
| 19510-19512 | ECalculator Controllers (L4/L6/L9) |
| 19520-19522 | EFabricator Controllers (L4/L6/L9) |
| 19530-19531 | Custom AE2 Hatches (reserved, not yet implemented) |

---

## Implementation Status

### Complete
- [x] Project structure and build configuration (RetroFuturaGradle)
- [x] Core framework (entry point, proxies, config)
- [x] Registration infrastructure (loaders)
- [x] Multiblock base class with AE2 proxy (`IGridProxyable`)
- [x] EStorage controller: 2x2xN stackable structure, AE2 cell provider, MUI2 GUI
- [x] EStorage: GT5U hatch support (energy, maintenance, debug variants)
- [x] EStorage: Storage capacity scales with voltage tier and segment count
- [x] ECalculator controller: 3x3x3 fixed section, virtual CPUs, crafting provider, MUI2 GUI
- [x] ECalculator: Job completion notification via AE2 grid events
- [x] EFabricator controller: 3x3x3 fixed section, pattern crafting, overclock, cooling, MUI2 GUI
- [x] AE2 integration helper classes (GTNH rv3 API)
- [x] Item classes (storage cells, calculator cells) with registration
- [x] Block classes (meta blocks with metadata variants)
- [x] GUI system migrated to GT5 MUI2 framework
- [x] Recipe registration (assembler recipes with StainlessSteel casing)
- [x] Localization files (en_US.lang, zh_CN.lang) with correct key format
- [x] StructureLib construct() method for auto-build
- [x] StructureLib element definitions (ofBlock) for all controllers
- [x] Block name localization (tile.ecoaeext.* format)
- [x] Controller texture system (stainless steel casing texture ID 48)
- [x] NEI integration (recipe maps registered)
- [x] EStorageCellHandler custom cell inventory implementation
- [x] EStorageCellInventory NBT-backed inject/extract logic
- [x] Config.java proper long value loading for cell drive capacities
- [x] All 66 unit tests passing

### Remaining (Polish & Testing)
- [ ] Actual in-game testing with GTNH dev environment
- [ ] Custom AE2 hatches (HatchAEStorageBus, HatchAEPatternProvider)
- [ ] Item renderer registration
- [ ] Custom block textures (optional - currently using GT5 defaults)

---

## Architecture Notes

### Multiblock Structure Patterns

**EStorage** (2x2xN stackable):
```
Base layer (2x2): Controller + ME Channel + 2 Casings
Stackable layers (2x2, 1-16): Cell Drives, Energy Cells, Vents, Casings, or GT5U Hatches
Top layer (2x2): 4 Casings (end cap)
```

**ECalculator** (3x3x3 fixed):
```
Fixed 3x3x3 section with controller at center, ME channel above
Repeating segments: thread cores, hyper-threads, parallel processors, cell drives, transmitter buses
End cap: tail block
```

**EFabricator** (3x3x3 fixed):
```
Fixed 3x3x3 section with controller, ME channel, vent, fluid I/O
Repeating segments: pattern buses, worker cores, parallel processors, vents
End cap: casings
```

### AE2 Integration
- All controllers implement `IGridProxyable` via `AENetworkProxy`
- EStorage implements `ICellProvider` for storage grid integration
- ECalculator implements `ICraftingCPU` and `ICraftingProvider` for crafting
- EFabricator delegates to `EFabricatorPatternHandler` for pattern-based crafting
- Cable type: DENSE (supports dense cable connections)

### Tier System
| Tier | GT Voltage Tier | EU/t | Parallel | EStorage Capacity | ECalculator Threads |
|------|----------------|------|----------|-------------------|---------------------|
| L4 | HV (3) | 512 | 1x | 2.5M EU/segment | 1 per core |
| L6 | IV (5) | 2048 | 4x | 25M EU/segment | 2 per core |
| L9 | LuV (6) | 8192 | 16x | 250M EU/segment | 4 per core |

### Test Status
- **66 tests, all passing**
- Tests cover: tier mapping, voltage values, metadata constants, machine ID allocation, config defaults, structure layout math, coordinate transforms, cross-system consistency

---

## Reference Documentation

- GTNH Modding Guide: https://github.com/GTNewHorizons/ExampleMod1.7.10
- GregTech 5 API: GT5-Unofficial:5.09.52.608
- StructureLib: 1.4.38
- AE2 GTNH Fork: Applied-Energistics-2-Unofficial:rv3-beta-982-GTNH
- TST Reference: https://github.com/Nxer/Twist-Space-Technology-Mod
- GTNL Reference: https://github.com/ABKQPO/GT-Not-Leisure

---

## License

Original mod: GPL-3.0 (NovaEngineering-ECOAEExtension)
Ported to GTNH with permission from original authors.
