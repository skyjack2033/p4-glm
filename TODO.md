# ECOAE Extension - GTNH Migration TODO

## Completed

### 1. Custom Block Registration
- [x] Create `BlockECOAEMeta` base class with metadata variants
- [x] Create `BlockECOAEItemBlock` for meta blocks
- [x] Update `BlockLoader` to register all subsystem blocks (5 EStorage + 8 ECalculator + 6 EFabricator)
- [x] Wire blocks into `RecipeLoader` outputs (replace `Blocks.air` placeholders)

### 2. Structure Validation Block References
- [x] Update `EStorageController.checkMachine()` to use `BlockLoader` references
- [x] Update `ECalculatorController.checkMachine()` similarly
- [x] Update `EFabricatorController.checkMachine()` similarly

### 3. Textures
- [x] Create 16x16 PNG block textures (19 total across 3 subsystems)
- [x] Create 176x166 PNG GUI backgrounds (3 files)

### 4. Localization
- [x] Create zh_CN.lang

## Remaining

### 5. AE2 Cell System
- [ ] Implement `EStorageCellInventory` for custom storage cells
- [ ] Register cell handler with AE2 registry

### 6. In-Game Testing
- [ ] Compile with GTNH dev environment
- [ ] Test multiblock formation
- [ ] Test AE2 network connectivity
- [ ] Test recipe registration

### 7. Polish
- [ ] NEI integration for ECOAE recipe map
- [ ] Advanced virtual CPU lifecycle for ECalculator
- [ ] EFabricator pattern execution pipeline
- [ ] Replace placeholder textures with proper art
