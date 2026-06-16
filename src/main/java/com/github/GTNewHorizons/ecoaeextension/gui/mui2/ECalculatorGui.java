package com.github.GTNewHorizons.ecoaeextension.gui.mui2;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator.ECalculatorController;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class ECalculatorGui extends MTEMultiBlockBaseGui<ECalculatorController> {

    public ECalculatorGui(ECalculatorController multiblock) {
        super(multiblock);
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager syncManager) {
        Flow column = super.createMainColumn(panel, syncManager);
        column.child(createStatusPanel(syncManager));
        return column;
    }

    private Flow createStatusPanel(PanelSyncManager syncManager) {
        StringSyncValue tierSyncer = new StringSyncValue(() -> multiblock.getCurrentTier().name);
        StringSyncValue threadsSyncer = new StringSyncValue(() -> {
            int total = multiblock.getTotalThreads();
            int hyper = multiblock.getInstalledHyperThreads();
            int regular = total - hyper;
            if (hyper > 0) {
                return StatCollector
                    .translateToLocalFormatted("ecoaeext.gui.ecalculator.threads_with_hyper", regular, hyper);
            }
            return StatCollector.translateToLocalFormatted("ecoaeext.gui.ecalculator.threads_count", regular);
        });
        StringSyncValue storageSyncer = new StringSyncValue(() -> formatBytes(multiblock.getTotalStorageBytes()));
        StringSyncValue cellDrivesSyncer = new StringSyncValue(
            () -> String.valueOf(multiblock.getInstalledCellDrives()));
        StringSyncValue parallelSyncer = new StringSyncValue(() -> String.valueOf(multiblock.getParallelCount()));
        StringSyncValue vcpuSyncer = new StringSyncValue(
            () -> multiblock.isVCPUActive() ? StatCollector.translateToLocal("ecoaeext.gui.ecalculator.vcpu_active")
                : StatCollector.translateToLocal("ecoaeext.gui.ecalculator.vcpu_inactive"));

        syncManager.syncValue("ec_tier", tierSyncer);
        syncManager.syncValue("ec_threads", threadsSyncer);
        syncManager.syncValue("ec_storage", storageSyncer);
        syncManager.syncValue("ec_drives", cellDrivesSyncer);
        syncManager.syncValue("ec_parallel", parallelSyncer);
        syncManager.syncValue("ec_vcpu", vcpuSyncer);

        return Flow.column()
            .widthRel(1f)
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted("ecoaeext.gui.tier", tierSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(() -> threadsSyncer.getStringValue())
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ecoaeext.gui.ecalculator.storage", storageSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "ecoaeext.gui.ecalculator.cell_drives",
                        cellDrivesSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ecoaeext.gui.parallel", parallelSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(() -> vcpuSyncer.getStringValue())
                    .asWidget()
                    .fullWidth());
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return (bytes / 1_000_000_000) + "B";
        if (bytes >= 1_000_000) return (bytes / 1_000_000) + "M";
        if (bytes >= 1_000) return (bytes / 1_000) + "K";
        return bytes + " bytes";
    }

    @Override
    protected int getTextBoxToInventoryGap() {
        return 40;
    }
}
