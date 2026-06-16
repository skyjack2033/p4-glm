package com.github.GTNewHorizons.ecoaeextension.gui.mui2;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class EFabricatorGui extends MTEMultiBlockBaseGui<EFabricatorController> {

    public EFabricatorGui(EFabricatorController multiblock) {
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
        StringSyncValue patternsSyncer = new StringSyncValue(() -> String.valueOf(multiblock.getTotalPatternSlots()));
        StringSyncValue workersSyncer = new StringSyncValue(() -> String.valueOf(multiblock.getInstalledWorkers()));
        StringSyncValue parallelSyncer = new StringSyncValue(() -> String.valueOf(multiblock.getParallelCount()));
        StringSyncValue overclockSyncer = new StringSyncValue(() -> multiblock.getOverclockModeName());
        StringSyncValue coolingSyncer = new StringSyncValue(
            () -> multiblock.isCoolingEnabled() ? StatCollector.translateToLocal("ecoaeext.gui.efabricator.cooling_on")
                : StatCollector.translateToLocal("ecoaeext.gui.efabricator.cooling_off"));

        syncManager.syncValue("ef_tier", tierSyncer);
        syncManager.syncValue("ef_patterns", patternsSyncer);
        syncManager.syncValue("ef_workers", workersSyncer);
        syncManager.syncValue("ef_parallel", parallelSyncer);
        syncManager.syncValue("ef_overclock", overclockSyncer);
        syncManager.syncValue("ef_cooling", coolingSyncer);

        return Flow.column()
            .widthRel(1f)
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted("ecoaeext.gui.tier", tierSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "ecoaeext.gui.efabricator.patterns_count",
                        patternsSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "ecoaeext.gui.efabricator.workers_count",
                        workersSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ecoaeext.gui.parallel", parallelSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ecoaeext.gui.efabricator.mode", overclockSyncer.getStringValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(() -> coolingSyncer.getStringValue())
                    .asWidget()
                    .fullWidth());
    }

    @Override
    protected int getTextBoxToInventoryGap() {
        return 40;
    }
}
