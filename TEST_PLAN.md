# ECOAE Extension - In-Game Test Plan

## Step 1: Basic Startup
- [ ] Place JAR in GTNH mods folder
- [ ] Start game - verify no crash on load
- [ ] Check log for "ECOAE Extension v... pre-initializing..."
- [ ] Check log for "ECOAE Extension machines registered."
- [ ] Check log for "ECOAE Extension recipes registered."

## Step 2: Creative Tab
- [ ] Open creative mode
- [ ] Search for "ecoaeext" or "EStorage" in creative tab
- [ ] Verify all 9 controller items appear (L4/L6/L9 x 3 types)
- [ ] Verify all 6 custom items appear (3 storage cells + 3 calculator cells)
- [ ] Verify all 19 block variants appear

## Step 3: NEI Recipes
- [ ] Open NEI
- [ ] Search for "EStorage Casing" - verify assembler recipe shows
- [ ] Click on recipe - verify ingredients display correctly
- [ ] Check all 19 recipes are visible in Assembler category

## Step 4: Block Placement
- [ ] Place EStorage Casing block - verify it renders (placeholder texture OK)
- [ ] Place ECalculator Casing block
- [ ] Place EFabricator Casing block
- [ ] Right-click each - verify no crash

## Step 5: Multiblock Formation (EStorage)
- [ ] Place EStorage Controller L4
- [ ] Build a simple 3x3x3 structure with casings
- [ ] Verify controller shows "Structure Invalid" in WAILA/tooltip
- [ ] Add ME channel block at correct position
- [ ] Add cell drives, energy cells, vents in repeating segments
- [ ] Verify controller shows "Structure Formed"

## Step 6: AE2 Connection
- [ ] Place AE2 ME cable near EStorage controller
- [ ] Verify controller shows "AE2: Connected" in GUI
- [ ] Open controller GUI by right-clicking
- [ ] Verify GUI displays tier, segments, cell drives info

## Step 7: Recipe Registration
- [ ] Open assembler machine
- [ ] Search for EStorage Casing recipe
- [ ] Verify recipe inputs are correct (steel plates + HV circuit + emitter)
- [ ] Craft one EStorage Casing

## If Crash Occurs
1. Copy full crash report
2. Look for "ecoaeextension" in the stack trace
3. Report the exact error and line number
