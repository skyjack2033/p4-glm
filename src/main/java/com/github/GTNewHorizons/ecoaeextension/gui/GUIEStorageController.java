package com.github.GTNewHorizons.ecoaeextension.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.estorage.EStorageController;

import org.lwjgl.opengl.GL11;

public class GUIEStorageController extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        ECOAEExtension.MODID + ":textures/gui/estorage.png");

    private final EStorageController controller;

    public GUIEStorageController(InventoryPlayer playerInventory, EStorageController controller) {
        super(new ContainerECOAE(playerInventory));
        this.controller = controller;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("ecoaeext.gui.estorage.title");
        fontRendererObj.drawString(title, (xSize / 2 - fontRendererObj.getStringWidth(title) / 2), 6, 4210752);

        // Draw status info
        String tier = "Tier: " + controller.getCurrentTier().name;
        fontRendererObj.drawString(tier, 8, 18, 4210752);

        String segments = "Segments: " + controller.getSegmentCount();
        fontRendererObj.drawString(segments, 8, 30, 4210752);

        String cellDrives = "Cell Drives: " + controller.getInstalledCellDrives();
        fontRendererObj.drawString(cellDrives, 8, 42, 4210752);

        String ae2Status = "AE2: " + (controller.isAE2Connected() ? "Connected" : "Disconnected");
        fontRendererObj.drawString(ae2Status, 8, 54, controller.isAE2Connected() ? 0x00AA00 : 0xAA0000);

        // Draw player inventory label
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
