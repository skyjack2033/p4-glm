package com.github.GTNewHorizons.ecoaeextension.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;

import org.lwjgl.opengl.GL11;

public class GUIEFabricatorController extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        ECOAEExtension.MODID + ":textures/gui/efabricator.png");

    private final EFabricatorController controller;

    public GUIEFabricatorController(InventoryPlayer playerInventory, EFabricatorController controller) {
        super(new ContainerECOAE(playerInventory));
        this.controller = controller;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("ecoaeext.gui.efabricator.title");
        fontRendererObj.drawString(title, (xSize / 2 - fontRendererObj.getStringWidth(title) / 2), 6, 4210752);

        String tier = "Tier: " + controller.getCurrentTier().name;
        fontRendererObj.drawString(tier, 8, 18, 4210752);

        String patterns = "Patterns: " + controller.getTotalPatternSlots();
        fontRendererObj.drawString(patterns, 8, 30, 4210752);

        String workers = "Workers: " + controller.getInstalledWorkers();
        fontRendererObj.drawString(workers, 8, 42, 4210752);

        String parallel = "Parallel: " + controller.getParallelCount() + "x";
        fontRendererObj.drawString(parallel, 8, 54, 4210752);

        String overclock = "Overclock: " + controller.getOverclockModeName();
        fontRendererObj.drawString(overclock, 8, 66, 4210752);

        String cooling = "Cooling: " + (controller.isCoolingEnabled() ? "Active" : "Inactive");
        fontRendererObj.drawString(cooling, 8, 78, controller.isCoolingEnabled() ? 0x00AA00 : 0xAA0000);

        String playerInv = I18n.format("container.inventory");
        fontRendererObj.drawString(playerInv, 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        int k = (width - xSize) / 2;
        int l = (height - ySize) / 2;
        drawTexturedModalRect(k, l, 0, 0, xSize, ySize);
    }
}
