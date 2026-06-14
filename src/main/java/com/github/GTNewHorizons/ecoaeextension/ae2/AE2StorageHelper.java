package com.github.GTNewHorizons.ecoaeextension.ae2;

import net.minecraft.tileentity.TileEntity;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.ICellRegistry;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.IGridProxyable;

/**
 * Utility class for AE2 network integration. Provides methods for:
 * - Finding AE2 grids from tile entities
 * - Accessing storage grids
 * - Registering cell providers
 * - Managing grid node lifecycle
 */
public class AE2StorageHelper {

    /**
     * Get the AE2 grid from a tile entity at the specified position.
     *
     * @param tileEntity The tile entity to get the grid from
     * @return The grid, or null if not connected
     */
    public static IGrid getGrid(TileEntity tileEntity) {
        if (tileEntity == null) return null;

        try {
            // In GTNH AE2 (rv1), IGridNode is obtained from IGridProxyable.getProxy().getNode()
            if (tileEntity instanceof IGridProxyable) {
                IGridProxyable proxyable = (IGridProxyable) tileEntity;
                IGridNode node = proxyable.getProxy().getNode();
                if (node != null) {
                    return node.getGrid();
                }
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to get AE2 grid from tile entity", e);
        }

        return null;
    }

    /**
     * Get the storage grid from a tile entity.
     *
     * @param tileEntity The tile entity to get the storage grid from
     * @return The storage grid, or null if not connected
     */
    public static IStorageGrid getStorageGrid(TileEntity tileEntity) {
        IGrid grid = getGrid(tileEntity);
        if (grid == null) return null;

        try {
            return grid.getCache(IStorageGrid.class);
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to get AE2 storage grid", e);
            return null;
        }
    }

    /**
     * Register a cell provider with the AE2 storage grid.
     *
     * @param grid The storage grid to register with
     * @param provider The cell provider to register
     */
    public static void registerCellProvider(IStorageGrid grid, ICellProvider provider) {
        if (grid == null || provider == null) return;

        try {
            grid.registerCellProvider(provider);
        } catch (Exception e) {
            ECOAEExtension.LOG.error("Failed to register cell provider", e);
        }
    }

    /**
     * Unregister a cell provider from the AE2 storage grid.
     *
     * @param grid The storage grid to unregister from
     * @param provider The cell provider to unregister
     */
    public static void unregisterCellProvider(IStorageGrid grid, ICellProvider provider) {
        if (grid == null || provider == null) return;

        try {
            grid.unregisterCellProvider(provider);
        } catch (Exception e) {
            ECOAEExtension.LOG.error("Failed to unregister cell provider", e);
        }
    }

    /**
     * Get the cell registry for managing custom storage cells.
     *
     * @return The cell registry
     */
    public static ICellRegistry getCellRegistry() {
        return AEApi.instance()
            .registries()
            .cell();
    }

    /**
     * Create a DimensionalCoord from a tile entity.
     *
     * @param tileEntity The tile entity
     * @return The dimensional coordinate
     */
    public static DimensionalCoord getDimensionalCoord(TileEntity tileEntity) {
        return new DimensionalCoord(tileEntity);
    }
}
