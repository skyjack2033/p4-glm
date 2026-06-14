package com.github.GTNewHorizons.ecoaeextension.util;

import net.minecraft.util.ResourceLocation;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

/**
 * Texture resource locations for ECOAE Extension blocks and GUIs.
 */
public class Textures {

    // Base path
    private static final String TEXTURE_PREFIX = ECOAEExtension.MODID + ":";

    // Block textures - EStorage
    public static final ResourceLocation ESTORAGE_CASING = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/estorage/casing");
    public static final ResourceLocation ESTORAGE_CONTROLLER = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/estorage/controller");
    public static final ResourceLocation ESTORAGE_CELL_DRIVE = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/estorage/cell_drive");
    public static final ResourceLocation ESTORAGE_ENERGY_CELL = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/estorage/energy_cell");
    public static final ResourceLocation ESTORAGE_ME_CHANNEL = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/estorage/me_channel");
    public static final ResourceLocation ESTORAGE_VENT = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/estorage/vent");

    // Block textures - ECalculator
    public static final ResourceLocation ECALCULATOR_CASING = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/ecalculator/casing");
    public static final ResourceLocation ECALCULATOR_CONTROLLER = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/ecalculator/controller");
    public static final ResourceLocation ECALCULATOR_THREAD_CORE = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/ecalculator/thread_core");
    public static final ResourceLocation ECALCULATOR_HYPER_THREAD = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/ecalculator/hyper_thread");
    public static final ResourceLocation ECALCULATOR_ME_CHANNEL = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/ecalculator/me_channel");

    // Block textures - EFabricator
    public static final ResourceLocation EFABRICATOR_CASING = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/efabricator/casing");
    public static final ResourceLocation EFABRICATOR_CONTROLLER = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/efabricator/controller");
    public static final ResourceLocation EFABRICATOR_WORKER = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/efabricator/worker");
    public static final ResourceLocation EFABRICATOR_PATTERN_BUS = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/efabricator/pattern_bus");
    public static final ResourceLocation EFABRICATOR_ME_CHANNEL = new ResourceLocation(
        TEXTURE_PREFIX + "blocks/efabricator/me_channel");

    // GUI textures
    public static final ResourceLocation GUI_ESTORAGE = new ResourceLocation(
        TEXTURE_PREFIX + "textures/gui/estorage.png");
    public static final ResourceLocation GUI_ECALCULATOR = new ResourceLocation(
        TEXTURE_PREFIX + "textures/gui/ecalculator.png");
    public static final ResourceLocation GUI_EFABRICATOR = new ResourceLocation(
        TEXTURE_PREFIX + "textures/gui/efabricator.png");
}
