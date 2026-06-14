package com.github.GTNewHorizons.ecoaeextension.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.estorage.EStorageController;

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

        // Cell drives with max allowed display
        String cellDrives = "Drives: " + controller.getInstalledCellDrives() + "/" + controller.getMaxCellDrives();
        fontRendererObj.drawString(cellDrives, 8, 42, 4210752);

        // Total storage capacity
        String capacity = "Capacity: " + formatStorageBytes(controller.getStorageCapacity());
        fontRendererObj.drawString(capacity, 8, 54, 0x55FF55);

        // Energy cells
        String energyCells = "Energy Cells: " + controller.getInstalledEnergyCells();
        fontRendererObj.drawString(energyCells, 8, 66, 4210752);

        // AE2 connection status with colored indicator bar
        boolean ae2Connected = controller.isAE2Connected();
        String ae2Status = "AE2: " + (ae2Connected ? "Connected" : "Disconnected");
        int ae2Color = ae2Connected ? 0x00AA00 : 0xAA0000;
        fontRendererObj.drawString(ae2Status, 8, 78, ae2Color);

        // Draw AE2 connection indicator bar
        int barX = 8;
        int barY = 88;
        int barWidth = xSize - 16;
        int barHeight = 3;
        // Background
        drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        // Fill based on connection status
        int fillColor = ae2Connected ? 0xFF00AA00 : 0xFFAA0000;
        drawRect(barX, barY, barX + barWidth, barY + barHeight, fillColor);
        // Pulse effect when connected
        if (ae2Connected) {
            drawRect(barX + 1, barY + 1, barX + barWidth - 1, barY + barHeight - 1, 0xFF00FF00);
        }

        // Draw player inventory label
        String playerInv = I18n.format("container.inventory");
        fontRendererObj.drawString(playerInv, 8, ySize - 96 + 2, 4210752);
    }

    /**
     * Format storage bytes to human-readable string.
     */
    private String formatStorageBytes(long bytes) {
        if (bytes >= 1_000_000_000L) return (bytes / 1_000_000_000L) + " GB";
        if (bytes >= 1_000_000L) return (bytes / 1_000_000L) + " MB";
        if (bytes >= 1_000L) return (bytes / 1_000L) + " KB";
        return bytes + " B";
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(BACKGROUND);
        int k = (width - xSize) / 2;
        int l = (height - ySize) / 2;
        drawTexturedModalRect(k, l, 0, 0, xSize, ySize);
    }
}
