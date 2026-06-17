# ECOAE Extension - GTNH Migration TODO

## Completed ✅

### Core Framework
- [x] ECOAEExtension main class with @Mod annotation
- [x] CommonProxy/ClientProxy lifecycle management
- [x] Config with all tier parameters
- [x] ECOAETier enum with voltage mapping

### Multiblock System
- [x] ECOAEExtendedPowerMultiBlockBase with AE2 proxy (IGridProxyable/IActionHost/IGridHost)
- [x] EStorageController with structure checking, ICellProvider, GUI
- [x] ECalculatorController with virtual CPUs, ICraftingProvider, GUI
- [x] EFabricatorController with pattern crafting, overclock, cooling, GUI

### Block/Item Registration
- [x] BlockECOAEMeta meta block class (19 block variants)
- [x] ItemLoader with IItemContainer enum (6 items)
- [x] RecipeLoader with 19 assembler recipes

### AE2 Integration
- [x] AE2StorageHelper with GTNH AE2 rv3 API
- [x] EStorageCellHandler with proper ICellHandler signatures
- [x] EStorageCellInventory - full ICellInventory implementation (NBT-backed, byte accounting)
- [x] EFabricatorPatternHandler - full ICraftingProvider implementation (pattern execution pipeline)
- [x] HatchAEStorageBus - AE2 Storage Bus hatch for EStorage (IGridProxyable, wire cutter support)
- [x] HatchAEPatternProvider - AE2 Pattern Provider hatch for EFabricator (ICraftingProvider, pattern management)

### GUI System
- [x] ECOAEGuiHandler with MetaTileEntity cast pattern
- [x] ContainerECOAE with player inventory
- [x] 3 GUI screens (EStorage, ECalculator, EFabricator)

### Textures & Localization
- [x] 19 block textures with distinct patterns per subsystem
- [x] 3 GUI background textures
- [x] en_US.lang and zh_CN.lang

### Build System
- [x] dependencies.gradle with correct version numbers
- [x] BUILD SUCCESSFUL - 0 errors
- [x] JAR builds successfully (112KB, 41 classes)

### Critical Bug Fixes (2026-06-16)
- [x] EStorage: Fix direction scanning (was always picking DOWN)
- [x] EStorage: Fix segment edge Y calculation
- [x] EStorage: Implement getCellArray() (was returning empty list)
- [x] EStorage: Add ISaveProvider for cell data persistence
- [x] EStorage: Add cellStacks field with NBT persistence
- [x] EStorage: Add insertCell()/removeCell() for cell management
- [x] EStorage: Add right-click cell insertion/removal
- [x] ECalculator: Fix NPE in ActiveCraftingJob.readFromNBT
- [x] ECalculator: Add pattern inventory (addPatternItem/removePatternItem/refreshPatterns)
- [x] ECalculator: Update provideCrafting() to register stored patterns
- [x] ECalculator: Update isBusy() to allow concurrent jobs
- [x] ECalculator: Add right-click pattern insertion/removal
- [x] EFabricator: Fix isBusy() (was preventing concurrent job acceptance)
- [x] EFabricator: Add pattern inventory with addPatternItem()/removePatternItem()
- [x] EFabricator: Add refreshPatterns() to decode pattern items
- [x] EFabricator: Update NBT persistence for pattern items
- [x] EFabricator: Add coolant type validation (water, distilled water, IC2 coolant, cryotheum)
- [x] EFabricator: Add right-click pattern insertion/removal

## Remaining

### In-Game Testing
- [ ] Test multiblock formation
- [ ] Test AE2 network connectivity
- [ ] Test recipe registration in NEI

### Known Limitations
- [ ] EStorage: No per-drive TileEntities (cells stored in controller NBT)
- [ ] EStorage: No IAEPowerStorage (uses GT energy hatches only)
- [ ] ECalculator: Works as crafting provider/medium (not as AE2 CPU)
- [ ] EFabricator: No parallel processor modifier system

### Polish
- [ ] NEI integration for ECOAE recipe map
- [ ] GUI interactive elements (buttons, toggles)
