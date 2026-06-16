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
| 1.12.2 AE2 Extended Life | GTNH AE2 Fork (rv1 API) |
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
- `loader/ItemLoader.java` - IItemContainer enum + item registration (6 items)
- `loader/BlockLoader.java` - Block registration (stub - MTEs use MachineLoader)
- `loader/MachineLoader.java` - Machine ID management and registration (9 controllers)
- `loader/RecipeLoader.java` - 19 assembler recipes for all blocks

### Multiblock System (4 files)
- `multiblock/ECOAEExtendedPowerMultiBlockBase.java` - Abstract base with AE2 proxy (`IGridProxyable`)
- `multiblock/estorage/EStorageController.java` - Storage multiblock (743+ lines)
- `multiblock/ecalculator/ECalculatorController.java` - Computing multiblock (1182+ lines)
- `multiblock/efabricator/EFabricatorController.java` - Crafting multiblock (842+ lines)

### AE2 Integration (4 files)
- `ae2/AE2StorageHelper.java` - Grid access utilities (GTNH AE2 rv1 API)
- `ae2/EStorageCellHandler.java` - Custom cell handler (ICellHandler)
- `ae2/ECalculatorCraftingHandler.java` - Crafting acceleration handler
- `ae2/EFabricatorPatternHandler.java` - Pattern-based auto-crafting handler

### Item System (3 files)
- `item/ItemECOAEBase.java` - Base item class
- `item/ItemStorageCell.java` - Storage cell items (16M/64M/256M)
- `item/ItemCalculatorCell.java` - Calculator cell items (64M/1024M/16384M)

### GUI System (5 files)
- `gui/ECOAEGuiHandler.java` - GUI handler for all controllers
- `gui/ContainerECOAE.java` - Basic container with player inventory
- `gui/GUIEStorageController.java` - EStorage GUI screen
- `gui/GUIECalculatorController.java` - ECalculator GUI screen
- `gui/GUIEFabricatorController.java` - EFabricator GUI screen

### Recipe System (1 file)
- `recipe/ECOAERecipeMaps.java` - Recipe map definitions (for future use)

### Utilities (2 files)
- `util/ECOAETier.java` - Tier enum with voltage mapping + config accessors
- `util/Textures.java` - Texture resource locations

### Resources (3 files)
- `mcmod.info` - Mod metadata
- `assets/ecoaeext/lang/en_US.lang` - Localization strings (149 lines)
- `assets/ecoaeext/textures/gui/README.txt` - GUI texture specs

**Total: 30 Java files + 3 resource files**

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
    api("com.github.GTNewHorizons:GT5-Unofficial:latest:dev")
    api("com.github.GTNewHorizons:Applied-Energistics-2-GTNH:latest:dev")
    api("com.github.GTNewHorizons:StructureLib:latest:dev")
    implementation("com.github.GTNewHorizons:GTNHLib:latest:dev")
    compileOnly("com.github.GTNewHorizons:NotEnoughItems:2.7.69-GTNH:dev")
    compileOnly("org.spongepowered:mixin:0.8.5-GTNH:dev")
}
```

---

## Machine ID Allocation

| ID Range | Purpose |
|---|---|
| 19500-19502 | EStorage Controllers (L4/L6/L9) |
| 19510-19512 | ECalculator Controllers (L4/L6/L9) |
| 19520-19522 | EFabricator Controllers (L4/L6/L9) |
| 19530-19531 | Custom AE2 Hatches (reserved) |

---

## Implementation Status

### Complete
- [x] Project structure and build configuration
- [x] Core framework (entry point, proxies, config)
- [x] Registration infrastructure (loaders)
- [x] Multiblock base class with AE2 proxy (`IGridProxyable`)
- [x] EStorage controller with structure checking, AE2 cell provider, GUI
- [x] ECalculator controller with virtual CPUs, crafting provider, GUI
- [x] EFabricator controller with pattern crafting, overclock, cooling, GUI
- [x] AE2 integration helper classes (GTNH rv1 API)
- [x] Item classes (storage cells, calculator cells) with registration
- [x] GUI system migrated to GT5 MUI2 framework
- [x] Recipe registration (19 assembler recipes)
- [x] Localization files (en_US.lang, zh_CN.lang) with correct key format
- [x] StructureLib construct() method for auto-build
- [x] Block name localization (tile.ecoaeext.* format)
- [x] Controller texture system (uses block textures)
- [x] NEI integration (recipe maps registered)

### Remaining (Polish & Testing)
- [ ] Custom cell inventory implementation for EStorageCellHandler
- [ ] Actual in-game testing with GTNH dev environment
- [ ] Custom block textures (optional - currently using GT5 defaults)
- [ ] Recipe output items refinement

---

## Architecture Notes

### Multiblock Structure Pattern
All three subsystems use a linear multiblock pattern:
```
[Controller/Fixed Section] -> [Segment 1] -> [Segment 2] -> ... -> [Segment N] -> [End Cap]
```

- **Fixed section**: 3x3x3 with controller, ME channel, and optional fluid I/O
- **Repeating segments**: 3 high x 2 deep x 1 wide with functional blocks
- **End cap**: 3 high x 2 deep x 1 wide with casings
- **Segment count**: 1 to 16 (configurable)

### AE2 Integration
- All controllers implement `IGridProxyable` via `AENetworkProxy`
- EStorage implements `ICellProvider` for storage grid integration
- ECalculator implements `ICraftingCPU` and `ICraftingProvider` for crafting
- EFabricator delegates to `EFabricatorPatternHandler` for pattern-based crafting

### Tier System
| Tier | Voltage | EU/t | Parallel | Use Case |
|------|---------|------|----------|----------|
| L4 | HV | 512 | 1x | EStorage, ECalculator, EFabricator |
| L6 | IV | 2048 | 4x | EStorage, ECalculator, EFabricator |
| L9 | LuV | 8192 | 16x | EStorage, ECalculator, EFabricator |

---

## Reference Documentation

- GTNH Modding Guide: https://github.com/GTNewHorizons/ExampleMod1.7.10
- GregTech 5 API: Available in GT5-Unofficial dependency
- StructureLib: https://github.com/GTNewHorizons/StructureLib
- AE2 GTNH Fork: https://github.com/GTNewHorizons/Applied-Energistics-2-GTNH
- TST Reference: https://github.com/Nxer/Twist-Space-Technology-Mod
- GTNL Reference: https://github.com/ABKQPO/GT-Not-Leisure

---

## License

Original mod: GPL-3.0 (NovaEngineering-ECOAEExtension)
Ported to GTNH with permission from original authors.
