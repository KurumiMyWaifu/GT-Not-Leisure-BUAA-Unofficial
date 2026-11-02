package com.science.gtnl.common.block.blocks;

import java.util.EnumSet;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.science.gtnl.common.block.blocks.tile.TileEntitySuperInterface;
import com.science.gtnl.common.gui.GTNLGuiHandler;
import com.science.gtnl.utils.enums.GuiType;

import appeng.api.util.IOrientable;
import appeng.core.features.AEFeature;

public class BlockSuperInterface extends BlockSuperInterfaceBase {

    public BlockSuperInterface() {
        super();
        setHardness(50.0F);
        setResistance(2000.0F);
        setHarvestLevel("pickaxe", 3);
        this.setTileEntity(TileEntitySuperInterface.class);
        this.setFeature(EnumSet.of(AEFeature.Core));
    }

    @Override
    public String getUnlocalizedName() {
        return "gtnl.block.super_interface";
    }

    @Override
    public boolean onActivated(final World w, final int x, final int y, final int z, final EntityPlayer p,
        final int side, final float hitX, final float hitY, final float hitZ) {
        if (p.isSneaking()) {
            return false;
        }

        final TileEntitySuperInterface tg = this.getTileEntity(w, x, y, z);
        if (tg == null) return false;

        GTNLGuiHandler.openGui(p, GuiType.SuperInterfaceGUI, null, w, x, y, z);
        return true;
    }

    @Override
    public void onNeighborBlockChange(World worldIn, int x, int y, int z, Block neighbor) {
        TileEntitySuperInterface tile = this.getTileEntity(worldIn, x, y, z);
        if (tile != null) {
            tile.getInterfaceDuality()
                .updateRedstoneState();
        }
    }

    @Override
    public boolean hasCustomRotation() {
        return true;
    }

    @Override
    public void customRotateBlock(final IOrientable rotatable, final ForgeDirection axis) {
        if (rotatable instanceof TileEntitySuperInterface superInterface) {
            superInterface.setSide(axis);
        }
    }
}
