package com.github.GTNewHorizons.ecoaeextension.gui.mui2;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.github.GTNewHorizons.ecoaeextension.multiblock.estorage.EStorageController;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class EStorageGui extends MTEMultiBlockBaseGui<EStorageController> {

    public EStorageGui(EStorageController multiblock) {
        super(multiblock);
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager syncManager) {
        Flow column = super.createMainColumn(panel, syncManager);

        // Add custom status widgets before the inventory row
        column.child(createStatusPanel(syncManager));

        return column;
    }

    private Flow createStatusPanel(PanelSyncManager syncManager) {
        StringSyncValue tierSyncer = new StringSyncValue(() -> multiblock.getCurrentTier().name);
        StringSyncValue segmentsSyncer = new StringSyncValue(() -> String.valueOf(multiblock.getSegmentCount()));
        StringSyncValue cellDrivesSyncer = new StringSyncValue(
            () -> multiblock.getInstalledCellDrives() + " / " + multiblock.getEnergyCellCapacity());
        StringSyncValue energyCellsSyncer = new StringSyncValue(
            () -> String.valueOf(multiblock.getInstalledEnergyCells()));
        StringSyncValue capacitySyncer = new StringSyncValue(() -> formatBytes(multiblock.getStorageCapacity()));
        StringSyncValue ae2Syncer = new StringSyncValue(
            () -> multiblock.isAE2Connected() ? StatCollector.translateToLocal("ecoaeext.gui.estorage.ae2_connected")
                : StatCollector.translateToLocal("ecoaeext.gui.estorage.ae2_disconnected"));

        syncManager.syncValue("es_tier", tierSyncer);
        syncManager.syncValue("es_segments", segmentsSyncer);
        syncManager.syncValue("es_drives", cellDrivesSyncer);
        syncManager.syncValue("es_energy", energyCellsSyncer);
        syncManager.syncValue("es_capacity", capacitySyncer);
        syncManager.syncValue("es_ae2", ae2Syncer);

        return Flow.column()
            .widthRel(1f)
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted("ecoaeext.gui.tier", tierSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ecoaeext.gui.estorage.segments", segmentsSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ecoaeext.gui.estorage.drives", cellDrivesSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "ecoaeext.gui.estorage.energy_cells",
                        energyCellsSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ecoaeext.gui.estorage.capacity", capacitySyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(() -> ae2Syncer.getStringValue())
                    .asWidget()
                    .fullWidth());
    }

    @Override
    protected int getTextBoxToInventoryGap() {
        return 40;
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return (bytes / 1_000_000_000) + "B";
        if (bytes >= 1_000_000) return (bytes / 1_000_000) + "M";
        if (bytes >= 1_000) return (bytes / 1_000) + "K";
        return bytes + " bytes";
    }
}
