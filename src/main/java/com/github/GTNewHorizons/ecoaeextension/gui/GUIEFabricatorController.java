package com.github.GTNewHorizons.ecoaeextension.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;

public class GUIEFabricatorController extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        ECOAEExtension.MODID + ":textures/gui/efabricator.png");

    private final EFabricatorController controller;

    private static final int BUTTON_OVERCLOCK = 0;
    private static final int BUTTON_COOLING = 1;

    public GUIEFabricatorController(InventoryPlayer playerInventory, EFabricatorController controller) {
        super(new ContainerECOAE(playerInventory));
        this.controller = controller;
        this.xSize = 176;
        this.ySize = 166;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        buttonList.add(new GuiButton(BUTTON_OVERCLOCK, x + 10, y + 90, 60, 20, "Overclock"));
        buttonList.add(new GuiButton(BUTTON_COOLING, x + 80, y + 90, 60, 20, "Cooling"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_OVERCLOCK:
                controller.cycleOverclockMode();
                break;
            case BUTTON_COOLING:
                controller.toggleCooling();
                break;
        }
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

        // Overclock mode with color-coded indicator
        int ocMode = controller.getOverclockMode();
        int ocColor;
        switch (ocMode) {
            case 1:
                ocColor = 0xFFFF55;
                break; // Yellow - OC I
            case 2:
                ocColor = 0xFFAA00;
                break; // Orange - OC II
            case 3:
                ocColor = 0xFF5555;
                break; // Red - OC III
            default:
                ocColor = 0x55FF55;
                break; // Green - Normal
        }
        String overclock = "Mode: " + controller.getOverclockModeName();
        fontRendererObj.drawString(overclock, 8, 66, ocColor);
        // Draw overclock mode indicator bar (4 segments, filled to current mode)
        for (int i = 0; i < 4; i++) {
            int segColor = (i <= ocMode) ? ocColor : 0x333333;
            drawRect(8 + i * 12, 76, 8 + i * 12 + 10, 78, 0xFF000000 | segColor);
        }

        // Cooling state with visual indicator
        boolean coolingActive = controller.isCoolingEnabled();
        int coolingColor = coolingActive ? 0x00AA00 : 0xAA0000;
        String cooling = "Cooling: " + (coolingActive ? "ON" : "OFF");
        fontRendererObj.drawString(cooling, 8, 82, coolingColor);
        // Draw cooling indicator dot
        drawRect(
            8 + fontRendererObj.getStringWidth(cooling) + 3,
            83,
            8 + fontRendererObj.getStringWidth(cooling) + 9,
            89,
            0xFF000000 | (coolingActive ? 0x00FF00 : 0xFF0000));

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
}
