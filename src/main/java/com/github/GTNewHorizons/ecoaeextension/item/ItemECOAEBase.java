package com.github.GTNewHorizons.ecoaeextension.item;

import net.minecraft.item.Item;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

public class ItemECOAEBase extends Item {

    public ItemECOAEBase(String unlocalizedName) {
        super();
        setUnlocalizedName(ECOAEExtension.MODID + "." + unlocalizedName);
        setTextureName(ECOAEExtension.MODID + ":" + unlocalizedName);
        setCreativeTab(gregtech.api.util.GTModHandler.getGTCreativeTab());
    }
}
