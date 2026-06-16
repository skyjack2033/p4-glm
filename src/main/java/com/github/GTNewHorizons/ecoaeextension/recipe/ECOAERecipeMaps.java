package com.github.GTNewHorizons.ecoaeextension.recipe;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

/**
 * Recipe map definitions for ECOAE Extension.
 *
 * Note: The three main multiblocks (EStorage, ECalculator, EFabricator) don't use traditional
 * recipe maps - they interact directly with AE2. These recipe maps are for any auxiliary
 * processing that might be added in the future.
 */
public class ECOAERecipeMaps {

    /**
     * Recipe map for ECOAE-specific material processing.
     * This could be used for:
     * - Crafting storage cells
     * - Processing coolant fluids
     * - Manufacturing circuit components
     *
     * Currently empty - add recipes as needed.
     */
    public static final RecipeMap<RecipeMapBackend> ECOAEProcessingRecipes = RecipeMapBuilder
        .of("ecoaeext.recipe.ECOAEProcessingRecipes")
        .maxIO(4, 4, 2, 2)
        .progressBar(gregtech.api.gui.modularui.GTUITextures.PROGRESSBAR_ARROW)
        .build();
}
