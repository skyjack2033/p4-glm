package com.github.GTNewHorizons.ecoaeextension.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator.ECalculatorController;

public class GUIECalculatorController extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        ECOAEExtension.MODID + ":textures/gui/ecalculator.png");

    private final ECalculatorController controller;

    public GUIECalculatorController(InventoryPlayer playerInventory, ECalculatorController controller) {
        super(new ContainerECOAE(playerInventory));
        this.controller = controller;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("ecoaeext.gui.ecalculator.title");
        fontRendererObj.drawString(title, (xSize / 2 - fontRendererObj.getStringWidth(title) / 2), 6, 4210752);

        String tier = "Tier: " + controller.getCurrentTier().name;
        fontRendererObj.drawString(tier, 8, 18, 4210752);

        // Thread count breakdown (regular + hyper)
        int totalThreads = controller.getTotalThreads();
        int hyperThreads = controller.getInstalledHyperThreads();
        int regularThreads = totalThreads - hyperThreads;
        String threads = "Threads: " + regularThreads;
        if (hyperThreads > 0) {
            threads += " + " + hyperThreads + " HT";
        }
        fontRendererObj.drawString(threads, 8, 30, 4210752);

        // Total storage bytes
        String storage = "Storage: " + formatBytes(controller.getTotalStorageBytes());
        fontRendererObj.drawString(storage, 8, 42, 0x55FF55);

        // Cell drives and parallel processors
        String cellDrives = "Cell Drives: " + controller.getInstalledCellDrives();
        fontRendererObj.drawString(cellDrives, 8, 54, 4210752);

        String parallel = "Parallel: " + controller.getParallelCount() + "x";
        fontRendererObj.drawString(parallel, 8, 66, 4210752);

        // vCPU status with color indicator bar
        boolean vcpuActive = controller.isVCPUActive();
        String vcpu = "vCPU: " + (vcpuActive ? "ACTIVE" : "INACTIVE");
        int vcpuColor = vcpuActive ? 0x00FF00 : 0xFF5555;
        fontRendererObj.drawString(vcpu, 8, 78, vcpuColor);

        // Draw vCPU status indicator bar
        int barX = 8;
        int barY = 88;
        int barWidth = xSize - 16;
        int barHeight = 3;
        // Background
        drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        // Status fill
        int fillColor = vcpuActive ? 0xFF00FF00 : 0xFFFF5555;
        drawRect(barX, barY, barX + barWidth, barY + barHeight, fillColor);

        String playerInv = I18n.format("container.inventory");
        fontRendererObj.drawString(playerInv, 8, ySize - 96 + 2, 4210752);
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

    private String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return (bytes / 1_000_000_000) + "B";
        if (bytes >= 1_000_000) return (bytes / 1_000_000) + "M";
        if (bytes >= 1_000) return (bytes / 1_000) + "K";
        return bytes + " bytes";
    }
}
