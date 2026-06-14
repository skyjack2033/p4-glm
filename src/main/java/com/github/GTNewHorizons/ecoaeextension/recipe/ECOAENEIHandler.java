package com.github.GTNewHorizons.ecoaeextension.recipe;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

/**
 * NEI (Not Enough Items) integration for ECOAE Extension.
 *
 * In GTNH, NEI recipe handlers for custom RecipeMaps are registered automatically by GregTech's
 * NEI plugin (GTNEIDefaultHandler + RecipeMapFrontend) when the RecipeMap is built with
 * {@code .neiHandlerInfo()}. The builder invocation is delayed until FMLLoadCompleteEvent, so
 * item stacks that don't yet exist at init time can be safely referenced.
 *
 * This class ensures the ECOAE recipe maps are loaded and their NEI handlers are registered.
 * The assembler recipes registered via {@code RecipeMaps.assemblerRecipes} automatically show
 * up in NEI's assembler recipe handler.
 *
 * For the custom ECOAEProcessingRecipes map, the NEI handler is configured in
 * {@link ECOAERecipeMaps} via {@code .neiHandlerInfo()}, which sets:
 * - A custom display stack (EStorage Controller)
 * - The recipe category appears under "ECOAE Processing" in NEI
 * - All recipes in the map are browsable with click-to-recipe/usage support
 */
public class ECOAENEIHandler {

    /**
     * Register NEI handlers for ECOAE Extension recipes.
     *
     * This should be called during postInit (after recipes are registered) and only when
     * NEI is present. The actual NEI handler registration is done automatically by GregTech's
     * NEI plugin when it discovers RecipeMaps with neiHandlerInfo configured.
     *
     * This method ensures the ECOAE recipe maps are loaded, triggering the deferred NEI
     * handler setup that was configured in ECOAERecipeMaps.
     */
    public static void register() {
        ECOAEExtension.LOG.info("Registering ECOAE NEI handlers...");

        // Access the recipe map to ensure it is loaded. GregTech's NEI plugin will
        // automatically register a GTNEIDefaultHandler for this recipe map since it has
        // .neiHandlerInfo() configured. The handler supports:
        // - Browsing all recipes in the map
        // - Clicking on items to see their recipes (loadCraftingRecipes)
        // - Clicking on items to see their usage (loadUsageRecipes)
        // - Custom display stack and category name
        if (ECOAERecipeMaps.ECOAEProcessingRecipes == null) {
            ECOAEExtension.LOG.error("ECOAEProcessingRecipes recipe map is null!");
        }

        ECOAEExtension.LOG.info("ECOAE NEI handlers registered.");
    }
}
