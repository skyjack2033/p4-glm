# ECOAE Extension - In-Game Test Plan

## Step 1: Basic Startup
- [ ] Place JAR in GTNH mods folder
- [ ] Start game - verify no crash on load
- [ ] Check log for "ECOAE Extension v... pre-initializing..."
- [ ] Check log for "ECOAE Extension machines registered."
- [ ] Check log for "ECOAE Extension recipes registered."
- [ ] Check log for "ECOAE Extension items registered."

## Step 2: Creative Tab
- [ ] Open creative mode
- [ ] Search for "ecoaeext" or "EStorage" in creative tab
- [ ] Verify all 9 controller items appear (L4/L6/L9 x 3 types)
- [ ] Verify all 6 custom items appear (3 storage cells + 3 calculator cells)
- [ ] Verify all 19 block variants appear
- [ ] Verify 2 hatch items appear (AE Storage Bus, AE Pattern Provider)

## Step 3: NEI Recipes
- [ ] Open NEI
- [ ] Search for "EStorage Casing" - verify assembler recipe shows
- [ ] Click on recipe - verify ingredients display correctly (Stainless Steel plates)
- [ ] Check all recipes are visible in Assembler category

## Step 4: Block Placement
- [ ] Place EStorage Casing block - verify it renders
- [ ] Place ECalculator Casing block
- [ ] Place EFabricator Casing block
- [ ] Right-click each - verify no crash

## Step 5: Multiblock Formation (EStorage - 2x2xN stackable)
- [ ] Place EStorage Controller L4
- [ ] Build base layer (2x2): controller + ME channel + 2 casings
- [ ] Add stackable layers (2x2): cell drives, energy cells, vents, or casings
- [ ] Add top layer (2x2): 4 casings (end cap)
- [ ] Verify controller shows "Structure Formed"
- [ ] Test with GT5U energy hatch in casing position - should be accepted
- [ ] Test with GT5U maintenance hatch - should be accepted (debug variant too)

## Step 6: Multiblock Formation (ECalculator - 3x3x3 fixed)
- [ ] Place ECalculator Controller L4
- [ ] Build 3x3x3 fixed section with casings, ME channel, thread cores
- [ ] Verify controller shows "Structure Formed"

## Step 7: Multiblock Formation (EFabricator - 3x3x3 fixed)
- [ ] Place EFabricator Controller L4
- [ ] Build 3x3x3 fixed section with casings, ME channel, vent
- [ ] Verify controller shows "Structure Formed"

## Step 8: AE2 Connection
- [ ] Place AE2 ME cable near EStorage controller
- [ ] Verify controller shows "AE2: Connected" in GUI
- [ ] Open controller GUI by right-clicking
- [ ] Verify GUI displays tier, segments, cell drives info
- [ ] Test HatchAEStorageBus - place near EStorage, connect to ME network
- [ ] Test HatchAEPatternProvider - place near EFabricator, add patterns

## Step 9: Recipe Registration
- [ ] Open assembler machine
- [ ] Search for EStorage Casing recipe
- [ ] Verify recipe inputs are correct (6 Stainless Steel plates + HV circuit + HV emitter)
- [ ] Craft one EStorage Casing

## Step 10: Cell Operations
- [ ] Right-click EStorage controller with storage cell - verify insertion
- [ ] Sneak+right-click to remove cell - verify removal
- [ ] Verify chat messages display correctly

## If Crash Occurs
1. Copy full crash report
2. Look for "ecoaeextension" in the stack trace
3. Report the exact error and line number
