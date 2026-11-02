package com.science.gtnl.common.block.blocks;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import appeng.api.util.AEColor;
import appeng.block.misc.BlockInterface;
import appeng.client.render.blocks.RenderBlockInterface;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class BlockSuperInterfaceBase extends BlockInterface {

    @SideOnly(Side.CLIENT)
    public IIcon iconAlternate;
    @SideOnly(Side.CLIENT)
    public IIcon iconAlternateArrow;
    @SideOnly(Side.CLIENT)
    public IIcon[][] coloredAlternateTextures;

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        String texture = getTextureName();
        iconAlternate = register.registerIcon(texture + "_alternate");
        iconAlternateArrow = register.registerIcon(texture + "_alternate_arrow");
        coloredAlternateTextures = new IIcon[2][AEColor.VALUES.length];
        String coloredTexture = texture.replace(":", ":interface/");
        for (AEColor color : AEColor.VALID_COLORS) {
            coloredAlternateTextures[0][color.ordinal()] = register
                .registerIcon(coloredTexture + "_alternate_" + color.name());
            coloredAlternateTextures[1][color.ordinal()] = register
                .registerIcon(coloredTexture + "_alternate_arrow_" + color.name());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getRenderTexture(int id, AEColor color) {
        if (id < 0 || id > 1) {
            return super.getRenderTexture(id, color);
        }
        if (color != AEColor.Transparent) {
            return coloredAlternateTextures[id][color.ordinal()];
        }
        return id == 0 ? iconAlternate : iconAlternateArrow;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RenderBlockInterface getRenderer() {
        return new RenderBlockInterface();
    }
}
