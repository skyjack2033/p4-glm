# ECOAE Extension - GTNH Migration TODO

## Critical (Must Complete)

### 1. Custom Block Registration
- [ ] Create `BlockECOAEMeta` base class with metadata variants
- [ ] Create `BlockECOAEItemBlock` for meta blocks
- [ ] Update `BlockLoader` to register all subsystem blocks
- [ ] Wire blocks into `RecipeLoader` outputs (replace `Blocks.air` placeholders)

### 2. Structure Validation Block References
- [ ] Update `EStorageController.checkMachine()` to use `BlockLoader` references
- [ ] Update `ECalculatorController.checkMachine()` similarly
- [ ] Update `EFabricatorController.checkMachine()` similarly

### 3. Textures
- [ ] Create 16x16 PNG block textures (19 total across 3 subsystems)
- [ ] Create 176x166 PNG GUI backgrounds (3 files)

## Important

### 4. AE2 Cell System
- [ ] Implement `EStorageCellInventory` for custom storage cells
- [ ] Register cell handler with AE2 registry

### 5. Localization
- [ ] Create zh_CN.lang

## Nice to Have
- [ ] NEI integration
- [ ] Advanced virtual CPU lifecycle
- [ ] EFabricator pattern execution pipeline
