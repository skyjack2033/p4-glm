package com.github.GTNewHorizons.ecoaeextension.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Base meta block for ECOAE Extension. Uses damage values to distinguish block variants.
 * Each subsystem (EStorage, ECalculator, EFabricator) has its own instance.
 */
public class BlockECOAEMeta extends Block {

    private final String[] subNames;
    private final String[] iconPaths;
    private IIcon[] icons;

    public BlockECOAEMeta(String blockName, String[] subNames, String[] iconPaths) {
        super(Material.iron);
        this.subNames = subNames;
        this.iconPaths = iconPaths;
        setBlockName(ECOAEExtension.MODID + "." + blockName);
        setCreativeTab(gregtech.api.util.GTModHandler.getGTCreativeTab());
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @SuppressWarnings("unchecked")
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < subNames.length; i++) {
            list.add(new ItemStack(item, 1, i));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[iconPaths.length];
        for (int i = 0; i < iconPaths.length; i++) {
            icons[i] = register.registerIcon(ECOAEExtension.MODID + ":" + iconPaths[i]);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (meta >= 0 && meta < icons.length) {
            return icons[meta];
        }
        return icons[0];
    }

    public String getSubName(int meta) {
        if (meta >= 0 && meta < subNames.length) {
            return subNames[meta];
        }
        return subNames[0];
    }

    public int getSubBlockCount() {
        return subNames.length;
    }
}
